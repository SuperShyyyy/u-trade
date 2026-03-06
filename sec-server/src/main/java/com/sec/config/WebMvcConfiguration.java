package com.sec.config;

import com.sec.interceptor.JwtAuthInterceptor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer { // 改为实现接口

    private final JwtAuthInterceptor jwtAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");

        String[] excludePaths = {
                "/admin/login",
                "/user/login",
                "/user/register",
                "/user/shop/status",
                "/doc.html",
                "/webjars/**",
                "/v3/api-docs/**",
                "/swagger-resources/**",
                "/favicon.ico",
                "/error"
        };

        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns(excludePaths);

        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/user/**")
                .excludePathPatterns(excludePaths);
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("项目接口文档")
                        .version("1.0")
                        .description("基于 Spring Boot 3 + OpenAPI 3 的接口定义"));
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("管理端")
                .packagesToScan("com.sec.controller.admin")
                .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用户端")
                .packagesToScan("com.sec.controller.user")
                .build();
    }

    // 注意：实现 WebMvcConfigurer 后，通常不需要手动写 addResourceHandlers
    // 因为 Spring Boot 会自动处理 /doc.html 和 /webjars
}