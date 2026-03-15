package com.sec.mq.sender;

import com.sec.constant.RabbitMQConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderCancelDelaySender {
    private final RabbitTemplate rabbitTemplate;

    public void sendDelayCancelMessage(String orderNo, Long orderId) {
        String messageId = "cancel_" + orderNo + "_" + UUID.randomUUID();
        Map<String, Object> msg = new HashMap<>();
        msg.put("orderNo", orderNo);
        msg.put("orderId", orderId);
        msg.put("createTime", System.currentTimeMillis());

        CorrelationData cd = new CorrelationData(messageId);
        Message message = rabbitTemplate.getMessageConverter().toMessage(msg, null);
        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        message.getMessageProperties().setMessageId(messageId);

        rabbitTemplate.convertAndSend(
                RabbitMQConstant.EXCHANGE_ORDER_CANCEL_DELAY,
                RabbitMQConstant.ROUTING_KEY_ORDER_CANCEL_DELAY,
                message,
                cd
        );
    }
}