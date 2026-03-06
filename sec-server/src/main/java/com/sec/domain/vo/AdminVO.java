package com.sec.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private Integer role; // 确保是 Integer 或者是 String，需与 Admin 实体一致
    private Integer status;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private LocalDateTime createTime;
}