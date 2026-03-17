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
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate redisTemplate ;
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

        // ... (前面的商品锁定、价格计算、地址查询逻辑保持不变) ...
        // 1. 乐观锁锁定商品
        LambdaUpdateWrapper<Item> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Item::getId, itemId)
                .eq(Item::getStatus, ItemStatusConstant.ON_SALE)
                .set(Item::getStatus, ItemStatusConstant.LOCKED)
                .set(Item::getUpdateTime, LocalDateTime.now());
        boolean isLocked = itemService.update(updateWrapper);
        if (!isLocked) {
            throw new BusinessException("宝贝已售出或正在被他人购买中，请稍后重试");
        }
        Item item = itemService.getById(itemId);

        // 2. 获取价格和运费
        BigDecimal itemPrice = item.getPrice();
        BigDecimal shippingFee = (item.getIsFreeShipping() == 0) ? item.getShippingFee() : BigDecimal.ZERO;

        // 3. 查询地址
        UserAddress address = userAddressService.getById(dto.getAddressId());
        if (address == null) {
            throw new BusinessException("收货地址不存在");
        }

        // 4. 创建订单对象
        String orderNo = SerialNoUtil.generateOrderNo();
        Order order = new Order()
                .setBuyerId(userId)
                .setSellerId(dto.getSellerId())
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
    public void userCancelById(Long orderId) throws Exception {
        Long userId = BaseContext.getCurrentId();
        Order order = lambdaQuery()
                .eq(Order::getId, orderId)
                .eq(Order::getBuyerId, userId)
                .one();
        if (order == null) {
            throw new BusinessException("订单不存在，无法取消");
        }
        // 核心复用
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
                // 这里可以选择抛出异常触发事务回滚，或者记录失败后通过补偿任务处理
                // 为了保持数据一致性，建议抛出异常，让订单回滚，或者引入补偿机制
                throw new BusinessException("订单已取消，但商品上架失败，需人工介入");
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
        redisTemplate.opsForZSet().remove("order:auto_confirm:queue", String.valueOf(orderId));

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
        // 1. 获取当前卖家 ID
        Long sellerId = BaseContext.getCurrentId();
        if (sellerId == null) {
            throw new BusinessException("用户未登录");
        }

        // 2. 查询订单（用于后续校验）
        Order dbOrder = orderMapper.selectById(orderId);
        if (dbOrder == null) {
            throw new BusinessException("订单不存在");
        }
        if (!dbOrder.getSellerId().equals(sellerId)) {
            throw new BusinessException("无权操作该订单");
        }

        // 3. 状态校验
        if (!OrderStatusConstant.PAID.equals(dbOrder.getStatus())) {
            if (OrderStatusConstant.SHIPPED.equals(dbOrder.getStatus())) {
                log.info("订单 {} 已发货，本次操作将更新物流信息", orderId);
            } else {
                throw new BusinessException("订单状态不可发货（当前状态：" + dbOrder.getStatus() + "）");
            }
        }

        // 4. 更新订单状态
        Order updateOrder = new Order();
        updateOrder.setId(orderId);
        updateOrder.setStatus(OrderStatusConstant.SHIPPED);

        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, orderId)
                .eq(Order::getSellerId, sellerId)
                .in(Order::getStatus, OrderStatusConstant.PAID, OrderStatusConstant.SHIPPED);

        int orderUpdateCount = orderMapper.update(updateOrder, updateWrapper);
        if (orderUpdateCount == 0) {
            Order freshOrder = orderMapper.selectById(orderId);
            if (freshOrder == null) {
                throw new BusinessException("订单不存在");
            }
            if (!freshOrder.getSellerId().equals(sellerId)) {
                throw new BusinessException("无权操作该订单");
            }
            throw new BusinessException("订单状态已变更，无法发货（当前状态：" + freshOrder.getStatus() + "）");
        }

        // 5. 处理物流信息（新增或更新）
        Shipment existingShipment = shipmentMapper.selectOne(
                new LambdaQueryWrapper<Shipment>()
                        .eq(Shipment::getOrderId, orderId)
                        .eq(Shipment::getSellerId, sellerId)
        );

        int shipmentOperateCount;
        if (existingShipment == null) {
            // 新增物流记录
            Shipment shipment = new Shipment();
            shipment.setOrderId(orderId);
            shipment.setSellerId(sellerId);
            shipment.setStatus(ShipmentStatusConstant.SHIPPED);
            shipment.setCompany(logisticsCompany);
            shipment.setTrackingNo(trackingNumber);
            shipment.setCreatedAt(LocalDateTime.now());
            shipment.setUpdateTime(LocalDateTime.now());
            shipmentOperateCount = shipmentMapper.insert(shipment);
        } else {
            // 更新已有物流记录
            Shipment updateShipment = new Shipment();
            updateShipment.setCompany(logisticsCompany);
            updateShipment.setTrackingNo(trackingNumber);
            updateShipment.setStatus(ShipmentStatusConstant.SHIPPED);
            updateShipment.setUpdateTime(LocalDateTime.now());

            LambdaUpdateWrapper<Shipment> shipmentUpdateWrapper = new LambdaUpdateWrapper<>();
            shipmentUpdateWrapper.eq(Shipment::getOrderId, orderId)
                    .eq(Shipment::getSellerId, sellerId);
            shipmentOperateCount = shipmentMapper.update(updateShipment, shipmentUpdateWrapper);
        }

        if (shipmentOperateCount == 0) {
            throw new BusinessException("物流信息保存失败");
        }

        // 6.写入 Redis ZSet 延迟队列（7 天后自动确认收货）
        try {
            String zsetKey = "order:auto_confirm:queue";
            // 过期时间 = 当前时间 + 7 天
            //redis扫过期时间 ： 定时扫描 判断当前时间是否超过过期时间
            long expireTimestamp = System.currentTimeMillis() / 1000 + 7 * 24 * 3600;
            String orderIdStr = String.valueOf(orderId);
            redisTemplate.opsForZSet().add(zsetKey, orderIdStr, expireTimestamp);
            log.info("订单 {} 发货成功，已加入自动确认队列（过期时间：{}）",
                    orderId, LocalDateTime.ofEpochSecond(expireTimestamp, 0, ZoneOffset.ofHours(8)));

        } catch (Exception e) {
            // Redis 写入失败不影响主流程，DB 兜底任务会处理
            log.error("订单 {} 发货成功，但写入 Redis 延迟队列失败，将由 DB 兜底任务处理", orderId, e);
        }

        // 7. 清理订单缓存（传入完整订单对象）
        try {
            clearOrderCache(dbOrder);
            log.info("订单 {} 缓存已清理", orderId);
        } catch (Exception e) {
            log.error("订单 {} 清理缓存失败", orderId, e);
        }

        log.info("订单 {} 发货完成，物流公司：{}，运单号：{}", orderId, logisticsCompany, trackingNumber);
    }

    @Override
    public ShipmentVO queryShipmentByOrderId(Long orderId) {
        Long userId = BaseContext.getCurrentId();

        // 1. 正确的查询方式：通过 orderId 关联查询，而不是 selectById
        LambdaQueryWrapper<Shipment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Shipment::getOrderId, orderId);
        Shipment shipment = shipmentMapper.selectOne(queryWrapper);

        if (shipment == null) {
            // 情况A：还没发货，自然没有物流信息。
            // 根据业务需求，是抛异常还是返回空对象？
            // 如果是“查询物流”接口，通常意味着用户认为已发货。
            // 建议先查订单状态确认是否已发货
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

    @Scheduled(cron = "0 */3 * * * ?") // 3分钟一次
    public void scanRedisAutoConfirm() {
        String zsetKey = "order:auto_confirm:queue";
        long nowSeconds = System.currentTimeMillis() / 1000;

        // 1. 取出最多 500 条到期的订单 (避免一次处理太多阻塞)
        Set<String> expiredOrderIds = redisTemplate.opsForZSet()
                .rangeByScore(zsetKey, 0, nowSeconds, 0, 500);

        if (expiredOrderIds == null || expiredOrderIds.isEmpty()) {
            return;
        }

        for (String idStr : expiredOrderIds) {
            Long orderId = Long.valueOf(idStr);

            // 2. 【关键】尝试从 ZSet 移除，作为“抢占锁”
            // 如果 remove 返回 1，说明我抢到了；如果返回 0，说明可能被其他实例或兜底任务处理了
            Long removed = redisTemplate.opsForZSet().remove(zsetKey, idStr);

            if (removed != null && removed > 0) {
                try {
                    //创建代理对象调用自身
                    OrderServiceImpl proxy = applicationContext.getBean(OrderServiceImpl.class);
                    proxy.processSingleOrderConfirm(orderId);
                } catch (Exception e) {
                    log.error("Redis 扫描任务处理订单 {} 失败，将在下次 DB 兜底中重试", orderId, e);
                    // 可选：如果失败，可以重新加回 ZSet (带一点延迟)，或者直接放弃让 DB 兜底
                    // 最简单策略：放弃，让 30min 的 DB 兜底任务去捞
                }
            }
        }
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
            redisTemplate.delete(keys);
            log.debug("清除订单缓存: {}", keys);
        }
    }

  @Scheduled(cron = "0 */30 * * * ?")
    public void autoConfirmDbCompensation() {
        // 1. 给 Redis 留足时间：查询发货超过 7 天 + 30 分钟的订单
        LocalDateTime thresholdTime = LocalDateTime.now().minusDays(7).minusMinutes(30);

        List<Order> orders = lambdaQuery()
                .eq(Order::getStatus, OrderStatusConstant.SHIPPED)
                .lt(Order::getUpdatedAt, thresholdTime)
                .last("limit 200")
                .list();

        if (orders.isEmpty()) return;

        // 2. 循环处理，彻底告别大事务
        for (Order order : orders) {
            try {
                // 通过 Spring 代理对象调用
                OrderServiceImpl proxy = applicationContext.getBean(OrderServiceImpl.class);
                proxy.processSingleOrderConfirm(order.getId());
            } catch (Exception e) {
                log.error("DB 兜底任务：订单 {} 自动确认异常，跳过等待下次扫描", order.getId(), e);
            }
        }

        // 3. 清理 Redis 残留（可选，双重保险）
        List<String> orderIdsStr = orders.stream().map(o -> String.valueOf(o.getId())).toList();
        redisTemplate.opsForZSet().remove("order:auto_confirm:queue", orderIdsStr.toArray());
    }

    @Transactional(rollbackFor = Exception.class)
    public void processSingleOrderConfirm(Long orderId) {
        // 1. 尝试更新订单状态 (行锁/乐观锁)
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

        // 2. 更新物流表
        shipmentService.lambdaUpdate()
                .eq(Shipment::getOrderId, orderId)
                .eq(Shipment::getStatus, ShipmentStatusConstant.SHIPPED)
                .set(Shipment::getStatus, ShipmentStatusConstant.FINISHED)
                .set(Shipment::getDeliveredAt, LocalDateTime.now())
                .update();

        // 3. 组装结算消息
        Order order = orderMapper.selectById(orderId);
        OrderSettlementMessage msg = new OrderSettlementMessage(
                order.getBuyerId(), order.getSellerId(), order.getTotalPrice(), order.getOrderNo()
        );
        String messageId = order.getOrderNo() + "_" + UUID.randomUUID().toString().replace("-", "");
        msg.setMessageId(messageId);
        msg.setTimestamp(System.currentTimeMillis());

        // 4. 在当前数据库事务中 直接落库消息记录
        MqMessageLog mqLog = new MqMessageLog();
        mqLog.setMessageId(messageId);
        mqLog.setExchange(RabbitMQConstant.EXCHANGE_ORDER_SETTLE_EXEC);
        mqLog.setRoutingKey(RabbitMQConstant.ROUTING_KEY_ORDER_SETTLE_EXEC);
        mqLog.setMessageBody(JSON.toJSONString(msg));
        mqLog.setStatus(0); // 0-发送中
        mqLog.setRetryCount(0);
        mqMessageLogService.insert(mqLog);
        // 缓存清理
        clearOrderCache(order);

        mqMessageLogService.lambdaUpdate()
                .eq(MqMessageLog::getMessageId, messageId)
                .set(MqMessageLog::getStatus, 1) // 1-成功
                .set(MqMessageLog::getUpdateTime, LocalDateTime.now())
                .update();
        orderSettlementSender.send(msg);

        log.info("订单 {} 自动确认成功，本地消息表已记录", orderId);
    }
}
