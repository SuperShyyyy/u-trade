package com.u.item.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.u.api.dto.item.ItemTradeDTO;
import com.u.api.internal.item.InternalOrderItemApi;
import com.u.common.constant.ItemStatusConstant;
import com.u.common.constant.RedisConstant;
import com.u.common.exception.BusinessException;
import com.u.common.result.Result;
import com.u.item.domain.po.Item;
import com.u.item.mq.ItemEsSender;
import com.u.item.service.IItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/inner/order/items")
@RequiredArgsConstructor
public class InternalOrderItemController implements InternalOrderItemApi {

    private final IItemService itemService;
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
        if (item.getImages() != null && !item.getImages().isEmpty()) {
            vo.setImages(List.of(item.getImages().split(",")));
        }
        return Result.success(vo);
    }

    private void updateStatus(Long id, Integer fromStatus, Integer toStatus, String errorMessage) {
        boolean updated = itemService.update(new LambdaUpdateWrapper<Item>()
                .eq(Item::getId, id)
                .eq(Item::getStatus, fromStatus)
                .set(Item::getStatus, toStatus)
                .set(Item::getUpdateTime, LocalDateTime.now()));
        if (!updated) {
            throw new BusinessException(errorMessage);
        }

        Item latestItem = itemService.getById(id);
        if (latestItem != null) {
            itemEsSender.sendUpdateMessage(latestItem);
        }
        stringRedisTemplate.delete(RedisConstant.ITEM_DETAIL + id);
    }

}
