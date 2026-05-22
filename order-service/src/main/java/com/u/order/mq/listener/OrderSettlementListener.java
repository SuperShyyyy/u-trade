package com.u.order.mq.listener;

import com.u.api.client.wallet.WalletClient;
import com.u.common.constant.RabbitMQConstant;
import com.u.common.constant.RedisConstant;
import com.u.common.message.OrderSettlementMessage;
import com.u.common.result.Result;
import com.u.common.result.ResultCode;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSettlementListener {

    private final WalletClient walletClient;
    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitMQConstant.QUEUE_ORDER_SETTLE_EXEC )
    public void handle(OrderSettlementMessage msg, Channel channel, Message message) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();
        String orderNo = msg.getOrderNo();
        String idempotentKey = RedisConstant.WALLET_SETTLE + orderNo;
        try {
            String status = stringRedisTemplate.opsForValue().get(idempotentKey);

            if ("SUCCESS".equals(status)) {
                log.warn("[资金结算] 已处理过，直接ACK orderNo={}", orderNo);
                channel.basicAck(tag, false);
                return;
            }

            if ("PROCESSING".equals(status)) {
                log.warn("[资金结算] 正在处理，稍后重试 orderNo={}", orderNo);
                channel.basicNack(tag, false, true);
                return;
            }
            Boolean locked = stringRedisTemplate.opsForValue()
                    .setIfAbsent(idempotentKey, "PROCESSING", 5, TimeUnit.MINUTES);

            if (!Boolean.TRUE.equals(locked)) {
                channel.basicNack(tag, false, true);
                return;
            }

            Result<Void> result = walletClient.transferFrozenToSeller(
                    msg.getBuyerId(),
                    msg.getSellerId(),
                    msg.getAmount(),
                    msg.getOrderNo()
            );
            if (result == null || !ResultCode.SUCCESS.equals(result.getCode())) {
                throw new IllegalStateException(result == null ? "钱包结算失败" : result.getMessage());
            }

            // 成功 → 标记成功
            stringRedisTemplate.opsForValue().set(idempotentKey, "SUCCESS", 7, TimeUnit.DAYS);

            log.info("[资金结算] 成功 orderNo={}", orderNo);
            channel.basicAck(tag, false);

        } catch (Exception e) {
            log.error("[资金结算] 异常 orderNo={}", orderNo, e);
            if (e instanceof DuplicateKeyException) {
                log.info("[资金结算] 触发数据库唯一索引防重，视为处理成功 orderNo={}", orderNo);
                stringRedisTemplate.opsForValue().set(idempotentKey, "SUCCESS", 7, TimeUnit.DAYS);
                channel.basicAck(tag, false);
                return;
            }
            stringRedisTemplate.delete(idempotentKey);

            if (isSystemException(e)) {
                try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException ignored) {}
                channel.basicNack(tag, false, true);
            } else {
                channel.basicNack(tag, false, false);
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

    /**
     * 判断是否为业务异常（通常不需要重试，应直接丢弃或进死信队列）
     */
    private boolean isBusinessException(Exception e) {
        if (e instanceof org.springframework.dao.DuplicateKeyException) {
            return true;
        }
        if (e instanceof NullPointerException) {
            return true;
        }

        return false;
    }
}
