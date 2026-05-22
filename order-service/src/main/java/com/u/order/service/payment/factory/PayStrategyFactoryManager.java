package com.u.order.service.payment.factory;

import com.u.common.constant.PayMethodConstant;
import com.u.common.exception.BusinessException;
import com.u.order.service.payment.strategy.OrderPayStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PayStrategyFactoryManager {

    private final Map<Integer, PayStrategyFactory> factoryMap;

    public PayStrategyFactoryManager(List<PayStrategyFactory> factories) {
        this.factoryMap = factories.stream()
                .collect(Collectors.toMap(PayStrategyFactory::supportPayType, f -> f));
    }

    public Integer resolvePayType(Integer payType) {
        return payType == null ? PayMethodConstant.BALANCE : payType;
    }

    public OrderPayStrategy createStrategy(Integer payType) {
        Integer realPayType = resolvePayType(payType);
        PayStrategyFactory factory = factoryMap.get(realPayType);
        if (factory == null) {
            throw new BusinessException("不支持的支付方式: " + realPayType);
        }
        return factory.createStrategy();
    }
}
