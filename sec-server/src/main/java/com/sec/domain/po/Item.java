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
 * 商品表
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("item")
@ApiModel(value="Items对象", description="商品表")
public class Item implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "卖家ID (隔离关键字段)")
    private Long sellerId;

    private String title;

    private String description;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Long categoryId;

    private String cover;

    @ApiModelProperty(value = "图片URL列表，逗号分隔")
    private String images;

    @ApiModelProperty(value = "2-锁定 1-上架, 0-下架, -1-已售, -2-审核中")
    private Integer status;

    @ApiModelProperty(value = "审核状态: 0-待审核, 1-通过, -1-拒绝")
    private Integer auditStatus;

    @ApiModelProperty(value = "审核拒绝理由")
    private String rejectReason;

    private Integer viewCount;

    private Integer wantCount;

    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isFreeShipping;  //是否包邮 0不包邮 1包邮

    private BigDecimal shippingFee; //不包邮 邮费

}
