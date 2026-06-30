package com.u.item.config;

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

@Configuration
@Import(CommonSecurityConfiguration.class)
@Slf4j
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("Register JwtAuthInterceptor.");

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

        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/inner/**");

    }

    @Bean
    @org.springframework.context.annotation.Profile("!prod")
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Item API")
                        .version("1.0")
                        .description("OpenAPI 3"));
    }

    @Bean
    @org.springframework.context.annotation.Profile("!prod")
    public GroupedOpenApi itemApi() {
        return GroupedOpenApi.builder()
                .group("item")
                .packagesToScan("com.u.item.controller")
                .build();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("${cors.allowed-origins:*}")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "token", "X-Gateway-Auth")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/favicon.ico");
    }
}
