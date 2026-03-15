package com.sec.mq.listener;

import com.sec.constant.RabbitMQConstant;
import com.sec.message.OrderSettlementMessage;
import com.sec.service.IUserWalletService;
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

    private final IUserWalletService userWalletService;
    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitMQConstant.QUEUE_ORDER_SETTLE_EXEC )
    public void handle(OrderSettlementMessage msg, Channel channel, Message message) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();
        String orderNo = msg.getOrderNo();
        String idempotentKey = "wallet:settle:" + orderNo;

        try {
            // 1. Redis 拦截并发请求 (防抖)
            Boolean isFirst = stringRedisTemplate.opsForValue()
                    .setIfAbsent(idempotentKey, "1", 7, TimeUnit.DAYS);

            if (!Boolean.TRUE.equals(isFirst)) {
                log.warn("[资金结算] Redis 判定重复消息, orderNo={}", orderNo);
                channel.basicAck(tag, false); // 直接确认掉
                return;
            }

            // 2. 核心结算逻辑
            // 【注意】这里面一定要有数据库级别的防重！(比如往流水表插数据，order_no 是唯一索引，重复插会报 DuplicateKeyException)
            userWalletService.transferFrozenToSeller(
                    msg.getBuyerId(), msg.getSellerId(), msg.getAmount(), msg.getOrderNo()
            );

            log.info("[资金结算] 消费成功, orderNo={}", orderNo);
            channel.basicAck(tag, false);

        } catch (Exception e) {
            log.error("[资金结算] 消费失败, orderNo={}", orderNo, e);

            // 【关键防坑】不要轻易删除 Redis 的 Key！
            // 如果是因为数据库唯一索引冲突报错，说明已经打过款了，绝对不能删 Key 重试。
            if (e instanceof DuplicateKeyException || isBusinessException(e)) {
                log.error("[资金结算] 业务异常或已处理过，消息扔掉或进入死信队列");
                channel.basicNack(tag, false, false);
            } else if (isSystemException(e)) {
                // 只有确认为网络超时、数据库连接断开等纯系统级异常，且明确数据库事务回滚了，才允许重试
                stringRedisTemplate.delete(idempotentKey);
                channel.basicNack(tag, false, true);
            } else {
                // 拿捏不准的异常，宁愿人工介入，也不要无限重试导致多打钱！
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
        // 1. 如果是数据库唯一索引冲突，说明该订单已经处理过结算（最强幂等保障）
        if (e instanceof org.springframework.dao.DuplicateKeyException) {
            return true;
        }

        // 2. 如果是你自定义的业务异常 (比如：余额不足、状态不对等)
        // if (e instanceof com.sec.exception.BusinessException) { return true; }

        // 3. 如果是空指针等代码逻辑错误，重试通常也没用
        if (e instanceof NullPointerException) {
            return true;
        }

        return false;
    }
}