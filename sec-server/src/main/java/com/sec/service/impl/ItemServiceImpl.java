package com.sec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sec.context.BaseContext;
import com.sec.domain.dto.ItemDTO;
import com.sec.domain.po.Item;
import com.sec.domain.vo.ItemVO;
import com.sec.exception.BusinessException;
import com.sec.mapper.ItemMapper;
import com.sec.result.PageDTO;
import com.sec.service.IItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Service
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements IItemService {

    public void updateItemStatus(Long id, Integer status) {
        if (id == null){
            throw new BusinessException("未查询到商品，无法修改");
        }
        Item item = lambdaQuery().eq(Item::getId, id).one();
        if (item == null){
            throw new BusinessException("未查询到商品，无法修改");
        }
        if (!Arrays.asList(0, 1).contains(status)){
            throw new BusinessException("只能修改为上架或下架状态");
        }
        if (item.getStatus().equals(status)){
            throw new BusinessException("状态未发生变化");
        }
        Item update = new Item();
        update.setId(id);
        update.setStatus(status);
        this.updateById(update);

        // TODO: 删除或更新缓存，首页推荐、分类列表
    }

    @Override
    public void addItem(ItemDTO itemDTO){
        if (itemDTO == null){
            throw new BusinessException("数据为空 无法上架");
        }
        Long userId = BaseContext.getCurrentId();
        Item item = new Item();
        BeanUtils.copyProperties(itemDTO,item);
        item.setSellerId(userId);
        item.setWantCount(0);
        item.setViewCount(0);
        item.setCreateTime(LocalDateTime.now());
        item.setStatus(-2);       // 待审核状态
        item.setAuditStatus(0);   // 待审核
        item.setPrice(itemDTO.getOriginalPrice()); // 当前售价
        item.setIsDeleted(0);
        if(itemDTO.getImages() != null && itemDTO.getImages().length > 0){
            item.setImages(String.join(",", itemDTO.getImages()));
            item.setCover(itemDTO.getImages()[0]);
        }
        this.save(item);

    }

    @Override
    public void deleteItem(Long id){
        Item item = this.getById(id);
        if(item == null){
            throw new BusinessException("商品不存在，无法删除");
        }
        item.setIsDeleted(1);
        this.updateById(item);
    }

    @Override
    public PageDTO<ItemVO> pageQueryItemsBySellerId(Long id,int page,int pageSize){
        //非空判断
        if (id==null){
            throw new BusinessException("参数为空，无法查询");
        }
        //分页查询Item
        Page<Item> pageParam = new Page<>(page,pageSize);
        LambdaQueryWrapper<Item> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(Item::getSellerId,id)
                .eq(Item::getIsDeleted,0)
                .eq(Item::getStatus,1)
        .orderByAsc(Item::getCreateTime);
        IPage<Item> pageResult = this.page(pageParam,queryWrapper);
        //转VO
        IPage<ItemVO> voPage = pageResult.convert(item -> {
            ItemVO itemVO = new ItemVO();
            itemVO.setSellerId(item.getSellerId());
            itemVO.setTitle(item.getTitle());
            itemVO.setDescription(item.getDescription());
            itemVO.setPrice(item.getPrice());
            itemVO.setCover(item.getCover());
            itemVO.setId(item.getId());
            itemVO.setStatus(item.getStatus());
            return itemVO;
        });
        return PageDTO.of(voPage);
    }
}
