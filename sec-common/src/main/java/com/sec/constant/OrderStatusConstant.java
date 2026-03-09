package com.sec.constant;

public class OrderStatusConstant {
    private OrderStatusConstant() {}

    public static final Integer WAIT_PAY = 0;
    public static final Integer PAID = 1;
    public static final Integer SHIPPED = 2;
    public static final Integer FINISHED = 3;
    public static final Integer CANCELLED = 4;
    public static final Integer REFUNDING = 5;
    public static final Integer REFUNDED = 6;
}