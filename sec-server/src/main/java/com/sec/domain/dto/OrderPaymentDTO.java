package com.sec.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("订单支付请求DTO")
public class OrderPaymentDTO {

    @ApiModelProperty("订单ID")
    private Long orderId;

    @ApiModelProperty("支付方式: 1-微信, 2-支付宝, 3-余额等")
    private Integer payType;
}