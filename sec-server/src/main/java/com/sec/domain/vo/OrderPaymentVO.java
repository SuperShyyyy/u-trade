package com.sec.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("订单支付返回VO")
public class OrderPaymentVO {

    @ApiModelProperty("订单ID")
    private Long orderId;

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("支付金额")
    private String amount;

    @ApiModelProperty("支付方式")
    private Integer payType;

    @ApiModelProperty("支付状态: 0-未支付, 1-已支付, 2-支付失败")
    private Integer status;

    @ApiModelProperty("支付预交易单信息（可用于微信/支付宝前端调起支付）")
    private String prepayInfo;
}