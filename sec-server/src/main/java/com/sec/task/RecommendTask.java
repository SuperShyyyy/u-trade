package com.sec.task;

import com.sec.service.HomeRecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendTask {

    private final HomeRecommendService homeRecommendService;

    /**
     * 每5分钟刷新推荐缓存
     */
    @Scheduled(cron = "0 */2 * * * ?")
    public void refreshRecommendCache() {
        log.info("定时刷新推荐缓存...");
        homeRecommendService.refreshRecommendCache();
    }
}