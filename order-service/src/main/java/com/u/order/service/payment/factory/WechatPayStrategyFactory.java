package com.u.order.service.payment.factory;

import com.u.common.constant.PayMethodConstant;
import com.u.order.service.payment.strategy.OrderPayStrategy;
import com.u.order.service.payment.strategy.WechatPayStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WechatPayStrategyFactory implements PayStrategyFactory {

    private final WechatPayStrategy wechatPayStrategy;

    @Override
    public Integer supportPayType() {
        return PayMethodConstant.WECHAT;
    }

    @Override
    public OrderPayStrategy createStrategy() {
        return wechatPayStrategy;
    }
}
