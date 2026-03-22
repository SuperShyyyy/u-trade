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
 * 
 * </p>
 *
 * @author author
 * @since 2026-03-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("mq_message_log")
@ApiModel(value="MqMessageLog对象", description="")
public class MqMessageLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "全局消息ID")
    private String messageId;

    private String exchange;

    private String routingKey;

    @ApiModelProperty(value = "消息内容(JSON)")
    private String messageBody;

    @ApiModelProperty(value = "0-发送中 1-发送成功 2-发送失败 3-已消费")
    private Integer status;

    private Integer retryCount;

    private String errorReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


}
