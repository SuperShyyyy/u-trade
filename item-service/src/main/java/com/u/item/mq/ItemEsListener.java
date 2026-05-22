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
import org.springframework.stereotype.Component;
import org.springframework.beans.BeanUtils;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.UpdateOperation;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.IOException;
import java.util.List;
/**
 * 商品同步 ES 消费�?
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ItemEsListener {

    private final ItemEsService itemEsService;
    // 修改字段注入
    private final ElasticsearchClient elasticsearchClient; // 替代 RestHighLevelClient
    @RabbitListener(queues = RabbitMQConstant.QUEUE_ITEM_SYNC)
    public void handleItemSync(ItemSyncMessage msg,
                               Channel channel,
                               Message message) throws IOException {

        long tag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();

        log.info("收到商品同步消息，Operation: {}, MessageId: {}",
                msg.getOperationType(), messageId);

        try {
            // 根据操作类型执行不同逻辑
            switch (msg.getOperationType()) {
                case ADD:
                case UPDATE:
                    // 新增或更新：构建文档并保�?
                    ItemDocument document = convertToDocument(msg);
                    itemEsService.save(document);
                    log.info("ES 同步成功 (ADD/UPDATE), ItemId: {}", msg.getId());
                    break;

                case DELETE:
                    // 删除：从 ES 删除
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
                    // 未知操作类型，不重试，进入死�?
                    channel.basicNack(tag, false, false);
                    return;
            }

            // 手动确认消息
            channel.basicAck(tag, false);

        } catch (IllegalArgumentException e) {
            // 参数错误等永久异常，进入死信队列
            log.error("ES 同步失败 (永久异常), ItemId: {}, Error: {}", msg.getId(), e.getMessage());
            channel.basicNack(tag, false, false);

        } catch (Exception e) {
            // 统一捕获异常，决定是否重�?
            log.error("ES 同步失败, MessageId: {}, Error: {}", messageId, e.getMessage(), e);
            // 如果是参数错误等不可恢复异常，不重试
            // 如果�?ES 宕机等临时异常，重试
            boolean requeue = !(e instanceof IllegalArgumentException);
            channel.basicNack(tag, false, requeue);
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
    /**
     * 消息体转 ES 文档
     */
    /**
     * 消息体转 ES 文档
     */
    private ItemDocument convertToDocument(ItemSyncMessage msg) {
        log.info("开始转�?Document, 原始消息: {}", msg);

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