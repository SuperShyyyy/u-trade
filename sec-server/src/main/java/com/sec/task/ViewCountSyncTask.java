package com.sec.task;

import com.sec.constant.RedisConstant;
import com.sec.mapper.ItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ViewCountSyncTask {

    private final StringRedisTemplate stringRedisTemplate;
    private final ItemMapper itemMapper;

    @Scheduled(cron = "0 */5 * * * ?")
    public void syncViewCount() {
        Set<String> ids = stringRedisTemplate.opsForSet().members(RedisConstant.ITEM_VIEW_IDS);

        if (ids == null || ids.isEmpty()) {
            return;
        }

        for (String idStr : ids) {
            Long itemId = Long.valueOf(idStr);
            String key = RedisConstant.ITEM_VIEW_COUNT + itemId;

            try {
                //读取并设为 0
                String countStr = stringRedisTemplate.opsForValue().getAndSet(key, "0");

                if (countStr == null || "0".equals(countStr)) {
                    // 如果是0，说明没有新增量，从处理列表中移除
                    stringRedisTemplate.opsForSet().remove(RedisConstant.ITEM_VIEW_IDS, idStr);
                    continue;
                }

                Long count = Long.valueOf(countStr);

                // 数据库累加
                itemMapper.incrementViewCount(itemId, count);

                // 从集合中移除该商品ID
                stringRedisTemplate.opsForSet().remove(RedisConstant.ITEM_VIEW_IDS, idStr);

            } catch (Exception e) {
                log.error("同步浏览量失败 itemId={}", itemId, e);
            }
        }
    }
}