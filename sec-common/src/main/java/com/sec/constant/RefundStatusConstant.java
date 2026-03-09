package com.sec.constant;

public class RefundStatusConstant {
    private RefundStatusConstant() {}

    public static final Integer WAIT_SELLER = 0;       // 待卖家处理
    public static final Integer SELLER_REFUSED = 1;    // 卖家拒绝
    public static final Integer WAIT_BUYER_RETURN = 2; // 待买家退货
    public static final Integer REFUND_COMPLETED = 3;  // 退款完成
    public static final Integer CUSTOMER_SERVICE = 4;  // 客服介入
}