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
 * 退款记录表
 * </p>
 *
 * @author author
 * @since 2026-03-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("refund_record")
@ApiModel(value="RefundRecord对象", description="退款记录表")
public class RefundRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "退款用户ID（买家）")
    private Long userId;

    @ApiModelProperty(value = "关联交易ID")
    private Long transactionId;

    @ApiModelProperty(value = "退款金额")
    private BigDecimal amount;

    @ApiModelProperty(value = "退款状态：0 PROCESSING/1 SUCCESS/2 FAIL")
    private Integer status;

    @ApiModelProperty(value = "退款渠道：0 WALLET/ 1 BANK/ 2 ALIPAY/ 3 WECHAT")
    private Integer payChannel;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


}
