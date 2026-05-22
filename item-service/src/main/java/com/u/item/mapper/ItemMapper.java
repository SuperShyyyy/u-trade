package com.u.item.mapper;

import com.u.item.domain.po.Item;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>
 * 商品�?Mapper 接口
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Mapper
public interface ItemMapper extends BaseMapper<Item> {

    // selectWithSellerCredit
    List<Item>  selectForRecommend(@Param("limit") int limit);
    @Update("UPDATE item SET view_count = view_count + #{count} WHERE id = #{itemId}")
    void incrementViewCount(@Param("itemId") Long itemId, @Param("count") Long count);
}
