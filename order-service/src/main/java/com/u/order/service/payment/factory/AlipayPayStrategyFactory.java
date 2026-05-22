package com.u.order.service.payment.factory;

import com.u.common.constant.PayMethodConstant;
import com.u.order.service.payment.strategy.AlipayPayStrategy;
import com.u.order.service.payment.strategy.OrderPayStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlipayPayStrategyFactory implements PayStrategyFactory {

    private final AlipayPayStrategy alipayPayStrategy;

    @Override
    public Integer supportPayType() {
        return PayMethodConstant.ALIPAY;
    }

    @Override
    public OrderPayStrategy createStrategy() {
        return alipayPayStrategy;
    }
}
