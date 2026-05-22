package com.u.order.config;

import com.u.common.config.CommonSecurityConfiguration;
import com.u.common.security.JwtAuthInterceptor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

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
                .packagesToScan("com.u.order.controller.admin")
                .build();
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用户端")
                .packagesToScan("com.u.order.controller")
                .build();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/favicon.ico");
    }
}
