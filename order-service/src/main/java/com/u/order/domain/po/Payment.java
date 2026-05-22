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
 * 支付表
 * </p>
 *
 * @author author
 * @since 2026-03-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("payment")
@Schema(name="Payment对象", description="支付表")
public class Payment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "支付ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "关联订单ID")
    private String orderNo;

    @Schema(description = "支付流水号")
    private String paymentNo;

    @Schema(description = "付款人ID")
    private Long userId;

    @Schema(description = "支付金额")
    private BigDecimal amount;

    @Schema(description = " BALANCE = 0" + "CARD = 1;" +" WECHAT = 2 " + " ALIPAY = 3; ")
    private Integer method;

    @Schema(description = "支付状态: 0-未支付, 1-成功, 2-失败")
    private Integer status;

    @Schema(description = "第三方交易号")
    private String transactionId;

    @Schema(description = "实际支付时间")
    private LocalDateTime paidAt;

    @Schema(description = "记录创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "记录更新时间")
    private LocalDateTime updateTime;


}
