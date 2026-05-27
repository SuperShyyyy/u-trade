package com.u.chat.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsResponse {

    private String type;
    private String message;
    private WsChatMessage data;

    public static WsResponse ack(WsChatMessage data) {
        return WsResponse.builder().type("ACK").data(data).build();
    }

    public static WsResponse chat(WsChatMessage data) {
        return WsResponse.builder().type("CHAT").data(data).build();
    }

    public static WsResponse error(String message) {
        return WsResponse.builder().type("ERROR").message(message).build();
    }

    public static WsResponse connected(Long userId) {
        return WsResponse.builder()
                .type("CONNECTED")
                .message("WebSocket connected, userId=" + userId)
                .build();
    }
}
