package com.u.order.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.u.common.constant.OrderStatusConstant;
import com.u.order.domain.po.Order;
import com.u.order.mapper.OrderMapper;
import com.u.order.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTask {

    private final IOrderService orderService;
    private final OrderMapper orderMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 每3分钟扫描 Redis 延迟队列，执行自动确认收货
     */
    @Scheduled(cron = "0 */3 * * * ?")
    public void scanRedisAutoConfirm() {
        log.info("========== 订单自动确认任务开始 [{}] ==========", LocalDateTime.now());
        String zsetKey = "order:auto_confirm:queue";
        long nowSeconds = System.currentTimeMillis() / 1000;

        // 【修复】使用 stringRedisTemplate 避免反序列化异常
        Set<String> expiredOrderIds = stringRedisTemplate.opsForZSet()
                .rangeByScore(zsetKey, 0, nowSeconds, 0, 500);

        if (expiredOrderIds == null || expiredOrderIds.isEmpty()) {
            log.info("没有到期订单，本次任务结束");
            return;
        }

        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;

        log.info("开始处理 {} 个到期订单", expiredOrderIds.size());
        for (String idStr : expiredOrderIds) {
            Long orderId = Long.valueOf(idStr);

            // 抢占锁
            Long removed = stringRedisTemplate.opsForZSet().remove(zsetKey, idStr);

            if (removed != null && removed > 0) {
                log.debug("抢到订单处理权: {}", orderId);
                try {
                    // 【修复】直接通过接口调用，天然具有事务代理
                    orderService.processSingleOrderConfirm(orderId);
                    log.info("订单 {} 自动确认成功", orderId);
                    successCount++;
                } catch (Exception e) {
                    log.error("Redis 扫描任务处理订单 {} 失败，等待 DB 兜底", orderId, e);
                    failCount++;
                }
            } else {
                skipCount++;
            }
        }
        log.info("========== 订单自动确认任务结束 成功:{} 失败:{} 跳过:{} ==========",
                successCount, failCount, skipCount);
    }

    /**
     * 每30分钟执行一次数据库兜底扫描 (防止 Redis 宕机或丢数据)
     */
    @Scheduled(cron = "0 */30 * * * ?")
    public void autoConfirmDbCompensation() {
        LocalDateTime thresholdTime = LocalDateTime.now().minusDays(7);

        // 使用 Mapper 直接查询，避免在 Task 中写复杂的 LambdaQuery
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatusConstant.SHIPPED)
                .lt(Order::getShippedAt, thresholdTime)
                .last("limit 200"));

        if (orders.isEmpty()) return;

        for (Order order : orders) {
            try {
                orderService.processSingleOrderConfirm(order.getId());
            } catch (Exception e) {
                log.error("DB 兜底任务：订单 {} 自动确认异常", order.getId(), e);
            }
        }

        // 统一清理 Redis 残留
        List<String> orderIdsStr = orders.stream().map(o -> String.valueOf(o.getId())).toList();
        stringRedisTemplate.opsForZSet().remove("order:auto_confirm:queue", orderIdsStr.toArray());
    }

    /**
     * 每5分钟扫描超时未支付订单（DB 兜底，防止延迟 MQ 发送失败导致订单永久 WAIT_PAY）
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void cancelExpiredUnpaidOrders() {
        log.info("========== 超时未支付订单兜底扫描开始 [{}] ==========", LocalDateTime.now());

        // 30分钟前创建的待支付订单视为超时
        LocalDateTime expireThreshold = LocalDateTime.now().minusMinutes(30);

        List<Order> expiredOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatusConstant.WAIT_PAY)
                .lt(Order::getCreatedAt, expireThreshold)
                .last("limit 500"));

        if (expiredOrders.isEmpty()) {
            log.debug("无超时未支付订单");
            return;
        }

        log.info("扫描到 {} 个超时未支付订单", expiredOrders.size());
        int successCount = 0;
        int failCount = 0;

        for (Order order : expiredOrders) {
            try {
                orderService.cancelOrderInternal(order.getId(), "超时未支付自动取消");
                successCount++;
            } catch (Exception e) {
                log.error("超时取消订单失败 orderId={}", order.getId(), e);
                failCount++;
            }
        }

        log.info("========== 超时未支付订单兜底扫描结束 成功:{} 失败:{} ==========", successCount, failCount);
    }
}
