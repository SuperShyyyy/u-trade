package com.u.chat.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 消息状态机（统一语义）：
 * <pre>
 * 本地：SENT → DELIVERED / OFFLINE
 * 跨节点：SENT → ROUTED → CONSUMED → DELIVERED
 * ACK 响应固定 SERVER_ACK（仅表示服务端已接收并处理，≠ 已投递 / 已读）
 * </pre>
 */
public enum MessageStatus {
    SENT,
    SERVER_ACK,
    ROUTED,
    CONSUMED,
    DELIVERED,
    OFFLINE;

    @JsonValue
    public String toValue() {
        return name();
    }
}
