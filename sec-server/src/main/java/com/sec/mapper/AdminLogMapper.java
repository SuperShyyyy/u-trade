package com.sec.mapper;

import com.sec.domain.po.AdminLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
