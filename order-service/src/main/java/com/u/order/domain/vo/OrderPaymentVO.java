package com.u.order.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "订单支付返回VO")
public class OrderPaymentVO {

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "支付金额")
    private String amount;

    @Schema(description = "支付方式")
    private Integer payType;

    @Schema(description = "支付状态: 0-未支付, 1-已支付, 2-支付失败")
    private Integer status;

    @Schema(description = "支付预交易单信息（可用于微信/支付宝前端调起支付）")
    private String prepayInfo;
}
