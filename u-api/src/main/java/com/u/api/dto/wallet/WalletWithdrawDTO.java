package com.u.api.dto.wallet;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletWithdrawDTO {
    @NotNull(message = "提现金额不能为空")
    private BigDecimal amount;
    private String bizOrderNo;
    private String payChannel;
}