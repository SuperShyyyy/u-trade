package com.u.wallet.domain.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WalletWithdrawDTO {
    @NotNull(message = "提现金额不能为空")
    private BigDecimal amount;

    // bizOrderNo 可由后端生成
    private String bizOrderNo;

    // 提现渠道，例如 ALIPAY、BANK
    private String payChannel;
}
