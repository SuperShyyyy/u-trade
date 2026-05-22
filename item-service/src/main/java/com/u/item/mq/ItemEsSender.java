package com.u.item.mq;

import com.u.common.constant.RabbitMQConstant;
import com.u.item.domain.po.Item;
import com.u.common.message.ItemSyncMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 商品同步 ES 消息发送器
 * 封装 MQ 发送逻辑，使 Service 层更简�?
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ItemEsSender {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送商品新增消�?
     */
    public void sendAddMessage(Item item) {
        send(item, ItemSyncMessage.OperationType.ADD);
    }

    /**
     * 发送商品更新消�?
     */
    public void sendUpdateMessage(Item item) {
        send(item, ItemSyncMessage.OperationType.UPDATE);
    }

    /**
     * 发送商品删除消�?
     */
    public void sendDeleteMessage(Item item) {
        send(item, ItemSyncMessage.OperationType.DELETE);
    }

    /**
     * 统一发送方�?
     */
    private void send(Item item, ItemSyncMessage.OperationType operationType) {
        if (item == null || item.getId() == null) {
            log.warn("商品数据为空，跳过发�?MQ 消息");
            return;
        }

        ItemSyncMessage message = buildMessage(item, operationType);
        String messageId = UUID.randomUUID().toString();
        String exchange = RabbitMQConstant.EXCHANGE_ITEM_SYNC;
        String routingKey = RabbitMQConstant.ROUTING_KEY_ITEM_SYNC;

        log.info("准备发送商品同�?MQ 消息，ItemId: {}, Operation: {}, MessageId: {}",
                item.getId(), operationType, messageId);

        try {
            // 【修复】使�?CorrelationData 方式发送，配合全局 ConfirmCallback
            CorrelationData correlationData = new CorrelationData(messageId);

            rabbitTemplate.convertAndSend(exchange, routingKey, message, msg -> {
                msg.getMessageProperties().setMessageId(messageId);
                msg.getMessageProperties().setTimestamp(new Date());
                return msg;
            }, correlationData);

            log.info("商品同步 MQ 消息发送完成，ItemId: {}, Operation: {}",
                    item.getId(), operationType);

        } catch (Exception e) {
            log.error("MQ send error, ItemId: {}, Operation: {}, Error: {}",
                    item.getId(), operationType, e.getMessage(), e);
            throw new RuntimeException("MQ send failed", e);
        }
    }

    /**
     * 构建消息�?
     */
    private ItemSyncMessage buildMessage(Item item, ItemSyncMessage.OperationType operationType) {
        ItemSyncMessage message = new ItemSyncMessage();
        message.setId(item.getId());
        message.setOperationType(operationType);
        message.setSellerId(item.getSellerId());
        message.setTitle(item.getTitle());
        message.setDescription(item.getDescription());
        message.setPrice(item.getPrice());
        message.setOriginalPrice(item.getOriginalPrice());
        message.setCategoryId(item.getCategoryId());
        message.setCover(item.getCover());
        message.setImages(item.getImages());
        message.setStatus(item.getStatus());
        message.setAuditStatus(item.getAuditStatus());
        message.setViewCount(item.getViewCount());
        message.setWantCount(item.getWantCount());
        message.setIsDeleted(item.getIsDeleted());
        message.setCreateTime(item.getCreateTime());
        message.setUpdateTime(item.getUpdateTime());
        message.setOperateTime(LocalDateTime.now());
        return message;
    }

    /**
     * 发送商品批量状态更新消�?
     * @param ids 商品ID列表
     * @param status 目标状�?
     */
    public void sendBatchUpdateStatusMessage(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty() || status == null) {
            log.warn("批量更新数据为空，跳过发�?MQ 消息");
            return;
        }

        // 1. 构建 批量更新的消息体
        ItemSyncMessage message = new ItemSyncMessage();
        message.setIds(ids);
        message.setStatus(status);
        message.setOperationType(ItemSyncMessage.OperationType.BATCH_UPDATE_STATUS);
        message.setOperateTime(LocalDateTime.now());

        String messageId = UUID.randomUUID().toString();
        String exchange = RabbitMQConstant.EXCHANGE_ITEM_SYNC;
        String routingKey = RabbitMQConstant.ROUTING_KEY_ITEM_SYNC;

        log.info("准备发送批量状态更�?MQ 消息，Count: {}, NewStatus: {}, MessageId: {}",
                ids.size(), status, messageId);

        try {
            CorrelationData correlationData = new CorrelationData(messageId);

            rabbitTemplate.convertAndSend(exchange, routingKey, message, msg -> {
                msg.getMessageProperties().setMessageId(messageId);
                msg.getMessageProperties().setTimestamp(new Date());
                return msg;
            }, correlationData);

            log.info("批量状态更�?MQ 消息发送完成，Count: {}", ids.size());

        } catch (Exception e) {
            log.error("发送批量状态更�?MQ 消息异常，Count: {}, Error: {}",
                    ids.size(), e.getMessage(), e);

        }
    }
}