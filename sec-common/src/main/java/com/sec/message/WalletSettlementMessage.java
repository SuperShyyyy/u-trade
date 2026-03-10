package com.sec.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletSettlementMessage {
    private Long buyerId;
    private Long sellerId;
    private BigDecimal amount;
    private String orderNo;
}