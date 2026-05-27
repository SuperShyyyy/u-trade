package com.u.api.dto.item;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ItemTradeDTO {

    private Long itemId;

    private Long sellerId;

    private Long skuId;
    private String skuName;

    private String title;
    private String description;

    private BigDecimal price; // 成交价（核心）

    private List<String> images;

    private Long categoryId;
    private String categoryName;

    private Integer isFreeShipping;
    private BigDecimal shippingFee;

    private Map<String, Object> attributes;
}