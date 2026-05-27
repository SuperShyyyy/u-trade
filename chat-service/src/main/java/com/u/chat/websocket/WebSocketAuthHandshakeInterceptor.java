package com.u.chat.websocket;

import com.u.common.constant.JwtClaimsConstant;
import com.u.common.properties.JwtProperties;
import com.u.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }

        String token = servletRequest.getServletRequest().getParameter("token");
        String userIdParam = servletRequest.getServletRequest().getParameter("userId");

        Long userId = resolveUserId(token, userIdParam);
        if (userId == null) {
            log.warn("WebSocket 握手鉴权失败: token 或 userId 无效");
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

    private Long resolveUserId(String token, String userIdParam) {
        if (StringUtils.hasText(token)) {
            Long userIdFromToken = parseUserIdFromToken(token);
            if (userIdFromToken != null) {
                return userIdFromToken;
            }
        }

        if (StringUtils.hasText(userIdParam)) {
            try {
                return Long.parseLong(userIdParam.trim());
            } catch (NumberFormatException e) {
                log.warn("WebSocket userId 参数格式错误: {}", userIdParam);
            }
        }
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
}
