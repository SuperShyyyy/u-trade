package com.sec.mq.listener;

import com.sec.constant.RabbitMQConstant;
import com.sec.message.WalletSettlementMessage;
import com.sec.service.IUserWalletService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSettlementListener {

    private final IUserWalletService userWalletService;
    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitMQConstant.QUEUE_ORDER_SETTLE_EXEC )
    public void handle(WalletSettlementMessage msg, Channel channel, Message message) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();
        String orderNo = msg.getOrderNo();
        String idempotentKey = "wallet:settle:" + orderNo;

        try {
            // 幂等性处理
            Boolean isFirst = stringRedisTemplate.opsForValue()
                    .setIfAbsent(idempotentKey, "1", 7, TimeUnit.DAYS);

            if (Boolean.TRUE.equals(isFirst)) {
                // 结算逻辑
                userWalletService.transferFrozenToSeller(
                        msg.getBuyerId(),
                        msg.getSellerId(),
                        msg.getAmount(),
                        msg.getOrderNo()
                );
                log.info("[资金结算] 消费成功, orderNo={}, messageId={}", orderNo, messageId);
            } else {
                log.warn("[资金结算] 订单 {} 已结算过，忽略重复消息, messageId={}", orderNo, messageId);
            }

            // 手动确认
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("[资金结算] 消费失败, orderNo={}, messageId={}, 原因={}",
                    orderNo, messageId, e.getMessage(), e);

            // 删除幂等键，允许重试
            stringRedisTemplate.delete(idempotentKey);

            // 判断是否应该重新入队
            // 如果是业务异常，可以记录后不再重试
            // 如果是系统异常，重新入队
            boolean isSystemException = isSystemException(e);

            if (isSystemException) {
                // 重新入队
                channel.basicNack(tag, false, true);
            } else {
                // 拒绝消息，不重新入队（可配合死信队列）
                channel.basicNack(tag, false, false);
                log.error("[资金结算] 业务异常，消息进入死信队列, orderNo={}", orderNo);
            }
        }
    }

    /**
     * 判断是否为系统异常（可重试）
     */
    private boolean isSystemException(Exception e) {
        // 网络异常、数据库连接异常等可重试
        String msg = e.getMessage();
        return msg != null && (
                msg.contains("Connection") ||
                        msg.contains("Timeout") ||
                        msg.contains("Network")
        );
    }
}