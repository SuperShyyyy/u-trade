package com.u.order.service.payment.strategy;

import com.u.common.exception.BusinessException;
import com.u.order.domain.po.Order;
import org.springframework.stereotype.Component;

@Component
public class CardPayStrategy implements OrderPayStrategy {
    @Override
    public void pay(Order order) {
        throw new BusinessException("银行卡支付暂未接入，请选择余额支付");
    }

    @Override
    public String buildPrepayInfo(Order order) {
        throw new BusinessException("银行卡支付暂未接入，请选择余额支付");
    }
}
