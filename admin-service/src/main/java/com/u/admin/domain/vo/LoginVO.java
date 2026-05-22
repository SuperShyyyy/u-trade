package com.u.admin.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginVO {
    private Long id;
    private String token;
    private String role;
    private String sourceType;
}
