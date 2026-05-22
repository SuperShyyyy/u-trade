package com.u.item.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.u.api.client.user.UserClient;
import com.u.item.domain.po.Item;
import com.u.item.domain.vo.ItemVO;
import com.u.item.mapper.ItemMapper;
import com.u.common.result.PageDTO;
import com.u.common.result.Result;
import com.u.item.service.HomeRecommendService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.type.TypeReference;
@Service
@RequiredArgsConstructor
@Slf4j
public class HomeRecommendServiceImpl implements HomeRecommendService {

    private final UserClient userClient;
    private final ItemMapper itemMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String CACHE_KEY = "recommend:top1000";
    private static final int CANDIDATE_SIZE = 1000;
    private static final int CACHE_EXPIRE_MINUTES = 10;
    private static final ZoneId SYSTEM_ZONE = ZoneId.of("Asia/Shanghai");
    // 推荐算法参数
    private static final double K1 = 100.0;
    private static final double K2 = 1.0;
    private static final double K3 = 3.0;
    private static final double K4 = 0.5;
    private static final double G = 1.5;

    @PostConstruct
    public void initCache() {
        log.info("应用启动，预热推荐缓存...");
        try {
            refreshRecommendCache();
            log.info("推荐缓存预热完成");
        } catch (Exception e) {
            log.error("推荐缓存预热失败", e);
        }
    }

    /**
     * 刷新推荐缓存
     */
    public void refreshRecommendCache() {
        List<Item> itemList = itemMapper.selectForRecommend(CANDIDATE_SIZE);
        if (itemList == null || itemList.isEmpty()) return;

        // 远程调用获取卖家信用分
        List<Long> sellerIds = itemList.stream()
                .map(Item::getSellerId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Integer> creditMap = getUserCreditScores(sellerIds);

        // 填充 VO
        List<ItemVO> itemVOList = itemList.stream()
                .map(item -> {
                    ItemVO vo = new ItemVO();
                    BeanUtils.copyProperties(item, vo);
                    vo.setSellerCreditScore(creditMap.getOrDefault(item.getSellerId(), 0));
                    return vo;
                })
                .collect(Collectors.toList());

        // 排序、打散、缓存
        List<ItemVO> sortedList = calculateAndSort(itemVOList);
        shuffleTop(sortedList);
        cacheToRedis(sortedList);
    }

    /**
     * 获取推荐列表（分页）
     */
    @Override
    public PageDTO<ItemVO> pageQueryRecommendItem(int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize <= 0) pageSize = 10;

        List<ItemVO> allList = getCachedRecommendList();
        if (allList == null || allList.isEmpty()) {
            log.warn("推荐缓存为空，降级走数据库查询 page={}, pageSize={}", page, pageSize);
            return fallbackQuery(page, pageSize);
        }
        return paginate(allList, page, pageSize);
    }

    /** ====================== 内部工具方法 ====================== **/

    private List<ItemVO> getCachedRecommendList() {
        Object obj = redisTemplate.opsForValue().get(CACHE_KEY);
        if (obj == null) return null;

        try {
            return objectMapper.readValue(String.valueOf(obj), new TypeReference<List<ItemVO>>() {});
        } catch (Exception e) {
            log.error("Redis 反序列化推荐列表失败", e);
            return null;
        }
    }

    private void cacheToRedis(List<ItemVO> list) {
        try {
            redisTemplate.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(list),
                    CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("缓存写入失败", e);
        }
    }

    private void shuffleTop(List<ItemVO> list) {
        if (list.size() <= 1) return;
        int shuffleRange = Math.min(30, list.size());
        List<ItemVO> topN = new ArrayList<>(list.subList(0, shuffleRange));
        Collections.shuffle(topN);
        for (int i = 0; i < shuffleRange; i++) {
            list.set(i, topN.get(i));
        }
    }

    private PageDTO<ItemVO> paginate(List<ItemVO> list, int page, int pageSize) {
        int total = list.size();
        int fromIndex = Math.min((page - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<ItemVO> pageItems = list.subList(fromIndex, toIndex);
        long pages = (total + pageSize - 1) / pageSize;
        return new PageDTO<>((long) total, pages, (long) page, pageItems);
    }

    private PageDTO<ItemVO> fallbackQuery(int page, int pageSize) {
        int fetchSize = page * pageSize * 2;
        List<Item> itemList = itemMapper.selectForRecommend(fetchSize);
        if (itemList == null || itemList.isEmpty()) {
            return new PageDTO<>(0L, 0L, (long) page, new ArrayList<>());
        }

        List<Long> sellerIds = itemList.stream().map(Item::getSellerId).distinct().toList();
        Map<Long, Integer> creditMap = getUserCreditScores(sellerIds);

        List<ItemVO> voList = itemList.stream()
                .map(item -> {
                    ItemVO vo = new ItemVO();
                    BeanUtils.copyProperties(item, vo);
                    vo.setSellerCreditScore(creditMap.getOrDefault(item.getSellerId(), 0));
                    return vo;
                })
                .collect(Collectors.toList());

        return paginate(calculateAndSort(voList), page, pageSize);
    }

    private List<ItemVO> calculateAndSort(List<ItemVO> itemList) {
        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        return itemList.stream()
                .map(vo -> new ItemVOWithScore(vo, calculateRecommendScore(vo, currentTimeSeconds)))
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .map(v -> v.vo)
                .collect(Collectors.toList());
    }

    private double calculateRecommendScore(ItemVO item, long currentTimeSeconds) {
        long createTimeSeconds = item.getCreateTime().atZone(SYSTEM_ZONE).toEpochSecond();
        long diffSeconds = Math.max(0, currentTimeSeconds - createTimeSeconds);
        double hourDiff = diffSeconds / 3600.0;

        Long viewCount = item.getViewCount() != null ? item.getViewCount() : 0;
        Long wantCount = item.getWantCount() != null ? item.getWantCount() : 0;
        int credit = item.getSellerCreditScore() != null ? item.getSellerCreditScore() : 0;

        double qualityScore = (K2 * viewCount) + (K3 * wantCount) + (K4 * credit);
        double timeDecay = Math.pow(hourDiff + 2, G);

        return (K1 + qualityScore) / timeDecay;
    }

    private Map<Long, Integer> getUserCreditScores(List<Long> sellerIds) {
        Result<Map<Long, Integer>> result = userClient.getUserCreditScores(sellerIds);
        if (result == null || result.getData() == null) {
            return Collections.emptyMap();
        }
        return result.getData();
    }

    private static class ItemVOWithScore {
        ItemVO vo;
        double score;

        ItemVOWithScore(ItemVO vo, double score) {
            this.vo = vo;
            this.score = score;
        }
    }
}