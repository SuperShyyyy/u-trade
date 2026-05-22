package com.u.order.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "下单返回VO")
public class OrderSubmitVO {

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "买家ID")
    private Long buyerId;

    @Schema(description = "卖家ID")
    private Long sellerId;

    @Schema(description = "商品ID")
    private Long itemId;

    @Schema(description = "商品单价")
    private BigDecimal price;

    @Schema(description = "购买数量")
    private Integer quantity;

    @Schema(description = "总价")
    private BigDecimal totalPrice;

    @Schema(description = "订单状态")
    private Integer status;

    @Schema(description = "商品快照信息")
    private String itemSnapshot;

    @Schema(description = "订单创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "支付时间")
    private LocalDateTime paidAt;

    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    @Schema(description = "取消时间")
    private LocalDateTime cancelledAt;
}
