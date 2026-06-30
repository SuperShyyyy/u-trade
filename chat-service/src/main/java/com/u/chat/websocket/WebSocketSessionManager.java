package com.u.chat.websocket;

import com.u.chat.config.ChatProperties;
import com.u.chat.redis.ChatOnlineStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class WebSocketSessionManager {

    private final ConcurrentHashMap<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> sessionUsers = new ConcurrentHashMap<>();

    private final ChatProperties chatProperties;
    private final ChatOnlineStatusService onlineStatusService;
    private final String chatNodeId;

    public WebSocketSessionManager(ChatProperties chatProperties,
                                   ChatOnlineStatusService onlineStatusService,
                                   String chatNodeId) {
        this.chatProperties = chatProperties;
        this.onlineStatusService = onlineStatusService;
        this.chatNodeId = chatNodeId;
    }

    public void register(Long userId, WebSocketSession session) {
        WebSocketSession previous = userSessions.get(userId);
        if (previous != null && !previous.getId().equals(session.getId())) {
            remove(previous);
            if (previous.isOpen()) {
                log.info("用户 {} 建立新连接，关闭旧连接 sessionId={}", userId, previous.getId());
                try {
                    previous.close();
                } catch (Exception e) {
                    log.warn("关闭用户 {} 旧连接失败", userId, e);
                }
            }
        }

        // 1. 写本地 session（双向映射）
        userSessions.put(userId, session);
        sessionUsers.put(session.getId(), userId);
        log.info("本地 session 注册成功 userId={}, sessionId={}", userId, session.getId());

        // 2 & 3. 写 Redis user:online + user:node
        syncRedisOnRegister(userId);
    }

    public void remove(WebSocketSession session) {
        if (session == null) {
            return;
        }

        // 1. 删除本地 session（双向映射）
        Long userId = sessionUsers.remove(session.getId());
        if (userId == null) {
            return;
        }
        userSessions.computeIfPresent(userId, (id, current) ->
                current.getId().equals(session.getId()) ? null : current);
        log.info("本地 session 清理成功 userId={}, sessionId={}", userId, session.getId());

        // 2 & 3. 删除 Redis user:online + user:node
        syncRedisOnRemove(userId);
    }

    public Optional<WebSocketSession> getSession(Long userId) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            return Optional.of(session);
        }
        if (session != null) {
            remove(session);
        }
        return Optional.empty();
    }

    public Optional<Long> getUserId(WebSocketSession session) {
        if (session == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessionUsers.get(session.getId()));
    }

    public boolean isOnline(Long userId) {
        return getSession(userId).isPresent();
    }

    /**
     * 定期清理已关闭的WebSocket Session，防止内存泄漏
     * 每60秒执行一次
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupClosedSessions() {
        List<WebSocketSession> closedSessions = new ArrayList<>();
        for (WebSocketSession session : userSessions.values()) {
            if (session == null || !session.isOpen()) {
                closedSessions.add(session);
            }
        }
        if (!closedSessions.isEmpty()) {
            log.info("清理已关闭的WebSocket Session，数量: {}", closedSessions.size());
            for (WebSocketSession session : closedSessions) {
                remove(session);
            }
        }
    }

    private void syncRedisOnRegister(Long userId) {
        if (!chatProperties.getDistributed().isEnabled()) {
            return;
        }
        onlineStatusService.markOnline(userId, chatNodeId);
    }

    private void syncRedisOnRemove(Long userId) {
        if (!chatProperties.getDistributed().isEnabled()) {
            return;
        }
        onlineStatusService.markOffline(userId);
    }
}
