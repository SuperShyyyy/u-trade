package com.sec.mq.sender;

import com.alibaba.fastjson2.JSON;
import com.sec.constant.RabbitMQConstant;
import com.sec.domain.po.MqMessageLog;
import com.sec.message.WalletSettlementMessage;
import com.sec.service.IMqMessageLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSettlementSender {
    private IMqMessageLogService mqMessageLogService;
    private final RabbitTemplate rabbitTemplate;

    public void send(WalletSettlementMessage msg) {
        String messageId = msg.getOrderNo() + "_" + UUID.randomUUID().toString().replace("-", "");
        msg.setMessageId(messageId);
        msg.setTimestamp(System.currentTimeMillis());

        // 1. 插入消息发送记录（发送中）
        MqMessageLog log = new MqMessageLog();
        log.setMessageId(messageId);
        log.setExchange(RabbitMQConstant.EXCHANGE_ORDER_SETTLE_DELAY);
        log.setRoutingKey(RabbitMQConstant.ROUTING_KEY_ORDER_SETTLE_DELAY);
        log.setMessageBody(JSON.toJSONString(msg));
        log.setStatus(0); // 发送中
        log.setRetryCount(0);
        mqMessageLogService.insert(log);

        // 2. 发送消息
        CorrelationData correlationData = new CorrelationData(messageId);
        Message message = rabbitTemplate.getMessageConverter().toMessage(msg, null);
        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        message.getMessageProperties().setMessageId(messageId);
        message.getMessageProperties().setTimestamp(new Date());

        rabbitTemplate.convertAndSend(
                RabbitMQConstant.EXCHANGE_ORDER_SETTLE_DELAY,
                RabbitMQConstant.ROUTING_KEY_ORDER_SETTLE_DELAY,
                message,
                correlationData
        );
    }
}