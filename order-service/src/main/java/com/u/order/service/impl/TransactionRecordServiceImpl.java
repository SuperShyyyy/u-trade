package com.u.order.service.impl;

import com.u.order.domain.po.TransactionRecord;
import com.u.order.mapper.TransactionRecordMapper;
import com.u.order.service.ITransactionRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * C2C交易记录表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-03-09
 */
@Service
public class TransactionRecordServiceImpl extends ServiceImpl<TransactionRecordMapper, TransactionRecord> implements ITransactionRecordService {

}
