package com.u.order.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "用户下单请求DTO")
public class OrderSubmitDTO {

    @Schema(description = "商品ID")
    @NotNull(message = "商品ID不能为空")
    private Long itemId;

    @Schema(description = "卖家ID")
    private Long sellerId;

    @Schema(description = "收货地址ID，可选")
    private Long addressId;
}
