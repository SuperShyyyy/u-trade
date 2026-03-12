package com.sec.domain.dto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
@NotNull(message = "itemId不能为空")
@ApiModel("新增收藏请求")
public class FavoriteDTO {
    @ApiModelProperty("商品ID")
    private Long itemId;
}