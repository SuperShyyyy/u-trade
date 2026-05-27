package com.u.chat.websocket;

import com.u.chat.domain.dto.RoutingResult;
import com.u.chat.domain.dto.WsChatMessage;

/**
 * 消息路由抽象，MVP 使用本地实现，分布式场景由 DistributedMessageRouter 扩展。
 */
public interface MessageRouter {

    RoutingResult route(WsChatMessage message);

    RoutingResult routeToLocal(WsChatMessage message);

    RoutingResult routeToRemote(WsChatMessage message, String targetNodeId);
}
