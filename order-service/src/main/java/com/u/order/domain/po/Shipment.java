package com.u.order.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 物流发货信息表
 * </p>
 *
 * @author author
 * @since 2026-03-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("shipment")
@Schema(name="Shipment对象", description="物流发货信息表")
public class Shipment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "关联订单ID")
    private Long orderId;

    @Schema(description = "关联卖家id")
    private Long sellerId;

    @Schema(description = "物流公司名称 (如: 顺丰, 邮政, 或者是 '线下自提')")
    private String company;

    @Schema(description = "快递单号")
    private String trackingNo;

    @Schema(description = "收货人姓名快照")
    private String receiverName;

    @Schema(description = "收货人手机号快照")
    private String receiverPhone;

    @Schema(description = "收货省份快照")
    private String receiverProvince;

    @Schema(description = "收货城市快照")
    private String receiverCity;

    @Schema(description = "收货区/县快照")
    private String receiverDistrict;

    @Schema(description = "收货详细地址快照（街道/门牌号）")
    private String receiverAddress;

    @Schema(description = "物流状态: 0-待发货, 1-已发货, 2-已完成")
    private Integer status;

    @Schema(description = "发货时间")
    private LocalDateTime shippedAt;

    @Schema(description = "签收时间")
    private LocalDateTime deliveredAt;

    @Schema(description = "记录创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "信息更新时间")
    private LocalDateTime updateTime;


}
