package com.u.order.task;

import com.alibaba.fastjson2.JSON;
import com.u.common.constant.MqMessageLogStatus;
import com.u.order.domain.po.MqMessageLog;
import com.u.common.message.OrderSettlementMessage;
import com.u.order.mq.sender.OrderSettlementSender;
import com.u.order.service.IMqMessageLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class RetrySendMessage {
    private final IMqMessageLogService mqMessageLogService;
    private final OrderSettlementSender orderSettlementSender;
    @Scheduled(cron = "0 */5 * * * ?")
    public void retrySendMessage() {

        List<MqMessageLog> list = mqMessageLogService.lambdaQuery()
                .in(MqMessageLog::getStatus,
                        MqMessageLogStatus.SENDING, // 发送中
                        MqMessageLogStatus.FAILED) //发送失败
                .lt(MqMessageLog::getRetryCount, 3)
                .orderByAsc(MqMessageLog::getCreateTime)
                .last("limit 100")
                .list();

        for (MqMessageLog msgLog : list) {
            try {
                OrderSettlementMessage msg = JSON.parseObject(
                        msgLog.getMessageBody(),
                        OrderSettlementMessage.class
                );
                boolean locked = mqMessageLogService.lambdaUpdate()
                        .eq(MqMessageLog::getMessageId, msgLog.getMessageId())
                        .eq(MqMessageLog::getRetryCount, msgLog.getRetryCount())
                        .set(MqMessageLog::getRetryCount, msgLog.getRetryCount() + 1)
                        .update();
                if (!locked) {
                    continue; // 被其他线程抢走
                }
                orderSettlementSender.send(msg);

                log.info("MQ重试成功 messageId={}", msgLog.getMessageId());

            } catch (Exception e) {
                log.error("MQ重试失败 messageId={}", msgLog.getMessageId(), e);
            }
        }
    }

}
