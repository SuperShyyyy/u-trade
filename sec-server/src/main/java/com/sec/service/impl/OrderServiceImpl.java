package com.sec.service.impl;

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
import com.sec.message.WalletSettlementMessage;
import com.sec.mq.sender.WalletSettlementSender;
import com.sec.result.PageDTO;
import com.sec.result.Result;
import com.sec.service.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sec.utils.SerialNoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.support.BeanDefinitionDsl;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
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
    private final ItemMapper itemMapper;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public OrderSubmitVO orderSubmit(OrderSubmitDTO dto) {
        Long userId = BaseContext.getCurrentId();
        Long itemId = dto.getItemId();

        //1.乐观锁锁定商品
        // 只有状态为 ON_SALE的商品才能改为LOCKED
        LambdaUpdateWrapper<Item> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Item::getId, itemId)
                .eq(Item::getStatus, ItemStatusConstant.ON_SALE) // 核心：检查旧状态
                .set(Item::getStatus, ItemStatusConstant.LOCKED)
                .set(Item::getUpdateTime, LocalDateTime.now());
        boolean isLocked = itemService.update(updateWrapper);
        if (!isLocked) {
            log.warn("商品锁定失败，ID: {}", itemId);
            throw new BusinessException("宝贝已售出或正在被他人购买中，请稍后重试");
        }
        Item item = itemService.getById(itemId);
        //2.获取价格和运费
        BigDecimal itemPrice = item.getPrice();
        BigDecimal shippingFee = null;
        if(item.getIsFreeShipping()==0){
            shippingFee = item.getShippingFee();
        }
        //3.查询地址
        UserAddress address = userAddressService.getById(dto.getAddressId());
        if (address == null) {
            throw new BusinessException("收货地址不存在");
        }

        //3.创建订单
        Order order = new Order()
                .setBuyerId(userId)
                .setSellerId(dto.getSellerId())
                .setItemId(itemId)
                // 单价 数量 运费 总价
                .setPrice(itemPrice)
                .setQuantity(1)
                .setShippingFee(shippingFee)
                .setTotalPrice(itemPrice.add(shippingFee))//总价 = 运费 + 商品价格
                //状态更新
                .setStatus(OrderStatusConstant.WAIT_PAY)
                //创建时间
                .setCreatedAt(LocalDateTime.now())
                //物流
                .setReceiverProvince(address.getProvince())
                .setReceiverCity(address.getCity())
                .setReceiverDistrict(address.getDistrict())
                .setReceiverAddress(address.getDetailAddress())
                .setReceiverPhone(address.getReceiverPhone())
                .setReceiverName(address.getReceiverName())
                //生成唯一订单号
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

        // 5. 资金结算 调用 WalletService
        userWalletService.transferFrozenToSeller(
                order.getBuyerId(),
                order.getSellerId(),
                order.getTotalPrice(),
                order.getOrderNo()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoConfirm() {
        List<Order> orders = lambdaQuery()
                .eq(Order::getStatus, OrderStatusConstant.SHIPPED)
                .lt(Order::getUpdatedAt, LocalDateTime.now().minusDays(7))
                .last("limit 50")
                .list();

        if (orders.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        List<Long> orderIds = orders.stream().map(Order::getId).toList();

        // 1. 批量更新订单 (带上状态校验)
        boolean orderSuccess = lambdaUpdate()
                .in(Order::getId, orderIds)
                .eq(Order::getStatus, OrderStatusConstant.SHIPPED)
                .set(Order::getStatus, OrderStatusConstant.FINISHED)
                .set(Order::getCompletedAt, now)
                .update();

        // 2. 批量更新物流
        shipmentService.lambdaUpdate()
                .in(Shipment::getOrderId, orderIds)
                .eq(Shipment::getStatus, OrderStatusConstant.SHIPPED)
                .set(Shipment::getStatus, ShipmentStatusConstant.FINISHED)
                .set(Shipment::getDeliveredAt, now)
                .update();

        // 3. 清理缓存 & 发送结算消息
        // 建议：将发送消息放在循环内，如果发送失败，记录日志，后续通过对账任务补偿
        for (Order order : orders) {
            clearOrderCache(order);
            try {
                WalletSettlementMessage msg = new WalletSettlementMessage(
                        order.getBuyerId(),
                        order.getSellerId(),
                        order.getTotalPrice(),
                        order.getOrderNo()
                );
                walletSettlementSender.send(msg);
            } catch (Exception e) {
                log.error("订单 {} 自动确认收货后，发送结算消息失败", order.getId(), e);
                // 这里最好记录到一张"待结算补偿表"中，由另一个定时任务扫描重试
            }
        }
    }

    // 添加定时任务
    @Override
    @Transactional
    public void autoCancelTimeoutOrders() {

        LocalDateTime timeout = LocalDateTime.now().minusMinutes(30);

        List<Long> ids = lambdaQuery()
                .select(Order::getId)
                .eq(Order::getStatus, OrderStatusConstant.WAIT_PAY)
                .lt(Order::getCreatedAt, timeout)
                .last("limit 100")
                .list()
                .stream()
                .map(Order::getId)
                .toList();

        if (ids.isEmpty()) {
            return;
        }

        boolean success = lambdaUpdate()
                .in(Order::getId, ids)
                .eq(Order::getStatus, OrderStatusConstant.WAIT_PAY) // 核心：双重检查
                .set(Order::getStatus, OrderStatusConstant.CANCELLED)
                .set(Order::getCancelledAt, LocalDateTime.now())
                .set(Order::getCancelReason, "超时未支付自动取消")
                .update();
        // 3. 根据 boolean 结果判断是否继续后续逻辑
        if (!success) {
            log.info("自动取消订单任务执行完毕，但没有订单状态发生变更（可能已被支付或并发处理）");
            return;
        }

        log.info("自动取消订单成功，开始释放商品库存...");

        // 4. 释放被锁定的商品状态
        // 因为 updateSuccess 为 true，说明至少有部分订单被取消了，需要释放对应的商品
        // 为了精准，最好再次查询这些 ID 中实际被更新的订单对应的 itemId
        // 简单做法：直接尝试释放这些 itemId 带上 LOCKED 条件保证安全

        List<Order> cancelledOrders = lambdaQuery()
                .in(Order::getId, ids)
                .eq(Order::getStatus, OrderStatusConstant.CANCELLED) // 只查刚刚被成功的
                .select(Order::getItemId)
                .list();

        if (!cancelledOrders.isEmpty()) {
            List<Long> itemIds = cancelledOrders.stream()
                    .map(Order::getItemId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            // 批量释放商品：只有状态是 LOCKED 的才改为 ON_SALE
            boolean itemReleaseSuccess = itemService.lambdaUpdate()
                    .in(Item::getId, itemIds)
                    .eq(Item::getStatus, ItemStatusConstant.LOCKED)
                    .set(Item::getStatus, ItemStatusConstant.ON_SALE)
                    .set(Item::getUpdateTime, LocalDateTime.now())
                    .update();

            if (!itemReleaseSuccess) {
                log.warn("部分商品状态释放失败，可能需要人工介入检查。涉及商品ID: {}", itemIds);
            } else {
                log.info("成功释放 {} 个商品库存", itemIds.size());
            }
        }

    }


    @Transactional
    @Override
    public void shipment(Long orderId, String logisticsCompany, String trackingNumber) {
        Long sellerId  = BaseContext.getCurrentId();
        Order updateOrder = new Order();
        updateOrder.setId(orderId);
        updateOrder.setStatus(OrderStatusConstant.SHIPPED);
        // order 不包含物流信息 在物流表关联orderId
        //updateOrder.setLogisticsCompany(logisticsCompany); // 保存物流公司
        //updateOrder.setTrackingNumber(trackingNumber);     //  保存物流单号
        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, orderId)
                .eq(Order::getSellerId, sellerId) // 权限校验放在 SQL 中
                // 关键：确保只有未发货的订单才能被更新，防止并发和状态错乱
                // 允许发货的状态是：已支付
                .in(Order::getStatus, OrderStatusConstant.PAID, OrderStatusConstant.SHIPPED);
        int success = orderMapper.update(updateOrder, updateWrapper);
        if (success == 0) {
            // 更新失败，说明要么 ID 不存在，要么卖家不对，要么状态不对（已被别人发货或取消）
            // 需要查询具体原因返回给用户，或者直接抛通用异常
            Order dbOrder = orderMapper.selectById(orderId);
            if (dbOrder == null || !dbOrder.getSellerId().equals(sellerId)) {
                throw new BusinessException("订单不存在或无权操作");
            } else {
                throw new BusinessException("订单状态已变更，无法重复发货（当前状态：" + dbOrder.getStatus() + "）");
            }
        }
        //新增物流信息
        Shipment shipment = new Shipment();
        shipment.setOrderId(orderId);
        shipment.setSellerId(sellerId);
        shipment.setStatus(OrderStatusConstant.SHIPPED);
        shipment.setCreatedAt(LocalDateTime.now());
        shipment.setCompany(logisticsCompany);
        shipment.setTrackingNo(trackingNumber);
        int insertSuccess = shipmentMapper.insert(shipment);
        if (insertSuccess == 0) {
            throw new BusinessException("物流信息保存失败");
        }
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


}
