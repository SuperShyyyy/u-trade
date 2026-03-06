package com.sec.domain.dto;

import lombok.Data;

@Data
public class LoginDTO {
    private String username; // 或 account, phone 等
    private String password;
}
