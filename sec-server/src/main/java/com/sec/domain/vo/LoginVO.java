package com.sec.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginVO {
    private Long id;          // 当前登录者的 ID
    private String token;     // JWT 令牌
    private String role;      // 角色标识 (null, "1", "2")
    private String sourceType;// 来源 ("USER", "ADMIN")
}