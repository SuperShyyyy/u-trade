package com.u.chat.redis;

import com.u.chat.config.ChatProperties;
import com.u.chat.constant.ChatRedisConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息消费幂等与状态追踪：LOCK → CONSUMED → DELIVERED。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageIdempotencyService {

    private static final String STATE_LOCK = "LOCK";
    private static final String STATE_CONSUMED = "CONSUMED";
    private static final String STATE_DELIVERED = "DELIVERED";

    private final StringRedisTemplate redisTemplate;
    private final ChatProperties chatProperties;
    private final ConcurrentHashMap<String, String> localStates = new ConcurrentHashMap<>();

    /**
     * 幂等检查：是否已完成投递。
     */
    public boolean isDelivered(String messageId) {
        return STATE_DELIVERED.equals(getState(messageId));
    }

    /**
     * 尝试获取消费锁（setnx），防止重复消费。
     *
     * @return true 表示首次获取；false 表示重复或进行中
     */
    public boolean tryAcquire(String messageId) {
        try {
            Duration ttl = Duration.ofSeconds(chatProperties.getMessageIdempotencyTtlSeconds());
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(consumedKey(messageId), STATE_LOCK, ttl);
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.warn("Redis 幂等锁获取失败，降级本地 Map, messageId={}", messageId, e);
            return localStates.putIfAbsent(messageId, STATE_LOCK) == null;
        }
    }

    /**
     * 标记 MQ 消息已进入消费阶段。
     */
    public void markConsumed(String messageId) {
        setState(messageId, STATE_CONSUMED);
        log.info("[消费状态] messageId={} → CONSUMED", messageId);
    }

    /**
     * 标记 WebSocket 推送成功。
     */
    public void markDelivered(String messageId) {
        setState(messageId, STATE_DELIVERED);
        log.info("[消费状态] messageId={} → DELIVERED", messageId);
    }

    /**
     * 推送失败时释放锁，允许 MQ 重试。
     */
    public void releaseForRetry(String messageId) {
        try {
            String current = getState(messageId);
            if (STATE_LOCK.equals(current) || STATE_CONSUMED.equals(current)) {
                redisTemplate.delete(consumedKey(messageId));
                localStates.remove(messageId);
                log.warn("[重试钩子] messageId={} 已释放消费锁，等待 MQ 重投", messageId);
            }
        } catch (Exception e) {
            localStates.remove(messageId);
            log.warn("[重试钩子] messageId={} 释放消费锁失败", messageId, e);
        }
    }

    private void setState(String messageId, String state) {
        try {
            Duration ttl = Duration.ofSeconds(chatProperties.getMessageIdempotencyTtlSeconds());
            redisTemplate.opsForValue().set(consumedKey(messageId), state, ttl);
        } catch (Exception e) {
            log.warn("Redis 状态写入失败，降级本地 Map, messageId={}, state={}", messageId, state, e);
            localStates.put(messageId, state);
        }
    }

    private String getState(String messageId) {
        try {
            String state = redisTemplate.opsForValue().get(consumedKey(messageId));
            return state != null ? state : localStates.get(messageId);
        } catch (Exception e) {
            return localStates.get(messageId);
        }
    }

    private String consumedKey(String messageId) {
        return ChatRedisConstant.MSG_CONSUMED_PREFIX + messageId;
    }
}
