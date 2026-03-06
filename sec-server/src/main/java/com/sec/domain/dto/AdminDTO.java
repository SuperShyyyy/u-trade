package com.sec.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@ApiModel(description = "新增管理员参数对象")
public class AdminDTO {

    @ApiModelProperty(value = "登录账号", required = true)
    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度需在4-20位之间")
    private String username;

    @ApiModelProperty(value = "登录密码", required = true)
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在6-32位之间")
    private String password;

    @ApiModelProperty(value = "昵称", required = true)
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @ApiModelProperty(value = "角色标识 (1-超级管理员, 2-普通管理员)", required = true)
    @NotNull(message = "角色不能为空")
    private Integer role;

    @ApiModelProperty(value = "头像URL")
    private String avatar;
}