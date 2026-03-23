package com.sec.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sec.constant.*;
import com.sec.context.BaseContext;
import com.sec.domain.dto.OrderPaymentDTO;
import com.sec.domain.dto.OrderSubmitDTO;
import com.sec.domain.po.*;
import com.sec.domain.vo.OrderPaymentVO;
import com.sec.domain.vo.OrderSubmitVO;
import com.sec.domain.vo.OrderVO;
import com.sec.domain.vo.ShipmentVO;
import com.sec.exception.BusinessException;
import com.sec.mapper.ItemMapper;
import com.sec.mapper.OrderMapper;
import com.sec.mapper.PaymentMapper;
import com.sec.mapper.ShipmentMapper;
import com.sec.message.OrderSettlementMessage;
import com.sec.mq.sender.OrderCancelDelaySender;
import com.sec.mq.sender.OrderSettlementSender;
import com.sec.result.PageDTO;
import com.sec.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sec.utils.SerialNoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final IItemService itemService;
    private final IUserWalletService userWalletService;
    private final OrderSettlementSender orderSettlementSender;
    private final ItemMapper itemMapper;
    private final OrderCancelDelaySender orderCancelDelaySender;
    private final IMqMessageLogService mqMessageLogService;
    @Autowired
    private ApplicationContext applicationContext;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public OrderSubmitVO orderSubmit(OrderSubmitDTO dto) {
        Long userId = BaseContext.getCurrentId();
        Long itemId = dto.getItemId();
        // 1.先用乐观锁 锁定商品

        boolean isLocked = itemService.update(
                new LambdaUpdateWrapper<Item>()
                        .eq(Item::getId, itemId)
                        .eq(Item::getStatus, ItemStatusConstant.ON_SALE)
                        .set(Item::getStatus, ItemStatusConstant.LOCKED)
                        .set(Item::getUpdateTime, LocalDateTime.now())
        );
        if (!isLocked) {
            throw new BusinessException("宝贝已售出或正在被他人购买中，请稍后重试");
        }
        Item item = itemService.getById(itemId);
        if (item == null ) {
            throw new BusinessException("商品状态异常，请重试");
        }
        Long sellerId = item.getSellerId();
        if (sellerId == null) {
            throw new BusinessException("商品数据异常");
        }
        if (userId.equals(sellerId)) {
            throw new BusinessException("不能购买自己的商品");
        }
        if (dto.getSellerId() != null && !dto.getSellerId().equals(sellerId)) {
            throw new BusinessException("卖家信息与商品不一致");
        }
        // 2. 获取价格和运费
        BigDecimal itemPrice = item.getPrice();
        BigDecimal shippingFee = (item.getIsFreeShipping() != null && item.getIsFreeShipping() == 1)
                ? BigDecimal.ZERO
                : (item.getShippingFee() != null ? item.getShippingFee() : BigDecimal.ZERO);

        // 3. 查询地址
        UserAddress address = userAddressService.getById(dto.getAddressId());
        if (address == null) {
            throw new BusinessException("收货地址不存在");
        }

        // 4. 创建订单对象
        String orderNo = SerialNoUtil.generateOrderNo();
        Order order = new Order()
                .setBuyerId(userId)
                .setSellerId(sellerId)
                .setItemId(itemId)
                .setPrice(itemPrice)
                .setQuantity(1)
                .setShippingFee(shippingFee)
                .setTotalPrice(itemPrice.add(shippingFee))
                .setStatus(OrderStatusConstant.WAIT_PAY)
                .setCreatedAt(LocalDateTime.now())
                .setReceiverProvince(address.getProvince())
                .setReceiverCity(address.getCity())
                .setReceiverDistrict(address.getDistrict())
                .setReceiverAddress(address.getDetailAddress())
                .setReceiverPhone(address.getReceiverPhone())
                .setReceiverName(address.getReceiverName())
                .setOrderNo(orderNo);

        // 5. 插入订单
        orderMapper.insert(order);

        // 6. 发送延迟取消消息 (30分钟后检查是否支付)
        try {
            orderCancelDelaySender.sendDelayCancelMessage(orderNo, order.getId());
            log.info("订单 {} 创建成功，已发送30分钟未支付自动取消延迟消息", orderNo);
        } catch (Exception e) {
            // 如果MQ发送失败，订单已经创建了。
            // 记录错误日志，依靠定时任务兜底取消。不让用户感知，保证下单流程顺畅。
            log.error("订单 {} 发送延迟取消消息失败，将依赖定时任务兜底", orderNo, e);
        }
        // 7. 清除缓存
        clearOrderCache(order);
        // 8. 返回 VO
        OrderSubmitVO vo = new OrderSubmitVO();
        BeanUtils.copyProperties(order, vo);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public OrderPaymentVO payment(OrderPaymentDTO dto) throws Exception {
        Long userId = BaseContext.getCurrentId();
        String orderNo = dto.getOrderNo();

        // 1. 查询订单
        Order order = lambdaQuery()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getBuyerId, userId)
                .one();

        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != OrderStatusConstant.WAIT_PAY) {
            //已支付直接返回成功
            if (order.getStatus() == OrderStatusConstant.PAID) {
                log.info("订单 {} 重复支付请求，直接返回成功", orderNo);
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
                .setOrderNo(order.getOrderNo())
                .setUserId(order.getBuyerId())
                .setAmount(order.getTotalPrice())
                .setMethod(dto.getPayType())
                .setStatus(PaymentStatusConstant.SUCCESS)
                .setPaymentNo(SerialNoUtil.generatePayNo())
                .setPaidAt(LocalDateTime.now())
                .setCreatedAt(LocalDateTime.now());

        paymentMapper.insert(payment);

        // 3. 更新订单状态为已支付
        LambdaUpdateWrapper<Order> orderUpdate = new LambdaUpdateWrapper<>();
        orderUpdate.eq(Order::getOrderNo, orderNo)
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
            log.error("支付成功但更新商品状态失败！订单ID: {}, 商品ID: {}", orderNo, order.getItemId());
            throw new BusinessException("支付成功但商品状态同步失败，请联系客服");
        }

        // 5. 清除缓存
        clearOrderCache(order);
        return buildPaymentVO(order, dto);
    }

    // 辅助方法 构建支付返回VO
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

        //9.批量查询 item 表
        Map<Long, Item> itemMap = itemIds.isEmpty()
                ? new HashMap<>()
                : itemMapper.selectList(new LambdaQueryWrapper<Item>().in(Item::getId, itemIds))
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Item::getId, item -> item, (v1, v2) -> v1));
        //10.组装VO列表
        List<OrderVO> voList = records.stream().map(order -> {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);

            //物流信息
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

            //商品信息 从内存Map获取
            Long currentItemId = order.getItemId();
            if (currentItemId != null && itemMap.containsKey(currentItemId)) {
                Item item = itemMap.get(currentItemId);
                vo.setItemTitle(item.getTitle());
                vo.setItemDescription(item.getDescription());
                vo.setItemImage(item.getImages());
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
        long ttl = 10 + ThreadLocalRandom.current().nextInt(10); //10-20min
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
        cancelOrderInternal(orderId);
    }

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

        // 2. 释放商品库存
        // 需要先查询订单以获取商品ID（因为上面只更新了订单，没有返回订单信息）
        Order order = getById(orderId);
        if (order != null && order.getItemId() != null) {
            boolean released = itemService.lambdaUpdate()
                    .eq(Item::getId, order.getItemId())
                    .eq(Item::getStatus, ItemStatusConstant.LOCKED)
                    .set(Item::getStatus, ItemStatusConstant.ON_SALE)
                    .set(Item::getUpdateTime, LocalDateTime.now())
                    .update();
            if (!released) {
                log.warn("自动取消订单成功，但商品状态释放失败！订单ID: {}, 商品ID: {}", orderId, order.getItemId());
            }
        }

        // 3. 清除缓存等
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
                .eq(Shipment::getStatus, OrderStatusConstant.SHIPPED) // 仅当物流为已发货时允许改为已签收
                .set(Shipment::getStatus, OrderStatusConstant.FINISHED) // 已签收
                .set(Shipment::getDeliveredAt, LocalDateTime.now());

        boolean shipmentUpdated = shipmentService.update(shipmentWrapper);

        if (!shipmentUpdated) {
            log.warn("订单 {} 已确认收货，但同步物流表状态失败，可能状态不一致", orderId);
        }

        // 4. 更新缓存
        clearOrderCache(order);

        // 5. 删除发货时在redis创建的过期时间
        stringRedisTemplate.opsForZSet().remove("order:auto_confirm:queue", String.valueOf(orderId));

        // 6. 资金结算 调用 WalletService
        userWalletService.transferFrozenToSeller(
                order.getBuyerId(),
                order.getSellerId(),
                order.getTotalPrice(),
                order.getOrderNo()
        );

    }


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
                    long expireTimestamp = System.currentTimeMillis() / 1000 + 7 * 24 * 3600;
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

    @Override
    public ShipmentVO queryShipmentByOrderId(Long orderId) {
        Long userId = BaseContext.getCurrentId();

        // 1. 正确的查询方式：通过 orderId 关联查询，而不是 selectById
        LambdaQueryWrapper<Shipment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Shipment::getOrderId, orderId);
        Shipment shipment = shipmentMapper.selectOne(queryWrapper);

        if (shipment == null) {
            Order order = orderMapper.selectById(orderId);
            if (order != null && OrderStatusConstant.SHIPPED.equals(order.getStatus())) {
                throw new BusinessException("物流信息同步延迟，请稍后重试");
            }
            throw new BusinessException("该订单尚未发货，无物流信息");
        }

        // 2. 权限校验 买家和卖家都能看
        // 获取订单信息以校验买家身份
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        boolean isSeller = shipment.getSellerId().equals(userId);
        boolean isBuyer = order.getBuyerId().equals(userId);

        if (!isSeller && !isBuyer) {
            throw new BusinessException("无权查看该订单的物流信息");
        }
        // 4. 转换 VO
        ShipmentVO shipmentVO = new ShipmentVO();
        BeanUtils.copyProperties(shipment, shipmentVO);


        return shipmentVO;
    }

    private void clearOrderCache(Order order) {

        Set<String> keys = new HashSet<>();

        // 订单详情
        keys.add(RedisConstant.ORDER_DETAILS + order.getId());

        // 买家订单缓存
        if (order.getBuyerId() != null) {
            keys.add(RedisConstant.ORDER_PAGE_QUERY_BUYER + order.getBuyerId());
        }

        // 卖家订单缓存
        if (order.getSellerId() != null) {
            keys.add(RedisConstant.ORDER_PAGE_QUERY_SELLER + order.getSellerId());
            keys.add(RedisConstant.ITEM_LIST_SELLER + order.getSellerId());
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

    @Transactional(rollbackFor = Exception.class)
    public void processSingleOrderConfirm(Long orderId) {
        // 1. 更新订单状态
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
        // 2. 更新物流状态
        shipmentService.lambdaUpdate()
                .eq(Shipment::getOrderId, orderId)
                .eq(Shipment::getStatus, ShipmentStatusConstant.SHIPPED)
                .set(Shipment::getStatus, ShipmentStatusConstant.FINISHED)
                .set(Shipment::getDeliveredAt, LocalDateTime.now())
                .update();
        // 3. 查询订单信息
        Order order = orderMapper.selectById(orderId);

        // 4. 构建消息
        OrderSettlementMessage msg = new OrderSettlementMessage(
                order.getBuyerId(),
                order.getSellerId(),
                order.getTotalPrice(),
                order.getOrderNo()
        );
        String messageId = order.getOrderNo() + "_" + UUID.randomUUID().toString().replace("-", "");
        msg.setMessageId(messageId);
        msg.setTimestamp(System.currentTimeMillis());
        // 5. 保存本地消息表
        MqMessageLog mqLog = new MqMessageLog();
        mqLog.setMessageId(messageId);
        mqLog.setExchange(RabbitMQConstant.EXCHANGE_ORDER_SETTLE_EXEC);
        mqLog.setRoutingKey(RabbitMQConstant.ROUTING_KEY_ORDER_SETTLE_EXEC);
        mqLog.setMessageBody(JSON.toJSONString(msg));
        mqLog.setStatus(MqMessageLogStatus.SENDING);
        mqLog.setRetryCount(0);
        mqLog.setCreateTime(LocalDateTime.now());
        mqMessageLogService.save(mqLog);
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            orderSettlementSender.send(msg);
                            log.info("订单 {} MQ发送成功 messageId={}", orderId, messageId);
                        } catch (Exception e) {
                            log.error("订单 {} MQ发送失败 messageId={}", orderId, messageId, e);
                        }
                    }
                }
        );

        // 7. 清理缓存
        clearOrderCache(order);

        log.info("订单 {} 自动确认完成（消息已入库，等待发送）", orderId);
    }

}
