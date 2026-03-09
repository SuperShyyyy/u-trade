package com.sec.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sec.constant.RedisConstant;
import com.sec.context.BaseContext;
import com.sec.domain.dto.FavoriteDTO;
import com.sec.domain.po.Favorite;
import com.sec.domain.po.Item;
import com.sec.domain.vo.FavoriteVO;
import com.sec.exception.BusinessException;
import com.sec.mapper.FavoriteMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sec.mapper.ItemMapper;
import com.sec.result.PageDTO;
import com.sec.service.IFavoriteService;
import lombok.RequiredArgsConstructor;
import org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.Redefinable;
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
 * <p>
 * 商品收藏表 服务实现类
 * </p>
 *
 * @author author
 * @since 2026-03-04
 */
@Service
@RequiredArgsConstructor
public class FavoritesServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements IFavoriteService {
    private final FavoriteMapper favoriteMapper;
    private final ItemMapper itemMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Transactional
    @Override
    public void addFavorite(FavoriteDTO favoriteDTO) {
        Long userId = BaseContext.getCurrentId();
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setItemId(favoriteDTO.getItemId());
        favorite.setCreateTime(LocalDateTime.now());
        try {
            this.save(favorite);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 捕获唯一索引冲突
            throw new BusinessException("该商品已收藏");
        }
        // ===== 修改点 1：Redis 删除缓存支持通配符 =====
        Set<String> keys = stringRedisTemplate.keys(RedisConstant.USER_FAVORITE_KEY + userId + ":*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    @Override
    public PageDTO<FavoriteVO> pageQueryFavorite(int page, int pageSize) {
        Long userId = BaseContext.getCurrentId();
        String cacheKey = RedisConstant.USER_FAVORITE_KEY + userId + ":page:" + page + ":size:" + pageSize;
        // 1. 先查 Redis
        String json = stringRedisTemplate.opsForValue().get(cacheKey);
        if (json != null && !json.isEmpty()) {
            return JSON.parseObject(json, new TypeReference<PageDTO<FavoriteVO>>() {});
        }

        Page<Favorite> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Favorite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(Favorite::getUserId, userId)
                .select(
                Favorite::getId,
                Favorite::getUserId,
                Favorite::getItemId,
                Favorite::getCreateTime
        ).orderByDesc(Favorite::getCreateTime);

        IPage<Favorite> favoritePage = favoriteMapper.selectPage(pageParam, queryWrapper);
        List<Long> itemIds = favoritePage.getRecords().stream().map(Favorite::getItemId).toList();
        List<Item> items = itemMapper.selectBatchIds(itemIds);
        Map<Long,Item> itemMap = items.stream().collect(Collectors.toMap(Item::getId, i-> i));
        IPage<FavoriteVO> voPage = favoritePage.convert(favorite->{
                    FavoriteVO vo = new FavoriteVO();
                    BeanUtils.copyProperties(favorite,vo);
                    Item item = itemMap.get(favorite.getItemId());
                    if (item!=null) {
                        vo.setItemName(item.getTitle());
                        vo.setItemPrice(item.getPrice());
                        vo.setItemStatus(item.getStatus());
                        vo.setItemImage(item.getCover());//查询封面
                    }
                    return vo;
                }
        );

        PageDTO<FavoriteVO> resultPage = PageDTO.of(voPage);

        stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(resultPage), Duration.ofMinutes(30));

        return resultPage;
    }
    @Transactional
    @Override
    public void deleteFavorite(Long id) {
        Long userId = BaseContext.getCurrentId();
        Favorite favorite = lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getId, id)
                .one();
        if (favorite == null) {
            throw new BusinessException("收藏的商品不存在");
        }
        this.removeById(id);
        Set<String> keys = stringRedisTemplate.keys(RedisConstant.USER_FAVORITE_KEY + userId + ":*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

}
