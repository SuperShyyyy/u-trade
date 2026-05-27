package com.u.chat.config;

import com.u.chat.constant.ChatMqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

@Configuration
@EnableRabbit
@ConditionalOnProperty(prefix = "chat.distributed", name = "enabled", havingValue = "true")
public class ChatRabbitMqConfig {

    @Bean
    public MessageConverter chatMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate chatRabbitTemplate(ConnectionFactory connectionFactory,
                                             MessageConverter chatMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(chatMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public TopicExchange chatRouteExchange() {
        return new TopicExchange(ChatMqConstant.EXCHANGE_CHAT_ROUTE, true, false);
    }

    @Bean
    public Queue chatRouteQueue() {
        return new Queue(ChatMqConstant.QUEUE_CHAT_ROUTE, true);
    }

    @Bean
    public Binding chatRouteBinding(Queue chatRouteQueue, TopicExchange chatRouteExchange) {
        return BindingBuilder.bind(chatRouteQueue)
                .to(chatRouteExchange)
                .with(ChatMqConstant.ROUTING_KEY_CHAT_ROUTE);
    }
}
