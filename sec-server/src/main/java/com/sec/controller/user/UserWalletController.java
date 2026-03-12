package com.sec.controller.user;


import com.sec.domain.dto.WalletLogQueryDTO;
import com.sec.domain.dto.WalletRechargeDTO;
import com.sec.domain.dto.WalletWithdrawDTO;
import com.sec.domain.po.UserWallet;
import com.sec.domain.po.WalletLog;
import com.sec.domain.vo.UserWalletVO;
import com.sec.domain.vo.WalletLogVO;
import com.sec.result.PageDTO;
import com.sec.result.Result;
import com.sec.service.IUserWalletService;
import io.swagger.annotations.ApiOperation;
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
    @ApiOperation("查询钱包余额")
    @GetMapping
    public Result<UserWalletVO> getWallet() {
        return Result.success(userWalletService.getWallet());
    }

    @ApiOperation("查询流水列表")
    @GetMapping("logs")
    public Result<PageDTO<WalletLogVO>> getWalletLog(WalletLogQueryDTO dto) {
        return Result.success(userWalletService.getWalletLog(dto));
    }

    @ApiOperation("充值")
    @PostMapping("/recharge")
    public Result recharge(@RequestBody @Valid WalletRechargeDTO dto) {
        userWalletService.recharge(dto);
        return Result.success();
    }

    @ApiOperation("提现")
    @PostMapping("withdraw")
    public Result withdraw(@RequestBody @Valid WalletWithdrawDTO dto) {
        userWalletService.withdraw(dto);
        return Result.success();
    }
}
