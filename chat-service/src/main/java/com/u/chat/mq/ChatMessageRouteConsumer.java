package com.u.chat.mq;

import com.rabbitmq.client.Channel;
import com.u.chat.constant.ChatMqConstant;
import com.u.chat.domain.dto.ChatRouteMessage;
import com.u.chat.domain.dto.WsChatMessage;
import com.u.chat.domain.dto.WsResponse;
import com.u.chat.domain.enums.MessageStatus;
import com.u.chat.redis.ChatMessageIdempotencyService;
import com.u.chat.websocket.WebSocketMessageSender;
import com.u.chat.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "chat.distributed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ChatMessageRouteConsumer {

    private final String chatNodeId;
    private final WebSocketSessionManager sessionManager;
    private final WebSocketMessageSender messageSender;
    private final ChatMessageIdempotencyService idempotencyService;

    @RabbitListener(queues = ChatMqConstant.QUEUE_CHAT_ROUTE)
    public void onMessage(ChatRouteMessage routeMessage,
                          Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        String messageId = routeMessage.getMessageId();
        try {
            if (!chatNodeId.equals(routeMessage.getTargetNodeId())) {
                log.debug("[MQ消费] messageId={} 非本节点目标，忽略 targetNodeId={}",
                        messageId, routeMessage.getTargetNodeId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 1. 幂等检查
            if (idempotencyService.isDelivered(messageId)) {
                log.info("[MQ消费] messageId={} 已 DELIVERED，跳过重复消费", messageId);
                channel.basicAck(deliveryTag, false);
                return;
            }
            if (!idempotencyService.tryAcquire(messageId)) {
                log.info("[MQ消费] messageId={} 消费进行中，跳过重复", messageId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 2. 标记 CONSUMED
            idempotencyService.markConsumed(messageId);

            // 3. 二次确认本地 session
            Optional<WebSocketSession> sessionOpt = sessionManager.getSession(routeMessage.getReceiverId());
            if (sessionOpt.isEmpty()) {
                log.warn("[MQ消费] messageId={} receiverId={} 本地无 session，触发重试",
                        messageId, routeMessage.getReceiverId());
                idempotencyService.releaseForRetry(messageId);
                channel.basicNack(deliveryTag, false, true);
                return;
            }

            // 4. WebSocket 推送
            WsChatMessage chatMessage = WsChatMessage.builder()
                    .messageId(messageId)
                    .senderId(routeMessage.getSenderId())
                    .receiverId(routeMessage.getReceiverId())
                    .content(routeMessage.getContent())
                    .messageType(routeMessage.getMessageType())
                    .messageStatus(MessageStatus.DELIVERED)
                    .timestamp(routeMessage.getTimestamp())
                    .build();

            boolean pushed = messageSender.sendJson(sessionOpt.get(), WsResponse.chat(chatMessage));
            if (!pushed) {
                onPushFailedForRetry(messageId, routeMessage.getReceiverId());
                idempotencyService.releaseForRetry(messageId);
                channel.basicNack(deliveryTag, false, true);
                return;
            }

            // 5. 推送成功 → 标记 DELIVERED
            idempotencyService.markDelivered(messageId);
            log.info("[MQ消费] messageId={} receiverId={} 处理完成 status=DELIVERED",
                    messageId, routeMessage.getReceiverId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MQ消费] messageId={} 处理异常，触发重试", messageId, e);
            idempotencyService.releaseForRetry(messageId);
            channel.basicNack(deliveryTag, false, true);
        }
    }

    /**
     * 推送失败重试钩子（当前 log + MQ nack 重投，后续可扩展为延迟队列）。
     */
    private void onPushFailedForRetry(String messageId, Long receiverId) {
        log.warn("[重试钩子] WebSocket 推送失败 messageId={} receiverId={}，已释放幂等锁等待 MQ 重投",
                messageId, receiverId);
    }
}
