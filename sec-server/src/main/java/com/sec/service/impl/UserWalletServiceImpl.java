package com.sec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.injector.methods.SelectOne;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
                    .setTotalExpense(BigDecimal.ZERO);
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
    @Transactional
    @Override
    public void recharge(WalletRechargeDTO dto) {

        Long userId = BaseContext.getCurrentId();

        String bizOrderNo = SerialNoUtil.generateRechargeNo();

        // 写充值记录
        WalletRecharge record = new WalletRecharge();
        record.setUserId(userId);
        record.setAmount(dto.getAmount());
        record.setBizOrderNo(bizOrderNo);
        record.setStatus(0); // WAIT_PAY
        record.setCreateTime(LocalDateTime.now());

        walletRechargeMapper.insert(record);

        // 调用支付接口
        // payService.createPayOrder()

    }


    @Transactional
    @Override
    public void withdraw(WalletWithdrawDTO dto) {
        Long userId = BaseContext.getCurrentId();
        String bizOrderNo  = dto.getBizOrderNo();
        if (bizOrderNo  == null || bizOrderNo .isEmpty()) {
            bizOrderNo  = SerialNoUtil.generateWithdrawNo();
        }
        BigDecimal amount = dto.getAmount();
        // 查询钱包余额
        UserWallet wallet = userWalletMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserWallet>()
                        .eq(UserWallet::getUserId, userId)
        );
        if (wallet == null || wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("余额不足");
        }

        // 扣减余额 + 生成流水（bizType=5）
        BigDecimal balanceBefore = wallet.getBalance();
        wallet.setBalance(wallet.getBalance().subtract(amount))
                .setTotalExpense(wallet.getTotalExpense().add(amount))
                .setUpdateTime(LocalDateTime.now());
        userWalletMapper.updateById(wallet);

        // 写流水
        WalletLog log = new WalletLog();
        log.setUserId(userId);
        log.setWalletId(wallet.getId());
        log.setBizType(5);
        log.setAmount(amount.negate());
        log.setBalanceBefore(balanceBefore);
        log.setBalanceAfter(wallet.getBalance());
        log.setFrozenBefore(wallet.getFrozenAmount());
        log.setFrozenAfter(wallet.getFrozenAmount());
        log.setBizOrderNo(bizOrderNo);
        log.setDescription("提现");
        log.setCreateTime(LocalDateTime.now());
        walletLogMapper.insert(log);

        // TODO: 调用提现支付接口 支付宝
    }


    @Transactional
    @Override
    public void handleRechargeSuccess(String bizOrderNo) {

        LambdaQueryWrapper<WalletRecharge> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WalletRecharge::getBizOrderNo, bizOrderNo);
        WalletRecharge record = walletRechargeMapper.selectOne(queryWrapper);
        if (record == null) {
            throw new RuntimeException("充值记录不存在");
        }
        if ( record.getStatus() == 1 ) {
            return; // 防重复回调
        }
        Long userId = record.getUserId();
        BigDecimal amount = record.getAmount();

        UserWallet wallet = userWalletMapper.selectOne(
                Wrappers.<UserWallet>lambdaQuery()
                        .eq(UserWallet::getUserId, userId)
                        .last("for update")
        );
        BigDecimal before = wallet.getBalance();

        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setTotalIncome(wallet.getTotalIncome().add(amount));

        userWalletMapper.updateById(wallet);
        // 更新充值状态
        record.setStatus(1);
        walletRechargeMapper.updateById(record);
        // 写流水
        WalletLog log = new WalletLog();
        log.setUserId(userId);
        log.setWalletId(wallet.getId());
        log.setBizType(1);
        log.setAmount(amount);
        log.setBalanceBefore(before);
        log.setBalanceAfter(wallet.getBalance());
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
                        .last("for update")
        );
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("余额不足");
        }
        BigDecimal before = wallet.getBalance();
        BigDecimal frozenBefore = wallet.getFrozenAmount();
        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setFrozenAmount(wallet.getFrozenAmount().add(amount));
        userWalletMapper.updateById(wallet);
        WalletLog log = new WalletLog();
        log.setUserId(userId);
        log.setWalletId(wallet.getId());
        log.setBizType(2);
        log.setAmount(amount.negate());
        log.setBalanceBefore(before);
        log.setBalanceAfter(wallet.getBalance());
        log.setFrozenBefore(frozenBefore);
        log.setFrozenAfter(wallet.getFrozenAmount());
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
                        .last("for update")
        );

        BigDecimal before = wallet.getBalance();
        BigDecimal frozenBefore = wallet.getFrozenAmount();

        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setFrozenAmount(wallet.getFrozenAmount().subtract(amount));

        userWalletMapper.updateById(wallet);

        WalletLog log = new WalletLog();
        log.setUserId(userId);
        log.setWalletId(wallet.getId());
        log.setBizType(4);
        log.setAmount(amount);
        log.setBalanceBefore(before);
        log.setBalanceAfter(wallet.getBalance());
        log.setFrozenBefore(frozenBefore);
        log.setFrozenAfter(wallet.getFrozenAmount());
        log.setBizOrderNo(orderNo);
        log.setDescription("订单解冻");
        log.setCreateTime(LocalDateTime.now());

        walletLogMapper.insert(log);
    }


    @Transactional
    @Override
    public void transferFrozenToSeller(Long buyerId, Long sellerId, BigDecimal amount, String orderNo) {

        // 锁 buyer
        UserWallet buyerWallet = userWalletMapper.selectOne(
                Wrappers.<UserWallet>lambdaQuery()
                        .eq(UserWallet::getUserId, buyerId)
                        .last("for update")
        );

        if (buyerWallet.getFrozenAmount().compareTo(amount) < 0) {
            throw new RuntimeException("冻结金额不足");
        }

        BigDecimal buyerFrozenBefore = buyerWallet.getFrozenAmount();

        buyerWallet.setFrozenAmount(buyerWallet.getFrozenAmount().subtract(amount));
        buyerWallet.setTotalExpense(buyerWallet.getTotalExpense().add(amount));

        userWalletMapper.updateById(buyerWallet);

        // 锁 seller
        UserWallet sellerWallet = userWalletMapper.selectOne(
                Wrappers.<UserWallet>lambdaQuery()
                        .eq(UserWallet::getUserId, sellerId)
                        .last("for update")
        );

        if (sellerWallet == null) {
            throw new RuntimeException("卖家钱包不存在");
        }

        BigDecimal sellerBalanceBefore = sellerWallet.getBalance();

        sellerWallet.setBalance(sellerWallet.getBalance().add(amount));
        sellerWallet.setTotalIncome(sellerWallet.getTotalIncome().add(amount));

        userWalletMapper.updateById(sellerWallet);

        // buyer流水
        WalletLog buyerLog = new WalletLog();
        buyerLog.setUserId(buyerId);
        buyerLog.setWalletId(buyerWallet.getId());
        buyerLog.setBizType(3);
        buyerLog.setAmount(amount.negate());
        buyerLog.setBalanceBefore(buyerWallet.getBalance());
        buyerLog.setBalanceAfter(buyerWallet.getBalance());
        buyerLog.setFrozenBefore(buyerFrozenBefore);
        buyerLog.setFrozenAfter(buyerWallet.getFrozenAmount());
        buyerLog.setBizOrderNo(orderNo);
        buyerLog.setDescription("订单支付给卖家");
        buyerLog.setCreateTime(LocalDateTime.now());

        walletLogMapper.insert(buyerLog);

        // seller流水
        WalletLog sellerLog = new WalletLog();
        sellerLog.setUserId(sellerId);
        sellerLog.setWalletId(sellerWallet.getId());
        sellerLog.setBizType(6);
        sellerLog.setAmount(amount);
        sellerLog.setBalanceBefore(sellerBalanceBefore);
        sellerLog.setBalanceAfter(sellerWallet.getBalance());
        sellerLog.setFrozenBefore(sellerWallet.getFrozenAmount());
        sellerLog.setFrozenAfter(sellerWallet.getFrozenAmount());
        sellerLog.setBizOrderNo(orderNo);
        sellerLog.setDescription("订单收款");
        sellerLog.setCreateTime(LocalDateTime.now());

        walletLogMapper.insert(sellerLog);
    }
}
