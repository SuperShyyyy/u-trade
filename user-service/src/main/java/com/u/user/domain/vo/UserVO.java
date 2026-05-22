package com.u.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "个人信息展示对象")
public class UserVO implements Serializable {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名/昵称")
    private String username;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "信用分")
    private Integer creditScore;

    @Schema(description = "状态: 1-正常, 0-禁用")
    private Integer status;

    @Schema(description = "账户余额")
    private BigDecimal balance;

    @Schema(description = "注册时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
