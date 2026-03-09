package com.sec.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.sec.domain.dto.ItemSnapshotDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("订单详情VO")
public class OrderVO {

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

    @ApiModelProperty(value = "商品快照，保存下单时商品信息")
    private ItemSnapshotDTO itemSnapshot;

    @ApiModelProperty("收货人姓名")
    private String receiverName;

    @ApiModelProperty("收货人电话")
    private String receiverPhone;

    @ApiModelProperty("省")
    private String receiverProvince;

    @ApiModelProperty("市")
    private String receiverCity;

    @ApiModelProperty("区")
    private String receiverDistrict;

    @ApiModelProperty("详细地址")
    private String receiverAddress;

    @ApiModelProperty("物流公司")
    private String shipmentCompany;

    @ApiModelProperty("快递单号")
    private String trackingNo;

    @ApiModelProperty("发货时间")
    private LocalDateTime shippedAt;

    @ApiModelProperty("签收时间")
    private LocalDateTime deliveredAt;

    @ApiModelProperty("订单创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty("支付时间")
    private LocalDateTime paidAt;

    @ApiModelProperty("完成时间")
    private LocalDateTime completedAt;

    @ApiModelProperty("取消时间")
    private LocalDateTime cancelledAt;
}