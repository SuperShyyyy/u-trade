package com.u.item.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemDTO {

    @Schema(description = "商品ID（更新时传入，新增时不传）")
    private Long itemId;

    @NotBlank(message = "商品标题不能为空")
    @Size(max = 100, message = "商品标题最长100字")
    private String title;

    private String description;

    @NotNull(message = "商品分类不能为空")
    private Long categoryId;

    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于0")
    private BigDecimal originalPrice;

    private String[] images;

    @Schema(description = "是否包邮", example = "1")
    private Integer isFreeShipping = 1;

    @Schema(description = "邮费", example = "0.00")
    private BigDecimal shippingFee = BigDecimal.ZERO;
}
