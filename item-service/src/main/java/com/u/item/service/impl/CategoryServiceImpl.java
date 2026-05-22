package com.u.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.u.item.domain.po.Category;
import com.u.item.domain.po.Item;
import com.u.item.domain.vo.CategoryVO;
import com.u.item.domain.vo.ItemVO;
import com.u.common.exception.BusinessException;
import com.u.item.mapper.CategoryMapper;
import com.u.item.mapper.ItemMapper;
import com.u.common.result.PageDTO;
import com.u.item.service.ICategoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 商品分类�?服务实现�?
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements ICategoryService {
    private final ItemMapper itemMapper;
    @Override
    public List<CategoryVO> listTree(){
        List<Category> list = lambdaQuery()
                .orderByAsc(Category::getSort).list();
        List<CategoryVO> voList = list.stream().map(c->{
            CategoryVO vo = new CategoryVO();
            BeanUtils.copyProperties(c,vo);
            return vo;
        }).toList();
        Map<Integer,CategoryVO> map =voList.stream().collect(Collectors.toMap(CategoryVO::getId , v->v));
        List<CategoryVO> roots = new ArrayList<>();
        for (CategoryVO vo : voList) {
            if (vo.getParentId()==null||vo.getParentId()==0){
                roots.add(vo);
            }
            else{
                CategoryVO parent = map.get(vo.getParentId());
                if (parent!=null){
                    if (parent.getChildren()==null){
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(vo);
                }
            }
        }
        return roots;
    }


    @Override
    public PageDTO<ItemVO> pageQueryItemByCategoryId(Long categoryId, int page, int pageSize) {
        Category category = getById(categoryId);
        if (category == null) {
            throw new BusinessException("category not found");
        }
        Page<Item> pageParam = new Page<>(page,pageSize);
        LambdaQueryWrapper<Item> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Item::getCategoryId,categoryId).orderByDesc(Item::getCreateTime);
        IPage<Item> itemIPage = itemMapper.selectPage(pageParam,queryWrapper);
        IPage<ItemVO> voPage = itemIPage.convert(item->{
                ItemVO itemVO = new ItemVO();
                BeanUtils.copyProperties(item,itemVO);
                return itemVO;
        });
        return PageDTO.of(voPage);
    }
}
