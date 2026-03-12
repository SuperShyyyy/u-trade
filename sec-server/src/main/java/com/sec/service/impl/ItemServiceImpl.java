package com.sec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sec.constant.ItemAuditStatusConstant;
import com.sec.constant.ItemStatusConstant;
import com.sec.context.BaseContext;
import com.sec.domain.dto.ItemDTO;
import com.sec.domain.es.ItemDocument;
import com.sec.domain.po.Item;
import com.sec.domain.vo.ItemVO;
import com.sec.exception.BusinessException;
import com.sec.exception.PermissionDeniedException;
import com.sec.mapper.ItemMapper;
import com.sec.result.PageDTO;
import com.sec.service.IItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sec.service.ItemEsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.awt.print.PrinterException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements IItemService {

    private final ItemEsService itemEsService;

    public void updateItemStatus(Long id, Integer status) {
        Long userId = BaseContext.getCurrentId();
        if (id == null){
            throw new BusinessException("未查询到商品，无法修改");
        }
        Item item = lambdaQuery().eq(Item::getId, id).one();
        if (item == null){
            throw new BusinessException("未查询到商品，无法修改");
        }
        if (!Set.of(ItemStatusConstant.ON_SALE,ItemStatusConstant.OFF_SALE).contains(status)) throw new BusinessException("只能修改为上架或下架状态");
        if (item.getStatus().equals(status)){
            throw new BusinessException("状态未发生变化");
        }
        Item update = new Item();
        update.setSellerId(userId);
        update.setId(id);
        update.setStatus(status);
        this.updateById(update);


        Item updatedItem = this.getById(id);
        try {
            if (status == ItemStatusConstant.ON_SALE) {
                itemEsService.save(convertToDocument(updatedItem));
            } else {
                itemEsService.delete(id);
            }
        } catch (IOException e) {
            // 可以考虑记录失败日志，后续定时任务补偿
            log.error("ES同步失败", e);
            throw new BusinessException("ES同步异常");
        }

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
        item.setStatus(ItemStatusConstant.ON_SALE);       // 待审核状态
        item.setAuditStatus(ItemAuditStatusConstant.PASS_AUDIT);   // 待审核
        item.setPrice(itemDTO.getOriginalPrice()); // 当前售价
        item.setIsDeleted(0);
        if(itemDTO.getImages() != null && itemDTO.getImages().length > 0){
            item.setImages(String.join(",", itemDTO.getImages()));
            item.setCover(itemDTO.getImages()[0]);
        }

        // 保存DB
        this.save(item);
        //保存到es
        try {
            itemEsService.save(convertToDocument(item));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //todo 缓存操作

        //暂定 自动审核成功
        //必须审核后才能同步到es


    }


    @Override
    public void deleteItem(Long id){
        Long userId = BaseContext.getCurrentId();
        Item item = this.getById(id);
        if(item == null){
            throw new BusinessException("商品不存在，无法删除");
        }
        if (!item.getSellerId().equals(userId)){
            throw new PermissionDeniedException("非当前用户售卖商品，无法删除");
        }
        item.setIsDeleted(1);
        this.updateById(item);

        try {
            itemEsService.delete(id);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //todo 缓存操作

    }

    //数据库增加seller_id字段
    @Override
    public PageDTO<ItemVO> pageQueryItemsBySellerId(Long id,int page,int pageSize){
        //非空判断
        if (id==null){
            throw new BusinessException("参数为空，无法查询");
        }

        //todo 先查缓存

        if (page<0 ) page=0;
        //分页查询Item
        Page<Item> pageParam = new Page<>(page+1,pageSize);
        LambdaQueryWrapper<Item> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(Item::getSellerId,id)
                .eq(Item::getIsDeleted,0)
                .eq(Item::getStatus,ItemStatusConstant.ON_SALE)
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
        //todo写入缓存

        return PageDTO.of(voPage);
    }

    @Override
    public PageDTO<ItemVO> searchItems(String keyword, int page, int pageSize){
        try {
            var esPage = itemEsService.searchByTitle(keyword, page, pageSize);
            var voList = esPage.getContent().stream().map(doc -> {
                ItemVO vo = new ItemVO();
                BeanUtils.copyProperties(doc, vo);
                return vo;
            }).collect(Collectors.toList());

            return new PageDTO<>(
                    esPage.getTotalElements(),  // 总记录数
                    (long) esPage.getTotalPages(),  // 总页数 转换为 Long
                    (long) (page + 1),  // 当前页码，Long 类型
                    voList // 当前页数据
            );
        } catch (IOException e) {
            e.printStackTrace();
            throw new BusinessException("搜索服务异常");
        }
    }


    //工具方法：Item -> ItemDocument
    private ItemDocument convertToDocument(Item item){
        ItemDocument doc = new ItemDocument();
        BeanUtils.copyProperties(item, doc);
        return doc;
    }

}
