package com.sec.service.payment.factory;

import com.sec.constant.PayMethodConstant;
import com.sec.service.payment.strategy.AlipayPayStrategy;
import com.sec.service.payment.strategy.OrderPayStrategy;
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
