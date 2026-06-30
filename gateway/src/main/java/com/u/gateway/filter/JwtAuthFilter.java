package com.u.gateway.filter;

import com.u.common.constant.JwtClaimsConstant;
import com.u.common.constant.RedisConstant;
import com.u.common.properties.GatewayAuthProperties;
import com.u.common.properties.JwtProperties;
import com.u.common.security.GatewayAuthConstants;
import com.u.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final GatewayAuthProperties gatewayAuthProperties;
    private final JwtProperties jwtProperties;
    private final WebClient webClient;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    public JwtAuthFilter(JwtProperties jwtProperties,
                         GatewayAuthProperties gatewayAuthProperties,
                         WebClient.Builder webClientBuilder) {
        this.jwtProperties = jwtProperties;
        this.gatewayAuthProperties = gatewayAuthProperties;
        this.webClient = webClientBuilder.build();
    }

    /**
     * 提供负载均衡的 WebClient.Builder 供 Spring 容器注入
     */
    @Configuration
    static class WebClientConfig {
        @Bean
        @LoadBalanced
        public WebClient.Builder loadBalancedWebClientBuilder() {
            return WebClient.builder();
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = normalizePath(request.getURI().getPath());

        log.debug("Gateway request path: {}", path);

        // =========================
        // 1. 白名单直接放行（但需清除客户端伪造的内部 header）
        // =========================
        if (isWhiteList(path)) {
            // 即使白名单路径也必须清除内部 header，防止伪造
            ServerHttpRequest cleanedRequest = request.mutate()
                    .headers(headers -> clearInternalHeaders(headers))
                    .build();
            return chain.filter(exchange.mutate().request(cleanedRequest).build());
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
        // 4. 提取用户信息（新鉴权体系）
        // =========================
        Object currentId = claims.get(JwtClaimsConstant.CURRENT_ID);
        Object role = claims.get(JwtClaimsConstant.ROLE);
        Object sourceType = claims.get(JwtClaimsConstant.SOURCE_TYPE);
        Object sessionId = claims.get(JwtClaimsConstant.SESSION_ID);
        Object tokenVersion = claims.get(JwtClaimsConstant.TOKEN_VERSION);

        if (currentId == null) {
            log.warn("❌ JWT缺少userId, path: {}", path);
            return unauthorized(exchange.getResponse(), "Invalid token");
        }

        // =========================
        // 4.5 路径角色校验：admin 路径只允许 ADMIN 角色访问
        // =========================
        if (isAdminPath(path)) {
            String sourceTypeStr = sourceType != null ? sourceType.toString() : "";
            if (!"ADMIN".equals(sourceTypeStr)) {
                log.warn("🚫 非管理员访问admin路径, userId: {}, path: {}", currentId, path);
                return forbidden(exchange.getResponse(), "Admin access required");
            }
        }

        // =========================
        // Step 1: 封禁检查
        // =========================
        if (isUserBanned(currentId, sourceType)) {
            log.warn("🚫 用户已被封禁, userId: {}, path: {}", currentId, path);
            return unauthorized(exchange.getResponse(), "Account banned");
        }

        // =========================
        // Step 2: tokenVersion 校验（logout/改密/封禁后旧 Token 失效）
        // =========================
        if (isTokenVersionMismatch(currentId, tokenVersion)) {
            log.warn("🚫 tokenVersion不匹配(已登出/改密/封禁), userId: {}, path: {}", currentId, path);
            return unauthorized(exchange.getResponse(), "Token expired, please login again");
        }

        // =========================
        // Step 3: session 校验（单设备登录互踢）
        // =========================
        if (isSessionMismatch(currentId, sessionId)) {
            log.warn("🚫 session不匹配(已在其他设备登录), userId: {}, path: {}", currentId, path);
            return unauthorized(exchange.getResponse(), "Logged in from another device");
        }

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

                    headers.set(GatewayAuthConstants.GATEWAY_AUTH_HEADER,
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
    // admin 路径检查
    // =========================
    private boolean isAdminPath(String path) {
        return path.startsWith(GatewayAuthConstants.ADMIN_PATH_PREFIX)
                || path.startsWith(GatewayAuthConstants.MANAGER_PATH_PREFIX);
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
        headers.remove(GatewayAuthConstants.GATEWAY_AUTH_HEADER);
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
    // Step 1: 封禁检查 — user:ban:{userId} 存在则拒绝
    // =========================
    private boolean isUserBanned(Object currentIdObj, Object sourceTypeObj) {
        if (!"USER".equals(sourceTypeObj != null ? sourceTypeObj.toString() : "")) return false;
        if (currentIdObj == null || stringRedisTemplate == null) return false;

        String userId = String.valueOf(currentIdObj);
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisConstant.USER_BAN_KEY + userId));
        } catch (Exception e) {
            log.warn("Redis查询封禁状态失败, 降级放行, userId: {}, error: {}", userId, e.getMessage());
            return false;
        }
    }

    // =========================
    // Step 2: tokenVersion 校验 — jwt.tokenVersion != redis.tokenVersion → 失效
    // =========================
    private boolean isTokenVersionMismatch(Object currentIdObj, Object tokenVersionObj) {
        if (currentIdObj == null || stringRedisTemplate == null) return false;

        int jwtVersion = 0;
        if (tokenVersionObj != null) {
            jwtVersion = tokenVersionObj instanceof Number
                    ? ((Number) tokenVersionObj).intValue()
                    : Integer.parseInt(tokenVersionObj.toString());
        }

        String userId = String.valueOf(currentIdObj);
        try {
            String redisVersionStr = stringRedisTemplate.opsForValue()
                    .get(RedisConstant.TOKEN_VERSION_KEY + userId);
            if (redisVersionStr == null) return false;  // 无版本记录，放行

            int redisVersion = Integer.parseInt(redisVersionStr);
            return jwtVersion != redisVersion;  // 不相等 → 被 logout/改密/封禁 递增了
        } catch (Exception e) {
            log.warn("Redis查询tokenVersion失败, 降级放行, userId: {}", userId);
            return false;
        }
    }

    // =========================
    // Step 3: session 校验 — jwt.sessionId != redis.sessionId → 被踢下线
    // =========================
    private boolean isSessionMismatch(Object currentIdObj, Object sessionIdObj) {
        if (currentIdObj == null || sessionIdObj == null || stringRedisTemplate == null) return false;

        String jwtSessionId = sessionIdObj.toString();
        String userId = String.valueOf(currentIdObj);

        try {
            String redisSessionId = stringRedisTemplate.opsForValue()
                    .get(RedisConstant.USER_SESSION_KEY + userId);
            // Redis 无 session → 首次登录后未存储，放行
            if (redisSessionId == null) return false;
            return !jwtSessionId.equals(redisSessionId);
        } catch (Exception e) {
            log.warn("Redis查询session失败, 降级放行, userId: {}", userId);
            return false;
        }
    }

    // =========================
    // 统一 401 响应
    // =========================
    private Mono<Void> unauthorized(ServerHttpResponse response, String msg) {
        log.warn("🚫 Gateway拒绝请求: {}", msg);
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    // =========================
    // 统一 403 响应
    // =========================
    private Mono<Void> forbidden(ServerHttpResponse response, String msg) {
        log.warn("🚫 Gateway权限不足: {}", msg);
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}