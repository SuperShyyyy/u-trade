package com.sec.domain.po;

import java.math.BigDecimal;
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
 * 用户钱包表
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_wallet")
@ApiModel(value="UserWallet对象", description="用户钱包表")
public class UserWallet implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    @ApiModelProperty(value = "可用余额")
    private BigDecimal balance;

    @ApiModelProperty(value = "状态。0为禁用，1为可用。默认为1")
    private Integer status;

    @ApiModelProperty(value = "冻结金额")
    private BigDecimal frozenAmount;

    private BigDecimal totalIncome;

    private BigDecimal totalExpense;

    @ApiModelProperty(value = "乐观锁版本号")
    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;



}
