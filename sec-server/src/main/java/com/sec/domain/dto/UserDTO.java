package com.sec.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "用户信息修改请求对象")
public class UserUpdateDTO implements Serializable {

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("头像URL")
    private String avatar;

    @ApiModelProperty("手机号")
    private String phone;
}