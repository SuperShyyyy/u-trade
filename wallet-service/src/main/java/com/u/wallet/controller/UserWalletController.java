package com.u.wallet.controller;


import com.u.wallet.domain.dto.WalletLogQueryDTO;
import com.u.wallet.domain.dto.WalletRechargeDTO;
import com.u.wallet.domain.dto.WalletWithdrawDTO;
import com.u.wallet.domain.vo.UserWalletVO;
import com.u.wallet.domain.vo.WalletLogVO;
import com.u.common.result.PageDTO;
import com.u.common.result.Result;
import com.u.wallet.service.IUserWalletService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 用户钱包表 前端控制器
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@RestController
@RequestMapping("/user/wallet")
@RequiredArgsConstructor
public class UserWalletController {
    private final IUserWalletService userWalletService;
    @Operation(summary = "查询钱包余额")
    @GetMapping
    public Result<UserWalletVO> getWallet() {
        return Result.success(userWalletService.getWallet());
    }

    @Operation(summary = "查询流水列表")
    @GetMapping("logs")
    public Result<PageDTO<WalletLogVO>> getWalletLog(WalletLogQueryDTO dto) {
        return Result.success(userWalletService.getWalletLog(dto));
    }

    @Operation(summary = "充值")
    @PostMapping("/recharge")
    public Result recharge(@RequestBody @Valid WalletRechargeDTO dto) {
        userWalletService.recharge(dto);
        return Result.success();
    }

    @Operation(summary = "提现")
    @PostMapping("withdraw")
    public Result withdraw(@RequestBody @Valid WalletWithdrawDTO dto) {
        userWalletService.withdraw(dto);
        return Result.success();
    }
}
