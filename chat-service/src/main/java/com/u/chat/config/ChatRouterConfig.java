package com.u.chat.config;

import com.u.chat.websocket.LocalMessageRouter;
import com.u.chat.websocket.WebSocketMessageSender;
import com.u.chat.websocket.WebSocketSessionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatRouterConfig {

    @Bean
    public LocalMessageRouter localMessageRouter(WebSocketSessionManager sessionManager,
                                                 WebSocketMessageSender messageSender) {
        return new LocalMessageRouter(sessionManager, messageSender);
    }
}
