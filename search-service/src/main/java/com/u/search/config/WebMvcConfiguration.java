package com.u.search.config;

import com.u.common.config.CommonSecurityConfiguration;
import com.u.common.security.JwtAuthInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Import(CommonSecurityConfiguration.class)
@Slf4j
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");

        String[] excludePaths = {
                "/doc.html",
                "/webjars/**",
                "/v3/api-docs/**",
                "/swagger-resources/**",
                "/favicon.ico",
                "/error"
        };

        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/search/**")
                .excludePathPatterns(excludePaths);

        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/inner/**");
    }
}
