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
@ApiModel(value="ChatConversation对象", description="聊天会话表")
public class ChatConversation implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "会话唯一 ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "买家 userId")
    private Long buyerId;

    @ApiModelProperty(value = "卖家 userId")
    private Long sellerId;

    @ApiModelProperty(value = "商品 ID")
    private Long itemId;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "最近消息时间，用于会话列表排序")
    private LocalDateTime lastMsgTime;

    @ApiModelProperty(value = "未读消息数量")
    private Integer unreadCount;


}
