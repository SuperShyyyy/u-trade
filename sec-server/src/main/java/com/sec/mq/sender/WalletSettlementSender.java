package com.sec.mq.sender;

import com.sec.constant.RabbitMQConstant;
import com.sec.message.WalletSettlementMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalletSettlementSender {

    private final RabbitTemplate rabbitTemplate;

    public void send(WalletSettlementMessage msg) {
        rabbitTemplate.convertAndSend(
                RabbitMQConstant.EXCHANGE_ORDER_SETTLE,
                RabbitMQConstant.ROUTING_KEY_ORDER_SETTLE,
                msg
        );
    }
}