package com.u.common.config;

import com.u.common.properties.GatewayAuthProperties;
import com.u.common.properties.InternalAuthProperties;
import com.u.common.security.JwtAuthInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import(CommonJwtConfiguration.class)
public class CommonSecurityConfiguration {

    @Bean
    public JwtAuthInterceptor jwtAuthInterceptor(GatewayAuthProperties gatewayAuthProperties,
                                                 InternalAuthProperties internalAuthProperties) {
        return new JwtAuthInterceptor(gatewayAuthProperties, internalAuthProperties);
    }
}
