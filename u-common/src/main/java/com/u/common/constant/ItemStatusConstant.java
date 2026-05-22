package com.u.common.constant;

/**
 * 商品状态常量
 * 状态流转逻辑：
 * 1. 卖家发布/审核通过 -> ON_SALE (1)
 * 2. 买家下单 -> LOCKED (2)  [此时商品被占用，其他人不可买]
 * 3. 买家支付成功 -> SOLD (-1) [交易完成]
 * 4. 订单取消/超时 -> 回退到 ON_SALE (1)
 * 5. 卖家手动下架 -> OFF_SALE (0)
 * 6. 待审核 -> AUDIT_PENDING (-2)
 */
public class ItemStatusConstant {
    public static final Integer AUDIT_PENDING = -2;
    public static final Integer SOLD = -1;
    public static final Integer OFF_SALE = 0;
    public static final Integer ON_SALE = 1;
    public static final Integer LOCKED = 2;
}
