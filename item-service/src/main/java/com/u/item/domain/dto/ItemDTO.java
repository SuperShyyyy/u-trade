package com.u.item.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "是否包邮", example = "1")
    private Integer isFreeShipping = 1;

    @Schema(description = "邮费", example = "0.00")
    private BigDecimal shippingFee = BigDecimal.ZERO;
}
