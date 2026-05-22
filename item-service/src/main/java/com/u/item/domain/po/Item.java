package com.u.item.domain.po;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 商品�?
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("item")
@Schema(description = "商品�?")
public class Item implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "卖家ID (隔离关键字段)")
    private Long sellerId;

    private String title;

    private String description;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Long categoryId;

    private String cover;

    @Schema(description = "图片URL列表，逗号分隔")
    private String images;

    @Schema(description = "2-锁定 1-上架, 0-下架, -1-已售, -2-审核�?")
    private Integer status;

    @Schema(description = "审核状�? 0-待审�? 1-通过, -1-拒绝")
    private Integer auditStatus;

    @Schema(description = "审核拒绝理由")
    private String rejectReason;

    private Long viewCount;

    private Long wantCount;

    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isFreeShipping;  //是否包邮 0不包�?1包邮

    private BigDecimal shippingFee; //不包�?邮费


    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Integer sellerCreditScore;
}
