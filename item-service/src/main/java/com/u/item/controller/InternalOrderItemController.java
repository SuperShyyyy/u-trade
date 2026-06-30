package com.u.item.controller;

import com.u.api.dto.item.ItemTradeDTO;
import com.u.api.internal.item.InternalOrderItemApi;
import com.u.common.constant.ItemStatusConstant;
import com.u.common.constant.RedisConstant;
import com.u.common.exception.BusinessException;
import com.u.common.result.Result;
import com.u.item.domain.po.Item;
import com.u.item.mq.ItemEsSender;
import com.u.item.service.ICategoryService;
import com.u.item.service.IItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/inner/order/items")
@RequiredArgsConstructor
@Slf4j
public class InternalOrderItemController implements InternalOrderItemApi {

    private final IItemService itemService;
    private final ICategoryService categoryService;
    private final ItemEsSender itemEsSender;
    private final StringRedisTemplate stringRedisTemplate;
/*
    @Override
    public Result<OrderItemDTO> getOrderItem(Long id) {
        Item item = itemService.getById(id);
        if (item == null || Integer.valueOf(1).equals(item.getIsDeleted())) {
            throw new BusinessException("商品不存在");
        }

        OrderItemDTO vo = new OrderItemDTO();
        BeanUtils.copyProperties(item, vo);
        if (item.getImages() != null && !item.getImages().isEmpty()) {
            vo.setImages(List.of(item.getImages().split(",")));
        }
        return Result.success(vo);
    }
*/
    @Override
    public Result<Void> lockItem(Long id) {
        updateStatus(id, ItemStatusConstant.ON_SALE, ItemStatusConstant.LOCKED, "宝贝已售出或正在被他人购买中");
        return Result.success();
    }

    @Override
    public Result<Void> markItemSold(Long id) {
        updateStatus(id, ItemStatusConstant.LOCKED, ItemStatusConstant.SOLD, "商品状态同步失败");
        return Result.success();
    }

    @Override
    public Result<Void> releaseItem(Long id) {
        updateStatus(id, ItemStatusConstant.LOCKED, ItemStatusConstant.ON_SALE, "商品状态释放失败");
        return Result.success();
    }

    @Override
    public Result<ItemTradeDTO> getItemTrade(Long id) {
        Item item = itemService.getById(id);
        if (item == null || Integer.valueOf(1).equals(item.getIsDeleted())) {
            throw new BusinessException("商品不存在");
        }
        ItemTradeDTO vo = new ItemTradeDTO();
        BeanUtils.copyProperties(item, vo);
        vo.setItemId(item.getId());
        if (item.getImages() != null && !item.getImages().isEmpty()) {
            vo.setImages(List.of(item.getImages().split(",")));
        } else if (StringUtils.hasText(item.getCover())) {
            vo.setImages(List.of(item.getCover()));
        }
        if (item.getCategoryId() != null) {
            var category = categoryService.getById(item.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }
        return Result.success(vo);
    }

    /**
     * 乐观锁 + 状态机 更新商品状态
     * 先查询当前记录（携带 version），校验状态，再通过 MyBatis-Plus @Version 自动追加 WHERE version=?
     * 若 version 不匹配（被并发修改），update 返回 false，抛出业务异常
     */
    private void updateStatus(Long id, Integer expectedStatus, Integer targetStatus, String errorMessage) {
        // 1. 查询当前商品（获取 version 快照）
        Item item = itemService.getById(id);
        if (item == null) {
            throw new BusinessException("商品不存在");
        }

        // 2. 状态机校验：当前状态必须符合预期
        if (!expectedStatus.equals(item.getStatus())) {
            log.warn("商品状态机校验失败, itemId={}, expectedStatus={}, actualStatus={}, version={}",
                    id, expectedStatus, item.getStatus(), item.getVersion());
            throw new BusinessException(errorMessage);
        }

        // 3. 乐观锁更新：MyBatis-Plus @Version 自动在 SQL 中追加 WHERE version=?
        // 生成 SQL: UPDATE item SET status=?, update_time=?, version=version+1 WHERE id=? AND version=?
        item.setStatus(targetStatus);
        item.setUpdateTime(LocalDateTime.now());
        boolean updated = itemService.updateById(item);

        if (!updated) {
            // version 不匹配，说明被并发修改
            log.warn("商品乐观锁更新失败(并发冲突), itemId={}, expectedStatus={}, targetStatus={}, version={}",
                    id, expectedStatus, targetStatus, item.getVersion());
            throw new BusinessException(errorMessage);
        }

        // 4. 同步 ES & 清缓存
        Item latestItem = itemService.getById(id);
        if (latestItem != null) {
            itemEsSender.sendUpdateMessage(latestItem);
        }
        stringRedisTemplate.delete(RedisConstant.ITEM_DETAIL + id);
    }

}
