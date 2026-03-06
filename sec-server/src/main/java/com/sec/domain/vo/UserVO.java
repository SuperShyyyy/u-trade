package com.sec.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ApiModel(description = "个人信息展示对象")
public class UserVO implements Serializable {

    @ApiModelProperty("用户ID")
    private Long id;

    @ApiModelProperty("用户名/昵称")
    private String username;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("头像地址")
    private String avatar;

    @ApiModelProperty("信用分")
    private Integer creditScore;

    @ApiModelProperty("状态: 1-正常, 0-禁用")
    private Integer status;

    // 扩展字段：通常前端在“我的”页面需要看到钱包信息
    @ApiModelProperty("账户余额")
    private BigDecimal balance;

    @ApiModelProperty("注册时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}