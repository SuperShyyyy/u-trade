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
 * 充值记录表
 * </p>
 *
 * @author author
 * @since 2026-03-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wallet_recharge")
@Schema(description = "充值记录表")
public class WalletRecharge implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "充值金额")
    private BigDecimal amount;

    @Schema(description = "充值流水号，唯一")
    private String bizOrderNo;

    @Schema(description = "充值状态：0 WAIT_PAY/ 1 SUCCESS/ 2 FAIL")
    private Integer status;

    @Schema(description = "支付渠道：0 CARD /1 ALIPAY/2 WECHAT")
    private Integer payChannel;

    @Schema(description = "支付成功时间")
    private LocalDateTime payTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


}
