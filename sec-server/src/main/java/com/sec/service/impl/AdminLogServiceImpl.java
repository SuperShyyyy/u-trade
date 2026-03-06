package com.sec.service.impl;

import com.sec.domain.po.AdminLog;
import com.sec.mapper.AdminLogMapper;
import com.sec.service.IAdminLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 管理员操作日志表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Service
public class AdminLogServiceImpl extends ServiceImpl<AdminLogMapper, AdminLog> implements IAdminLogService {

}
