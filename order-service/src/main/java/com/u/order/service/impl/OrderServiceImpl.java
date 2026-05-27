package com.u.order.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.u.api.client.item.ItemClient;
import com.u.api.client.user.UserAddressClient;
import com.u.api.client.wallet.WalletClient;
import com.u.api.dto.item.OrderItemDTO;
import com.u.api.dto.user.UserAddressDTO;
import com.u.common.constant.MqMessageLogStatus;
import com.u.common.constant.OrderStatusConstant;
import com.u.common.constant.PayMethodConstant;
import com.u.common.constant.RabbitMQConstant;
import com.u.common.constant.RedisConstant;
import com.u.common.constant.ShipmentStatusConstant;
import com.u.common.context.BaseContext;
import com.u.order.domain.dto.OrderPaymentDTO;
import com.u.order.domain.dto.OrderSubmitDTO;
import com.u.order.domain.po.*;
import com.u.order.domain.vo.OrderPaymentVO;
import com.u.order.domain.vo.OrderSubmitVO;
import com.u.order.domain.vo.OrderVO;
import com.u.order.domain.vo.ShipmentVO;
import com.u.common.exception.BusinessException;
import com.u.order.mapper.OrderMapper;
import com.u.order.mapper.ShipmentMapper;
import com.u.common.message.OrderSettlementMessage;
import com.u.common.message.ItemSnapshotDTO;
import com.u.order.mq.sender.OrderCancelDelaySender;
import com.u.order.mq.sender.OrderSettlementSender;
import com.u.common.result.PageDTO;
import com.u.common.result.Result;
import com.u.common.result.ResultCode;
import com.u.order.service.*;
import com.u.order.service.payment.PayExecuteResult;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.u.common.utils.SerialNoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
    private final ShipmentMapper shipmentMapper;
    private final IShipmentService shipmentService;
    private final ItemClient itemClient;
    private final UserAddressClient userAddressClient;
    private final WalletClient walletClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final OrderSettlementSender orderSettlementSender;
    private final OrderCancelDelaySender orderCancelDelaySender;
    private final IMqMessageLogService mqMessageLogService;
    private final IPaymentService paymentService;

    private  final RedissonClient redissonClient;

    /*
        * [用户下单请求]
             │
             ▼
        1. 校验与前置准备 (无锁, 无本地事务)
           ├── 远程查询商品、地址信息
           └── 各种基础业务风控校验（是不是自己的商品等）
             │
             ▼
        2. 分布式锁控制并发 (Redisson)
             │
             ▼
        3. 调用远程商品微服务扣减库存/锁定商品 (RPC)
           └── 成功后拿到结果
             │
             ▼
        4. 开启本地事务，写入订单 (Order微服务本地事务)
           ├── orderMapper.insert(order)
           └── 发送 MQ 延迟取消消息
             │
             ▼
        5. 事务提交完成 ──> 释放分布式锁*/
    //A 提交订单 主方法
    @Transactional(rollbackFor = Exception.class)
    @Override
    public OrderSubmitVO orderSubmit(OrderSubmitDTO dto) {

        Long userId = BaseContext.getCurrentId();
        Long itemId = dto.getItemId();
        Result<OrderItemDTO> itemResult = itemClient.getOrderItem(itemId);
        ensureSuccess(itemResult, "查询商品失败");
        OrderItemDTO item = itemResult.getData();
        if (item == null) {
            throw new BusinessException("商品不存在");
        }
        Long sellerId = item.getSellerId();
        if (userId.equals(sellerId)) {
            throw new BusinessException("不能购买自己的商品");
        }
        if (dto.getSellerId() != null && !dto.getSellerId().equals(sellerId)) {
            throw new BusinessException("卖家信息与商品不一致");
        }
        UserAddressDTO address = null;
        if (dto.getAddressId() != null) {
            Result<UserAddressDTO> addressResult = userAddressClient.getAddress(dto.getAddressId(), userId);
            ensureSuccess(addressResult, "查询收货地址失败");
            address = addressResult.getData();
            if (address == null) {
                throw new BusinessException("收货地址不存在");
            }
        }//前面操作都是查询请求无需事务

        String lockKey = "order:lock:item:" + itemId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean isLock = false;
        boolean releaseRemoteItemLock = false;

        try {
            // 不等待，立刻获取锁
            isLock = lock.tryLock(0, -1, TimeUnit.SECONDS);
            if (!isLock) {
                throw new BusinessException("宝贝正在被他人购买中，请稍后重试");
            }
            // 1. 远程调用：锁定/扣减商品库存 (RPC)
            ensureSuccess(itemClient.lockItem(itemId), "锁定商品失败");
            releaseRemoteItemLock = true;
            // 2. 借助 Spring 编程式事务或内部申明式事务，只包裹本地 DB 操作
            Order order = createOrderInLocalTransaction(userId, sellerId, item, address);

            // 3. 异步发送延迟取消消息
            // 超时自动取消
            sendDelayMessageLazy(order);

            // 4. 清理缓存
            clearOrderCache(order);

            // 5. 核心：事务成功后，注册钩子释放分布式锁
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (lock.isHeldByCurrentThread()) {
                                lock.unlock();
                            }
                        }
                    }
            );

            OrderSubmitVO vo = new OrderSubmitVO();
            BeanUtils.copyProperties(order, vo);
            return vo;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            rollbackRemoteLockQuietly(releaseRemoteItemLock, itemId);
            // 异常发生且事务未成功，当前线程立刻释放锁
            unlockQuietly(isLock, lock);
            throw new BusinessException("系统繁忙，请稍后再试");
        } catch (Exception e) {
            log.error("订单创建异常, itemId={}", itemId, e);
            rollbackRemoteLockQuietly(releaseRemoteItemLock, itemId);
            unlockQuietly(isLock, lock);
            if (e instanceof BusinessException) {
                throw (BusinessException) e;
            }
            throw new BusinessException("订单创建失败，请稍后重试");
        }finally {
            if (isLock && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    //A.2 辅助方法：在本地事务创建订单
    @Transactional(rollbackFor = Exception.class)
    public Order createOrderInLocalTransaction(Long userId, Long sellerId, OrderItemDTO item, UserAddressDTO address) {
        BigDecimal itemPrice = item.getPrice();
        BigDecimal shippingFee = (item.getIsFreeShipping() != null && item.getIsFreeShipping() == 1)
                ? BigDecimal.ZERO
                : (item.getShippingFee() != null ? item.getShippingFee() : BigDecimal.ZERO);
        String orderNo = SerialNoUtil.generateOrderNo();
        Order order = new Order()
                .setBuyerId(userId)
                .setSellerId(sellerId)
                .setItemId(item.getId())
                .setPrice(itemPrice)
                .setQuantity(1)
                .setShippingFee(shippingFee)
                .setTotalPrice(itemPrice.add(shippingFee))
                .setStatus(OrderStatusConstant.WAIT_PAY)
                .setCreatedAt(LocalDateTime.now())
                .setOrderNo(orderNo)
                .setItemSnapshot(buildItemSnapshot(item));
        if (address != null) {
            order.setReceiverProvince(address.getProvince())
                    .setReceiverCity(address.getCity())
                    .setReceiverDistrict(address.getDistrict())
                    .setReceiverAddress(address.getDetailAddress())
                    .setReceiverPhone(address.getReceiverPhone())
                    .setReceiverName(address.getReceiverName());
        }
        orderMapper.insert(order);
        return order;
    }
    //A.3 辅助方法：发送超时未支付取消订单延时消息
    private void sendDelayMessageLazy(Order order) {
        try {
            orderCancelDelaySender.sendDelayCancelMessage(order.getOrderNo(), order.getId());
        } catch (Exception e) {
            log.error("订单 {} 延迟消息发送失败，依赖调度兜底", order.getOrderNo(), e);
        }
    }
    //A.4 辅助方法：回滚
    private void rollbackRemoteLockQuietly(boolean needRollback, Long itemId) {
        if (needRollback) {
            try {
                itemClient.releaseItem(itemId);
            } catch (Exception ex) {
                log.error("分布式事务补偿：释放商品锁定失败, itemId={}", itemId, ex);
            }
        }
    }
    //A.5 辅助方法：释放锁
    private void unlockQuietly(boolean isLock, RLock lock) {
        if (isLock && lock.isHeldByCurrentThread()) {
            try { lock.unlock(); } catch (Exception ignored) {}
        }
    }

    //B.1 支付 主方法
    @Transactional(rollbackFor = Exception.class)
    @Override
    public OrderPaymentVO payment(OrderPaymentDTO dto)  {
        Long userId = BaseContext.getCurrentId();
        String orderNo = dto.getOrderNo();
        RLock payLock = redissonClient.getLock("order:pay:" + orderNo);
        boolean locked = false;

        try {
            locked = payLock.tryLock(0, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException("订单正在支付处理中，请稍后重试");
            }

            // 1. 查询订单
            Order order = lambdaQuery()
                    .eq(Order::getOrderNo, orderNo)
                    .eq(Order::getBuyerId, userId)
                    .one();

            if (order == null) {
                throw new BusinessException("订单不存在");
            }
            if (order.getStatus() != null && !order.getStatus().equals(OrderStatusConstant.WAIT_PAY)) {
                //已支付直接返回成功
                if (order.getStatus().equals(OrderStatusConstant.PAID)) {
                    log.info("订单 {} 重复支付请求，直接返回成功", orderNo);
                    Integer payType = dto.getPayType() == null ? PayMethodConstant.BALANCE : dto.getPayType();
                    return buildPaymentVO(order, payType, "PAID_SUCCESS");
                }
                throw new BusinessException("订单状态异常，无法支付 (当前状态:" + order.getStatus() + ")");
            }

            // 2. 执行支付（工厂方法 + 策略），并写入支付流水
            PayExecuteResult payResult = paymentService.executeOrderPay(order, dto.getPayType());
            boolean walletFrozen = PayMethodConstant.BALANCE.equals(payResult.getPayType());

            try {
                // 3. 更新订单状态为已支付
                LambdaUpdateWrapper<Order> orderUpdate = new LambdaUpdateWrapper<>();
                orderUpdate.eq(Order::getOrderNo, orderNo)
                        .eq(Order::getStatus, OrderStatusConstant.WAIT_PAY)
                        .set(Order::getStatus, OrderStatusConstant.PAID)
                        .set(Order::getPaymentMethod, String.valueOf(payResult.getPayType()))
                        .set(Order::getPaidAt, LocalDateTime.now());

                int orderUpdated = orderMapper.update(null, orderUpdate);
                if (orderUpdated == 0) {
                    throw new BusinessException("支付失败，订单状态已变更，请刷新");
                }

                ensureSuccess(itemClient.markItemSold(order.getItemId()), "支付成功但商品状态同步失败，请联系客服");

                // 5. 清除缓存
                clearOrderCache(order);
                return buildPaymentVO(order, payResult.getPayType(), payResult.getPrepayInfo());
            } catch (Exception e) {
                compensateWalletFreeze(walletFrozen, order);
                throw e;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("系统繁忙，请稍后再试");
        } finally {
            if (locked && payLock.isHeldByCurrentThread()) {
                payLock.unlock();
            }
        }
    }
    //B.2 辅助方法 构建支付返回VO
    private OrderPaymentVO buildPaymentVO(Order order, Integer payType, String prepayInfo) {
        OrderPaymentVO vo = new OrderPaymentVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setPayType(payType);
        vo.setAmount(order.getTotalPrice().toString());
        vo.setStatus(OrderStatusConstant.PAID);
        vo.setPrepayInfo(prepayInfo);
        return vo;
    }

    private void compensateWalletFreeze(boolean walletFrozen, Order order) {
        if (!walletFrozen || order == null) {
            return;
        }
        try {
            ensureSuccess(walletClient.unfreezeAmount(
                    order.getBuyerId(),
                    order.getTotalPrice(),
                    order.getOrderNo()
            ), "余额冻结补偿失败");
            log.info("订单 {} 支付失败，已回滚钱包冻结金额", order.getOrderNo());
        } catch (Exception ex) {
            log.error("订单 {} 支付失败后钱包解冻补偿失败，请人工处理", order.getOrderNo(), ex);
        }
    }

    //C 查询用户订单
    @Override
    public PageDTO<OrderVO> pageQuery4User(int page, int pageSize, Integer status) {
        Long userId = BaseContext.getCurrentId();
        //1.缓存Key
        String cacheKey = RedisConstant.ORDER_PAGE_QUERY_BUYER
                + userId + ":" + page + ":" + pageSize + ":" + (status == null ? "all" : status);
        //2.从Redis中获取缓存
        PageDTO<OrderVO> cachedResult = (PageDTO<OrderVO>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }
        //3.构建分页参数
        Page<Order> pageParam = new Page<>(page, pageSize);
        //4.构建查询条件
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getBuyerId, userId);

        if (status != null) {
            queryWrapper.eq(Order::getStatus, status);
        }
        //按创建时间倒序
        queryWrapper.orderByDesc(Order::getCreatedAt);

        //5.执行分页查询
        IPage<Order> orderIPage = orderMapper.selectPage(pageParam, queryWrapper);
        List<Order> records = orderIPage.getRecords();

        //6.处理无数据情况
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

        //7.准备关联数据所需的 ID 集合
        List<Long> orderIds = records.stream().map(Order::getId).collect(Collectors.toList());
        List<Long> itemIds = records.stream()
                .map(Order::getItemId)
                .filter(Objects::nonNull) // 防止 itemId 为空
                .distinct() // 去重，减少查询量
                .collect(Collectors.toList());

        //8.批量查询shipment表
        Map<Long, Shipment> shipmentMap = orderIds.isEmpty()
                ? new HashMap<>()
                : shipmentMapper.selectList(new LambdaQueryWrapper<Shipment>().in(Shipment::getOrderId, orderIds))
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Shipment::getOrderId, s -> s, (s1, s2) -> s1));

        // TODO: 9.批量查询 item 表 - 后续通过 Feign 调用商品服务
        // Map<Long, Item> itemMap = itemIds.isEmpty()
        //         ? new HashMap<>()
        //         : itemMapper.selectList(new LambdaQueryWrapper<Item>().in(Item::getId, itemIds))
        //         .stream()
        //         .filter(Objects::nonNull)
        //         .collect(Collectors.toMap(Item::getId, item -> item, (v1, v2) -> v1));

        //10.组装VO列表
        List<OrderVO> voList = records.stream().map(order -> {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);

            if (order.getItemSnapshot() != null) {
                ItemSnapshotDTO snapshot = order.getItemSnapshot();
                vo.setItemTitle(snapshot.getTitle());
                if (snapshot.getImages() != null && !snapshot.getImages().isEmpty()) {
                    vo.setItemImage(snapshot.getImages().get(0));
                }
                vo.setItemDescription(snapshot.getDescription());
                vo.setPrice(snapshot.getPrice());
            }

            //物流信息
            Shipment shipment = shipmentMap.get(order.getId());
            if (shipment != null) {
                vo.setShippedAt(shipment.getShippedAt());
                vo.setDeliveredAt(shipment.getDeliveredAt());
            }

            return vo;
        }).collect(Collectors.toList());

        //构建最终结果 缓存&返回
        PageDTO<OrderVO> result = new PageDTO<>(
                orderIPage.getTotal(),
                orderIPage.getPages(),
                orderIPage.getCurrent(),
                voList
        );

        //12.缓存 添加随机过期时间 防止缓存雪崩
        long ttl = 3 + ThreadLocalRandom.current().nextInt(2);
        redisTemplate.opsForValue().set(cacheKey, result, ttl, TimeUnit.MINUTES);
        return result;
    }

    //D 查询订单详情
    @Override
    public OrderVO details(Long orderId) {
        Long userId = BaseContext.getCurrentId();
        //查询redis之前 走索引校验订单用户信息
        Order order = lambdaQuery()
                .select(Order::getId, Order::getBuyerId, Order::getSellerId)
                .eq(Order::getId, orderId)
                .one();

        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        if (!userId.equals(order.getBuyerId()) && !userId.equals(order.getSellerId())) {
            throw new BusinessException("无权限访问该订单");
        }
        //查redis
        String cacheKey = RedisConstant.ORDER_DETAILS + orderId;

        OrderVO cachedOrder = (OrderVO) redisTemplate.opsForValue().get(cacheKey);
        if (cachedOrder != null) {
            return cachedOrder; // 如果缓存存在，直接返回
        }

        // 查询订单时可以分别获取买家和卖家的订单
        Order fullOrder = lambdaQuery()
                .eq(Order::getId, orderId)
                .and(wrapper -> wrapper.eq(Order::getBuyerId, userId).or().eq(Order::getSellerId, userId))
                .one();

        if (fullOrder == null) {
            throw new BusinessException("订单不存在");
        }

        // 没有缓存   OrderVO
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(fullOrder, orderVO);
        if (fullOrder.getItemSnapshot() != null) {
            ItemSnapshotDTO snapshot = fullOrder.getItemSnapshot();
            orderVO.setItemTitle(snapshot.getTitle());
            orderVO.setItemDescription(snapshot.getDescription());
            if (snapshot.getImages() != null && !snapshot.getImages().isEmpty()) {
                orderVO.setItemImage(snapshot.getImages().get(0));
            }
            orderVO.setPrice(snapshot.getPrice());
        }

        // 存入缓存
        redisTemplate.opsForValue().set(cacheKey, orderVO, 30, TimeUnit.MINUTES);

        return orderVO;
    }

    //E 用户取消订单 主方法
    @Transactional
    @Override
    public void userCancelById(Long orderId)  {
        Long userId = BaseContext.getCurrentId();
        Order order = lambdaQuery()
                .eq(Order::getId, orderId)
                .eq(Order::getBuyerId, userId)
                .one();
        if (order == null) {
            throw new BusinessException("订单不存在，无法取消");
        }
        if(!order.getStatus().equals(OrderStatusConstant.WAIT_PAY)){
            throw new BusinessException("订单状态异常，无法取消");
        }
        cancelOrderInternal(orderId);
    }

    //E.2 辅助方法 取消订单
    @Transactional
    public void cancelOrderInternal(Long orderId) {
        // 1. 更新订单状态，仅当状态为待支付时才更新
        boolean updated = lambdaUpdate()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, OrderStatusConstant.WAIT_PAY)
                .set(Order::getStatus, OrderStatusConstant.CANCELLED)
                .set(Order::getCancelledAt, LocalDateTime.now())
                .set(Order::getCancelReason, "超时未支付自动取消")
                .update();

        if (!updated) {
            // 订单可能已被支付或已取消，直接返回（无需抛异常，视为处理成功）
            log.info("订单 {} 状态已变更，无需自动取消", orderId);
            return;
        }

        Order order = getById(orderId);
        if (order != null) {
            if (order.getItemId() != null) {
                try {
                    ensureSuccess(itemClient.releaseItem(order.getItemId()), "商品状态释放失败");
                } catch (Exception e) {
                    log.warn("自动取消订单成功，但商品状态释放失败！订单ID: {}, 商品ID: {}", orderId, order.getItemId(), e);
                }
            }
            clearOrderCache(order);
        }
    }

    //F 确认收货 主方法
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

        //获取乐观锁版本
        Integer currentVersion = order.getVersion();
        if (currentVersion == null) {
            currentVersion = 0;
        }
        //更新
        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, order.getId())
                // 1.判断版本号 避免高并发问题
                .eq(Order::getVersion, currentVersion)
                // 2. 更新状态
                .set(Order::getStatus, OrderStatusConstant.FINISHED)
                .set(Order::getCompletedAt, LocalDateTime.now())
                // 3. 手动更改版本号 +1
                .setSql("version = version + 1");

        if (!this.update(updateWrapper)) {
            throw new BusinessException("操作失败，订单状态已变更，请刷新后重试");
        }


        // 3. 同步更新物流信息
        LambdaUpdateWrapper<Shipment> shipmentWrapper = new LambdaUpdateWrapper<>();
        shipmentWrapper.eq(Shipment::getOrderId, orderId)
                .eq(Shipment::getStatus, ShipmentStatusConstant.SHIPPED) // 仅当物流为已发货时允许改为已签收
                .set(Shipment::getStatus, ShipmentStatusConstant.FINISHED) // 已签收
                .set(Shipment::getDeliveredAt, LocalDateTime.now());

        boolean shipmentUpdated = shipmentService.update(shipmentWrapper);

        if (!shipmentUpdated) {
            log.warn("订单 {} 已确认收货，但同步物流表状态失败，可能状态不一致", orderId);
        }

        // 4. 更新缓存
        clearOrderCache(order);

        // 5. 删除发货时在redis创建的过期时间
        stringRedisTemplate.opsForZSet().remove("order:auto_confirm:queue", String.valueOf(orderId));

        if (String.valueOf(PayMethodConstant.BALANCE).equals(order.getPaymentMethod())) {
            ensureSuccess(walletClient.transferFrozenToSeller(
                    order.getBuyerId(),
                    order.getSellerId(),
                    order.getTotalPrice(),
                    order.getOrderNo()
            ), "资金结算失败");
        }

    }

    //G 用户发货 主方法
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipment(Long orderId, String logisticsCompany, String trackingNumber) {
        Long sellerId = BaseContext.getCurrentId();
        LocalDateTime now = LocalDateTime.now();
        // 1.校验权限
        Order dbOrder = orderMapper.selectById(orderId);
        if (dbOrder == null || !dbOrder.getSellerId().equals(sellerId)) {
            throw new BusinessException("订单不存在或无权操作");
        }

        // 2. 状态校验
        if (!OrderStatusConstant.PAID.equals(dbOrder.getStatus()) &&
                !OrderStatusConstant.SHIPPED.equals(dbOrder.getStatus())) {
            throw new BusinessException("当前状态不可发货");
        }

        // 3. 更新订单状态
        boolean orderUpdated = lambdaUpdate()
                .eq(Order::getId, orderId)
                .eq(Order::getSellerId, sellerId)
                .in(Order::getStatus, OrderStatusConstant.PAID, OrderStatusConstant.SHIPPED)
                .set(Order::getStatus, OrderStatusConstant.SHIPPED)
                .set(Order::getShippedAt, now)
                .update();

        if (!orderUpdated) {
            throw new BusinessException("订单状态已变更，请刷新重试");
        }

        Shipment shipment = new Shipment()
                .setOrderId(orderId)
                .setSellerId(sellerId)
                .setCompany(logisticsCompany)
                .setTrackingNo(trackingNumber)
                .setStatus(ShipmentStatusConstant.SHIPPED)
                .setUpdateTime(now);

        shipmentService.saveOrUpdate(shipment, new LambdaQueryWrapper<Shipment>()
                .eq(Shipment::getOrderId, orderId));

        // 5.注册事务同步：只有 DB 提交成功，才操作 Redis
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    // 写入自动确认队列
                    String zsetKey = RedisConstant.AUTO_CONFIRM_KEY;
                    long expireTimestamp = System.currentTimeMillis() / 1000
                            + TimeUnit.DAYS.toSeconds(7)
                            - TimeUnit.MINUTES.toSeconds(3); // 7天后，提前3分钟触发
                    stringRedisTemplate.opsForZSet().add(zsetKey, String.valueOf(orderId), expireTimestamp);
                    // 清理缓存
                    clearOrderCache(dbOrder);
                    log.info("订单 {} 事务提交成功，同步更新 Redis 数据", orderId);
                } catch (Exception e) {
                    log.error("订单 {} 事务提交后 Redis 操作异常", orderId, e);
                }
            }
        });

        log.info("订单 {} 发货 DB 操作完成，等待事务提交", orderId);
    }


    //H 通过订单id查询物流状态
    @Override
    public ShipmentVO queryShipmentByOrderId(Long orderId) {
        Long userId = BaseContext.getCurrentId();

        // 1. 先校验订单权限，避免通过物流错误信息探测任意订单状态
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        boolean isBuyer = order.getBuyerId().equals(userId);
        boolean isSeller = order.getSellerId().equals(userId);

        if (!isSeller && !isBuyer) {
            throw new BusinessException("无权查看该订单的物流信息");
        }

        // 2. 正确的查询方式：通过 orderId 关联查询，而不是 selectById
        LambdaQueryWrapper<Shipment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Shipment::getOrderId, orderId);
        Shipment shipment = shipmentMapper.selectOne(queryWrapper);

        if (shipment == null) {
            if (OrderStatusConstant.SHIPPED.equals(order.getStatus())) {
                throw new BusinessException("物流信息同步延迟，请稍后重试");
            }
            throw new BusinessException("该订单尚未发货，无物流信息");
        }
        // 4. 转换 VO
        ShipmentVO shipmentVO = new ShipmentVO();
        BeanUtils.copyProperties(shipment, shipmentVO);


        return shipmentVO;
    }

    //辅助方法清理缓存
    private void clearOrderCache(Order order) {

        Set<String> keys = new HashSet<>();

        // 订单详情
        keys.add(RedisConstant.ORDER_DETAILS + order.getId());

        // 买家订单缓存（分页 key 包含 page/size/status，按前缀删除）
        if (order.getBuyerId() != null) {
            deleteKeysByPrefix(RedisConstant.ORDER_PAGE_QUERY_BUYER + order.getBuyerId() + ":");
        }

        // 卖家订单缓存（分页 key 包含 page/size/status，按前缀删除）
        if (order.getSellerId() != null) {
            deleteKeysByPrefix(RedisConstant.ORDER_PAGE_QUERY_SELLER + order.getSellerId() + ":");
            deleteKeysByPrefix(RedisConstant.ITEM_LIST_SELLER + order.getSellerId() + ":");
        }

        // 商品详情
        if (order.getItemId() != null) {
            keys.add(RedisConstant.ITEM_DETAIL + order.getItemId());
        }

        if (!keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
            log.debug("清除订单缓存: {}", keys);
        }
    }

    /**
     * 分页缓存 key 通常携带动态维度（page/size/status），不能只删固定前缀 key。
     * 使用 SCAN 按前缀匹配并批量删除，避免删不干净。
     */
    private void deleteKeysByPrefix(String prefix) {
        Set<String> matchedKeys = new HashSet<>();
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(prefix + "*")
                .count(200)
                .build();
        RedisConnectionFactory factory = stringRedisTemplate.getConnectionFactory();
        if (factory == null) {
            log.warn("RedisConnectionFactory 为空，跳过前缀清理: {}", prefix);
            return;
        }

        try (RedisConnection connection = factory.getConnection();
             Cursor<byte[]> cursor = connection.scan(scanOptions)) {
            while (cursor.hasNext()) {
                matchedKeys.add(new String(cursor.next(), StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            log.error("按前缀扫描缓存 key 失败，prefix={}", prefix, e);
        }

        if (!matchedKeys.isEmpty()) {
            stringRedisTemplate.delete(matchedKeys);
            log.debug("按前缀清理缓存: prefix={}, count={}", prefix, matchedKeys.size());
        }
    }

    //I 处理单订单确认收货
    @Transactional(rollbackFor = Exception.class)
    public void processSingleOrderConfirm(Long orderId) {
        // 1. 乐观锁更新订单状态
        boolean updated = lambdaUpdate()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, OrderStatusConstant.SHIPPED)
                .set(Order::getStatus, OrderStatusConstant.FINISHED)
                .set(Order::getCompletedAt, LocalDateTime.now())
                .update();
        if (!updated) {
            log.info("订单 {} 状态已变更或已处理，跳过", orderId);
            return;
        }

        // 2. 更新物流状态（增加失败校验）
        boolean shipUpdated = shipmentService.lambdaUpdate()
                .eq(Shipment::getOrderId, orderId)
                .eq(Shipment::getStatus, ShipmentStatusConstant.SHIPPED)
                .set(Shipment::getStatus, ShipmentStatusConstant.FINISHED)
                .set(Shipment::getDeliveredAt, LocalDateTime.now())
                .update();
        if (!shipUpdated) {
            log.warn("订单 {} 物流状态更新失败，可能已被处理", orderId);
            // 根据业务决定是否抛出异常回滚，或者直接返回
            throw new BusinessException("物流状态更新失败，防止数据不一致");
        }

        // 3. 查询订单信息并作防错判空
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            log.error("订单 {} 不存在", orderId);
            throw new BusinessException("订单不存在");
        }

        // 4. 校验支付方式（假设 PaymentMethod 是 Integer/String，尽量避免用 String.valueOf 强转）
        if (!String.valueOf(PayMethodConstant.BALANCE).equals(order.getPaymentMethod())) {
            clearOrderCache(order);
            log.info("订单 {} 自动确认完成（非余额支付，无需钱包结算）", orderId);
            return;
        }

        // 5. 构建并保存本地消息表
        String messageId = order.getOrderNo() + "_" + UUID.randomUUID().toString().replace("-", "");
        OrderSettlementMessage msg = new OrderSettlementMessage(
                order.getBuyerId(),
                order.getSellerId(),
                order.getTotalPrice(),
                order.getOrderNo()
        );
        msg.setMessageId(messageId);
        msg.setTimestamp(System.currentTimeMillis());

        MqMessageLog mqLog = new MqMessageLog();
        mqLog.setMessageId(messageId);
        mqLog.setExchange(RabbitMQConstant.EXCHANGE_ORDER_SETTLE_EXEC);
        mqLog.setRoutingKey(RabbitMQConstant.ROUTING_KEY_ORDER_SETTLE_EXEC);
        mqLog.setMessageBody(JSON.toJSONString(msg));
        mqLog.setStatus(MqMessageLogStatus.SENDING); // 初始状态为发送中
        mqLog.setRetryCount(0);
        mqLog.setCreateTime(LocalDateTime.now());
        mqMessageLogService.save(mqLog);

        // 6. 注册事务同步器（注意：afterCommit 里的操作不属于当前事务）
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            // 发送 MQ
                            orderSettlementSender.send(msg);
                            log.info("订单 {} MQ发送成功 messageId={}", orderId, messageId);

                            // 【重要】更新本地消息表状态为 SUCCESS，避免定时任务重复发送
                            // 注意：这里需要传播行为为 REQUIRES_NEW 的方法去更新，或者异步更新
                            mqMessageLogService.updateStatus(messageId, MqMessageLogStatus.SUCCESS,null);

                        } catch (Exception e) {
                            log.error("订单 {} MQ发送失败，等待定时任务补偿 messageId={}", orderId, messageId, e);
                            // 这里不需要抛出异常，因为事务已经提交了。留给定时任务去重试。
                        }
                    }
                }
        );

        // 7. 清理缓存
        clearOrderCache(order);
        log.info("订单 {} 自动确认完成（消息已入库，等待事务提交后发送）", orderId);
    }

    private ItemSnapshotDTO buildItemSnapshot(OrderItemDTO item) {
        ItemSnapshotDTO snapshot = new ItemSnapshotDTO();
        snapshot.setItemId(item.getId());
        snapshot.setTitle(item.getTitle());
        snapshot.setDescription(item.getDescription());
        snapshot.setPrice(item.getPrice());
        snapshot.setImages(item.getImages());
        return snapshot;
    }

    private void ensureSuccess(Result<?> result, String defaultErrorMsg) {
        if (result == null) {
            throw new BusinessException(defaultErrorMsg);
        }
        if (!ResultCode.SUCCESS.equals(result.getCode())) {
            String message = result.getMessage() == null ? defaultErrorMsg : result.getMessage();
            throw new BusinessException(message);
        }
    }

}
