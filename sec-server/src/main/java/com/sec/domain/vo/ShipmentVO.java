package com.sec.domain.vo;

import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;

public class ShipmentVO {

    private Long orderId;

    private Long sellerId;

    private String company;

    private String trackingNo;

    @ApiModelProperty(value = "物流状态: 0-待发货, 1-已发货, 2-已完成")
    private Integer status;

    @ApiModelProperty(value = "发货时间")
    private LocalDateTime shippedAt;

    @ApiModelProperty(value = "签收时间")
    private LocalDateTime deliveredAt;

}
