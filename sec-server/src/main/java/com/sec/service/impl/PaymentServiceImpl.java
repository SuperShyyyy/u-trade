package com.sec.service.impl;

import com.sec.constant.PaymentStatusConstant;
import com.sec.domain.po.Order;
import com.sec.domain.po.Payment;
import com.sec.mapper.PaymentMapper;
import com.sec.service.IPaymentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sec.service.payment.PayExecuteResult;
import com.sec.service.payment.factory.PayStrategyFactoryManager;
import com.sec.service.payment.strategy.OrderPayStrategy;
import com.sec.utils.SerialNoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 * 支付表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-03-08
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> implements IPaymentService {

    private final PayStrategyFactoryManager payStrategyFactoryManager;

    @Override
    public PayExecuteResult executeOrderPay(Order order, Integer payType) {
        Integer realPayType = payStrategyFactoryManager.resolvePayType(payType);
        OrderPayStrategy strategy = payStrategyFactoryManager.createStrategy(realPayType);

        strategy.pay(order);

        Payment payment = new Payment()
                .setOrderNo(order.getOrderNo())
                .setUserId(order.getBuyerId())
                .setAmount(order.getTotalPrice())
                .setMethod(realPayType)
                .setStatus(PaymentStatusConstant.SUCCESS)
                .setPaymentNo(SerialNoUtil.generatePayNo())
                .setPaidAt(LocalDateTime.now())
                .setCreatedAt(LocalDateTime.now());
        this.save(payment);

        return new PayExecuteResult(realPayType, strategy.buildPrepayInfo(order));
    }
}
