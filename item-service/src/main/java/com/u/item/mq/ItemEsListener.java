package com.u.item.mq;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.u.common.constant.RabbitMQConstant;
import com.u.item.domain.es.ItemDocument;
import com.u.common.message.ItemSyncMessage;
import com.u.item.service.ItemEsService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.BeanUtils;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.UpdateOperation;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 商品同步 ES 消费者
 * Redis 幂等方案：每个消息在消费前检查 Redis key，消费成功后设置 key（TTL=5min）
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ItemEsListener {

    private static final String IDEMPOTENT_KEY_PREFIX = "item:es:sync:";

    private final ItemEsService itemEsService;
    private final ElasticsearchClient elasticsearchClient;
    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitMQConstant.QUEUE_ITEM_SYNC)
    public void handleItemSync(ItemSyncMessage msg,
                               Channel channel,
                               Message message) throws IOException {

        long tag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();

        log.info("收到商品同步消息，Operation: {}, MessageId: {}",
                msg.getOperationType(), messageId);

        // =========================
        // Redis 幂等去重：同一消息 5 分钟内不重复消费
        // =========================
        String idempotentKey = IDEMPOTENT_KEY_PREFIX + messageId;
        Boolean isNew = stringRedisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", 5, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(isNew)) {
            log.info("ES 同步消息已消费，跳过，MessageId: {}", messageId);
            channel.basicAck(tag, false);
            return;
        }

        try {
            // 根据操作类型执行不同逻辑
            switch (msg.getOperationType()) {
                case ADD:
                case UPDATE:
                    ItemDocument document = convertToDocument(msg);
                    itemEsService.save(document);
                    log.info("ES 同步成功 (ADD/UPDATE), ItemId: {}", msg.getId());
                    break;

                case DELETE:
                    itemEsService.delete(msg.getId());
                    log.info("ES 同步成功 (DELETE), ItemId: {}", msg.getId());
                    break;
                case BATCH_UPDATE_STATUS:
                    handleBatchUpdateStatus(msg.getIds(), msg.getStatus());
                    log.info("ES 批量同步成功 (BATCH_UPDATE_STATUS), 数量: {}",
                            msg.getIds() != null ? msg.getIds().size() : 0);
                    break;

                default:
                    log.error("未知的操作类型：{}", msg.getOperationType());
                    // 未知操作类型，不重试，进入死信
                    stringRedisTemplate.delete(idempotentKey);
                    channel.basicNack(tag, false, false);
                    return;
            }

            // 手动确认消息
            channel.basicAck(tag, false);

        } catch (IllegalArgumentException e) {
            // 参数错误等永久异常，进入死信队列
            log.error("ES 同步失败 (永久异常), ItemId: {}, Error: {}", msg.getId(), e.getMessage());
            stringRedisTemplate.delete(idempotentKey);
            channel.basicNack(tag, false, false);

        } catch (Exception e) {
            // 统一捕获异常，决定是否重试
            log.error("ES 同步失败, MessageId: {}, Error: {}", messageId, e.getMessage(), e);
            // 清除幂等 key，允许重试时重新消费
            stringRedisTemplate.delete(idempotentKey);
            // 限制重试次数：通过检查x-death header判断是否已重试超过3次
            List<Map<String, ?>> xDeath = message.getMessageProperties().getReceivedDeliveryMode() != null
                    ? (List<Map<String, ?>>) message.getMessageProperties().getHeaders().get("x-death")
                    : null;
            int retryCount = 0;
            if (xDeath != null && !xDeath.isEmpty()) {
                Long count = (Long) xDeath.get(0).get("count");
                retryCount = count != null ? count.intValue() : 0;
            }
            // 重试超过3次则不重新入队，避免消息风暴
            boolean requeue = !(e instanceof IllegalArgumentException) && retryCount < 3;
            channel.basicNack(tag, false, requeue);
            if (!requeue) {
                log.error("ES 同步消息重试次数超限，进入死信队列, MessageId: {}, retryCount: {}", messageId, retryCount);
            }
        }
    }


    private void handleBatchUpdateStatus(List<Long> ids, Integer status) throws IOException {
        if (ids == null || ids.isEmpty() || status == null) {
            log.warn("batch update skipped: empty params");
            return;
        }

        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
        String indexName = "item";

        for (Long id : ids) {
            if (id == null) continue;

            Map<String, Object> updateDoc = new HashMap<>();
            updateDoc.put("status", status);

            bulkBuilder.operations(op -> op.update(
                    UpdateOperation.of(u -> u
                            .index(indexName)
                            .id(id.toString())
                            .action(a -> a.doc(updateDoc))
                    )
            ));
        }

        BulkResponse response = elasticsearchClient.bulk(bulkBuilder.build());

        if (response.errors()) {
            String failures = response.items().stream()
                    .filter(item -> item.error() != null)
                    .map(item -> String.format("[ID:%s] %s (Status:%d)",
                            item.id(),
                            item.error().reason(),
                            item.status()))
                    .collect(Collectors.joining("; "));

            log.error("ES 批量更新失败: {}", failures);
            throw new RuntimeException("ES 批量更新部分失败: " + failures);
        }

        log.info("ES 批量状态更新成功，更新数量: {}", ids.size());
    }

    private ItemDocument convertToDocument(ItemSyncMessage msg) {
        log.info("开始转换 Document, 原始消息: {}", msg);

        if (msg == null) {
            throw new IllegalArgumentException("ItemSyncMessage 为空");
        }

        if (msg.getId() == null) {
            throw new IllegalArgumentException("ItemId 为空");
        }

        ItemDocument doc = new ItemDocument();
        doc.setId(msg.getId());
        BeanUtils.copyProperties(msg, doc, "itemId");

        log.info("Document 转换完成，doc.getId() = {}", doc.getId());

        if (doc.getId() == null) {
            throw new IllegalArgumentException("ItemDocument ID 为空");
        }

        return doc;
    }

}
