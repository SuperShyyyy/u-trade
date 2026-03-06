package com.sec.domain.po;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 订单主表
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("order")
@ApiModel(value="Order对象", description="订单主表")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "订单号")
    private String orderNo;

    private Long buyerId;

    private Long sellerId;

    private Long itemId;

    private BigDecimal totalAmount;

    @ApiModelProperty(value = "0-待支付, 1-待发货, 2-待收货, 3-成功, 4-关闭, 5-退款中")
    private Integer status;

    @ApiModelProperty(value = "退款状态: 0-无, 1-申请中, 2-已退款, 3-退款拒绝")
    private Integer refundStatus;

    @ApiModelProperty(value = "退款原因")
    private String refundReason;

    private LocalDateTime payTime;

    private LocalDateTime closeTime;

    @ApiModelProperty(value = "收货信息快照")
    private String receiverInfo;

    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


}
