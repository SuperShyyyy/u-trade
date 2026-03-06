package com.sec.service.impl;

import com.sec.domain.po.ChatMessage;
import com.sec.mapper.ChatMessageMapper;
import com.sec.service.IChatMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 聊天消息表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Service
public class ChatMessagServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements IChatMessageService {

}
