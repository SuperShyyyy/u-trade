package com.u.item.domain.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

/**
 * 商品快照模型：记录下单瞬间的商品核心信息
 */
@Data
public class ItemSnapshotDTO {

    private Long itemId;        // 商品原始ID

    private String title;       // 下单时的标题

    private String description; // 下单时的详细描述 (维权关键)

    private BigDecimal price;   // 下单时的单价

    private List<String> images; // 下单时的图集 (List)
}