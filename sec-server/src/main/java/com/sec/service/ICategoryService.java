package com.sec.service;

import com.sec.domain.po.Category;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sec.domain.vo.CategoryVO;
import com.sec.domain.vo.ItemVO;
import com.sec.result.PageDTO;

import java.util.List;
import java.util.function.IntPredicate;

/**
 * <p>
 * 商品分类表 服务类
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
public interface ICategoryService extends IService<Category> {

    List<CategoryVO> listTree();

    PageDTO<ItemVO> pageQueryItemByCategoryId(Long categoryId , int page, int pageSize);
}
