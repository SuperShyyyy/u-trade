package com.u.order.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "订单支付请求DTO")
public class OrderPaymentDTO {

    @Schema(description = "订单号 ")
    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @Schema(description = " BALANCE=0 CARD = 1 WECHAT = 2 ALIPAY = 3;")
    @NotNull(message = "支付方式不能为空")
    private Integer payType;
}
