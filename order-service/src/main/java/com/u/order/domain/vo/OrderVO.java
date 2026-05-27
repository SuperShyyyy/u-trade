package com.u.order.domain.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.u.common.message.ItemSnapshotDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name = "订单详情VO")
public class OrderVO implements Serializable {


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

    @Schema(description = "商品标题")
    private String itemTitle;

    @Schema(description = "商品描述")
    private String itemDescription;

    @Schema(description = "商品图片")
    private String itemImage;

    @Schema(description = "商品单价")
    private BigDecimal price;

    @Schema(description = "购买数量")
    private Integer quantity;

    @Schema(description = "总价")
    private BigDecimal totalPrice;

    @Schema(description = "订单状态")
    private Integer status;

    @Schema(description = "商品快照，保存下单时商品信息")
    private ItemSnapshotDTO itemSnapshot;

    @Schema(description = "物流号 关联shipmen表")
    private Long shipmentId;

    @Schema(description = "收货人姓名")
    private String receiverName;

    @Schema(description = "收货人电话")
    private String receiverPhone;

    @Schema(description = "省")
    private String receiverProvince;

    @Schema(description = "市")
    private String receiverCity;

    @Schema(description = "区")
    private String receiverDistrict;

    @Schema(description = "详细地址")
    private String receiverAddress;

    @Schema(description = "发货时间")
    private LocalDateTime shippedAt;

    @Schema(description = "签收时间")
    private LocalDateTime deliveredAt;

    @Schema(description = "订单创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "支付时间")
    private LocalDateTime paidAt;

    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    @Schema(description = "取消时间")
    private LocalDateTime cancelledAt;
}
