package com.u.chat.mq;

import com.u.chat.constant.ChatMqConstant;
import com.u.chat.domain.dto.ChatRouteMessage;
import com.u.chat.domain.dto.WsChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "chat.distributed", name = "enabled", havingValue = "true")
@Slf4j
public class ChatMessageRouteProducer {

    private final RabbitTemplate chatRabbitTemplate;

    public ChatMessageRouteProducer(@Qualifier("chatRabbitTemplate") RabbitTemplate chatRabbitTemplate) {
        this.chatRabbitTemplate = chatRabbitTemplate;
    }

    public boolean publish(WsChatMessage message, String targetNodeId) {
        ChatRouteMessage routeMessage = ChatRouteMessage.builder()
                .messageId(message.getMessageId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .timestamp(message.getTimestamp())
                .targetNodeId(targetNodeId)
                .build();
        try {
            chatRabbitTemplate.convertAndSend(
                    ChatMqConstant.EXCHANGE_CHAT_ROUTE,
                    ChatMqConstant.ROUTING_KEY_CHAT_ROUTE,
                    routeMessage);
            log.info("消息 {} 已投递 MQ, targetNodeId={}", message.getMessageId(), targetNodeId);
            return true;
        } catch (Exception e) {
            log.error("消息 {} 投递 MQ 失败, targetNodeId={}", message.getMessageId(), targetNodeId, e);
            return false;
        }
    }
}
