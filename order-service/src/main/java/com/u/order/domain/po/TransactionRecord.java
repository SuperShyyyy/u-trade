package com.u.order.domain.po;

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
 * C2C交易记录表
 * </p>
 *
 * @author author
 * @since 2026-03-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("transaction_record")
@Schema(name="TransactionRecord对象", description="C2C交易记录表")
public class TransactionRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "买家ID")
    private Long buyerId;

    @Schema(description = "卖家ID")
    private Long sellerId;

    @Schema(description = "C2C订单ID")
    private Long orderId;

    @Schema(description = "支付金额")
    private BigDecimal amount;

    @Schema(description = "支付类型：0 WALLET/ 1 CARD / 2 ALIPAY / 3 WECHAT")
    private Integer payType;

    @Schema(description = "交易状态： 0 PENDING/ 1 SUCCESS/2 FAIL")
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


}
