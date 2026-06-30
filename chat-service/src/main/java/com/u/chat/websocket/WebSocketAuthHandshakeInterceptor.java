package com.u.chat.websocket;

import com.u.common.constant.JwtClaimsConstant;
import com.u.common.constant.RedisConstant;
import com.u.common.properties.JwtProperties;
import com.u.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

    public static final String USER_ID_ATTR = "userId";

    private final JwtProperties jwtProperties;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }

        String token = servletRequest.getServletRequest().getParameter("token");

        Long userId = resolveUserId(token);
        if (userId == null) {
            log.warn("WebSocket 握手鉴权失败: token 无效");
            return false;
        }

        // 有状态Token检查：用户是否被封禁
        if (isUserBanned(userId)) {
            log.warn("WebSocket 握手拒绝: 用户已被封禁, userId: {}", userId);
            return false;
        }

        attributes.put(USER_ID_ATTR, userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }

    private Long resolveUserId(String token) {
        if (StringUtils.hasText(token)) {
            Long userIdFromToken = parseUserIdFromToken(token);
            if (userIdFromToken != null) {
                return userIdFromToken;
            }
        }
        log.warn("WebSocket 握手鉴权失败: token 无效");
        return null;
    }

    private Long parseUserIdFromToken(String token) {
        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            Object currentId = claims.get(JwtClaimsConstant.CURRENT_ID);
            if (currentId != null) {
                return Long.parseLong(currentId.toString());
            }
        } catch (Exception e) {
            log.debug("用户 token 解析失败，尝试 admin token");
        }

        try {
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
            Object currentId = claims.get(JwtClaimsConstant.CURRENT_ID);
            if (currentId != null) {
                return Long.parseLong(currentId.toString());
            }
        } catch (Exception e) {
            log.warn("WebSocket token 解析失败");
        }
        return null;
    }

    /**
     * 检查用户是否被封禁
     * 优先查Redis，Redis失败时降级放行（不做数据库查询，保持握手轻量）
     */
    private boolean isUserBanned(Long userId) {
        String banKey = RedisConstant.USER_BAN_KEY + userId;
        try {
            String banValue = stringRedisTemplate.opsForValue().get(banKey);
            return banValue != null;
        } catch (Exception e) {
            log.warn("WebSocket ban检查Redis异常，降级放行, userId: {}", userId);
            return false;
        }
    }
}
