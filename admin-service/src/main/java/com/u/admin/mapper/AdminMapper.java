package com.u.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.u.admin.domain.po.Admin;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 系统管理员表 Mapper 接口
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {

}
