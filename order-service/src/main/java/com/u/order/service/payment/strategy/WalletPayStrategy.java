package com.u.order.service.payment.strategy;

import com.u.api.client.wallet.WalletClient;
import com.u.common.exception.BusinessException;
import com.u.common.result.Result;
import com.u.common.result.ResultCode;
import com.u.order.domain.po.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalletPayStrategy implements OrderPayStrategy {

    private final WalletClient walletClient;

    @Override
    public void pay(Order order) {
        Result<Void> result = walletClient.freezeAmount(
                order.getBuyerId(),
                order.getTotalPrice(),
                order.getOrderNo()
        );
        if (result == null || !ResultCode.SUCCESS.equals(result.getCode())) {
            throw new BusinessException(result == null ? "钱包冻结失败" : result.getMessage());
        }
    }

    @Override
    public String buildPrepayInfo(Order order) {
        return "WALLET_PAID_SUCCESS";
    }
}
