package com.u.wallet.domain.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WalletRechargeDTO {
    @NotNull(message = "充值金额不能为空")
    private BigDecimal amount;

    // bizOrderNo 可由后端生成，前端可不传
    private String bizOrderNo;
}
