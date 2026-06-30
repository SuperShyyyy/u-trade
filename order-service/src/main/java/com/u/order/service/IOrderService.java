package com.u.order.service;

import com.u.order.domain.dto.OrderPaymentDTO;
import com.u.order.domain.dto.OrderSubmitDTO;
import com.u.order.domain.po.Order;
import com.baomidou.mybatisplus.extension.service.IService;
import com.u.order.domain.vo.OrderPaymentVO;
import com.u.order.domain.vo.OrderSubmitVO;
import com.u.order.domain.vo.OrderVO;
import com.u.order.domain.vo.ShipmentVO;
import com.u.common.result.PageDTO;
import com.u.common.result.Result;

/**
 * <p>
 * 订单表 服务类
 * </p>
 *
 * @author author
 * @since 2026-03-08
 */
public interface IOrderService extends IService<Order> {
    /**
     * 用户下单
     */
    OrderSubmitVO orderSubmit(OrderSubmitDTO dto);

    /**
     * 支付订单
     */
    OrderPaymentVO payment(OrderPaymentDTO dto);

    /**
     * 查询用户历史订单
     */
    PageDTO<OrderVO> pageQuery4User(int page, int pageSize, Integer status);

    /**
     * 查询订单详情
     */
    OrderVO details(Long orderId);

    /**
     * 用户取消订单
     */
    void userCancelById(Long orderId);


    /**
     * 确认收货
     */
    void confirm(Long id);
    /**
     * 定时任务
     * 自动检查
     * 发货7天以上的订单 自动确认收货
     */


 //   void autoCancelTimeoutOrders();

    void shipment(Long orderId, String logisticsCompany, String trackingNumber);

    ShipmentVO queryShipmentByOrderId(Long orderId);

    void cancelOrderInternal(Long orderId);

    void cancelOrderInternal(Long orderId, String cancelReason);

    void processSingleOrderConfirm(Long orderId);
}
