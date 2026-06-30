package com.u.order.domain.po;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;
import java.io.Serializable;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.u.common.message.ItemSnapshotDTO;
import io.swagger.v3.oas.annotations.media.Schema;
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
@TableName(value = "`order`", autoResultMap = true)
@Schema(name="Order对象", description="订单表")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableLogic
    @TableField("is_deleted")
    @Schema(description = "是否删除 0未删除 1已删除")
    private Integer isDeleted;

    @Schema(description = "订单ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_no")
    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "买家ID")
    private Long buyerId;

    @Schema(description = "卖家ID")
    private Long sellerId;

    @Schema(description = "商品ID")
    private Long itemId;

    @Schema(description = "商品单价（下单时价格）")
    private BigDecimal price;

    @Schema(description = "购买数量")
    private Integer quantity;

    @Schema(description = "总价 = price * quantity")
    private BigDecimal totalPrice;

    @Schema(description = "订单状态 0待付款 1已付款 2已发货 3已完成 4已取消 5退款中 6已退款")
    private Integer status;

    @Schema(description = "订单创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "支付时间")
    private LocalDateTime paidAt;

    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    @Schema(description = "取消时间")
    private LocalDateTime cancelledAt;

    //地址
    private String receiverName;
    private String receiverPhone;
    private String receiverProvince;
    private String receiverCity;
    private String receiverDistrict;
    private String receiverAddress;


    //物流
   /* private Integer isFreeShipping;  //是否包邮
   */
    private BigDecimal shippingFee;

    //关联物流号
    private Long shipmentId;

  /*  private String logisticsCompany; //物流公司
    private String trackingNumber;   //单号
*/
    private String paymentMethod;

    /**
     * 这里建议使用 MyBatis-Plus 的 TypeHandler
     * 自动将数据库的 JSON 字符串转为 Java 对象
     */
    @Schema(description = "商品快照，保存下单时商品信息")
    @TableField(value = "item_snapshot", typeHandler = JacksonTypeHandler.class)
    private ItemSnapshotDTO itemSnapshot;

    private LocalDateTime updatedAt;

    @Version
    @Schema(description = "乐观锁版本号")
    private Integer version;

    @Schema(description = "发货时间")
    private LocalDateTime shippedAt;

    @Schema(description = "退款原因 0 超时未支付 1 用户取消 2 商家原因")
    private String cancelReason;
}
