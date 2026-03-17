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
import com.sec.mq.sender.ItemEsSender;
import com.sec.result.PageDTO;
import com.sec.service.IItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sec.service.ItemEsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商品表 服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements IItemService {

    private final ItemEsService itemEsService;
    private final ItemEsSender itemEsSender;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItemStatus(Long id, Integer status) {
        Long userId = BaseContext.getCurrentId();
        if (id == null) {
            throw new BusinessException("未查询到商品，无法修改");
        }

        Item item = lambdaQuery().eq(Item::getId, id).one();
        if (item == null) {
            throw new BusinessException("未查询到商品，无法修改");
        }

        if (!Set.of(ItemStatusConstant.ON_SALE, ItemStatusConstant.OFF_SALE).contains(status)) {
            throw new BusinessException("只能修改为上架或下架状态");
        }

        if (item.getStatus().equals(status)) {
            throw new BusinessException("状态未发生变化");
        }

        // 更新数据库
        Item update = new Item();
        update.setSellerId(userId);
        update.setId(id);
        update.setStatus(status);
        update.setUpdateTime(LocalDateTime.now());
        this.updateById(update);

        // 获取最新数据
        Item updatedItem = this.getById(id);

        // 使用发送器发送 MQ 消息
        if (status == ItemStatusConstant.ON_SALE) {
            itemEsSender.sendUpdateMessage(updatedItem);
        } else {
            // 下架时，通知 ES 删除或更新状态
            itemEsSender.sendUpdateMessage(updatedItem);
        }

        // TODO: 删除或更新缓存
        log.info("商品状态更新完成，ItemId: {}, NewStatus: {}", id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addItem(ItemDTO itemDTO) {
        if (itemDTO == null) {
            throw new BusinessException("数据为空 无法上架");
        }

        Long userId = BaseContext.getCurrentId();
        Item item = new Item();
        // 手动设置属性
        item.setTitle(itemDTO.getTitle());
        item.setDescription(itemDTO.getDescription());
        item.setPrice(itemDTO.getOriginalPrice());
        item.setOriginalPrice(itemDTO.getOriginalPrice());
        item.setCategoryId(itemDTO.getCategoryId());

        item.setSellerId(userId);
        item.setWantCount(0);
        item.setViewCount(0);
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        item.setStatus(ItemStatusConstant.ON_SALE);
        item.setAuditStatus(ItemAuditStatusConstant.PASS_AUDIT);
        item.setIsDeleted(0);

        if (itemDTO.getImages() != null && itemDTO.getImages().length > 0) {
            item.setImages(String.join(",", itemDTO.getImages()));
            item.setCover(itemDTO.getImages()[0]);
        }

        // 保存数据库
        this.save(item);

        // 使用发送器发送 MQ 消息
        itemEsSender.sendAddMessage(item);

        // TODO: 缓存操作
        log.info("商品新增完成，ItemId: {}", item.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(Long id) {
        Long userId = BaseContext.getCurrentId();
        Item item = this.getById(id);

        if (item == null) {
            throw new BusinessException("商品不存在，无法删除");
        }

        if (!item.getSellerId().equals(userId)) {
            throw new PermissionDeniedException("非当前用户售卖商品，无法删除");
        }

        // 逻辑删除
        item.setIsDeleted(1);
        item.setUpdateTime(LocalDateTime.now());
        this.updateById(item);

        // 使用发送器发送 MQ 消息
        itemEsSender.sendDeleteMessage(item);

        // TODO: 缓存操作
        log.info("商品删除完成，ItemId: {}", id);
    }

    // ==================== 查询方法保持不变 ====================

    @Override
    public PageDTO<ItemVO> pageQueryItemsBySellerId(Long id, int page, int pageSize) {
        if (id == null) {
            throw new BusinessException("参数为空，无法查询");
        }

        if (page < 0) page = 0;

        Page<Item> pageParam = new Page<>(page + 1, pageSize);
        LambdaQueryWrapper<Item> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(Item::getSellerId, id)
                .eq(Item::getIsDeleted, 0)
                .eq(Item::getStatus, ItemStatusConstant.ON_SALE)
                .orderByAsc(Item::getCreateTime);

        IPage<Item> pageResult = this.page(pageParam, queryWrapper);

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

    @Override
    public PageDTO<ItemVO> searchItems(String keyword, int page, int pageSize) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BusinessException("搜索关键词不能为空");
        }

        if (page < 0) {
            page = 0;
        }

        if (pageSize <= 0) {
            pageSize = 10;
        }

        if (pageSize > 100) {
            pageSize = 100;
        }

        log.info("执行商品搜索，keyword: {}, page: {}, pageSize: {}", keyword, page, pageSize);

        try {
            // 调用 ES 服务搜索
          org.springframework.data.domain.Page<ItemDocument> esPage =
                    itemEsService.searchByTitle(keyword, page, pageSize);

            // 将 ItemDocument 转换为 ItemVO
            List<ItemVO> voList = esPage.getContent().stream().map(doc -> {
                ItemVO vo = new ItemVO();
                vo.setId(doc.getId());
                vo.setSellerId(doc.getSellerId());
                vo.setTitle(doc.getTitle());
                vo.setDescription(doc.getDescription());
                vo.setCategoryId(doc.getCategoryId());
                vo.setPrice(doc.getPrice());
                vo.setOriginalPrice(doc.getOriginalPrice());
                vo.setCover(doc.getCover());
                // 处理图片字符串转 List
                if (doc.getImages() != null && !doc.getImages().isEmpty()) {
                    vo.setImages(List.of(doc.getImages().split(",")));
                }
                vo.setStatus(doc.getStatus());
                vo.setAuditStatus(doc.getAuditStatus());
                vo.setCreateTime(doc.getCreateTime());
                return vo;
            }).collect(Collectors.toList());

            // 构建返回结果
            PageDTO<ItemVO> result = new PageDTO<>(
                    esPage.getTotalElements(),
                    (long) esPage.getTotalPages(),
                    (long) (page + 1),
                    voList
            );

            log.info("商品搜索完成，总记录数: {}, 返回条数: {}",
                    esPage.getTotalElements(), voList.size());
            return result;

        } catch (Exception e) {
            log.error("商品搜索失败，keyword: {}, Error: {}", keyword, e.getMessage(), e);
            throw new BusinessException("搜索服务异常，请稍后重试");
        }
    }
}