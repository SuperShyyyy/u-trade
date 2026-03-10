package com.sec.task;

import com.sec.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTask {

    private final IOrderService orderService;

    /**
     * 每10分钟执行一次
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void autoConfirmOrder() {
        log.info("开始执行自动确认收货任务");

        orderService.autoConfirm();

        log.info("自动确认收货任务执行完成");
    }

    @Scheduled (cron = "0 */3 * * * ?")
    public void autoCancelOrder() {
        log.info("开始执行自动取消超时订单任务");

        orderService.autoCancelTimeoutOrders();

        log.info("自动取消超时订单任务完成");
    }
}