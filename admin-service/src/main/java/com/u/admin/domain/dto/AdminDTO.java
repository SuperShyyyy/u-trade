package com.u.admin.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "新增管理员参数对象")
public class AdminDTO {

    @Schema(description = "登录账号")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度需在4-20位之间")
    private String username;

    @Schema(description = "登录密码")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在6-32位之间")
    private String password;

    @Schema(description = "昵称")
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @Schema(description = "角色标识 (1-超级管理员, 2-普通管理员)")
    @NotNull(message = "角色不能为空")
    private Integer role;

    @Schema(description = "头像URL")
    private String avatar;
}
