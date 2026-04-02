package com.sec.service.payment.strategy;

import com.sec.domain.po.Order;

public interface OrderPayStrategy {
    /**
     * 执行具体支付逻辑
     */
    void pay(Order order);

    /**
     * 返回预支付信息（微信/支付宝可返回预下单参数）
     */
    String buildPrepayInfo(Order order);
}
