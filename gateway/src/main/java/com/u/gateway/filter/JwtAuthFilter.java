package com.u.gateway.filter;

import com.u.common.constant.GatewayAuthConstant;
import com.u.common.constant.JwtClaimsConstant;
import com.u.common.properties.GatewayAuthProperties;
import com.u.common.properties.JwtProperties;
import com.u.common.security.GatewayAuthConstants;
import com.u.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final GatewayAuthProperties gatewayAuthProperties;
    private final JwtProperties jwtProperties;

    public JwtAuthFilter(JwtProperties jwtProperties,
                         GatewayAuthProperties gatewayAuthProperties) {
        this.jwtProperties = jwtProperties;
        this.gatewayAuthProperties = gatewayAuthProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = normalizePath(request.getURI().getPath());

        log.debug("Gateway request path: {}", path);

        // =========================
        // 1. 白名单直接放行
        // =========================
        if (isWhiteList(path)) {
            return chain.filter(exchange);
        }

        // =========================
        // 2. 解析 Token
        // =========================
        String token = resolveToken(request);

        if (!StringUtils.hasText(token)) {
            log.warn("❌ 未携带token, path: {}", path);
            return unauthorized(exchange.getResponse(), "Missing token");
        }

        // =========================
        // 3. 解析 JWT
        // =========================
        Claims claims;
        try {
            claims = parseToken(token);
        } catch (Exception e) {
            log.warn("❌ Token解析异常, path: {}, err: {}", path, e.getMessage());
            return unauthorized(exchange.getResponse(), "Invalid token");
        }

        if (claims == null) {
            log.warn("❌ Token无效, path: {}", path);
            return unauthorized(exchange.getResponse(), "Invalid token");
        }

        // =========================
        // 4. 提取用户信息
        // =========================
        Object currentId = claims.get(JwtClaimsConstant.CURRENT_ID);
        Object role = claims.get(JwtClaimsConstant.ROLE);
        Object sourceType = claims.get(JwtClaimsConstant.SOURCE_TYPE);

        log.debug("✅ JWT通过, path: {}, userId: {}", path, currentId);

        // =========================
        // 5. 透传 header
        // =========================
        ServerHttpRequest mutatedRequest = request.mutate()
                .headers(headers -> {

                    clearInternalHeaders(headers);

                    if (currentId != null) {
                        headers.set(JwtClaimsConstant.CURRENT_ID, String.valueOf(currentId));
                    }
                    if (role != null) {
                        headers.set(JwtClaimsConstant.ROLE, String.valueOf(role));
                    }
                    if (sourceType != null) {
                        headers.set(JwtClaimsConstant.SOURCE_TYPE, String.valueOf(sourceType));
                    }

                    headers.set(GatewayAuthConstant.GATEWAY_AUTH_HEADER,
                            gatewayAuthProperties.getAuthSecret());

                    if (StringUtils.hasText(jwtProperties.getUserTokenName())) {
                        headers.set(jwtProperties.getUserTokenName(), token);
                    }

                    if (StringUtils.hasText(jwtProperties.getAdminTokenName())) {
                        headers.set(jwtProperties.getAdminTokenName(), token);
                    }
                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    // =========================
    // 白名单
    // =========================
    private boolean isWhiteList(String path) {
        return GatewayAuthConstants.DEFAULT_WHITE_LIST.stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    // =========================
    // path 统一处理（修复你的隐患）
    // =========================
    private String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    // =========================
    // token 提取（更稳版本）
    // =========================
    private String resolveToken(ServerHttpRequest request) {

        String token = request.getHeaders().getFirst(GatewayAuthConstants.TOKEN_HEADER);

        if (!StringUtils.hasText(token)) {
            token = request.getHeaders().getFirst(jwtProperties.getUserTokenName());
        }

        if (!StringUtils.hasText(token)) {
            token = request.getHeaders().getFirst(jwtProperties.getAdminTokenName());
        }

        if (!StringUtils.hasText(token)) {
            String auth = request.getHeaders().getFirst(GatewayAuthConstants.AUTHORIZATION_HEADER);

            if (StringUtils.hasText(auth) &&
                    auth.startsWith(GatewayAuthConstants.BEARER_PREFIX)) {
                token = auth.substring(GatewayAuthConstants.BEARER_PREFIX.length());
            }
        }

        return token;
    }

    // =========================
    // 清理内部 header
    // =========================
    private void clearInternalHeaders(HttpHeaders headers) {
        headers.remove(JwtClaimsConstant.CURRENT_ID);
        headers.remove(JwtClaimsConstant.ROLE);
        headers.remove(JwtClaimsConstant.SOURCE_TYPE);
        headers.remove(GatewayAuthConstant.GATEWAY_AUTH_HEADER);
    }

    // =========================
    // JWT 解析（双 key）
    // =========================
    private Claims parseToken(String token) {

        try {
            return JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
        } catch (Exception ignored) {}

        try {
            return JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
        } catch (Exception ignored) {}

        return null;
    }

    // =========================
    // 统一 401 响应
    // =========================
    private Mono<Void> unauthorized(ServerHttpResponse response, String msg) {
        log.warn("🚫 Gateway拒绝请求: {}", msg);
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}