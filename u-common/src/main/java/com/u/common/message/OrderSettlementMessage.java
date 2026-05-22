package com.u.common.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderSettlementMessage {
    private Long buyerId;
    private Long sellerId;
    private BigDecimal amount;
    private String orderNo;
    private String messageId;  // 新增字段
    private Long timestamp;    // 新增时间戳

    public OrderSettlementMessage(Long buyerId, Long sellerId, BigDecimal amount, String orderNo) {
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.amount = amount;
        this.orderNo = orderNo;
        this.messageId = null; // 发送时由 sender 重新生成
    }

}
