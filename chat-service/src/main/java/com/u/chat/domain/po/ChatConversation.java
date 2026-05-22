package com.u.chat.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 聊天会话表
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("chat_conversation")
@Schema(description = "聊天会话表")
public class ChatConversation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "会话唯一 ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "买家 userId")
    private Long buyerId;

    @Schema(description = "卖家 userId")
    private Long sellerId;

    @Schema(description = "商品 ID")
    private Long itemId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "最近消息时间，用于会话列表排序")
    private LocalDateTime lastMsgTime;

    @Schema(description = "未读消息数量")
    private Integer unreadCount;


}
