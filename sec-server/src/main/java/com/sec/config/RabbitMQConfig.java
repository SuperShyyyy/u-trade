package com.sec.config;


import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory){
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        //消息发送到exchange确认
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack){
                System.out.println("消息发送到Exchange成功");
            }else{
                System.out.println("消息发送失败：" + cause);
            }
        });

        //消息从exchange到queue失败
        rabbitTemplate.setReturnsCallback(returned -> {
            System.out.println("消息路由失败：" + returned.getMessage());
        });

        return rabbitTemplate;
    }
}
