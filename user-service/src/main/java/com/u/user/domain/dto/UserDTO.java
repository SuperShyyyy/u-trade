package com.u.user.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "用户信息修改请求对象")
public class UserDTO implements Serializable {

    @Schema(description = "用户名")
    @Size(min = 2, max = 20, message = "用户名长度需在2-20位之间")
    private String username;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
