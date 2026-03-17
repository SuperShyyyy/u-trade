package com.sec.mapper;

import com.sec.domain.po.Item;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 商品表 Mapper 接口
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Mapper
public interface ItemMapper extends BaseMapper<Item> {

    List<Item> selectWithSellerCredit(@Param("limit") int limit);
}
