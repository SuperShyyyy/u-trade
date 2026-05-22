package com.u.user.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "用户信息修改请求对象")
public class UserDTO implements Serializable {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "手机号")
    private String phone;
}
