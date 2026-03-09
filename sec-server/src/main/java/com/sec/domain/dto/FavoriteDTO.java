package com.sec.domain.dto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("新增收藏请求")
public class FavoriteDTO {

    @ApiModelProperty("商品ID")
    private Long itemId;

}