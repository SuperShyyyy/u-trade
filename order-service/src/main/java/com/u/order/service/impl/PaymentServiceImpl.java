package com.u.order.service.impl;

import com.u.common.constant.PaymentStatusConstant;
import com.u.common.result.Result;
import com.u.common.result.ResultCode;
import com.u.order.domain.po.Order;
import com.u.order.domain.po.Payment;
import com.u.order.mapper.PaymentMapper;
import com.u.order.service.IPaymentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.u.order.service.payment.PayExecuteResult;
import com.u.order.service.payment.factory.PayStrategyFactoryManager;
import com.u.order.service.payment.strategy.OrderPayStrategy;
import com.u.common.utils.SerialNoUtil;
import com.u.api.client.wallet.WalletClient;
import com.u.common.constant.PayMethodConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> implements IPaymentService {

    private final PayStrategyFactoryManager payStrategyFactoryManager;
    private final WalletClient walletClient;

    @Override
    public PayExecuteResult executeOrderPay(Order order, Integer payType) {
        Integer realPayType = payStrategyFactoryManager.resolvePayType(payType);
        OrderPayStrategy strategy = payStrategyFactoryManager.createStrategy(realPayType);

        strategy.pay(order);

        try {
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
        } catch (Exception e) {
            // 支付流水写入失败时，补偿解冻钱包冻结金额
            if (PayMethodConstant.BALANCE.equals(realPayType)) {
                log.error("支付流水写入失败，尝试补偿解冻钱包 orderNo={}", order.getOrderNo(), e);
                try {
                    Result<Void> result = walletClient.unfreezeAmount(
                            order.getBuyerId(),
                            order.getTotalPrice(),
                            order.getOrderNo()
                    );
                    if (result == null || !ResultCode.SUCCESS.equals(result.getCode())) {
                        log.error("钱包解冻补偿失败 orderNo={}，需人工处理", order.getOrderNo());
                    }
                } catch (Exception ex) {
                    log.error("钱包解冻补偿异常 orderNo={}，需人工处理", order.getOrderNo(), ex);
                }
            }
            throw e;
        }

        return new PayExecuteResult(realPayType, strategy.buildPrepayInfo(order));
    }
}
