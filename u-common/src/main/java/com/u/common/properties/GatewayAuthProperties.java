package com.u.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "sec.gateway")
public class GatewayAuthProperties {

    private String authSecret;
}
