package com.u.api.dto.wallet;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class WalletRechargeDTO {
    @NotNull(message = "充值金额不能为空")
    private BigDecimal amount;
    private String bizOrderNo;
}