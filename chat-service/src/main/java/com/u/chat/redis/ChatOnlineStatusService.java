package com.u.chat.redis;

import com.u.chat.config.ChatProperties;
import com.u.chat.constant.ChatRedisConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis 在线状态与 userId -> node 映射（辅助 SessionManager，不替代本地映射）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatOnlineStatusService {

    private final StringRedisTemplate redisTemplate;
    private final ChatProperties chatProperties;

    /**
     * 顺序写入：user:online → user:node，分别记录同步结果。
     */
    public void markOnline(Long userId, String nodeId) {
        Duration ttl = Duration.ofSeconds(chatProperties.getOnlineTtlSeconds());
        writeOnlineKey(userId, nodeId, ttl);
        writeNodeKey(userId, nodeId, ttl);
    }

    /**
     * 顺序删除：user:online → user:node。
     */
    public void markOffline(Long userId) {
        deleteOnlineKey(userId);
        deleteNodeKey(userId);
    }

    public Optional<String> getUserNode(Long userId) {
        try {
            String nodeId = redisTemplate.opsForValue().get(nodeKey(userId));
            if (nodeId == null || nodeId.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(nodeId);
        } catch (Exception e) {
            log.warn("Redis 查询 user node 失败 userId={}", userId, e);
            return Optional.empty();
        }
    }

    public boolean isOnlineInCluster(Long userId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(onlineKey(userId)));
        } catch (Exception e) {
            log.warn("Redis 查询在线状态失败 userId={}", userId, e);
            return false;
        }
    }

    private void writeOnlineKey(Long userId, String nodeId, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(onlineKey(userId), nodeId, ttl);
            log.info("Redis 同步成功 user:online:{}={}", userId, nodeId);
        } catch (Exception e) {
            log.warn("Redis 同步失败 user:online:{} userId={}", userId, userId, e);
        }
    }

    private void writeNodeKey(Long userId, String nodeId, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(nodeKey(userId), nodeId, ttl);
            log.info("Redis 同步成功 user:node:{}={}", userId, nodeId);
        } catch (Exception e) {
            log.warn("Redis 同步失败 user:node:{} userId={}", userId, userId, e);
        }
    }

    private void deleteOnlineKey(Long userId) {
        try {
            redisTemplate.delete(onlineKey(userId));
            log.info("Redis 清理成功 user:online:{}", userId);
        } catch (Exception e) {
            log.warn("Redis 清理失败 user:online:{} userId={}", userId, userId, e);
        }
    }

    private void deleteNodeKey(Long userId) {
        try {
            redisTemplate.delete(nodeKey(userId));
            log.info("Redis 清理成功 user:node:{}", userId);
        } catch (Exception e) {
            log.warn("Redis 清理失败 user:node:{} userId={}", userId, userId, e);
        }
    }

    private String onlineKey(Long userId) {
        return ChatRedisConstant.USER_ONLINE_PREFIX + userId;
    }

    private String nodeKey(Long userId) {
        return ChatRedisConstant.USER_NODE_PREFIX + userId;
    }
}
