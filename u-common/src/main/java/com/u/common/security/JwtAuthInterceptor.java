package com.u.common.security;

import com.u.common.constant.GatewayAuthConstant;
import com.u.common.constant.InternalAuthConstant;
import com.u.common.constant.JwtClaimsConstant;
import com.u.common.context.BaseContext;
import com.u.common.properties.GatewayAuthProperties;
import com.u.common.properties.InternalAuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final GatewayAuthProperties gatewayAuthProperties;
    private final InternalAuthProperties internalAuthProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if (isInternalPath(path)) {
            return validateInternalRequest(request, response, path);
        }

        return validateGatewayRequest(request, response, path);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        BaseContext.remove();
    }

    private boolean validateInternalRequest(HttpServletRequest request, HttpServletResponse response, String path) {
        String internalAuth = request.getHeader(InternalAuthConstant.INTERNAL_AUTH_HEADER);
        if (!StringUtils.hasText(internalAuthProperties.getAuthSecret())
                || !internalAuthProperties.getAuthSecret().equals(internalAuth)) {
            log.warn("服务间调用认证失败, path: {}", path);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
        return true;
    }

    private boolean validateGatewayRequest(HttpServletRequest request, HttpServletResponse response, String path) {
        String gatewayAuth = request.getHeader(GatewayAuthConstant.GATEWAY_AUTH_HEADER);
        String currentId = request.getHeader(JwtClaimsConstant.CURRENT_ID);

        if (!StringUtils.hasText(gatewayAuthProperties.getAuthSecret())) {
            log.warn("服务未配置网关认证密钥 u.gateway.auth-secret, path: {}", path);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
        if (!StringUtils.hasText(gatewayAuth)) {
            log.warn("请求缺少网关注入头 {}, path: {}", GatewayAuthConstant.GATEWAY_AUTH_HEADER, path);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
        if (!gatewayAuthProperties.getAuthSecret().equals(gatewayAuth)) {
            log.warn("网关注入头 {} 校验失败, path: {}", GatewayAuthConstant.GATEWAY_AUTH_HEADER, path);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
        if (!StringUtils.hasText(currentId)) {
            log.warn("请求缺少网关注入身份头 {}, path: {}", JwtClaimsConstant.CURRENT_ID, path);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }

        try {
            BaseContext.setCurrentId(Long.valueOf(currentId));
            BaseContext.setCurrentRole(request.getHeader(JwtClaimsConstant.ROLE));
            BaseContext.setCurrentSourceType(request.getHeader(JwtClaimsConstant.SOURCE_TYPE));
            return true;
        } catch (NumberFormatException ex) {
            log.warn("网关注入身份头格式异常: {}", currentId);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
    }

    private boolean isInternalPath(String path) {
        return path.startsWith("/inner/");
    }
}
