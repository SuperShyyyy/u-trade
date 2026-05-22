package com.u.api.service.wallet;

import com.u.api.client.wallet.WalletClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WalletService {

    private final WalletClient walletClient;

    public WalletService(WalletClient walletClient) {
        this.walletClient = walletClient;
    }

    public void freezeAmount(Long userId, BigDecimal amount, String orderNo) {
        walletClient.freezeAmount(userId, amount, orderNo);
    }

    public void unfreezeAmount(Long userId, BigDecimal amount, String orderNo) {
        walletClient.unfreezeAmount(userId, amount, orderNo);
    }

    public void transferFrozenToSeller(Long buyerId, Long sellerId, BigDecimal amount, String orderNo) {
        walletClient.transferFrozenToSeller(buyerId, sellerId, amount, orderNo);
    }
}