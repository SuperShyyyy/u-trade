package com.u.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "用户收藏列表展示对象")
public class FavoriteVO implements Serializable {

    @Schema(description = "收藏记录ID")
    private Long id;

    @Schema(description = "商品ID")
    private Long itemId;

    @Schema(description = "商品名称")
    private String itemName;

    @Schema(description = "商品主图地址")
    private String itemImage;

    @Schema(description = "商品当前价格")
    private BigDecimal itemPrice;

    @Schema(description = "商品状态: 1-正常, 0-下架")
    private Integer itemStatus;

    @Schema(description = "收藏时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}
