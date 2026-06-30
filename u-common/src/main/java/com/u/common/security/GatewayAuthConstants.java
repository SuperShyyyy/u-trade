package com.u.common.security;

import java.util.List;

public final class GatewayAuthConstants {

    public static final String TOKEN_HEADER = "token";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String GATEWAY_AUTH_HEADER = "X-Gateway-Auth";
    public static final String GATEWAY_AUTH_VERIFIED = "verified";
    public static final String ADMIN_PATH_PREFIX = "/admin";
    public static final String MANAGER_PATH_PREFIX = "/manager";
    public static final List<String> DEFAULT_WHITE_LIST = List.of(
            "/user/login",
            "/admin/login",
            "/user/register",
            "/home/recommend",
            "/ws/**"
    );

    private GatewayAuthConstants() {
    }
}
