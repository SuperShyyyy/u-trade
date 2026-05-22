package com.u.user.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户表
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("`user`")
@Schema(description = "用户表")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "加密后的密码 (BCrypt)")
    private String password;

    @Schema(description = "手机号 (登录账号)")
    private String phone;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "信用分")
    private Integer creditScore;

    @Schema(description = "状态: 1-正常, 0-禁用")
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


}
