package com.example.learning.common;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
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
    public void unlock(String key, String value) {
        Object currentValue = get(key);
        if (currentValue != null && currentValue.equals(value)) {
            delete(key);
        }
    }
}
