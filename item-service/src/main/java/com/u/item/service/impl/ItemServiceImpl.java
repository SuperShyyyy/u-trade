package com.u.item.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.u.common.constant.ItemAuditStatusConstant;
import com.u.common.constant.ItemStatusConstant;
import com.u.common.constant.RedisConstant;
import com.u.common.context.BaseContext;
import com.u.item.domain.dto.ItemDTO;
import com.u.item.domain.es.ItemDocument;
import com.u.item.domain.po.Item;
import com.u.item.domain.vo.ItemVO;
import com.u.common.exception.BusinessException;
import com.u.common.exception.PermissionDeniedException;
import com.u.item.mapper.ItemMapper;
import com.u.item.mq.ItemEsSender;
import com.u.common.result.PageDTO;
import com.u.item.service.IItemService;
import com.u.item.service.ItemEsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 商品�?服务实现�?
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ItemServiceImpl extends ServiceImpl<ItemMapper, Item> implements IItemService {

    private final ItemEsService itemEsService;
    private final ItemEsSender itemEsSender;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItemStatus(Long id, Integer status) {
        Long userId = BaseContext.getCurrentId();
        if (id == null) {
            throw new BusinessException("id is null");
        }

        Item item = this.getById(id);
        if (item == null) {
            throw new BusinessException("item not found");
        }
        if(!item.getSellerId().equals(userId)){
            throw new PermissionDeniedException("not item owner");
        }
        if (!Set.of(ItemStatusConstant.ON_SALE, ItemStatusConstant.OFF_SALE).contains(status)) {
            throw new BusinessException("invalid status");
        }

        if (item.getStatus().equals(status)) {
            throw new BusinessException("状态未发生变化");
        }

        // 更新数据�?
        // 涔愯閿佹洿鏂帮細浣跨敤鏌ュ埌鐨?item 瀵硅薄锛堟惡甯?version锛夛紝MyBatis-Plus @Version 鑷姩杩藉姞 WHERE version=?
        item.setStatus(status);
        item.setUpdateTime(LocalDateTime.now());
        this.updateById(item);

        // 获取最新数据（用于发送MQ�?
        Item updatedItem = this.getById(id);

        if (updatedItem != null) {
            itemEsSender.sendUpdateMessage(updatedItem);
        }

        // 删除缓存（先更新数据库，再删除缓存）
        String cacheKey = RedisConstant.ITEM_DETAIL + id;
        stringRedisTemplate.delete(cacheKey);

        log.info("商品状态更新完成，ItemId: {}, NewStatus: {}", id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addItem(ItemDTO itemDTO) {
        if (itemDTO == null) {
            throw new BusinessException("数据为空 无法上架");
        }

        Long userId = BaseContext.getCurrentId();
        //0是不包邮 //1为包�?
        if(itemDTO.getIsFreeShipping()==1){
            itemDTO.setShippingFee(new BigDecimal(0));
        }
        Item item = new Item();
        item.setTitle(itemDTO.getTitle());
        item.setDescription(itemDTO.getDescription());
        item.setPrice(itemDTO.getOriginalPrice());
        item.setOriginalPrice(itemDTO.getOriginalPrice());  // 二手交易无打折机制，price 与 originalPrice 保持一致
        item.setCategoryId(itemDTO.getCategoryId());

        item.setSellerId(userId);
        item.setWantCount(0L);
        item.setViewCount(0L);
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        item.setStatus(ItemStatusConstant.ON_SALE);
        item.setAuditStatus(ItemAuditStatusConstant.PASS_AUDIT);
        item.setIsDeleted(0);
        item.setShippingFee(itemDTO.getShippingFee());
        if (itemDTO.getImages() != null && itemDTO.getImages().length > 0) {
            item.setImages(String.join(",", itemDTO.getImages()));
            item.setCover(itemDTO.getImages()[0]);
        }

        this.save(item);
        itemEsSender.sendAddMessage(item);

        ItemVO vo = convertToVO(item);
        String cacheKey = RedisConstant.ITEM_DETAIL + item.getId();
        stringRedisTemplate.opsForValue().set(
                cacheKey,
                JSON.toJSONString(vo),
                RedisConstant.ITEM_TTL,
                TimeUnit.MINUTES
        );

        log.info("商品新增完成，ItemId: {}", item.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(Long id) {
        if(id==null){
            throw new BusinessException("id is null");
        }
        Long userId = BaseContext.getCurrentId();
        Item item = this.getById(id);

        if (item == null) {
            throw new BusinessException("商品不存在，无法删除");
        }

        if (!item.getSellerId().equals(userId)) {
            throw new PermissionDeniedException("非当前用户售卖商品，无法删除");
        }

        item.setIsDeleted(1);
        item.setUpdateTime(LocalDateTime.now());
        this.updateById(item);

        itemEsSender.sendDeleteMessage(item);

        String key = RedisConstant.ITEM_DETAIL + item.getId();
        stringRedisTemplate.delete(key);
        log.info("商品删除完成，ItemId: {}", id);
    }

    @Override
    public PageDTO<ItemVO> pageQueryItemsBySellerId(Long id, int page, int pageSize) {
        if (id == null) {
            throw new BusinessException("id is null");
        }

        page = Math.max(page, 0);

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
            throw new BusinessException("keyword empty");
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


    @Override
    public ItemVO getItemById(Long id) {
        if (id == null) {
            throw new BusinessException("id is null");
        }
        String detailKey = RedisConstant.ITEM_DETAIL + id;
        String viewKey = RedisConstant.ITEM_VIEW_COUNT + id;
        stringRedisTemplate.opsForValue().increment(viewKey);
        stringRedisTemplate.opsForSet().add(RedisConstant.ITEM_VIEW_IDS, String.valueOf(id));
        stringRedisTemplate.expire(viewKey, 1, TimeUnit.DAYS);

        String json = stringRedisTemplate.opsForValue().get(detailKey);
        if (json != null) {
            if ("null".equals(json)) {
                return null;
            }
            ItemVO vo = JSON.parseObject(json, ItemVO.class);
            vo.setViewCount(vo.getViewCount() + getRedisIncrement(viewKey));
            return vo;
        }

        // 查数据库
        Item item = this.getById(id);

        // 防穿�?3min
        if (item == null) {
            stringRedisTemplate.opsForValue().set(detailKey, "null", 3, TimeUnit.MINUTES);
            return null;
        }
        if(!item.getStatus().equals( ItemStatusConstant.ON_SALE) ) {
            throw new BusinessException("bad item status");
        }
        // 只转换一次VO
        ItemVO baseVo = convertToVO(item);
        String baseJson = JSON.toJSONString(baseVo);

        // 写入缓存
        stringRedisTemplate.opsForValue().set(
                detailKey,
                baseJson,
                RedisConstant.ITEM_TTL,
                TimeUnit.MINUTES
        );
        long base = item.getViewCount() == null ? 0 : item.getViewCount();
        long increment = getRedisIncrement(viewKey);
        baseVo.setViewCount(base + increment);

        return baseVo;
    }

    private ItemVO convertToVO(Item item) {
        if (item == null) return null;
        ItemVO vo = new ItemVO();
        vo.setId(item.getId());
        vo.setTitle(item.getTitle());
        vo.setDescription(item.getDescription());
        vo.setPrice(item.getPrice());
        vo.setOriginalPrice(item.getOriginalPrice());
        vo.setCover(item.getCover());
        vo.setSellerId(item.getSellerId());
        vo.setCategoryId(item.getCategoryId());
        vo.setStatus(item.getStatus());
        vo.setAuditStatus(item.getAuditStatus());
        vo.setCreateTime(item.getCreateTime());
        vo.setUpdateTime(item.getUpdateTime());
        vo.setWantCount(item.getWantCount());
        vo.setViewCount(item.getViewCount());
        if (item.getImages() != null && !item.getImages().isEmpty()) {
            vo.setImages(List.of(item.getImages().split(",")));
        }
        return vo;
    }

    private long getRedisIncrement(String viewKey) {
        String countStr = stringRedisTemplate.opsForValue().get(viewKey);
        if (countStr == null || countStr.isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(countStr);
        } catch (NumberFormatException e) {
            log.warn("Redis浏览量数据格式异�? {}", countStr);
            return 0;
        }
    }
}