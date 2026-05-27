package com.u.common.message;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * 商品快照模型：记录下单瞬间的商品核心信息
 */
@Data
public class ItemSnapshotDTO {

    private Long itemId;          // 商品ID（用于追溯）
    private String title;         // 下单时商品标题
    private String description;   // 简要描述

    // ======================
    // 价格信息（必须）
    // ======================

    private BigDecimal price;     // 下单单价（核心）
    private String currency;      // 币种（可选：CNY）

    // ======================
    // 图片信息（必须）
    // ======================

    private List<String> images;  // 图片列表（第一张做主图）

    // ======================
    // SKU信息（很重要）
    // ======================

    private Long skuId;           // SKU ID
    private String skuName;       // SKU名称（如：黑色 / XL）

    // ======================
    // 类目信息（可选但推荐）
    // ======================

    private Long categoryId;
    private String categoryName;

    // ======================
    // 商家信息（可选）
    // ======================

    private Long sellerId;
    private String sellerName;

    // ======================
    // 扩展信息（避免以后改库）
    // ======================

    private Map<String, Object> attributes;
    /*

    private Long itemId;      // 商品原始ID

    private String title;       // 下单时的标题

    private String description; // 下单时的详细描述 (维权关键)

    private BigDecimal price;   // 下单时的单价

    private List<String> images; // 下单时的图集 (List)

    */
}
