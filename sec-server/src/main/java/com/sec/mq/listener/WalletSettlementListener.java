package com.sec.mq.listener;

import com.sec.constant.RabbitMQConstant;
import com.sec.message.WalletSettlementMessage;
import com.sec.service.IUserWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalletSettlementListener {

    private final IUserWalletService userWalletService;

    @RabbitListener(queues = RabbitMQConstant.QUEUE_ORDER_SETTLE)
    public void handle(WalletSettlementMessage msg) {
        userWalletService.transferFrozenToSeller(
                msg.getBuyerId(),
                msg.getSellerId(),
                msg.getAmount(),
                msg.getOrderNo()
        );
    }
}