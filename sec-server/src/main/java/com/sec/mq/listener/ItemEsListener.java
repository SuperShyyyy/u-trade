package com.sec.mq.listener;

import com.sec.constant.RabbitMQConstant;
import com.sec.domain.es.ItemDocument;
import com.sec.message.ItemSyncMessage;
import com.sec.service.ItemEsService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.beans.BeanUtils;

import java.io.IOException;

/**
 * 商品同步 ES 消费者
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ItemEsListener {

    private final ItemEsService itemEsService;

    @RabbitListener(queues = RabbitMQConstant.QUEUE_ITEM_SYNC)
    public void handleItemSync(ItemSyncMessage msg,
                               Channel channel,
                               Message message) throws IOException {

        long tag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();
        Long itemId = msg.getId();

        log.info("收到商品同步消息，ItemId: {}, Operation: {}, MessageId: {}",
                itemId, msg.getOperationType(), messageId);

        try {
            // 根据操作类型执行不同逻辑
            switch (msg.getOperationType()) {
                case ADD:
                case UPDATE:
                    // 新增或更新：构建文档并保存
                    ItemDocument document = convertToDocument(msg);
                    itemEsService.save(document);
                    log.info("ES 同步成功 (ADD/UPDATE), ItemId: {}", itemId);
                    break;

                case DELETE:
                    // 删除：从 ES 删除
                    itemEsService.delete(itemId);
                    log.info("ES 同步成功 (DELETE), ItemId: {}", itemId);
                    break;

                default:
                    log.error("未知的操作类型：{}", msg.getOperationType());
                    // 未知操作类型，不重试，进入死信
                    channel.basicNack(tag, false, false);
                    return;
            }

            // 手动确认消息
            channel.basicAck(tag, false);

        } catch (IllegalArgumentException e) {
            // 参数错误等永久异常，进入死信队列
            log.error("ES 同步失败 (永久异常), ItemId: {}, Error: {}", itemId, e.getMessage());
            channel.basicNack(tag, false, false);

        } catch (Exception e) {
            // 网络异常、ES 不可用等临时异常，重新入队重试
            log.error("ES 同步失败 (临时异常), ItemId: {}, Error: {}", itemId, e.getMessage(), e);
            channel.basicNack(tag, false, true);
        }
    }

    /**
     * 消息体转 ES 文档
     */
    /**
     * 消息体转 ES 文档
     */
    private ItemDocument convertToDocument(ItemSyncMessage msg) {
        log.info("开始转换 Document, 原始消息: {}", msg);

        if (msg == null) {
            throw new IllegalArgumentException("ItemSyncMessage 为空");
        }

        if (msg.getId() == null) {
            throw new IllegalArgumentException("ItemId 为空");
        }

        ItemDocument doc = new ItemDocument();
        doc.setId(msg.getId());  // ⚠️ 手动设置 ID
        BeanUtils.copyProperties(msg, doc, "itemId");

        log.info("Document 转换完成，doc.getId() = {}", doc.getId());

        if (doc.getId() == null) {
            throw new IllegalArgumentException("ItemDocument ID 为空");
        }

        return doc;
    }
}