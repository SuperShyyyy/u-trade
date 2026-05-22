package com.u.wallet.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class WalletLogVO {
    private Integer bizType;         // 1-充值, 2-冻结, 3-确认收货, 4-解冻, 5-提现, 6-退款
    private BigDecimal amount;       // 变动金额
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private BigDecimal frozenBefore;
    private BigDecimal frozenAfter;
    private String bizOrderNo;       // 业务订单号/流水号
    private String description;      // 描述
    private LocalDateTime createTime;
}
