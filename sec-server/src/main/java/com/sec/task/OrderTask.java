package com.sec.task;

import com.sec.domain.po.Order;
import com.sec.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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


}