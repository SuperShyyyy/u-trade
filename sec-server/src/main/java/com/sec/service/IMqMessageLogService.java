package com.sec.service;

import com.sec.domain.po.MqMessageLog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author author
 * @since 2026-03-15
 */
public interface IMqMessageLogService extends IService<MqMessageLog> {
    void insert(MqMessageLog log);

    /**
     * 更新消息发送状态
     * @param messageId 消息ID
     * @param status 状态（1成功，2失败）
     * @param errorReason 错误原因（失败时填写）
     */
    void updateStatus(String messageId, Integer status, String errorReason);
}
