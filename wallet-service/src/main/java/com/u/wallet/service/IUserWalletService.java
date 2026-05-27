package com.u.wallet.service;

import com.u.wallet.domain.dto.WalletLogQueryDTO;
import com.u.wallet.domain.dto.WalletRechargeDTO;
import com.u.wallet.domain.dto.WalletWithdrawDTO;
import com.u.wallet.domain.po.UserWallet;
import com.baomidou.mybatisplus.extension.service.IService;
import com.u.wallet.domain.vo.UserWalletVO;
import com.u.wallet.domain.vo.WalletLogVO;
import com.u.common.result.PageDTO;
import jakarta.validation.Valid;

import java.math.BigDecimal;

/**
 * <p>
 * 用户钱包表 服务类
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
public interface IUserWalletService extends IService<UserWallet> {

    void createWalletIfAbsent(Long userId);

    UserWalletVO getWallet();

    PageDTO<WalletLogVO> getWalletLog(WalletLogQueryDTO dto);

    void recharge(@Valid WalletRechargeDTO dto);

    void withdraw(@Valid WalletWithdrawDTO dto);

    // 支付回调
    void handleRechargeSuccess(String bizOrderNo);

    // 冻结金额（下单）
    void freezeAmount(Long userId, BigDecimal amount, String orderNo);

    // 解冻金额（订单取消）
    void unfreezeAmount(Long userId, BigDecimal amount, String orderNo);

    void transferFrozenToSeller(Long buyerId, Long sellerId, BigDecimal amount, String orderNo);
}
