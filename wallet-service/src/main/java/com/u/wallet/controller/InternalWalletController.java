package com.u.wallet.controller;

import com.u.api.internal.wallet.InternalWalletApi;
import com.u.common.result.Result;
import com.u.wallet.domain.vo.UserWalletVO;
import com.u.wallet.service.IUserWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/inner/wallet")
@RequiredArgsConstructor
public class InternalWalletController implements InternalWalletApi {

    private final IUserWalletService userWalletService;

    @Override
    public Result<Void> freezeAmount(Long userId, BigDecimal amount, String orderNo) {
        userWalletService.freezeAmount(userId, amount, orderNo);
        return Result.success();
    }

    @Override
    public Result<Void> unfreezeAmount(Long userId, BigDecimal amount, String orderNo) {
        userWalletService.unfreezeAmount(userId, amount, orderNo);
        return Result.success();
    }

    @Override
    public Result<Void> transferFrozenToSeller(Long buyerId, Long sellerId, BigDecimal amount, String orderNo) {
        userWalletService.transferFrozenToSeller(buyerId, sellerId, amount, orderNo);
        return Result.success();
    }

    @Override
    public Result<Void> createWallet(Long userId) {
        userWalletService.createWalletIfAbsent(userId);
        return Result.success();
    }
}
