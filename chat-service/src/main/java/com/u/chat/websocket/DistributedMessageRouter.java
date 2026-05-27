package com.u.chat.websocket;

import com.u.chat.domain.dto.RoutingResult;
import com.u.chat.domain.dto.WsChatMessage;
import com.u.chat.mq.ChatMessageRouteProducer;
import com.u.chat.redis.ChatOnlineStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 分布式路由决策层：本地 session 二次确认优先，避免 Redis 幽灵状态误路由。
 */
@Component
@ConditionalOnProperty(prefix = "chat.distributed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DistributedMessageRouter implements MessageRouter {

    private final LocalMessageRouter localMessageRouter;
    private final ChatOnlineStatusService onlineStatusService;
    private final ChatMessageRouteProducer messageRouteProducer;
    private final WebSocketSessionManager sessionManager;
    private final String chatNodeId;

    @Override
    public RoutingResult route(WsChatMessage message) {
        Long receiverId = message.getReceiverId();

        // 本地 session 存在 → 直接本地投递
        if (sessionManager.isOnline(receiverId)) {
            RoutingResult localResult = routeToLocal(message);
            if (localResult.isDelivered()) {
                return localResult;
            }
            log.warn("本地推送失败 messageId={}，尝试跨节点兜底", message.getMessageId());
        }

        Optional<String> remoteNodeId = onlineStatusService.getUserNode(receiverId);
        if (remoteNodeId.isPresent()) {
            String nodeId = remoteNodeId.get();

            // Redis 二次确认：node 指向本节点但本地无 session → 清理幽灵状态
            if (chatNodeId.equals(nodeId)) {
                if (sessionManager.isOnline(receiverId)) {
                    return routeToLocal(message);
                }
                log.warn("检测到 Redis 幽灵状态 userId={} nodeId={}，清理后标记 OFFLINE",
                        receiverId, nodeId);
                onlineStatusService.markOffline(receiverId);
                return RoutingResult.offline();
            }

            // Redis 指向其他节点 → MQ 跨节点投递
            return routeToRemote(message, nodeId);
        }

        log.info("接收方 {} 不在线，消息 {} 标记 OFFLINE", receiverId, message.getMessageId());
        return RoutingResult.offline();
    }

    @Override
    public RoutingResult routeToLocal(WsChatMessage message) {
        return localMessageRouter.routeToLocal(message);
    }

    @Override
    public RoutingResult routeToRemote(WsChatMessage message, String targetNodeId) {
        boolean published = messageRouteProducer.publish(message, targetNodeId);
        if (published) {
            log.info("消息 {} 已路由至 MQ, targetNodeId={}, status=ROUTED",
                    message.getMessageId(), targetNodeId);
            return RoutingResult.routed();
        }
        log.error("MQ 投递失败，消息 {} 标记 OFFLINE", message.getMessageId());
        return RoutingResult.offline();
    }
}
