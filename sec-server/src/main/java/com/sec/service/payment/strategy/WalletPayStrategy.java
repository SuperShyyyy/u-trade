package com.sec.service.payment.strategy;

import com.sec.domain.po.Order;
import com.sec.service.IUserWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalletPayStrategy implements OrderPayStrategy {

    private final IUserWalletService userWalletService;

    @Override
    public void pay(Order order) {
        userWalletService.freezeAmount(
                order.getBuyerId(),
                order.getTotalPrice(),
                order.getOrderNo()
        );
    }

    @Override
    public String buildPrepayInfo(Order order) {
        return "WALLET_PAID_SUCCESS";
    }
}
