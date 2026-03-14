package com.sec.mapper;

import com.sec.domain.po.UserWallet;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


import java.math.BigDecimal;

/**
 * <p>
 * 用户钱包表 Mapper 接口
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Mapper
public interface UserWalletMapper extends BaseMapper<UserWallet> {

        int updateBuyerWallet(
                @Param("id") Long id,
                @Param("frozenAmount") BigDecimal frozenAmount,
                @Param("totalExpense") BigDecimal totalExpense,
                @Param("version") Integer version
        );

        int updateSellerWallet(
                @Param("id") Long id,
                @Param("balance") BigDecimal balance,
                @Param("totalIncome") BigDecimal totalIncome,
                @Param("version") Integer version
        );

        int updateFreezeWallet(Long id,
              @Param("balanceAfter")  BigDecimal balanceAfter,
              @Param("frozenAfter") BigDecimal frozenAfter,
              @Param("version")  Integer version
        );


        int updateUnfreezeWallet(Long id,
              @Param("balanceAfter")  BigDecimal balanceAfter,
              @Param("frozenAfter")  BigDecimal frozenAfter,
              @Param("version")   Integer version
        );

        int updateWithdrawWallet(
                @Param("id") Long id,
                @Param("balance") BigDecimal balance,
                @Param("totalExpense") BigDecimal totalExpense,
                @Param("version") Integer version
        );

        int updateRechargeWallet(Long id,
                @Param("after")  BigDecimal after,
                @Param("income")  BigDecimal income,
                @Param("version") Integer version
        );
}

