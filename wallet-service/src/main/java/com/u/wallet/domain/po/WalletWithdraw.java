package com.u.wallet.domain.po;

import java.math.BigDecimal;
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
 * 提现记录表
 * </p>
 *
 * @author author
 * @since 2026-03-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wallet_withdraw")
@Schema(description = "提现记录表")
public class WalletWithdraw implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "提现金额")
    private BigDecimal amount;

    @Schema(description = "提现流水号，唯一")
    private String bizOrderNo;

    @Schema(description = "提现状态：0 WAIT_WITHDRAW /1 PROCESSING/ 2 SUCCESS/3 FAIL")
    private Integer status;

    @Schema(description = "提现渠道 : 0 CARD /1 ALIPAY/2 WECHAT")
    private Integer payChannel;

    @Schema(description = "到账时间")
    private LocalDateTime payTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


}
