package com.sec.domain.dto;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("用户下单请求DTO")
public class OrderSubmitDTO {

    @ApiModelProperty("商品ID")
    private Long itemId;

    @ApiModelProperty("购买数量")
    private Integer quantity;

    @ApiModelProperty("卖家ID")
    private Long sellerId;

    @ApiModelProperty("下单时单价（可选，如果前端不传可以后台读取商品当前价格）")
    private BigDecimal price;

    @ApiModelProperty("收货地址ID，可选")
    private Long addressId;

    @ApiModelProperty("商品快照")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private ItemSnapshotDTO itemSnapshot;

}