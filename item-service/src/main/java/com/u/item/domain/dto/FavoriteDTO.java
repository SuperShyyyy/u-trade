package com.u.item.domain.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
@NotNull(message = "itemId不能为空")
@Schema(description = "新增收藏请求")
public class FavoriteDTO {
    @Schema(description = "商品ID")
    private Long itemId;
}
