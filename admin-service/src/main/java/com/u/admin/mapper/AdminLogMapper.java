package com.u.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.u.admin.domain.po.AdminLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 管理员操作日志表 Mapper 接口
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Mapper
public interface AdminLogMapper extends BaseMapper<AdminLog> {

}
