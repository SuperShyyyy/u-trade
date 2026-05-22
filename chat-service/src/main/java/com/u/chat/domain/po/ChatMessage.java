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
 * 聊天消息表
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("chat_message")
@Schema(description = "聊天消息表")
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "消息唯一 ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "外键，指向 chat_conversations.id")
    private Long conversationId;

    @Schema(description = "发送者 ID")
    private Long senderId;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "消息类型：1=text, 2=image, 3=file, 4=system")
    private Integer type;

    @Schema(description = "状态：0=已发送, 1=已读, 2=撤回")
    private Integer status;

    @Schema(description = "发送时间")
    private LocalDateTime createTime;


}
