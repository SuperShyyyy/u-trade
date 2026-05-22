package com.u.admin.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 管理员操作日志表
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("admin_log")
@Schema(description = "管理员操作日志表")
public class AdminLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "执行操作的管理员ID")
    private Long adminId;

    @Schema(description = "操作对象ID (如商品ID/用户ID)")
    private String targetId;

    @Schema(description = "操作行为 (如: 审核通过/封禁用户)")
    private String action;

    @Schema(description = "具体操作描述/理由")
    private String content;

    @Schema(description = "管理员IP")
    private String ip;

    private LocalDateTime createTime;

}
