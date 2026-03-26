package com.sec.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sec.constant.RedisConstant;
import com.sec.context.BaseContext;
import com.sec.domain.po.Favorite;
import com.sec.domain.po.Item;
import com.sec.domain.vo.FavoriteVO;
import com.sec.exception.BusinessException;
import com.sec.mapper.FavoriteMapper;
import com.sec.mapper.ItemMapper;
import com.sec.result.PageDTO;
import com.sec.service.IFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商品收藏表 服务实现类（改造版，维护用户缓存key集合）
 */
@Service
@RequiredArgsConstructor
public class FavoritesServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements IFavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final ItemMapper itemMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 新增收藏
     */
    @Transactional
    @Override
    public void addFavorite(Long itemId) {
        Long userId = BaseContext.getCurrentId();
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setItemId(itemId);
        favorite.setCreateTime(LocalDateTime.now());
        boolean success = false;
        try {
            success = this.save(favorite);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new BusinessException("该商品已收藏");
        }
        if (success) {
            itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                    .setSql("want_count = want_count + 1")
                    .eq(Item::getId, itemId));
        }
        // 清理用户收藏缓存
        deleteUserFavoriteCache(userId);
    }

    /**
     * 分页查询收藏
     */
    @Override
    public PageDTO<FavoriteVO> pageQueryFavorite(int page, int pageSize) {
        Long userId = BaseContext.getCurrentId();
        String cacheKey = RedisConstant.USER_FAVORITE_KEY + userId + ":page:" + page + ":size:" + pageSize;
        // 1. 先查 Redis
        String json = stringRedisTemplate.opsForValue().get(cacheKey);
        if (json != null && !json.isEmpty()) {
            return JSON.parseObject(json, new TypeReference<PageDTO<FavoriteVO>>() {});
        }
        // 2. 查询数据库
        Page<Favorite> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Favorite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(Favorite::getUserId, userId)
                .select(Favorite::getId, Favorite::getUserId, Favorite::getItemId, Favorite::getCreateTime)
                .orderByDesc(Favorite::getCreateTime);
        IPage<Favorite> favoritePage = favoriteMapper.selectPage(pageParam, queryWrapper);
        List<Favorite> records = favoritePage.getRecords();
        PageDTO<FavoriteVO> resultPage;
        if (records.isEmpty()) {
            resultPage = PageDTO.of(favoritePage.convert(f -> new FavoriteVO()));
        } else {
            // 查询商品信息
            List<Long> itemIds = records.stream().map(Favorite::getItemId).toList();
            List<Item> items = itemMapper.selectBatchIds(itemIds);
            Map<Long, Item> itemMap = items.stream().collect(Collectors.toMap(Item::getId, i -> i));

            IPage<FavoriteVO> voPage = favoritePage.convert(favorite -> {
                FavoriteVO vo = new FavoriteVO();
                BeanUtils.copyProperties(favorite, vo);
                Item item = itemMap.get(favorite.getItemId());
                if (item != null) {
                    vo.setItemName(item.getTitle());
                    vo.setItemPrice(item.getPrice());
                    vo.setItemStatus(item.getStatus());
                    vo.setItemImage(item.getCover());
                }
                return vo;
            });

            resultPage = PageDTO.of(voPage);
        }
        // 3. 写缓存，并记录 key 到用户集合
        stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(resultPage), Duration.ofMinutes(30));
        String setKey = RedisConstant.USER_FAVORITE_KEY + "keys:" + userId;
        stringRedisTemplate.opsForSet().add(setKey, cacheKey);

        return resultPage;
    }

    /**
     * 删除收藏
     */
    @Transactional
    @Override
    public void deleteFavorite(Long itemId) {
        Long userId = BaseContext.getCurrentId();
        boolean rows = lambdaUpdate()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getItemId, itemId)
                .remove();
        if (!rows) {
            throw new BusinessException("收藏的商品不存在");
        }
        itemMapper.update(null, new LambdaUpdateWrapper<Item>()
                .setSql("want_count = CASE WHEN want_count > 0 THEN want_count - 1 ELSE 0 END")
                .eq(Item::getId, itemId));

        // 清理用户收藏缓存
        deleteUserFavoriteCache(userId);
    }

    /**
     * 删除用户收藏缓存（维护的 key 集合）
     */
    private void deleteUserFavoriteCache(Long userId) {
        String setKey = RedisConstant.USER_FAVORITE_KEY + "keys:" + userId;
        Set<String> keys = stringRedisTemplate.opsForSet().members(setKey);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
        stringRedisTemplate.delete(setKey);
    }
}