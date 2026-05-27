package com.u.chat.service;

import org.springframework.web.socket.WebSocketSession;

public interface ChatWebSocketService {

    void onConnect(WebSocketSession session);

    void onDisconnect(WebSocketSession session);

    void handleMessage(WebSocketSession session, String payload);
}
