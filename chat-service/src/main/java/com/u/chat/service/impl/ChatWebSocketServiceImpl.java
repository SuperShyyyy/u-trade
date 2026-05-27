package com.u.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.u.chat.domain.dto.RoutingResult;
import com.u.chat.domain.dto.WsChatMessage;
import com.u.chat.domain.dto.WsResponse;
import com.u.chat.domain.dto.WsSendRequest;
import com.u.chat.domain.enums.MessageStatus;
import com.u.chat.domain.enums.MessageType;
import com.u.chat.service.ChatWebSocketService;
import com.u.chat.websocket.MessageRouter;
import com.u.chat.websocket.WebSocketAuthHandshakeInterceptor;
import com.u.chat.websocket.WebSocketMessageSender;
import com.u.chat.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketSession;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketServiceImpl implements ChatWebSocketService {

    private final WebSocketSessionManager sessionManager;
    private final WebSocketMessageSender messageSender;
    private final MessageRouter messageRouter;
    private final ObjectMapper objectMapper;

    @Override
    public void onConnect(WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            messageSender.sendJson(session, WsResponse.error("鉴权失败，无法建立连接"));
            return;
        }
        sessionManager.register(userId, session);
        messageSender.sendJson(session, WsResponse.connected(userId));
    }

    @Override
    public void onDisconnect(WebSocketSession session) {
        sessionManager.remove(session);
    }

    @Override
    public void handleMessage(WebSocketSession session, String payload) {
        Long senderId = sessionManager.getUserId(session).orElse(null);
        if (senderId == null) {
            messageSender.sendJson(session, WsResponse.error("会话无效，请重新连接"));
            return;
        }

        try {
            WsSendRequest request = objectMapper.readValue(payload, WsSendRequest.class);
            WsChatMessage message = buildMessage(senderId, request);

            RoutingResult routingResult = messageRouter.route(message);
            message.setMessageStatus(routingResult.getStatus());

            WsChatMessage ackMessage = copyWithStatus(message, MessageStatus.SERVER_ACK);
            messageSender.sendJson(session, WsResponse.ack(ackMessage));
        } catch (IllegalArgumentException e) {
            messageSender.sendJson(session, WsResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("处理 WebSocket 消息失败, senderId={}", senderId, e);
            messageSender.sendJson(session, WsResponse.error("消息格式错误或处理失败"));
        }
    }

    private WsChatMessage buildMessage(Long senderId, WsSendRequest request) {
        validateRequest(senderId, request);
        return WsChatMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .senderId(senderId)
                .receiverId(request.getReceiverId())
                .content(request.getContent().trim())
                .messageType(request.getMessageType() != null ? request.getMessageType() : MessageType.text)
                .messageStatus(MessageStatus.SENT)
                .timestamp(System.currentTimeMillis())
                .build();
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

    private void validateRequest(Long senderId, WsSendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("消息体不能为空");
        }
        if (request.getReceiverId() == null) {
            throw new IllegalArgumentException("receiverId 不能为空");
        }
        if (request.getReceiverId().equals(senderId)) {
            throw new IllegalArgumentException("不能给自己发消息");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("content 不能为空");
        }
    }

    private Long getUserId(WebSocketSession session) {
        return sessionManager.getUserId(session)
                .orElseGet(() -> {
                    Object attr = session.getAttributes().get(WebSocketAuthHandshakeInterceptor.USER_ID_ATTR);
                    return attr instanceof Long id ? id : null;
                });
    }
}
