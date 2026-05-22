package com.u.order.service.payment.factory;

import com.u.common.constant.PayMethodConstant;
import com.u.order.service.payment.strategy.CardPayStrategy;
import com.u.order.service.payment.strategy.OrderPayStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardPayStrategyFactory implements PayStrategyFactory {

    private final CardPayStrategy cardPayStrategy;

    @Override
    public Integer supportPayType() {
        return PayMethodConstant.CARD;
    }

    @Override
    public OrderPayStrategy createStrategy() {
        return cardPayStrategy;
    }
}
