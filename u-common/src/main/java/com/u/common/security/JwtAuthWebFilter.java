package com.u.common.security;

import com.u.common.constant.GatewayAuthConstant;
import com.u.common.constant.InternalAuthConstant;
import com.u.common.constant.JwtClaimsConstant;
import com.u.common.context.BaseContext;
import com.u.common.properties.GatewayAuthProperties;
import com.u.common.properties.InternalAuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthWebFilter implements WebFilter {

    private final GatewayAuthProperties gatewayAuthProperties;
    private final InternalAuthProperties internalAuthProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String path = request.getURI().getPath();

        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            return chain.filter(exchange);
        }

        if (path.contains("/v3/api-docs") || path.contains("/swagger-resources")) {
            return chain.filter(exchange);
        }

        if (isInternalPath(path)) {
            String internalAuth = request.getHeaders().getFirst(InternalAuthConstant.INTERNAL_AUTH_HEADER);
            if (!StringUtils.hasText(internalAuthProperties.getAuthSecret())
                    || !internalAuthProperties.getAuthSecret().equals(internalAuth)) {
                log.warn("服务间调用认证失败, path: {}", path);
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }
            return chain.filter(exchange);
        }

        String gatewayAuth = request.getHeaders().getFirst(GatewayAuthConstant.GATEWAY_AUTH_HEADER);
        String currentId = request.getHeaders().getFirst(JwtClaimsConstant.CURRENT_ID);

        if (!StringUtils.hasText(gatewayAuthProperties.getAuthSecret())
                || !gatewayAuthProperties.getAuthSecret().equals(gatewayAuth)
                || !StringUtils.hasText(currentId)) {
            log.warn("请求未经过网关认证透传, path: {}", path);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        try {
            Long parsedId = Long.valueOf(currentId);
            String role = request.getHeaders().getFirst(JwtClaimsConstant.ROLE);
            String sourceType = request.getHeaders().getFirst(JwtClaimsConstant.SOURCE_TYPE);

            if (isAdminPath(path) && !StringUtils.hasText(role)) {
                BaseContext.remove();
                log.warn("越权尝试: 非管理员角色访问 {}", path);
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return response.setComplete();
            }

            BaseContext.setCurrentId(parsedId);
            BaseContext.setCurrentRole(role);
            BaseContext.setCurrentSourceType(sourceType);
            return chain.filter(exchange);
        } catch (NumberFormatException ex) {
            log.warn("网关注入身份头格式异常: {}", currentId);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        } finally {
            BaseContext.remove();
        }
    }

    private boolean isAdminPath(String path) {
        return path.startsWith(GatewayAuthConstant.ADMIN_PATH_PREFIX)
                || path.startsWith(GatewayAuthConstant.MANAGER_PATH_PREFIX);
    }

    private boolean isInternalPath(String path) {
        return path.startsWith("/inner/");
    }
}