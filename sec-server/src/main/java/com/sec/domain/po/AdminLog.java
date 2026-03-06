package com.sec.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

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
@ApiModel(value="AdminLog对象", description="管理员操作日志表")
public class AdminLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "执行操作的管理员ID")
    private Long adminId;

    @ApiModelProperty(value = "操作对象ID (如商品ID/用户ID)")
    private String targetId;

    @ApiModelProperty(value = "操作行为 (如: 审核通过/封禁用户)")
    private String action;

    @ApiModelProperty(value = "具体操作描述/理由")
    private String content;

    @ApiModelProperty(value = "管理员IP")
    private String ip;

    private LocalDateTime createTime;


}
