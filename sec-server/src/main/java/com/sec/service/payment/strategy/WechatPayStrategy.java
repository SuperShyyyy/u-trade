package com.sec.service.payment.strategy;

import com.sec.domain.po.Order;
import org.springframework.stereotype.Component;

@Component
public class WechatPayStrategy implements OrderPayStrategy {
    @Override
    public void pay(Order order) {
        // TODO 微信支付 API 暂未接入，先保留占位实现
    }

    @Override
    public String buildPrepayInfo(Order order) {
        return "WECHAT_PREPAY_TODO";
    }
}
