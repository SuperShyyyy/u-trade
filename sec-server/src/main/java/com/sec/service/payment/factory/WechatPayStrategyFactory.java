package com.sec.service.payment.factory;

import com.sec.constant.PayMethodConstant;
import com.sec.service.payment.strategy.OrderPayStrategy;
import com.sec.service.payment.strategy.WechatPayStrategy;
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
