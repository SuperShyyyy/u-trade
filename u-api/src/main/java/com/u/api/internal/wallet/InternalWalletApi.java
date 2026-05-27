package com.u.api.internal.wallet;

import com.u.common.result.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

public interface InternalWalletApi {

    @PostMapping("/freeze")
    Result<Void> freezeAmount(@RequestParam("userId") Long userId,
                              @RequestParam("amount") BigDecimal amount,
                              @RequestParam("orderNo") String orderNo);

    @PostMapping("/unfreeze")
    Result<Void> unfreezeAmount(@RequestParam("userId") Long userId,
                                @RequestParam("amount") BigDecimal amount,
                                @RequestParam("orderNo") String orderNo);

    @PostMapping("/settle")
    Result<Void> transferFrozenToSeller(@RequestParam("buyerId") Long buyerId,
                                        @RequestParam("sellerId") Long sellerId,
                                        @RequestParam("amount") BigDecimal amount,
                                        @RequestParam("orderNo") String orderNo);
    @PostMapping("/create")
    Result<Void> createWallet(@RequestParam("userId") Long userId);
}
