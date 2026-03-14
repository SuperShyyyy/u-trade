package com.sec.mq.listener;

import com.sec.constant.RabbitMQConstant;
import com.sec.message.WalletSettlementMessage;
import com.sec.service.IUserWalletService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletSettlementListener {

    private final IUserWalletService userWalletService;
    private final StringRedisTemplate stringRedisTemplate;


}