package com.u.chat.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageSender {

    private final ObjectMapper objectMapper;

    /**
     * @return true 推送成功；false session 不可用或发送异常
     */
    public boolean sendJson(WebSocketSession session, Object payload) {
        try {
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("WebSocket 消息发送失败, sessionId={}", session != null ? session.getId() : null, e);
            return false;
        }
    }
}
