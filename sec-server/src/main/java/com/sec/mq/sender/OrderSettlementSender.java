package com.sec.mq.sender;

import com.alibaba.fastjson2.JSON;
import com.sec.constant.RabbitMQConstant;
import com.sec.message.OrderSettlementMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSettlementSender {
    private final RabbitTemplate rabbitTemplate;

    // 只负责发消息，不再负责落库
    public void send(OrderSettlementMessage msg) {
        CorrelationData correlationData = new CorrelationData(msg.getMessageId());
        Message message = rabbitTemplate.getMessageConverter().toMessage(msg, null);
        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        message.getMessageProperties().setMessageId(msg.getMessageId());

        rabbitTemplate.convertAndSend(
                RabbitMQConstant.EXCHANGE_ORDER_SETTLE_EXEC,
                RabbitMQConstant.ROUTING_KEY_ORDER_SETTLE_EXEC,
                message,
                correlationData
        );
    }
}