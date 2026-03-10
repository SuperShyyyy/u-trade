
package com.sec.mq.listener;

import com.sec.constant.RabbitMQConstant;
import com.sec.message.WalletSettlementMessage;
import com.sec.service.IUserWalletService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
@Component
@RequiredArgsConstructor
public class WalletSettlementListener {

    private final IUserWalletService userWalletService;

    @RabbitListener(queues = RabbitMQConstant.QUEUE_ORDER_SETTLE)
    public void handle(WalletSettlementMessage msg,
                       Channel channel,
                       Message message) throws IOException {

        long tag = message.getMessageProperties().getDeliveryTag();

        try {

            userWalletService.transferFrozenToSeller(
                    msg.getBuyerId(),
                    msg.getSellerId(),
                    msg.getAmount(),
                    msg.getOrderNo()
            );

            //确认消费
            channel.basicAck(tag, false);

        } catch (Exception e) {

            //消费失败重新入队
            channel.basicNack(tag, false, true);
        }
    }
}