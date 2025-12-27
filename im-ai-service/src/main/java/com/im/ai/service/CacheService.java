package com.im.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存服务
 * 实现分布式锁和缓存功能
 */
@Slf4j
@Service
public class CacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 缓存前缀
     */
    private static final String CACHE_PREFIX = "im:ai:faq:";

    /**
     * 所有FAQ缓存 key
     */
    private static final String CACHE_KEY_ALL = CACHE_PREFIX + "all";

    /**
     * 缓存过期时间：30分钟
     */
    private static final long CACHE_TTL_MINUTES = 30;

    /**
     * 获取缓存
     */
    @SuppressWarnings("unchecked")
    public <T> T getCache(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("缓存命中: {}", key);
                return (T) value;
            }
            log.debug("缓存未命中: {}", key);
            return null;
        } catch (Exception e) {
            log.error("获取缓存失败: {}", key, e);
            return null;
        }
    }

    /**
     * 设置缓存
     */
    public <T> void setCache(String key, T value) {
        try {
            redisTemplate.opsForValue().set(key, value, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("缓存已设置: {}", key);
        } catch (Exception e) {
            log.error("设置缓存失败: {}", key, e);
        }
    }

    /**
     * 删除缓存
     */
    public void deleteCache(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("缓存已删除: {}", key);
        } catch (Exception e) {
            log.error("删除缓存失败: {}", key, e);
        }
    }

    /**
     * 删除所有FAQ缓存
     */
    public void deleteAllFaqCache() {
        deleteCache(CACHE_KEY_ALL);
    }

    /**
     * 获取分布式锁
     */
    public RLock getLock(String lockName) {
        return redissonClient.getLock(lockName);
    }

    /**
     * 带锁执行操作
     * 防止缓存击穿
     */
    public <T> T executeWithLock(String lockName, long waitTime, long leaseTime, TimeUnit unit,
                                  CacheCallback<T> callback) {
        RLock lock = redissonClient.getLock(lockName);
        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, unit);
            if (acquired) {
                try {
                    return callback.execute();
                } finally {
                    lock.unlock();
                }
            }
            return callback.execute(); // 没获取到锁也执行
        } catch (InterruptedException e) {
            log.error("获取锁被中断", e);
            Thread.currentThread().interrupt();
            return callback.execute();
        } catch (Exception e) {
            log.error("带锁执行失败", e);
            return callback.execute();
        }
    }

    /**
     * 缓存回调接口
     */
    @FunctionalInterface
    public interface CacheCallback<T> {
        T execute();
    }
}
