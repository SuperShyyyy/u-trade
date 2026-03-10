package com.sec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sec.constant.ItemStatusConstant;
import com.sec.constant.OrderStatusConstant;
import com.sec.constant.RedisConstant;
import com.sec.constant.ShipmentStatusConstant;
import com.sec.context.BaseContext;
import com.sec.domain.dto.OrderPaymentDTO;
import com.sec.domain.dto.OrderSubmitDTO;
import com.sec.domain.po.*;
import com.sec.domain.vo.OrderPaymentVO;
import com.sec.domain.vo.OrderSubmitVO;
import com.sec.domain.vo.OrderVO;
import com.sec.exception.BusinessException;
import com.sec.mapper.OrderMapper;
import com.sec.mapper.PaymentMapper;
import com.sec.mapper.ShipmentMapper;
import com.sec.message.WalletSettlementMessage;
import com.sec.mq.sender.WalletSettlementSender;
import com.sec.result.PageDTO;
import com.sec.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sec.utils.SerialNoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 订单表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-03-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final ShipmentMapper shipmentMapper;
    private final IShipmentService shipmentService;
    private final IUserAddressService userAddressService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate redisTemplate ;
    private final IItemService itemService;
    private final IUserWalletService userWalletService;
    private final WalletSettlementSender walletSettlementSender;
    @Transactional(rollbackFor = Exception.class)
    @Override
    public OrderSubmitVO orderSubmit(OrderSubmitDTO dto) {
        Long userId = BaseContext.getCurrentId();
        Long itemId = dto.getItemId();

        //1.尝试锁定商品
        //状态为 ON_SALE的商品才能改为LOCKED
        LambdaUpdateWrapper<Item> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Item::getId, itemId)
                .eq(Item::getStatus, ItemStatusConstant.ON_SALE) // 核心：检查旧状态
                .set(Item::getStatus, ItemStatusConstant.LOCKED)
                .set(Item::getUpdateTime, LocalDateTime.now());

        boolean isLocked = itemService.update(updateWrapper);

        if (!isLocked) {
            // 如果更新失败  说明商品不是 ON_SALE 状态
            Item item = itemService.getById(itemId);
            if (item == null) {
                throw new BusinessException("商品不存在");
            }
            if (item.getStatus().equals(ItemStatusConstant.SOLD)) {
                throw new BusinessException("宝贝已售出，手慢无！");
            } else if (item.getStatus().equals(ItemStatusConstant.LOCKED)) {
                throw new BusinessException("商品正在被他人购买中，请稍后重试");
            } else {
                throw new BusinessException("商品状态异常，无法购买");
            }
        }
        // 2.查询地址
        UserAddress address = userAddressService.getById(dto.getAddressId());
        if (address == null) {
            throw new BusinessException("收货地址不存在");
        }

        //3.创建订单
        Order order = new Order()
                .setBuyerId(userId)
                .setSellerId(dto.getSellerId())
                .setItemId(itemId)
                .setQuantity(dto.getQuantity())
                .setPrice(dto.getPrice())
                .setTotalPrice(dto.getPrice().multiply(BigDecimal.valueOf(dto.getQuantity())))
                .setStatus(OrderStatusConstant.WAIT_PAY)
                .setCreatedAt(LocalDateTime.now())
                .setReceiverProvince(address.getProvince())
                .setReceiverCity(address.getCity())
                .setReceiverDistrict(address.getDistrict())
                .setReceiverAddress(address.getDetailAddress())
                .setReceiverPhone(address.getReceiverPhone())
                .setReceiverName(address.getReceiverName())
                .setOrderNo(SerialNoUtil.generateOrderNo());

        orderMapper.insert(order);
        //4.订单状态更新需清楚缓存
        clearOrderCache(order);
        //5.封装返回 VO
        OrderSubmitVO vo = new OrderSubmitVO();
        BeanUtils.copyProperties(order, vo);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public OrderPaymentVO payment(OrderPaymentDTO dto) throws Exception {
        Long userId = BaseContext.getCurrentId();
        Long orderId = dto.getOrderId();

        // 1. 查询订单
        Order order = lambdaQuery()
                .eq(Order::getId, orderId)
                .eq(Order::getBuyerId, userId)
                .one();

        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != OrderStatusConstant.WAIT_PAY) {
            //已支付直接返回成功
            if (order.getStatus() == OrderStatusConstant.PAID) {
                log.info("订单 {} 重复支付请求，直接返回成功", orderId);
                return buildPaymentVO(order, dto);
            }
            throw new BusinessException("订单状态异常，无法支付 (当前状态:" + order.getStatus() + ")");
        }
        userWalletService.freezeAmount(
                order.getBuyerId(),
                order.getTotalPrice(),
                order.getOrderNo()
        );
        // 2. 创建支付记录
        Payment payment = new Payment()
                .setOrderId(order.getId())
                .setUserId(order.getBuyerId())
                .setAmount(order.getTotalPrice())
                .setMethod(dto.getPayType().toString())
                .setStatus(OrderStatusConstant.PAID)
                .setPaymentNo(SerialNoUtil.generatePayNo())
                .setPaidAt(LocalDateTime.now())
                .setCreatedAt(LocalDateTime.now());

        paymentMapper.insert(payment);

        // 3. 更新订单状态为已支付
        LambdaUpdateWrapper<Order> orderUpdate = new LambdaUpdateWrapper<>();
        orderUpdate.eq(Order::getId, orderId)
                .eq(Order::getStatus, OrderStatusConstant.WAIT_PAY)
                .set(Order::getStatus, OrderStatusConstant.PAID)
                .set(Order::getPaidAt, LocalDateTime.now());

        int orderUpdated = orderMapper.update(null, orderUpdate);
        if (orderUpdated == 0) {
            throw new BusinessException("支付失败，订单状态已变更，请刷新");
        }

        // 4. 更新商品状态为SOLD
        // 只有LOCKED的商品才能改为SOLD
        LambdaUpdateWrapper<Item> itemUpdate = new LambdaUpdateWrapper<>();
        itemUpdate.eq(Item::getId, order.getItemId())
                .eq(Item::getStatus, ItemStatusConstant.LOCKED)
                .set(Item::getStatus, ItemStatusConstant.SOLD)
                .set(Item::getUpdateTime, LocalDateTime.now());

        boolean itemUpdated = itemService.update(itemUpdate);
        if (!itemUpdated) {
            log.error("支付成功但更新商品状态失败！订单ID: {}, 商品ID: {}", orderId, order.getItemId());
            throw new BusinessException("支付成功但商品状态同步失败，请联系客服");
        }

        // 5. 清除缓存
        clearOrderCache(order);
        return buildPaymentVO(order, dto);
    }

    // 辅助方法：构建支付返回VO
    private OrderPaymentVO buildPaymentVO(Order order, OrderPaymentDTO dto) {
        OrderPaymentVO vo = new OrderPaymentVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setPayType(dto.getPayType());
        vo.setAmount(order.getTotalPrice().toString());
        vo.setStatus(OrderStatusConstant.PAID);
        vo.setPrepayInfo("PAID_SUCCESS");
        return vo;
    }

    @Override
    public PageDTO<OrderVO> pageQuery4User(int page, int pageSize, Integer status) {

        Long userId = BaseContext.getCurrentId();

        String cacheKey = RedisConstant.ORDER_PAGE_QUERY_BUYER
                + userId + ":" + page + ":" + pageSize + ":" + (status == null ? "all" : status);
        // 1. 尝试从 Redis 中获取缓存
        PageDTO<OrderVO> cachedResult = (PageDTO<OrderVO>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedResult != null) {
            return cachedResult; // 如果缓存存在，直接返回
        }
        // 构建分页参数
        Page<Order> pageParam = new Page<>(page, pageSize);

        // 构建查询条件
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getBuyerId, userId);

        if (status != null) {
            queryWrapper.eq(Order::getStatus, status);
        }

        queryWrapper.orderByDesc(Order::getCreatedAt);

        //  执行分页查询
        IPage<Order> orderIPage = orderMapper.selectPage(pageParam, queryWrapper);
        List<Order> records = orderIPage.getRecords();

        // 如果没有数据，返回空 PageDTO，但分页信息正确
        if (records.isEmpty()) {
            PageDTO<OrderVO> emptyResult = new PageDTO<>(
                    orderIPage.getTotal(),
                    orderIPage.getPages(),
                    orderIPage.getCurrent(),
                    Collections.emptyList()
            );
            redisTemplate.opsForValue().set(cacheKey, emptyResult, 3, TimeUnit.MINUTES);
            return emptyResult;
        }

        // 批量查询 shipment 表
        List<Long> orderIds = records.stream().map(Order::getId).collect(Collectors.toList());
        List<Shipment> shipments = shipmentMapper.selectList(
                new LambdaQueryWrapper<Shipment>().in(Shipment::getOrderId, orderIds)
        );
        Map<Long, Shipment> shipmentMap = shipments.stream()
                .collect(Collectors.toMap(Shipment::getOrderId, s -> s, (s1, s2) -> s1));

        // 组装 VO 列表
        List<OrderVO> voList = records.stream().map(order -> {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);

            // 填充物流和收货信息
            Shipment shipment = shipmentMap.get(order.getId());
            if (shipment != null) {
                vo.setShipmentCompany(shipment.getCompany());
                vo.setTrackingNo(shipment.getTrackingNo());
                vo.setShippedAt(shipment.getShippedAt());
                vo.setDeliveredAt(shipment.getDeliveredAt());
                vo.setReceiverName(shipment.getReceiverName());
                vo.setReceiverPhone(shipment.getReceiverPhone());
                vo.setReceiverProvince(shipment.getReceiverProvince());
                vo.setReceiverCity(shipment.getReceiverCity());
                vo.setReceiverDistrict(shipment.getReceiverDistrict());
                vo.setReceiverAddress(shipment.getReceiverAddress());
            }
            return vo;
        }).collect(Collectors.toList());

        // 缓存查询结果到 Redis
        PageDTO<OrderVO> result = new PageDTO<>(
                orderIPage.getTotal(),
                orderIPage.getPages(),
                orderIPage.getCurrent(),
                voList
        );
        long ttl = 10 + ThreadLocalRandom.current().nextInt(10);
        redisTemplate.opsForValue().set(cacheKey, result, ttl, TimeUnit.MINUTES);
        return result;
    }

    @Override
    public OrderVO details(Long orderId) {
        Long userId = BaseContext.getCurrentId();

        //先查redis
        String cacheKey = RedisConstant.ORDER_DETAILS + orderId;

        OrderVO cachedOrder = (OrderVO) redisTemplate.opsForValue().get(cacheKey);
        if (cachedOrder != null) {
            return cachedOrder; // 如果缓存存在，直接返回
        }

        // 查询订单时可以分别获取买家和卖家的订单
        Order order = lambdaQuery()
                .eq(Order::getId, orderId)
                .and(wrapper -> wrapper.eq(Order::getBuyerId, userId).or().eq(Order::getSellerId, userId))
                .one();

        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 没有缓存   OrderVO
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(order, orderVO);


        // 存入缓存
        redisTemplate.opsForValue().set(cacheKey, orderVO, 30, TimeUnit.MINUTES);

        return orderVO;
    }


    /*
    * 取消订单 只能取消已下单 未支付的
    * */
    @Transactional
    @Override
    public void userCancelById(Long orderId) throws Exception {
        Long userId = BaseContext.getCurrentId();
        Order order = lambdaQuery()
                .eq(Order::getId, orderId)
                .eq(Order::getBuyerId, userId)
                .one();
        if (order == null) {
            throw new BusinessException("订单不存在，无法取消");
        }
        if(order.getStatus()!=OrderStatusConstant.WAIT_PAY){
            throw new BusinessException("订单状态异常，无法取消");
        }
        order.setStatus(OrderStatusConstant.CANCELLED)
                .setCancelledAt(LocalDateTime.now());
        this.updateById(order);

        LambdaUpdateWrapper<Item> itemUpdate = new LambdaUpdateWrapper<>();
        itemUpdate.eq(Item::getId, order.getItemId())
                .eq(Item::getStatus, ItemStatusConstant.LOCKED) // 确保当前是锁定状态
                .set(Item::getStatus, ItemStatusConstant.ON_SALE)
                .set(Item::getUpdateTime, LocalDateTime.now());
        boolean released = itemService.update(itemUpdate);
        if (!released) {
            log.warn("取消订单成功，但商品状态释放失败！订单ID: {}, 商品ID: {}", orderId, order.getItemId());
            throw new BusinessException("订单已取消，但商品上架失败，请联系管理员");
        }

        clearOrderCache(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long orderId) {
        Long userId = BaseContext.getCurrentId();
        // 1. 查询订单
        Order order = lambdaQuery()
                .eq(Order::getBuyerId, userId)
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, OrderStatusConstant.SHIPPED) // 已发货
                .one();
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() == null) {
            throw new BusinessException("状态异常");
        }
        // 2. 状态校验：只有已发货(2)才能确认收货
        if (order.getStatus() != OrderStatusConstant.SHIPPED) {
            throw new BusinessException("订单未发货，无法确认收货！");
        }

        // 3. 更新订单状态 (updateById 会自动触发 @Version 检查)
        order.setStatus(OrderStatusConstant.FINISHED); // 已完成
        order.setCompletedAt(LocalDateTime.now());

        int count = this.baseMapper.updateById(order);
        if (count == 0) {
            throw new BusinessException("操作失败，订单状态已变更，请刷新后重试");
        }

        // 4. 同步更新物流信息
        LambdaUpdateWrapper<Shipment> shipmentWrapper = new LambdaUpdateWrapper<>();
        shipmentWrapper.eq(Shipment::getOrderId, orderId)
                .eq(Shipment::getStatus, OrderStatusConstant.SHIPPED) // 仅当物流为已发货时允许改为已签收
                .set(Shipment::getStatus, OrderStatusConstant.FINISHED) // 已签收
                .set(Shipment::getDeliveredAt, LocalDateTime.now());

        boolean shipmentUpdated = shipmentService.update(shipmentWrapper);

        if (!shipmentUpdated) {
            log.warn("订单 {} 已确认收货，但同步物流表状态失败，可能状态不一致", orderId);
        }
        // 5. 更新缓存
        clearOrderCache(order);

        // 6. 资金结算 调用 WalletService
        userWalletService.transferFrozenToSeller(
                order.getBuyerId(),
                order.getSellerId(),
                order.getTotalPrice(),
                order.getOrderNo()
        );
    }

    @Override
    @Transactional
    public void autoConfirm() {
        //查询发货7天的订单
        List<Order> orders = lambdaQuery()
                .eq(Order::getStatus, OrderStatusConstant.SHIPPED)
                .lt(Order::getUpdatedAt, LocalDateTime.now().minusDays(7))
                .last("limit 1000")
                .list();
        if (orders.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<Long> orderIds = orders.stream()
                .map(Order::getId)
                .toList();
        // 批量更新订单
        lambdaUpdate()
                .in(Order::getId, orderIds)
                .set(Order::getStatus, OrderStatusConstant.FINISHED)
                .set(Order::getCompletedAt, now)
                .update();
        // 批量更新物流
        shipmentService.lambdaUpdate()
                .in(Shipment::getOrderId, orderIds)
                .set(Shipment::getStatus, ShipmentStatusConstant.FINISHED)
                .set(Shipment::getDeliveredAt, now)
                .update();

        // 只删除状态实际变更的订单缓存
        orders.forEach(order -> clearOrderCache(order));
        //4. 结算 使用异步或消息队列，避免在定时任务里阻塞结算
        for (Order order : orders) {
            WalletSettlementMessage msg = new WalletSettlementMessage(
                    order.getBuyerId(),
                    order.getSellerId(),
                    order.getTotalPrice(),
                    order.getOrderNo()
            );
            walletSettlementSender.send(msg);
        }

    }

    private void clearOrderCache(Order order) {
        Long orderId = order.getId();
        Long buyerId = order.getBuyerId();
        Long sellerId = order.getSellerId();

        Set<String> allKeys = new HashSet<>();

        // 1. 订单详情
        allKeys.add(RedisConstant.ORDER_DETAILS + orderId);
        // 2. 买家订单列表
        if (buyerId != null) {
            Set<String> buyerKeys = redisTemplate.keys(RedisConstant.ORDER_PAGE_QUERY_BUYER + buyerId + "*");
            if (buyerKeys != null) allKeys.addAll(buyerKeys);
        }
        // 3. 卖家订单列表
        if (sellerId != null) {
            Set<String> sellerKeys = redisTemplate.keys(RedisConstant.ORDER_PAGE_QUERY_SELLER + sellerId + "*");
            if (sellerKeys != null) allKeys.addAll(sellerKeys);

            // 4. 卖家商品列表
            Set<String> itemKeys = redisTemplate.keys(RedisConstant.ITEM_LIST_SELLER + sellerId + ":*");
            if (itemKeys != null) allKeys.addAll(itemKeys);
        }

        // 5. 商品详情缓存
        if (order.getItemId() != null) {
            allKeys.add(RedisConstant.ITEM_DETAIL + order.getItemId());
        }

        if (!allKeys.isEmpty()) {
            redisTemplate.delete(allKeys);
            log.debug("已清除订单 {} 相关的 {} 个缓存 Key", orderId, allKeys.size());
        }
    }
}
