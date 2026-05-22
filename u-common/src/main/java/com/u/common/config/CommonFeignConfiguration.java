package com.u.common.config;

import com.u.common.constant.InternalAuthConstant;
import com.u.common.properties.InternalAuthProperties;
import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RequestInterceptor.class)
@Import(CommonJwtConfiguration.class)
public class CommonFeignConfiguration {

    @Bean
    public RequestInterceptor internalAuthRequestInterceptor(InternalAuthProperties internalAuthProperties) {
        return template -> template.header(InternalAuthConstant.INTERNAL_AUTH_HEADER, internalAuthProperties.getAuthSecret());
    }
}
