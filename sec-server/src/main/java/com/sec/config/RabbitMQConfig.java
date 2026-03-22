package com.sec.config;

import com.sec.service.IMqMessageLogService;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    /**
     * JSON消息转换器
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate 配置
     */
    // RabbitMQConfig.java 中 rabbitTemplate 方法
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter,
                                         IMqMessageLogService logService) { // 注入日志服务
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);

        // 开启发布确认
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (correlationData == null) return;
            String messageId = correlationData.getId();
            if (ack) {
                logService.updateStatus(messageId, 1, null);
            } else {
                logService.updateStatus(messageId, 2, cause);
                // 可触发重试机制（例如重新发送）
            }
        });

        // 开启返回确认（路由失败）
        rabbitTemplate.setReturnsCallback(returned -> {
            String messageId = returned.getMessage().getMessageProperties().getMessageId();
            logService.updateStatus(messageId, 2, "路由失败：" + returned.getReplyText());
            // 可触发重试
        });

        return rabbitTemplate;
    }
}