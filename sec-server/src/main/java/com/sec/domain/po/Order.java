package com.sec.domain.po;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;
import java.io.Serializable;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.sec.domain.dto.ItemSnapshotDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 订单表
 * </p>
 *
 * @author author
 * @since 2026-03-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("`order`")
@ApiModel(value="Order对象", description="订单表")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "订单ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_no")
    @ApiModelProperty(value = "订单号")
    private String orderNo;

    @ApiModelProperty(value = "买家ID")
    private Long buyerId;

    @ApiModelProperty(value = "卖家ID")
    private Long sellerId;

    @ApiModelProperty(value = "商品ID")
    private Long itemId;

    @ApiModelProperty(value = "商品单价（下单时价格）")
    private BigDecimal price;

    @ApiModelProperty(value = "购买数量")
    private Integer quantity;

    @ApiModelProperty(value = "总价 = price * quantity")
    private BigDecimal totalPrice;

    @ApiModelProperty(value = "订单状态 0待付款 1已付款 2已发货 3已完成 4已取消 5退款中 6已退款")
    private Integer status;

    @ApiModelProperty(value = "订单创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "支付时间")
    private LocalDateTime paidAt;

    @ApiModelProperty(value = "完成时间")
    private LocalDateTime completedAt;

    @ApiModelProperty(value = "取消时间")
    private LocalDateTime cancelledAt;

    private String receiverName;
    private String receiverPhone;
    private String receiverProvince;
    private String receiverCity;
    private String receiverDistrict;
    private String receiverAddress;


    private BigDecimal shippingFee;
    private String paymentMethod;

    /**
     * 这里建议使用 MyBatis-Plus 的 TypeHandler
     * 自动将数据库的 JSON 字符串转为 Java 对象
     */
    @ApiModelProperty(value = "商品快照，保存下单时商品信息")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private ItemSnapshotDTO itemSnapshot;

    private LocalDateTime updatedAt;

    @Version // MyBatis-Plus 自动处理版本号
    private Integer version;

    @ApiModelProperty("发货时间")
    private LocalDateTime shippedAt;

    @ApiModelProperty("退款原因 0 超时未支付 1 用户取消 2 商家原因")
    private String cancelReason;
}
