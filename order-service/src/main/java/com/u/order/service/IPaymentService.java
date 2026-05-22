package com.u.order.service;

import com.u.order.domain.po.Order;
import com.u.order.domain.po.Payment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.u.order.service.payment.PayExecuteResult;

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
