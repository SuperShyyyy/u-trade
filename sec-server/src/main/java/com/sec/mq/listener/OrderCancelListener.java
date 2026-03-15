package com.sec.mq.listener;

import com.rabbitmq.client.Channel;
import com.sec.constant.OrderStatusConstant;
import com.sec.constant.RabbitMQConstant;
import com.sec.domain.po.Order; // 引入 Order 实体
import com.sec.mapper.OrderMapper; // 引入 Mapper 直接查库，或者注入 Service 查
import com.sec.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCancelListener {

    private final IOrderService orderService;
    private final StringRedisTemplate redisTemplate;
    private final OrderMapper orderMapper; // 注入 Mapper 用于快速查询状态

    @RabbitListener(queues = RabbitMQConstant.QUEUE_ORDER_CANCEL_EXEC)
    public void handleCancel(Message message, Channel channel,
                             @Payload Map<String, Object> payload) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();

        // 安全转换
        Long orderId = null;
        String orderNo = null;
        try {
            orderId = Long.valueOf(payload.get("orderId").toString());
            orderNo = (String) payload.get("orderNo");
        } catch (Exception e) {
            log.error("消息格式错误，无法解析 orderId 或 orderNo", e);
            channel.basicAck(tag, false); // 格式错误的消息直接丢弃，避免死循环
            return;
        }

        log.info("收到延迟取消消息: orderId={}, orderNo={}", orderId, orderNo);

        // 1. 幂等性检查 (防止同一条消息被重复消费)
        String idempotentKey = "order:cancel:" + orderId;
        Boolean isFirst = redisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", 24, TimeUnit.HOURS); // 时间稍微长一点，覆盖重试窗口

        if (Boolean.FALSE.equals(isFirst)) {
            log.info("订单 {} 已处理过取消 (幂等拦截)，忽略重复消息", orderNo);
            channel.basicAck(tag, false);
            return;
        }

        try {
            Order order = orderMapper.selectById(orderId);

            if (order == null) {
                log.warn("订单 {} 不存在，可能是被手动删除了，直接确认消息", orderNo);
                channel.basicAck(tag, false);
                return;
            }

            // 只有状态为 WAIT_PAY (待支付) 的订单才需要取消
            // 如果已经是 PAID (已支付), SHIPPED (已发货), CANCELLED (已取消) 等，说明不需要操作
            if (!order.getStatus().equals(OrderStatusConstant.WAIT_PAY)) {
                log.info("订单 {} 当前状态为 [{}]，无需自动取消 (用户已支付或已处理)，直接确认消息",
                        orderNo, order.getStatus());

                // 关键：直接 Ack，不抛异常，不重试，不进入死信
                channel.basicAck(tag, false);
                return;
            }
            // ========================================================

            // 2. 执行真正的取消逻辑
            // 此时可以确定订单确实是 WAIT_PAY 状态，可以安全调用 Service
            orderService.cancelOrderInternal(orderId);

            log.info("订单 {} 自动取消成功", orderNo);
            channel.basicAck(tag, false);

        } catch (Exception e) {
            log.error("取消订单失败 orderNo={}, orderId={}", orderNo, orderId, e);

            // 删除幂等键，允许下次重试时重新处理
            // 如果是业务异常（如库存扣减失败但状态不对），其实不应该重试，但为了简单起见，
            // 通常系统异常重试，业务异常可以根据具体类型决定。
            // 这里保留你原有的逻辑判断
            if (isSystemException(e)) {
                log.warn("系统异常，消息将重新入队重试: {}", e.getMessage());
                channel.basicNack(tag, false, true); // requeue = true
            } else {
                log.warn("业务异常，消息不再重试，进入死信或丢弃: {}", e.getMessage());
                channel.basicNack(tag, false, false); // requeue = false
                // 对于确定的业务异常（如状态已被别人改了），其实也可以直接 Ack，视业务容忍度而定
                // 如果这里 Nack(false)，消息会去死信队列，需要有人监控死信队列
            }

            // 如果是系统异常重试，保留幂等锁；如果是彻底失败，可以删除锁让后续人工干预或死信处理逻辑再试
            if (!isSystemException(e)) {
                redisTemplate.delete(idempotentKey);
            }
        }
    }

    private boolean isSystemException(Exception e) {
        // 根据实际项目情况调整
        return e instanceof java.net.ConnectException
                || e instanceof org.springframework.dao.DataAccessException
                || e instanceof java.sql.SQLException;
    }
}