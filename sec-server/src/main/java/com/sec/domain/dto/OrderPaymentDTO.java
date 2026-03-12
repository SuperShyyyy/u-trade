package com.sec.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("订单支付请求DTO")
public class OrderPaymentDTO {

    @ApiModelProperty("订单号 ")
    private String orderNo;

    @ApiModelProperty(" BALANCE=0 CARD = 1 WECHAT = 2 ALIPAY = 3;")
    private Integer payType;
}