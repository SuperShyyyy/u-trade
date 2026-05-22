package com.u.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.u.order.domain.po.MqMessageLog;
import com.u.order.mapper.MqMessageLogMapper;
import com.u.order.service.IMqMessageLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqMessageLogServiceImpl extends ServiceImpl<MqMessageLogMapper, MqMessageLog>
        implements IMqMessageLogService {

    @Override
    public void insert(MqMessageLog log) {
        save(log);
    }

    @Override
    public void updateStatus(String messageId, Integer status, String errorReason) {
        lambdaUpdate()
                .eq(MqMessageLog::getMessageId, messageId)
                .set(MqMessageLog::getStatus, status)
                .set(errorReason != null, MqMessageLog::getErrorReason, errorReason)
                .update();
    }


}
