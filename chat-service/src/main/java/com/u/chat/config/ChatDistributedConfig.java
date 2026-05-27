package com.u.chat.config;

import com.u.chat.websocket.DistributedMessageRouter;
import com.u.chat.websocket.LocalMessageRouter;
import com.u.chat.websocket.MessageRouter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ChatDistributedConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "chat.distributed", name = "enabled", havingValue = "true")
    public MessageRouter distributedMessageRouter(DistributedMessageRouter distributedMessageRouter) {
        return distributedMessageRouter;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "chat.distributed", name = "enabled", havingValue = "false", matchIfMissing = true)
    public MessageRouter localMessageRouterAdapter(LocalMessageRouter localMessageRouter) {
        return localMessageRouter;
    }
}
