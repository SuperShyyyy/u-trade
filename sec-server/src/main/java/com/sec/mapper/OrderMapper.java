package com.sec.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sec.domain.po.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 订单主表 Mapper 接口
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

}
