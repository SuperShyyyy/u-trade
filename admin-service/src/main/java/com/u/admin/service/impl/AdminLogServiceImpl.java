package com.u.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.u.admin.domain.po.AdminLog;
import com.u.admin.mapper.AdminLogMapper;
import com.u.admin.service.IAdminLogService;
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
