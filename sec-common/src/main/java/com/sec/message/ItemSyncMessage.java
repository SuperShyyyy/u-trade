package com.sec.message;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品同步 ES 的 MQ 消息体
 */
@Data
public class ItemSyncMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品 ID */
    private Long id;
    /** 新增：用于批量操作的 ID 集合 */
    private List<Long> ids;
    /** 操作类型 */
    private OperationType operationType;

    /** 商品数据 (DELETE 操作时可部分为空) */
    private Long sellerId;
    private String title;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Long categoryId;
    private String cover;
    private String images;
    private Integer status;
    private Integer auditStatus;
    private Long viewCount;
    private Long wantCount;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 操作时间 */
    private LocalDateTime operateTime;

    /**
     * 操作类型枚举
     */
    public enum OperationType {
        ADD,
        UPDATE,
        DELETE,
        BATCH_UPDATE_STATUS
    }
}