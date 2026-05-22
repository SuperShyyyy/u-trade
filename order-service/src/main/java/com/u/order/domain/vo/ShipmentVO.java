package com.u.order.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShipmentVO {

    private Long orderId;

    private Long sellerId;

    private String company;

    private String trackingNo;

    @Schema(description = "物流状态: 0-待发货, 1-已发货, 2-已完成")
    private Integer status;

    @Schema(description = "发货时间")
    private LocalDateTime shippedAt;

    @Schema(description = "签收时间")
    private LocalDateTime deliveredAt;

}
