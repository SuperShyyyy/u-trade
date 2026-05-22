package com.u.item.domain.vo;


import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ItemVO implements Serializable {

    private Long id;                 // 商品ID
    private Long sellerId;
    private String title;            // 标题
    private String description;      // 描述
    private Long categoryId;         // 分类ID
    private BigDecimal originalPrice;// 原价
    private BigDecimal price;        // 当前价格
    private String cover;            // 封面图片
    private List<String> images;     // 图片列表
    private Integer status;          // 商品状态（0下架 1上架 2审核�?-1已售出）
    private Integer auditStatus;     // 审核状�?(0待审�?1审核通过 2拒绝)
    private LocalDateTime createTime;// 创建时间
    private LocalDateTime updateTime;
    // 补充推荐所需字段
    private Long viewCount;       // 浏览�?
    private Long wantCount;       // 收藏量（想要人数�?
    private Integer isFreeShipping;  // 是否包邮
    private BigDecimal shippingFee;  // 运费
    private Integer sellerCreditScore;
}