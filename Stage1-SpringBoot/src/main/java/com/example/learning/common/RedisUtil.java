package com.example.learning.common;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 设置缓存
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    // 获取缓存
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // 删除缓存
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    // 设置永不过期
    public void setNeverExpire(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    // 加锁（用于缓存击穿）
    public boolean lock(String key, String value, long timeout, TimeUnit unit) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
    }

    // 解锁
//    public void unlock(String key, String value) {
//        Object currentValue = get(key);
//        if (currentValue != null && currentValue.equals(value)) {
//            delete(key);
//        }
//    }

    /**
     * 获取分布式锁（基础版）
     * @param lockKey 锁的key
     * @param expireTime 过期时间（避免死锁）
     * @param unit 时间单位
     * @return 锁的value（用于解锁，防止误释放）
     */
    public String tryLock(String lockKey, long expireTime, TimeUnit unit) {
        // 生成唯一value（UUID），解锁时校验，防止误释放其他线程的锁
        String lockValue = UUID.randomUUID().toString();
        // SET NX EX：只有key不存在时才设置（互斥性），同时设置过期时间（防死锁）
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, expireTime, unit);
        // 成功获取锁，返回value；失败返回null
        return success != null && success ? lockValue : null;
    }

    /**
     * 释放分布式锁（安全版，校验value）
     * @param lockKey 锁的key
     * @param lockValue 锁的value（获取锁时返回的UUID）
     * @return 释放结果
     */
    public boolean unlock(String lockKey, String lockValue) {
        // 1. 获取当前锁的value
        Object currentValue = redisTemplate.opsForValue().get(lockKey);
        // 2. 校验：只有当前value和传入的value一致，才释放锁（防止误释放）
        if (lockValue != null && lockValue.equals(currentValue)) {
            // 3. 释放锁（删除key）
            redisTemplate.delete(lockKey);
            return true;
        }
        return false;
    }

    /**
     * 重入锁（进阶，同一线程可多次获取锁）
     * @param lockKey 锁的key
     * @param lockValue 第一次获取锁的value（同一线程复用）
     * @param expireTime 过期时间（重置过期时间，防止锁过期）
     * @param unit 时间单位
     * @return 是否获取成功
     */
    public boolean reentrantLock(String lockKey, String lockValue, long expireTime, TimeUnit unit) {
        // 1. 校验当前锁的value是否是当前线程的value（同一线程）
        Object currentValue = redisTemplate.opsForValue().get(lockKey);
        if (lockValue != null && lockValue.equals(currentValue)) {
            // 2. 重置过期时间（重入时续期，防止锁过期）
            redisTemplate.opsForValue().set(lockKey, lockValue, expireTime, unit);
            return true;
        }
        // 3. 不是同一线程，按正常流程获取锁
        return tryLock(lockKey, expireTime, unit) != null;
    }
}
