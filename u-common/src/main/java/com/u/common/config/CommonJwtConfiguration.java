package com.u.common.config;

import com.u.common.properties.GatewayAuthProperties;
import com.u.common.properties.InternalAuthProperties;
import com.u.common.properties.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({JwtProperties.class, GatewayAuthProperties.class, InternalAuthProperties.class})
public class CommonJwtConfiguration {
}
