package com.u.chat.domain.dto;

import com.u.chat.domain.enums.MessageStatus;
import com.u.chat.domain.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsChatMessage {

    private String messageId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private MessageType messageType;
    private MessageStatus messageStatus;
    private Long timestamp;
}
