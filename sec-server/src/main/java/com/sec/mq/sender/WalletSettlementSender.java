package com.sec.mq.sender;

import com.sec.constant.RabbitMQConstant;
import com.sec.message.WalletSettlementMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletSettlementSender {

    private final RabbitTemplate rabbitTemplate;


}