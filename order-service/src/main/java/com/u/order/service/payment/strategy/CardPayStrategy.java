package com.u.order.service.payment.strategy;

import com.u.order.domain.po.Order;
import org.springframework.stereotype.Component;

@Component
public class CardPayStrategy implements OrderPayStrategy {
    @Override
    public void pay(Order order) {
        // TODO 银行卡支付网关暂未接入，先保留占位实现
    }

    @Override
    public String buildPrepayInfo(Order order) {
        return "CARD_PAID_SUCCESS";
    }
}
