package com.u.api.client.fallback;

import com.u.api.client.wallet.WalletClient;
import com.u.common.result.Result;
import com.u.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * WalletClient 降级工厂
 * 资金操作降级策略：
 * - freezeAmount / transferFrozenToSeller：返回失败（资金操作不可静默丢弃）
 * - unfreezeAmount：静默成功（解冻操作丢失可人工补偿，避免订单卡死）
 * - createWallet：静默成功（钱包会在首次查询时自动创建）
 */
@Component
@Slf4j
public class WalletClientFallbackFactory implements FallbackFactory<WalletClient> {

    @Override
    public WalletClient create(Throwable cause) {
        log.error("wallet-service 调用失败，触发降级: {}", cause.getMessage());
        return new WalletClient() {
            @Override
            public Result<Void> freezeAmount(Long userId, BigDecimal amount, String orderNo) {
                return Result.error(ResultCode.ERROR, "钱包服务不可用，冻结金额失败");
            }

            @Override
            public Result<Void> unfreezeAmount(Long userId, BigDecimal amount, String orderNo) {
                log.warn("钱包服务不可用，解冻降级处理(静默)，userId={}, orderNo={}", userId, orderNo);
                return Result.success();
            }

            @Override
            public Result<Void> transferFrozenToSeller(Long buyerId, Long sellerId, BigDecimal amount, String orderNo) {
                return Result.error(ResultCode.ERROR, "钱包服务不可用，转账失败");
            }

            @Override
            public Result<Void> createWallet(Long userId) {
                log.warn("钱包服务不可用，创建钱包降级处理(静默)，userId={}", userId);
                return Result.success();
            }
        };
    }
}
