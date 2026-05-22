package com.u.item.service;

import com.u.item.domain.po.Category;
import com.baomidou.mybatisplus.extension.service.IService;
import com.u.item.domain.vo.CategoryVO;
import com.u.item.domain.vo.ItemVO;
import com.u.common.result.PageDTO;

import java.util.List;

/**
 * <p>
 * 商品分类�?服务�?
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
public interface ICategoryService extends IService<Category> {

    List<CategoryVO> listTree();

    PageDTO<ItemVO> pageQueryItemByCategoryId(Long categoryId , int page, int pageSize);
}
