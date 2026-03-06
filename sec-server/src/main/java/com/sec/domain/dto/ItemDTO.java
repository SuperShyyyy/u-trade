package com.sec.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemDTO {

    private String title;
    private String description;
    private Long categoryId;
    private BigDecimal originalPrice;
    private String[] images;

}