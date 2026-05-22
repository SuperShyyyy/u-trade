package com.gateway;

import com.u.common.constant.GatewayAuthConstant;
import com.u.common.constant.JwtClaimsConstant;
import com.u.common.properties.GatewayAuthProperties;
import com.u.common.properties.JwtProperties;
import com.u.common.utils.JwtUtil;
import com.u.gateway.filter.JwtAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GatewayApplicationTests {

    @Test
    void shouldInjectVerifiedIdentityHeadersForProtectedRequest() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setUserSecretKey("user-secret");
        jwtProperties.setAdminSecretKey("admin-secret");
        jwtProperties.setUserTokenName("token");
        jwtProperties.setAdminTokenName("token");

        GatewayAuthProperties gatewayAuthProperties = new GatewayAuthProperties();
        gatewayAuthProperties.setAuthSecret("gateway-secret");

        JwtAuthFilter filter = new JwtAuthFilter(jwtProperties, gatewayAuthProperties);

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.CURRENT_ID, 42L);
        claims.put(JwtClaimsConstant.ROLE, "ADMIN");
        claims.put(JwtClaimsConstant.SOURCE_TYPE, "USER");
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), 60_000, claims);

        MockServerHttpRequest request = MockServerHttpRequest.get("/user/me")
                .header("token", token)
                .header(JwtClaimsConstant.CURRENT_ID, "999")
                .header(GatewayAuthConstant.GATEWAY_AUTH_HEADER, "forged")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicReference<ServerHttpRequest> downstreamRequest = new AtomicReference<>();

        filter.filter(exchange, chainExchange -> {
            downstreamRequest.set(chainExchange.getRequest());
            return reactor.core.publisher.Mono.empty();
        }).block();

        HttpHeaders headers = downstreamRequest.get().getHeaders();
        assertNotNull(downstreamRequest.get());
        assertEquals("42", headers.getFirst(JwtClaimsConstant.CURRENT_ID));
        assertEquals("ADMIN", headers.getFirst(JwtClaimsConstant.ROLE));
        assertEquals("USER", headers.getFirst(JwtClaimsConstant.SOURCE_TYPE));
        assertEquals("gateway-secret", headers.getFirst(GatewayAuthConstant.GATEWAY_AUTH_HEADER));
        assertEquals(token, headers.getFirst("token"));
    }
}
