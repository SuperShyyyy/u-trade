package com.sec.service.impl;

import com.sec.domain.po.Order;
import com.sec.mapper.OrderMapper;
import com.sec.service.IOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 订单主表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Service
public class OrdersServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

}
