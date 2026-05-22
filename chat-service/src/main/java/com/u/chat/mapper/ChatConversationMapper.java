package com.u.chat.mapper;

import com.u.chat.domain.po.ChatConversation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 聊天会话表 Mapper 接口
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Mapper
public interface ChatConversationMapper extends BaseMapper<ChatConversation> {

}
