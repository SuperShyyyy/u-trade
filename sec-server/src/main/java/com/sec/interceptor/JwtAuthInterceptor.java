package com.sec.interceptor;

import com.sec.constant.JwtClaimsConstant;
import com.sec.context.BaseContext;
import com.sec.properties.JwtProperties;
import com.sec.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class JwtAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String requestUri = request.getRequestURI();
        // 1. 静态资源与非控制器方法直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 2. 特殊路径硬放行 (针对文档数据接口的二次保险)
        if (requestUri.contains("/v3/api-docs") || requestUri.contains("/swagger-resources")) {
            return true;
        }

        // 3. 确定当前请求的 Token 策略
        String tokenHeaderName;
        String secretKey;
        boolean isAdminPath = false;

        if (requestUri.startsWith("/admin") || requestUri.startsWith("/manager")) {
            tokenHeaderName = jwtProperties.getAdminTokenName();
            secretKey = jwtProperties.getAdminSecretKey();
            isAdminPath = true;
        } else {
            tokenHeaderName = jwtProperties.getUserTokenName();
            secretKey = jwtProperties.getUserSecretKey();
        }

        // 4. 校验令牌
        String token = request.getHeader(tokenHeaderName);
        if (token == null || token.trim().isEmpty()) {
            log.warn("未检测到有效令牌: {}, Path: {}", tokenHeaderName, requestUri);
            response.setStatus(401);
            return false;
        }

        try {
            Claims claims = JwtUtil.parseJWT(secretKey, token);

            // 获取并校验 ID
            Object idObj = claims.get(JwtClaimsConstant.CURRENT_ID);
            if (idObj == null) {
                response.setStatus(401);
                return false;
            }
            Long currentId = Long.valueOf(idObj.toString());

            // 获取角色与来源
            String role = claims.get(JwtClaimsConstant.ROLE, String.class);
            String sourceType = claims.get(JwtClaimsConstant.SOURCE_TYPE, String.class);

            // 权限校验：如果是管理员路径，角色不能为空
            if (isAdminPath && (role == null || "".equals(role))) {
                log.warn("越权尝试: 非管理员角色访问 {}", requestUri);
                response.setStatus(403);
                return false;
            }

            // 5. 存入上下文
            BaseContext.setCurrentId(currentId);
            BaseContext.setCurrentRole(role);
            BaseContext.setCurrentSourceType(sourceType);

            return true;
        } catch (Exception ex) {
            log.error("JWT 解析失败: {}", ex.getMessage());
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        BaseContext.remove();
    }
}