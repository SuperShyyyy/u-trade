package com.u.chat.websocket;

import com.u.chat.domain.dto.RoutingResult;
import com.u.chat.domain.dto.WsChatMessage;
import com.u.chat.domain.dto.WsResponse;
import com.u.chat.domain.enums.MessageStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalMessageRouter implements MessageRouter {

    private final WebSocketSessionManager sessionManager;
    private final WebSocketMessageSender messageSender;

    public LocalMessageRouter(WebSocketSessionManager sessionManager,
                              WebSocketMessageSender messageSender) {
        this.sessionManager = sessionManager;
        this.messageSender = messageSender;
    }

    @Override
    public RoutingResult route(WsChatMessage message) {
        return routeToLocal(message);
    }

    @Override
    public RoutingResult routeToLocal(WsChatMessage message) {
        Long receiverId = message.getReceiverId();

        if (!sessionManager.isOnline(receiverId)) {
            log.info("接收方 {} 不在本节点，消息 {} 暂未本地投递", receiverId, message.getMessageId());
            return RoutingResult.offline();
        }

        return sessionManager.getSession(receiverId)
                .map(session -> {
                    WsChatMessage deliveredMessage = copyWithStatus(message, MessageStatus.DELIVERED);
                    boolean pushed = messageSender.sendJson(session, WsResponse.chat(deliveredMessage));
                    if (pushed) {
                        return RoutingResult.delivered();
                    }
                    log.warn("接收方 {} 本地推送失败，消息 {}", receiverId, message.getMessageId());
                    return RoutingResult.offline();
                })
                .orElseGet(() -> {
                    log.info("接收方 {} session 不存在，消息 {} 暂未本地投递", receiverId, message.getMessageId());
                    return RoutingResult.offline();
                });
    }

    @Override
    public RoutingResult routeToRemote(WsChatMessage message, String targetNodeId) {
        log.debug("LocalMessageRouter 不支持跨节点路由 messageId={}, targetNodeId={}",
                message.getMessageId(), targetNodeId);
        return RoutingResult.offline();
    }

    private WsChatMessage copyWithStatus(WsChatMessage source, MessageStatus status) {
        return WsChatMessage.builder()
                .messageId(source.getMessageId())
                .senderId(source.getSenderId())
                .receiverId(source.getReceiverId())
                .content(source.getContent())
                .messageType(source.getMessageType())
                .messageStatus(status)
                .timestamp(source.getTimestamp())
                .build();
    }
}
