package com.sec.config;

import com.sec.constant.RabbitMQConstant;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 队列与交换机配置类
 * 采用 TopicExchange 以支持更灵活的路由策略
 */
@Configuration
public class RabbitMQQueueConfig {

    // ==================== 商品同步 ES ====================

    /** 商品同步交换机 */
    @Bean
    public DirectExchange itemSyncExchange() {
        return new DirectExchange(RabbitMQConstant.EXCHANGE_ITEM_SYNC, true, false);
    }

    /** 商品同步队列 (配置死信) */
    @Bean
    public Queue itemSyncQueue() {
        Map<String, Object> args = new HashMap<>(4);
        // 死信交换机
        args.put("x-dead-letter-exchange", RabbitMQConstant.DLX_EXCHANGE);
        // 死信路由键
        args.put("x-dead-letter-routing-key", RabbitMQConstant.DLX_ROUTING_KEY);

        return QueueBuilder.durable(RabbitMQConstant.QUEUE_ITEM_SYNC)
                .withArguments(args)
                .build();
    }

    /** 商品同步队列绑定 */
    @Bean
    public Binding itemSyncBinding() {
        return BindingBuilder.bind(itemSyncQueue())
                .to(itemSyncExchange())
                .with(RabbitMQConstant.ROUTING_KEY_ITEM_SYNC);
    }

    // ==================== 死信队列配置 ====================

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(RabbitMQConstant.DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(RabbitMQConstant.DLX_QUEUE).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(RabbitMQConstant.DLX_ROUTING_KEY);
    }


    // ==================== 场景一：订单自动取消 (30分钟未支付) ====================
    // 流程：发送 -> [Delay Exchange] -> [Delay Queue (TTL 30m)]
    //       -> (过期) -> [Exec Exchange] -> [Exec Queue] -> 消费者


    /** 1.1 延迟交换机 (入口) */
    @Bean
    public TopicExchange orderCancelDelayExchange() {
        return new TopicExchange(RabbitMQConstant.EXCHANGE_ORDER_CANCEL_DELAY, true, false);
    }

    /** 1.2 延迟队列 (核心：设置 TTL 和 死信指向) */
    @Bean
    public Queue orderCancelDelayQueue() {
        Map<String, Object> args = new HashMap<>(4);
        // 30分钟 TTL (毫秒)
        args.put("x-message-ttl", 30 * 60 * 1000);
        // 死信交换机：消息过期后发往哪里
        args.put("x-dead-letter-exchange", RabbitMQConstant.EXCHANGE_ORDER_CANCEL_EXEC);
        // 死信路由键：消息过期后使用什么 Routing Key
        args.put("x-dead-letter-routing-key", RabbitMQConstant.ROUTING_KEY_ORDER_CANCEL_EXEC);
        // 开启 Lazy 模式，消息尽量存磁盘，节省内存
        args.put("x-queue-mode", "lazy");

        return QueueBuilder.durable(RabbitMQConstant.QUEUE_ORDER_CANCEL_DELAY)
                .withArguments(args)
                .build();
    }

    /** 1.3 延迟队列绑定 */
    @Bean
    public Binding orderCancelDelayBinding() {
        return BindingBuilder.bind(orderCancelDelayQueue())
                .to(orderCancelDelayExchange())
                .with(RabbitMQConstant.ROUTING_KEY_ORDER_CANCEL_DELAY);
    }

    // --- 以下为实现“死信接收”的实际执行组件 ---

    /** 1.4 实际执行交换机 (接收死信) */
    @Bean
    public TopicExchange orderCancelExecExchange() {
        return new TopicExchange(RabbitMQConstant.EXCHANGE_ORDER_CANCEL_EXEC, true, false);
    }

    /** 1.5 实际执行队列 (消费者监听此处) */
    @Bean
    public Queue orderCancelExecQueue() {
        return QueueBuilder.durable(RabbitMQConstant.QUEUE_ORDER_CANCEL_EXEC)
                .withArgument("x-queue-mode", "lazy")
                .build();
    }

    /** 1.6 实际执行队列绑定 */
    @Bean
    public Binding orderCancelExecBinding() {
        return BindingBuilder.bind(orderCancelExecQueue())
                .to(orderCancelExecExchange())
                .with(RabbitMQConstant.ROUTING_KEY_ORDER_CANCEL_EXEC);
    }


    // ==================== 场景二：自动确认收货并结算 (发货后7天) ====================
    // 流程：发送 -> [Delay Exchange] -> [Delay Queue (TTL 7d)]
    //       -> (过期) -> [Exec Exchange] -> [Exec Queue] -> 消费者

    /** 2.1 延迟交换机 (入口) */
    @Bean
    public TopicExchange orderSettleDelayExchange() {
        return new TopicExchange(RabbitMQConstant.EXCHANGE_ORDER_SETTLE_DELAY, true, false);
    }

    /** 2.2 延迟队列 (核心：设置 TTL 和 死信指向) */
    @Bean
    public Queue orderSettleDelayQueue() {
        Map<String, Object> args = new HashMap<>(4);
        // 7天 TTL (毫秒)
        args.put("x-message-ttl", 7 * 24 * 60 * 60 * 1000);
        // 【关键修复】必须配置死信转发，否则消息过期直接丢失
        args.put("x-dead-letter-exchange", RabbitMQConstant.EXCHANGE_ORDER_SETTLE_EXEC);
        args.put("x-dead-letter-routing-key", RabbitMQConstant.ROUTING_KEY_ORDER_SETTLE_EXEC);
        // 开启 Lazy 模式
        args.put("x-queue-mode", "lazy");

        return QueueBuilder.durable(RabbitMQConstant.QUEUE_ORDER_SETTLE_DELAY)
                .withArguments(args)
                .build();
    }

    /** 2.3 延迟队列绑定 */
    @Bean
    public Binding orderSettleDelayBinding() {
        return BindingBuilder.bind(orderSettleDelayQueue())
                .to(orderSettleDelayExchange())
                .with(RabbitMQConstant.ROUTING_KEY_ORDER_SETTLE_DELAY);
    }

    // --- 以下为实现“死信接收”的实际执行组件 ---

    /** 2.4 实际执行交换机 (接收死信) */
    @Bean
    public TopicExchange orderSettleExecExchange() {
        return new TopicExchange(RabbitMQConstant.EXCHANGE_ORDER_SETTLE_EXEC, true, false);
    }

    /** 2.5 实际执行队列 (消费者监听此处) */
    @Bean
    public Queue orderSettleExecQueue() {
        return QueueBuilder.durable(RabbitMQConstant.QUEUE_ORDER_SETTLE_EXEC)
                .withArgument("x-queue-mode", "lazy")
                .build();
    }

    /** 2.6 实际执行队列绑定 */
    @Bean
    public Binding orderSettleExecBinding() {
        return BindingBuilder.bind(orderSettleExecQueue())
                .to(orderSettleExecExchange())
                .with(RabbitMQConstant.ROUTING_KEY_ORDER_SETTLE_EXEC);
    }
}