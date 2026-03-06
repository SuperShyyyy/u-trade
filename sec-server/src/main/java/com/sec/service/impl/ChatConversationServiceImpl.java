package com.sec.service.impl;

import com.sec.domain.po.ChatConversation;
import com.sec.mapper.ChatConversationMapper;
import com.sec.service.IChatConversationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 聊天会话表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Service
public class ChatConversationServiceImpl extends ServiceImpl<ChatConversationMapper, ChatConversation> implements IChatConversationService {

}
