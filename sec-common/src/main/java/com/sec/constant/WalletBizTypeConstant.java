package com.sec.constant;

public class WalletBizTypeConstant {
   // 1-充值
   public static final int RECHARGE = 1;
   // 2-下单冻结
   public static final int ORDER_FREEZE = 2;
   // 3-确认收货  (买家扣除冻结资金 卖家到账)
   public static final int ORDER_PAY = 3;
   // 订单取消/超时解冻
   public static final int CANCEL_UNFREEZE = 4;
   // 5-提现
   public static final int WITHDRAW = 5;
   // 6-退款
   public static final int REFUND = 6;

}
