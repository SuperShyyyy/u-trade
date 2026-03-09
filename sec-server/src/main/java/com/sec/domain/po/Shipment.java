package com.sec.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel(value="Shipment对象", description="物流发货信息表")
public class Shipment implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "关联订单ID")
    private Long orderId;

    @ApiModelProperty(value = "物流公司名称 (如: 顺丰, 邮政, 或者是 '线下自提')")
    private String company;

    @ApiModelProperty(value = "快递单号")
    private String trackingNo;

    @ApiModelProperty(value = "收货人姓名快照")
    private String receiverName;

    @ApiModelProperty(value = "收货人手机号快照")
    private String receiverPhone;

    @ApiModelProperty(value = "收货省份快照")
    private String receiverProvince;

    @ApiModelProperty(value = "收货城市快照")
    private String receiverCity;

    @ApiModelProperty(value = "收货区/县快照")
    private String receiverDistrict;

    @ApiModelProperty(value = "收货详细地址快照（街道/门牌号）")
    private String receiverAddress;

    @ApiModelProperty(value = "物流状态: 0-待发货, 1-已发货, 2-已完成")
    private Integer status;

    @ApiModelProperty(value = "发货时间")
    private LocalDateTime shippedAt;

    @ApiModelProperty(value = "妥投/签收时间")
    private LocalDateTime deliveredAt;

    @ApiModelProperty(value = "记录创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "信息更新时间")
    private LocalDateTime updateTime;


}
