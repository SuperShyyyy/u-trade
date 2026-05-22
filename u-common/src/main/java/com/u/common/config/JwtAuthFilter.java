package com.u.common.config;

import com.u.common.constant.GatewayAuthConstant;
import com.u.common.constant.InternalAuthConstant;
import com.u.common.constant.JwtClaimsConstant;
import com.u.common.context.BaseContext;
import com.u.common.properties.GatewayAuthProperties;
import com.u.common.properties.InternalAuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter implements WebFilter {

    private final GatewayAuthProperties gatewayAuthProperties;
    private final InternalAuthProperties internalAuthProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestUri = exchange.getRequest().getURI().getPath();

        // OPTIONS 直接放行
        if ("OPTIONS".equalsIgnoreCase(String.valueOf(exchange.getRequest().getMethod()))) {
            return chain.filter(exchange);
        }

        // Swagger 放行
        if (requestUri.contains("/v3/api-docs") || requestUri.contains("/swagger-resources")) {
            return chain.filter(exchange);
        }

        // 内部接口鉴权
        if (isInternalPath(requestUri)) {
            String internalAuth = exchange.getRequest().getHeaders()
                    .getFirst(InternalAuthConstant.INTERNAL_AUTH_HEADER);

            if (!StringUtils.hasText(internalAuthProperties.getAuthSecret())
                    || !internalAuthProperties.getAuthSecret().equals(internalAuth)) {
                log.warn("服务间调用认证失败, path: {}", requestUri);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            return chain.filter(exchange);
        }

        // 网关鉴权
        String gatewayAuth = exchange.getRequest().getHeaders()
                .getFirst(GatewayAuthConstant.GATEWAY_AUTH_HEADER);
        String currentId = exchange.getRequest().getHeaders()
                .getFirst(JwtClaimsConstant.CURRENT_ID);

        if (!StringUtils.hasText(gatewayAuthProperties.getAuthSecret())
                || !gatewayAuthProperties.getAuthSecret().equals(gatewayAuth)
                || !StringUtils.hasText(currentId)) {
            log.warn("请求未经过网关认证透传, path: {}", requestUri);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            Long parsedId = Long.valueOf(currentId);
            String role = exchange.getRequest().getHeaders()
                    .getFirst(JwtClaimsConstant.ROLE);
            String sourceType = exchange.getRequest().getHeaders()
                    .getFirst(JwtClaimsConstant.SOURCE_TYPE);

            // 管理员路径鉴权
            if (isAdminPath(requestUri) && !StringUtils.hasText(role)) {
                BaseContext.remove();
                log.warn("越权尝试: 非管理员角色访问 {}", requestUri);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // 设置线程上下文
            BaseContext.setCurrentId(parsedId);
            BaseContext.setCurrentRole(role);
            BaseContext.setCurrentSourceType(sourceType);

            // 继续处理
            return chain.filter(exchange)
                    .doFinally(signalType -> BaseContext.remove()); // 请求完成后清理
        } catch (NumberFormatException ex) {
            log.warn("网关注入身份头格式异常: {}", currentId);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
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