package com.u.order.service.payment.factory;

import com.u.common.constant.PayMethodConstant;
import com.u.order.service.payment.strategy.OrderPayStrategy;
import com.u.order.service.payment.strategy.WalletPayStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalletPayStrategyFactory implements PayStrategyFactory {

    private final WalletPayStrategy walletPayStrategy;

    @Override
    public Integer supportPayType() {
        return PayMethodConstant.BALANCE;
    }

    @Override
    public OrderPayStrategy createStrategy() {
        return walletPayStrategy;
    }
}
