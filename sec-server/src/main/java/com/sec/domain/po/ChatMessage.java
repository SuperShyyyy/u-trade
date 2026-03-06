package com.sec.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel(value="ChatMessages对象", description="聊天消息表")
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "消息唯一 ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "外键，指向 chat_conversations.id")
    private Long conversationId;

    @ApiModelProperty(value = "发送者 ID")
    private Long senderId;

    @ApiModelProperty(value = "消息内容")
    private String content;

    @ApiModelProperty(value = "消息类型：1=text, 2=image, 3=file, 4=system")
    private Integer type;

    @ApiModelProperty(value = "状态：0=已发送, 1=已读, 2=撤回")
    private Integer status;

    @ApiModelProperty(value = "发送时间")
    private LocalDateTime createTime;


}
