package com.u.api.controller.wallet;

import com.u.api.dto.wallet.WalletRechargeDTO;
import com.u.api.dto.wallet.WalletWithdrawDTO;
import com.u.api.service.wallet.WalletService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/freeze")
    public void freeze(@RequestParam Long userId,
                       @RequestParam BigDecimal amount,
                       @RequestParam String orderNo) {
        walletService.freezeAmount(userId, amount, orderNo);
    }

    @PostMapping("/unfreeze")
    public void unfreeze(@RequestParam Long userId,
                         @RequestParam BigDecimal amount,
                         @RequestParam String orderNo) {
        walletService.unfreezeAmount(userId, amount, orderNo);
    }

    @PostMapping("/settle")
    public void settle(@RequestParam Long buyerId,
                       @RequestParam Long sellerId,
                       @RequestParam BigDecimal amount,
                       @RequestParam String orderNo) {
        walletService.transferFrozenToSeller(buyerId, sellerId, amount, orderNo);
    }
}