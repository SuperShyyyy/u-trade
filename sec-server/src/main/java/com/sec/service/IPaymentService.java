package com.sec.service;

import com.sec.domain.po.Order;
import com.sec.domain.po.Payment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sec.service.payment.PayExecuteResult;

/**
 * <p>
 * 支付表 服务类
 * </p>
 *
 * @author author
 * @since 2026-03-08
 */
public interface IPaymentService extends IService<Payment> {
    /**
     * 执行订单支付（策略 + 工厂方法）
     */
    PayExecuteResult executeOrderPay(Order order, Integer payType);
}
