package com.sec.domain.vo;


import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ItemVO {

    private Long id;                 // 商品ID
    private Long sellerId;
    private String title;            // 标题
    private String description;      // 描述
    private Long categoryId;         // 分类ID
    private BigDecimal originalPrice;// 原价
    private BigDecimal Price;// 原价
    private String cover;            // 封面图片
    private List<String> images;     // 图片列表
    private Integer status;          // 商品状态（0下架 1上架 2审核中 -1已售出）
    private Integer auditStatus;     // 审核状态 (0待审核 1审核通过 2拒绝)
    private LocalDateTime createTime;// 创建时间
}