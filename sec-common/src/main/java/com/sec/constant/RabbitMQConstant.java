package com.sec.constant;

/**
 * RabbitMQ 常量定义
 * 规范说明：
 * 1. 所有队列名体现具体业务动作 (cancel, settle, confirm)
 * 2. 明确区分 .delay (休眠中) 和 .exec/.handle (执行中)
 * 3. 统一使用 order 作为主业务前缀 (假设结算也是订单域的一部分)
 */
public class RabbitMQConstant {

    // ==================== 场景一：订单自动取消 (30分钟未支付) ====================
    // 流程：用户下单 -> 发送消息到 [DELAY 队列] (TTL=30m) -> 过期 -> 转发到 [EXEC 队列] -> 消费者执行取消

    /** 取消订单 - 延迟交换机 (入口) */
    public static final String EXCHANGE_ORDER_CANCEL_DELAY = "order.cancel.delay.exchange";
    /** 取消订单 - 延迟队列 (设置 TTL 30分钟，绑定 DLX) */
    public static final String QUEUE_ORDER_CANCEL_DELAY = "order.cancel.delay.queue";
    /** 取消订单 - 延迟路由键 */
    public static final String ROUTING_KEY_ORDER_CANCEL_DELAY = "order.cancel.delay";

    /** 取消订单 - 实际执行交换机 (接收死信) */
    public static final String EXCHANGE_ORDER_CANCEL_EXEC = "order.cancel.exec.exchange";
    /** 取消订单 - 实际执行队列 (消费者监听这里，执行取消逻辑) */
    public static final String QUEUE_ORDER_CANCEL_EXEC = "order.cancel.exec.queue";
    /** 取消订单 - 实际执行路由键 */
    public static final String ROUTING_KEY_ORDER_CANCEL_EXEC = "order.cancel.exec";


    // ==================== 场景二：自动确认收货并结算 (发货后7天) ====================
    // 流程：商家发货 -> 发送消息到 [DELAY 队列] (TTL=7天) -> 过期 -> 转发到 [EXEC 队列] -> 消费者执行打款

    /** 自动结算 - 延迟交换机 (入口) */
    public static final String EXCHANGE_ORDER_SETTLE_DELAY = "order.settle.delay.exchange";
    /** 自动结算 - 延迟队列 (设置 TTL 7天，绑定 DLX) */
    public static final String QUEUE_ORDER_SETTLE_DELAY = "order.settle.delay.queue";
    /** 自动结算 - 延迟路由键 */
    public static final String ROUTING_KEY_ORDER_SETTLE_DELAY = "order.settle.delay";

    /** 自动结算 - 实际执行交换机 (接收死信) */
    public static final String EXCHANGE_ORDER_SETTLE_EXEC = "order.settle.exec.exchange";
    /** 自动结算 - 实际执行队列 (消费者监听这里，执行确认收货+打款) */
    public static final String QUEUE_ORDER_SETTLE_EXEC = "order.settle.exec.queue";
    /** 自动结算 - 实际执行路由键 */
    public static final String ROUTING_KEY_ORDER_SETTLE_EXEC = "order.settle.exec";

    // ==================== 场景三：(可选) 通用延迟占位 ====================
    // 如果之前那个 QUEUE_ORDER_DELAY 是想做通用的，建议废弃，改为具体业务命名。
    // 如果确实需要动态创建不同时间的延迟队列，可以使用前缀模式：
    public static final String PREFIX_QUEUE_ORDER_DELAY_DYNAMIC = "order.delay.dynamic.queue.";
}