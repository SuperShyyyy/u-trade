package com.sec.domain.po;

import java.math.BigDecimal;
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
 * 订单退款维权表
 * </p>
 *
 * @author author
 * @since 2026-03-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("refund")
@ApiModel(value="Refund对象", description="订单退款维权表")
public class Refund implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "关联订单ID")
    private Long orderId;

    @ApiModelProperty(value = "外部显示的退款单号 (业务编号)")
    private String refundNo;

    @ApiModelProperty(value = "申请退款金额 (不得超过订单实付金额)")
    private BigDecimal amount;

    @ApiModelProperty(value = "退款类型: 1-仅退款, 2-退货退款")
    private Integer type;

    @ApiModelProperty(value = "退款原因说明")
    private String reason;

    @ApiModelProperty(value = "退款凭证图片地址列表 (JSON 数组)")
    private String evidenceImages;

    @ApiModelProperty(value = "状态: 0-待卖家处理, 1-卖家拒绝, 2-待买家退货, 3-退款完成, 4-客服介入")
    private Integer status;

    @ApiModelProperty(value = "支付平台 (支付宝/微信) 退款流水号")
    private String outRefundNo;

    @ApiModelProperty(value = "退款发起时间")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "卖家/系统处理完成时间")
    private LocalDateTime processedAt;

    @ApiModelProperty(value = "卖家拒绝退款的原因")
    private String rejectReason;


}
