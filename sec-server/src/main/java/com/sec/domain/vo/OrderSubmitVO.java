package com.sec.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("下单返回VO")
public class OrderSubmitVO {

    @ApiModelProperty("订单ID")
    private Long id;

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("买家ID")
    private Long buyerId;

    @ApiModelProperty("卖家ID")
    private Long sellerId;

    @ApiModelProperty("商品ID")
    private Long itemId;

    @ApiModelProperty("商品单价")
    private BigDecimal price;

    @ApiModelProperty("购买数量")
    private Integer quantity;

    @ApiModelProperty("总价")
    private BigDecimal totalPrice;

    @ApiModelProperty("订单状态")
    private Integer status;

    @ApiModelProperty("商品快照信息")
    private String itemSnapshot;

    @ApiModelProperty("订单创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty("支付时间")
    private LocalDateTime paidAt;

    @ApiModelProperty("完成时间")
    private LocalDateTime completedAt;

    @ApiModelProperty("取消时间")
    private LocalDateTime cancelledAt;
}