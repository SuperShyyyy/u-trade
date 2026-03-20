package com.sec.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sec.domain.po.Item;
import com.sec.domain.vo.ItemVO;
import com.sec.mapper.ItemMapper;
import com.sec.result.PageDTO;
import com.sec.service.HomeRecommendService;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomeRecommendServiceImpl implements HomeRecommendService {

    private final ItemMapper itemMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // ========== 缓存配置 ==========
    private static final String CACHE_KEY = "recommend:top1000";
    private static final int CANDIDATE_SIZE = 1000;          // 候选商品数
    private static final int CACHE_EXPIRE_MINUTES = 10;      // 缓存过期时间（比定时任务间隔长）
    private static final ZoneId SYSTEM_ZONE = ZoneId.of("Asia/Shanghai");

    // ========== 推荐算法参数 ==========
    private static final double K1 = 100.0;
    private static final double K2 = 1.0;
    private static final double K3 = 3.0;
    private static final double K4 = 0.5;
    private static final double G = 1.5;

    /**
     * 应用启动时预热缓存
     */
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


    public void refreshRecommendCache() {
        List<Item> itemList = itemMapper.selectWithSellerCredit(CANDIDATE_SIZE);
        if (itemList == null || itemList.isEmpty()) {
            return;
        }

        // 1. 计算并排序
        List<ItemVO> sortedList = calculateAndSort(itemList);

        // 2. 【关键修复】在存入缓存前打散头部数据，保证分页的一致性
        if (sortedList.size() > 1) {
            int shuffleRange = Math.min(30, sortedList.size());
            List<ItemVO> topN = new ArrayList<>(sortedList.subList(0, shuffleRange));
            Collections.shuffle(topN); // 打散前N条

            // 替换原列表的前N条
            for (int i = 0; i < shuffleRange; i++) {
                sortedList.set(i, topN.get(i));
            }
        }
        try {
            redisTemplate.opsForValue().set(
                    CACHE_KEY,
                    objectMapper.writeValueAsString(sortedList),
                    CACHE_EXPIRE_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            log.error("缓存写入失败", e);
        }
    }


    /**
     * 接口：从 Redis 读取并分页
     */
    @Override
    public PageDTO<ItemVO> pageQueryRecommendItem(int page, int pageSize) {
        long startTime = System.currentTimeMillis();
        if (page < 1) page = 1;
        if (pageSize <= 0) pageSize = 10;
        // 1. 从 Redis 获取
        List<ItemVO> allList = getCachedRecommendList();

        // 2. 缓存为空，直接降级
        if (allList == null || allList.isEmpty()) {
            log.warn("推荐缓存为空，降级走数据库实时查询 page = {} ，pageSize = {}", page, pageSize);
            return fallbackQuery(page, pageSize);
        }
        PageDTO<ItemVO> result = paginate(allList, page, pageSize);
        // 3. 内存分页与打散
        long costTime = System.currentTimeMillis() - startTime;
        log.debug("推荐查询完成，页码: {}, 耗时: {}ms", page, costTime);

        return result;
    }

    /**
     * 从 Redis 获取缓存列表
     */
    private List<ItemVO> getCachedRecommendList() {
        Object obj = redisTemplate.opsForValue().get(CACHE_KEY);
        if (obj == null) {
            return null;
        }

        return (List<ItemVO>) obj;
    }

    /**
     * 内存分页
     */
    private PageDTO<ItemVO> paginate(List<ItemVO> allList, int page, int pageSize) {
        int total = allList.size();
        if (total == 0) {
            return new PageDTO<>(0L, 0L, (long) page, new ArrayList<>());
        }

        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        if (fromIndex >= total) {
            return new PageDTO((long) total, (long) ((total + pageSize - 1) / pageSize), (long) page, new ArrayList<>());
        }

        List<ItemVO> pageItems = allList.subList(fromIndex, toIndex);
        long pages = (total + pageSize - 1) / pageSize;

        return new PageDTO<>((long) total, pages, (long) page, pageItems);
    }

    /**
     * 降级查询：缓存失效时直接查数据库
     */
    private PageDTO<ItemVO> fallbackQuery(int page, int pageSize) {
        int fetchSize = page * pageSize * 2;
        List<Item> itemList = itemMapper.selectWithSellerCredit(fetchSize);

        if (itemList == null || itemList.isEmpty()) {
            return new PageDTO<>(0L, 0L, (long) page, new ArrayList<>());
        }

        List<ItemVO> sortedList = calculateAndSort(itemList);
        return paginate(sortedList, page, pageSize);
    }

    /**
     * 计算分数并排序
     */
    private List<ItemVO> calculateAndSort(List<Item> itemList) {
        long currentTimeSeconds = System.currentTimeMillis() / 1000;

        return itemList.stream()
                .map(item -> {
                    ItemVO vo = new ItemVO();
                    BeanUtils.copyProperties(item, vo);
                    double score = calculateRecommendScore(item, currentTimeSeconds);
                    return new ItemVOWithScore(vo, score);
                })
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .map(v -> v.vo)
                .collect(Collectors.toList());
    }

    /**
     * 推荐分数计算
     */
    private double calculateRecommendScore(Item item, long currentTimeSeconds) {
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

    /**
     * 内部类：VO + 分数
     */
    private static class ItemVOWithScore {
        ItemVO vo;
        double score;

        ItemVOWithScore(ItemVO vo, double score) {
            this.vo = vo;
            this.score = score;
        }
    }
}