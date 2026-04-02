package com.sec.service.payment.factory;

import com.sec.service.payment.strategy.OrderPayStrategy;

public interface PayStrategyFactory {
    /**
     * 支付方式类型（对应 PayMethodConstant）
     */
    Integer supportPayType();

    /**
     * 工厂方法：创建支付策略
     */
    OrderPayStrategy createStrategy();
}
