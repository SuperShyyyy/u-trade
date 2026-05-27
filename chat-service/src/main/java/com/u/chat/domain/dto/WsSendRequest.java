package com.u.chat.domain.dto;

import com.u.chat.domain.enums.MessageType;
import lombok.Data;

@Data
public class WsSendRequest {

    private Long receiverId;
    private String content;
    private MessageType messageType;
}
