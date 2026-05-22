package com.u.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "u.gateway")
public class GatewayAuthProperties {

    private String authSecret;
}
