package com.sec.domain.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class UserWalletVO {
    private BigDecimal balance;       // 可用余额
    private BigDecimal frozenAmount;  // 冻结金额
    private BigDecimal totalIncome;   // 总收入
    private BigDecimal totalExpense;  // 总支出
}