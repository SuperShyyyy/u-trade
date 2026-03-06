package com.sec.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ApiModel(description = "用户收藏列表展示对象")
public class FavoritesVO implements Serializable {

    @ApiModelProperty("收藏记录ID")
    private Long id;

    @ApiModelProperty("商品ID")
    private Long itemId;

    @ApiModelProperty("商品名称")
    private String itemName;

    @ApiModelProperty("商品主图地址")
    private String itemImage;

    @ApiModelProperty("商品当前价格")
    private BigDecimal itemPrice;

    @ApiModelProperty("商品状态: 1-正常, 0-下架")
    private Integer itemStatus;

    @ApiModelProperty("收藏时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}