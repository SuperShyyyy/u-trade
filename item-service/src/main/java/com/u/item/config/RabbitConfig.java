package com.u.item.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

@Configuration
public class RabbitConfig {
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}