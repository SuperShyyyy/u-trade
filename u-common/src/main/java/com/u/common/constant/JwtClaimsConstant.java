package com.u.common.constant;

/**
 * JWT Claims 常量
 * <pre>
 * Payload 结构：
 *   currentId   — 用户/管理员 ID
 *   role        — 角色（null=普通用户 / 1=普通管理员 / 2=超级管理员）
 *   sourceType  — 来源（USER / ADMIN）
 *   sessionId   — 会话标识（单设备登录控制，UUID）
 *   tokenVersion— 令牌版本（logout/改密时 +1，旧 token 全部失效）
 *   exp         — 过期时间（JJWT 自动生成）
 * </pre>
 */
public class JwtClaimsConstant {

    public static final String CURRENT_ID = "currentId";

    public static final String ROLE = "role";

    public static final String SOURCE_TYPE = "sourceType";

    /** 会话标识，用于单设备登录互踢 */
    public static final String SESSION_ID = "sessionId";

    /** 令牌版本，logout / 改密 / 封禁时递增 */
    public static final String TOKEN_VERSION = "tokenVersion";
}
