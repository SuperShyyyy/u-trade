package com.u.api.dto.wallet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 钱包数据传输对象
 * <p>
 * 用于服务间 Feign 调用时传递钱包核心信息
 * 对应 wallet-service 中 UserWallet 实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 可用余额 */
    private BigDecimal balance;

    /** 冻结金额 */
    private BigDecimal frozen;
}
