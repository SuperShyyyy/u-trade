package com.u.chat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "chat")
public class ChatProperties {

    private Distributed distributed = new Distributed();
    private long onlineTtlSeconds = 1800;
    private long messageIdempotencyTtlSeconds = 86400;

    @Data
    public static class Distributed {
        private boolean enabled = false;
    }
}
