package com.sec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.injector.methods.SelectOne;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sec.constant.WalletBizTypeConstant;
import com.sec.context.BaseContext;
import com.sec.domain.dto.WalletLogQueryDTO;
import com.sec.domain.dto.WalletRechargeDTO;
import com.sec.domain.dto.WalletWithdrawDTO;
import com.sec.domain.po.UserWallet;
import com.sec.domain.po.WalletLog;
import com.sec.domain.po.WalletRecharge;
import com.sec.domain.vo.UserWalletVO;
import com.sec.domain.vo.WalletLogVO;
import com.sec.mapper.UserWalletMapper;
import com.sec.mapper.WalletLogMapper;
import com.sec.mapper.WalletRechargeMapper;
import com.sec.result.PageDTO;
import com.sec.service.IUserWalletService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sec.utils.SerialNoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.jaxb.SpringDataJaxb;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户钱包表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserWalletServiceImpl extends ServiceImpl<UserWalletMapper, UserWallet> implements IUserWalletService {
    private final WalletLogMapper walletLogMapper;
    private final UserWalletMapper userWalletMapper;
    private final WalletRechargeMapper walletRechargeMapper;
    @Override
    public UserWalletVO getWallet() {
        Long userId = BaseContext.getCurrentId();
        UserWallet wallet = lambdaQuery().eq(UserWallet::getUserId, userId).one();
        if (wallet == null) {
            wallet = new UserWallet()
                    .setUserId(userId)
                    .setBalance(BigDecimal.ZERO)
                    .setFrozenAmount(BigDecimal.ZERO)
                    .setTotalIncome(BigDecimal.ZERO)
                    .setTotalExpense(BigDecimal.ZERO)
                    .setVersion(0); // 初始化 version
            this.save(wallet);
        }
        UserWalletVO walletVO = new UserWalletVO();
        BeanUtils.copyProperties(wallet, walletVO);
        return walletVO;
    }

    @Override
    public PageDTO<WalletLogVO> getWalletLog(WalletLogQueryDTO dto) {
        Long userId = BaseContext.getCurrentId();
        Page<WalletLog> pageParam = new Page<>(dto.getPage(),dto.getPageSize());
        LambdaQueryWrapper<WalletLog> query = new LambdaQueryWrapper<WalletLog>()
                .eq(WalletLog::getUserId, userId)
                .orderByDesc(WalletLog::getCreateTime);
        if (dto.getBizOrderNo() != null) {
            query.eq(WalletLog::getBizOrderNo, dto.getBizOrderNo());
        }
        if (dto.getStartTime() != null) {
            query.ge(WalletLog::getCreateTime, dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            query.le(WalletLog::getCreateTime, dto.getEndTime());
        }
        Page<WalletLog> walletLogPage = walletLogMapper.selectPage(pageParam, query);
        IPage<WalletLogVO> voLogs = walletLogPage.convert(log -> {
            WalletLogVO vo = new WalletLogVO();
            vo.setBizType(log.getBizType());
            vo.setAmount(log.getAmount());
            vo.setBalanceBefore(log.getBalanceBefore());
            vo.setBalanceAfter(log.getBalanceAfter());
            vo.setFrozenBefore(log.getFrozenBefore());
            vo.setFrozenAfter(log.getFrozenAfter());
            vo.setBizOrderNo(log.getBizOrderNo());
            vo.setDescription(log.getDescription());
            vo.setCreateTime(log.getCreateTime());
            return vo;
        });
        return PageDTO.of(voLogs);
    }

    // todo 充值
    @Transactional
    @Override
    public void recharge(WalletRechargeDTO dto) {
        Long userId = BaseContext.getCurrentId();
        String bizOrderNo = SerialNoUtil.generateRechargeNo();
        WalletRecharge record = new WalletRecharge();
        record.setUserId(userId);
        record.setAmount(dto.getAmount());
        record.setBizOrderNo(bizOrderNo);
        record.setStatus(0); // WAIT_PAY
        record.setCreateTime(LocalDateTime.now());

        walletRechargeMapper.insert(record);

        // TODO 调用第三方支付接口（支付宝 / 微信）
    }

    // todo 提现
    @Transactional
    @Override
    public void withdraw(WalletWithdrawDTO dto) {

        Long userId = BaseContext.getCurrentId();

        String bizOrderNo = dto.getBizOrderNo();
        if (bizOrderNo == null || bizOrderNo.isEmpty()) {
            bizOrderNo = SerialNoUtil.generateWithdrawNo();
        }

        BigDecimal amount = dto.getAmount();

        UserWallet wallet = userWalletMapper.selectOne(
                Wrappers.<UserWallet>lambdaQuery()
                        .eq(UserWallet::getUserId, userId)
        );

        if (wallet == null) {
            throw new RuntimeException("钱包不存在");
        }

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("余额不足");
        }

        BigDecimal before = wallet.getBalance();
        Integer version = wallet.getVersion();

        BigDecimal after = before.subtract(amount);
        BigDecimal totalExpense = wallet.getTotalExpense().add(amount);

        int update = userWalletMapper.updateWithdrawWallet(
                wallet.getId(),
                after,
                totalExpense,
                version
        );

        if (update == 0) {
            throw new RuntimeException("提现失败，发生并发");
        }

        // 写流水
        WalletLog log = new WalletLog();
        log.setUserId(userId);
        log.setWalletId(wallet.getId());
        log.setBizType(WalletBizTypeConstant.WITHDRAW);
        log.setAmount(amount.negate());
        log.setBalanceBefore(before);
        log.setBalanceAfter(after);
        log.setFrozenBefore(wallet.getFrozenAmount());
        log.setFrozenAfter(wallet.getFrozenAmount());
        log.setBizOrderNo(bizOrderNo);
        log.setDescription("提现");
        log.setCreateTime(LocalDateTime.now());

        walletLogMapper.insert(log);

        // TODO 调用提现接口（支付宝 / 微信）
    }


    @Transactional
    @Override
    public void handleRechargeSuccess(String bizOrderNo) {

        WalletRecharge record = walletRechargeMapper.selectOne(
                Wrappers.<WalletRecharge>lambdaQuery()
                        .eq(WalletRecharge::getBizOrderNo, bizOrderNo)
        );

        if (record == null) {
            throw new RuntimeException("充值记录不存在");
        }

        // 幂等控制
        if (record.getStatus() == WalletBizTypeConstant.RECHARGE) {
            return;
        }

        Long userId = record.getUserId();
        BigDecimal amount = record.getAmount();

        UserWallet wallet = userWalletMapper.selectOne(
                Wrappers.<UserWallet>lambdaQuery()
                        .eq(UserWallet::getUserId, userId)
        );

        BigDecimal before = wallet.getBalance();
        Integer version = wallet.getVersion();

        BigDecimal after = before.add(amount);
        BigDecimal income = wallet.getTotalIncome().add(amount);

        int update = userWalletMapper.updateRechargeWallet(
                wallet.getId(),
                after,
                income,
                version
        );

        if (update == 0) {
            throw new RuntimeException("充值更新失败，发生并发");
        }

        // 更新充值状态
        record.setStatus(WalletBizTypeConstant.RECHARGE);
        walletRechargeMapper.updateById(record);

        // 写流水
        WalletLog log = new WalletLog();
        log.setUserId(userId);
        log.setWalletId(wallet.getId());
        log.setBizType(WalletBizTypeConstant.RECHARGE);
        log.setAmount(amount);
        log.setBalanceBefore(before);
        log.setBalanceAfter(after);
        log.setFrozenBefore(wallet.getFrozenAmount());
        log.setFrozenAfter(wallet.getFrozenAmount());
        log.setBizOrderNo(bizOrderNo);
        log.setDescription("充值");
        log.setCreateTime(LocalDateTime.now());

        walletLogMapper.insert(log);
    }

    @Transactional
    @Override
    public void freezeAmount(Long userId, BigDecimal amount, String orderNo) {

        UserWallet wallet = userWalletMapper.selectOne(
                Wrappers.<UserWallet>lambdaQuery()
                        .eq(UserWallet::getUserId, userId)
        );

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("余额不足");
        }

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal frozenBefore = wallet.getFrozenAmount();
        Integer version = wallet.getVersion();

        BigDecimal balanceAfter = balanceBefore.subtract(amount);
        BigDecimal frozenAfter = frozenBefore.add(amount);

        int update = userWalletMapper.updateFreezeWallet(
                wallet.getId(),
                balanceAfter,
                frozenAfter,
                version
        );

        if (update == 0) {
            throw new RuntimeException("冻结失败，发生并发");
        }

        WalletLog log = new WalletLog();
        log.setUserId(userId);
        log.setWalletId(wallet.getId());
        log.setBizType(WalletBizTypeConstant.ORDER_FREEZE);
        log.setAmount(amount.negate());
        log.setBalanceBefore(balanceBefore);
        log.setBalanceAfter(balanceAfter);
        log.setFrozenBefore(frozenBefore);
        log.setFrozenAfter(frozenAfter);
        log.setBizOrderNo(orderNo);
        log.setDescription("订单冻结");
        log.setCreateTime(LocalDateTime.now());

        walletLogMapper.insert(log);
    }

    @Transactional
    @Override
    public void unfreezeAmount(Long userId, BigDecimal amount, String orderNo) {

        UserWallet wallet = userWalletMapper.selectOne(
                Wrappers.<UserWallet>lambdaQuery()
                        .eq(UserWallet::getUserId, userId)
        );

        if (wallet.getFrozenAmount().compareTo(amount) < 0) {
            throw new RuntimeException("冻结金额不足");
        }

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal frozenBefore = wallet.getFrozenAmount();
        Integer version = wallet.getVersion();

        BigDecimal balanceAfter = balanceBefore.add(amount);
        BigDecimal frozenAfter = frozenBefore.subtract(amount);

        int update = userWalletMapper.updateUnfreezeWallet(
                wallet.getId(),
                balanceAfter,
                frozenAfter,
                version
        );

        if (update == 0) {
            throw new RuntimeException("解冻失败，发生并发");
        }

        WalletLog log = new WalletLog();
        log.setUserId(userId);
        log.setWalletId(wallet.getId());
        log.setBizType(WalletBizTypeConstant.CANCEL_UNFREEZE);
        log.setAmount(amount);
        log.setBalanceBefore(balanceBefore);
        log.setBalanceAfter(balanceAfter);
        log.setFrozenBefore(frozenBefore);
        log.setFrozenAfter(frozenAfter);
        log.setBizOrderNo(orderNo);
        log.setDescription("订单解冻");
        log.setCreateTime(LocalDateTime.now());

        walletLogMapper.insert(log);
    }



    @Transactional
    @Override
    public void transferFrozenToSeller(Long buyerId, Long sellerId, BigDecimal amount, String orderNo) {

        // 1 查询买家钱包
        UserWallet buyerWallet = userWalletMapper.selectOne(
                Wrappers.<UserWallet>lambdaQuery()
                        .eq(UserWallet::getUserId, buyerId)
        );

        if (buyerWallet == null) {
            throw new RuntimeException("买家钱包不存在");
        }

        if (buyerWallet.getFrozenAmount().compareTo(amount) < 0) {
            throw new RuntimeException("冻结金额不足");
        }

        // 记录旧值
        BigDecimal buyerFrozenBefore = buyerWallet.getFrozenAmount();
        Integer buyerVersion = buyerWallet.getVersion();

        // 新值
        BigDecimal buyerFrozenAfter = buyerFrozenBefore.subtract(amount);
        BigDecimal buyerTotalExpense = buyerWallet.getTotalExpense().add(amount);

        // 2 乐观锁更新 buyer
        int buyerUpdate = userWalletMapper.updateBuyerWallet(
                buyerWallet.getId(),
                buyerFrozenAfter,
                buyerTotalExpense,
                buyerVersion
        );

        if (buyerUpdate == 0) {
            throw new RuntimeException("买家钱包更新失败，发生并发冲突");
        }

        // 3 查询卖家钱包
        UserWallet sellerWallet = userWalletMapper.selectOne(
                Wrappers.<UserWallet>lambdaQuery()
                        .eq(UserWallet::getUserId, sellerId)
        );

        if (sellerWallet == null) {
            throw new RuntimeException("卖家钱包不存在");
        }

        BigDecimal sellerBalanceBefore = sellerWallet.getBalance();
        Integer sellerVersion = sellerWallet.getVersion();

        BigDecimal sellerBalanceAfter = sellerBalanceBefore.add(amount);
        BigDecimal sellerTotalIncome = sellerWallet.getTotalIncome().add(amount);

        // 4 乐观锁更新 seller
        int sellerUpdate = userWalletMapper.updateSellerWallet(
                sellerWallet.getId(),
                sellerBalanceAfter,
                sellerTotalIncome,
                sellerVersion
        );

        if (sellerUpdate == 0) {
            throw new RuntimeException("卖家钱包更新失败，发生并发冲突");
        }

        // 5 buyer流水
        WalletLog buyerLog = new WalletLog();
        buyerLog.setUserId(buyerId);
        buyerLog.setWalletId(buyerWallet.getId());
        buyerLog.setBizType(WalletBizTypeConstant.ORDER_PAY);
        buyerLog.setAmount(amount.negate());
        buyerLog.setBalanceBefore(buyerWallet.getBalance());
        buyerLog.setBalanceAfter(buyerWallet.getBalance());
        buyerLog.setFrozenBefore(buyerFrozenBefore);
        buyerLog.setFrozenAfter(buyerFrozenAfter);
        buyerLog.setBizOrderNo(orderNo);
        buyerLog.setDescription("订单支付给卖家");
        buyerLog.setCreateTime(LocalDateTime.now());

        walletLogMapper.insert(buyerLog);

        // 6 seller流水
        WalletLog sellerLog = new WalletLog();
        sellerLog.setUserId(sellerId);
        sellerLog.setWalletId(sellerWallet.getId());
        sellerLog.setBizType(WalletBizTypeConstant.ORDER_PAY);
        sellerLog.setAmount(amount);
        sellerLog.setBalanceBefore(sellerBalanceBefore);
        sellerLog.setBalanceAfter(sellerBalanceAfter);
        sellerLog.setFrozenBefore(sellerWallet.getFrozenAmount());
        sellerLog.setFrozenAfter(sellerWallet.getFrozenAmount());
        sellerLog.setBizOrderNo(orderNo);
        sellerLog.setDescription("订单收款");
        sellerLog.setCreateTime(LocalDateTime.now());

        walletLogMapper.insert(sellerLog);
    }

}
