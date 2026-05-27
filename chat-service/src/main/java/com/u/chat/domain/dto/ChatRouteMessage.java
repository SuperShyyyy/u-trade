package com.u.chat.domain.dto;

import com.u.chat.domain.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MQ 跨节点路由消息体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRouteMessage {

    private String messageId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private MessageType messageType;
    private Long timestamp;
    private String targetNodeId;
}
