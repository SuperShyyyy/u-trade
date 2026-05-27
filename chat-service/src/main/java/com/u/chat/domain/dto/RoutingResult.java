package com.u.chat.domain.dto;

import com.u.chat.domain.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 路由决策结果，由 MessageRouter 返回，Service 据此更新 messageStatus。
 */
@Getter
@AllArgsConstructor
public class RoutingResult {

    private final MessageStatus status;
    private final boolean delivered;

    public static RoutingResult delivered() {
        return new RoutingResult(MessageStatus.DELIVERED, true);
    }

    public static RoutingResult offline() {
        return new RoutingResult(MessageStatus.OFFLINE, false);
    }

    public static RoutingResult routed() {
        return new RoutingResult(MessageStatus.ROUTED, false);
    }
}
