package com.example.learning.redis;

import com.example.learning.common.RedisUtil;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁实战：秒杀库存扣减（解决分布式系统并发安全）
 */
@Component
public class DistributedLockDemo {
    @Resource
    private RedisUtil redisUtil;

    // 库存key（Redis中存储库存数量）
    private static final String STOCK_KEY = "seckill:stock:1";
    // 分布式锁key
    private static final String LOCK_KEY = "lock:seckill:1";

    // 初始化库存（项目启动时执行）
    @PostConstruct
    public void initStock() {
        // 模拟秒杀库存100件
        redisUtil.set(STOCK_KEY, 100, 24, TimeUnit.HOURS);
    }

    /**
     * 秒杀扣减库存（使用分布式锁）
     * @return 秒杀结果（成功/失败）
     */
    public String seckill() {
        String lockValue = null;
        try {
            // 1. 获取分布式锁（过期时间5秒，防止死锁）
            lockValue = redisUtil.tryLock(LOCK_KEY, 5, TimeUnit.SECONDS);
            if (lockValue == null) {
                // 未抢到锁，返回秒杀失败
                return "秒杀失败，当前排队人数过多，请重试！";
            }

            // 2. 抢到锁，查询库存
            Integer stock = (Integer) redisUtil.get(STOCK_KEY);
            if (stock == null || stock <= 0) {
                return "秒杀失败，库存已售罄！";
            }

            // 3. 扣减库存（模拟业务耗时）
            Thread.sleep(100);
            redisUtil.set(STOCK_KEY, stock - 1, 24, TimeUnit.HOURS);
            System.out.println("秒杀成功，剩余库存：" + (stock - 1));
            return "秒杀成功！剩余库存：" + (stock - 1);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return "秒杀异常，请重试！";
        } finally {
            // 4. 释放锁（必须在finally中，确保锁一定释放）
            if (lockValue != null) {
                redisUtil.unlock(LOCK_KEY, lockValue);
            }
        }
    }

    /**
     * 重入锁实战（同一线程多次获取锁）
     */
    public void testReentrantLock() {
        String lockKey = "lock:reentrant:test";
        // 第一次获取锁
        String lockValue = redisUtil.tryLock(lockKey, 10, TimeUnit.SECONDS);
        if (lockValue != null) {
            System.out.println("第一次获取锁成功");
            // 同一线程重入锁
            boolean reentrant = redisUtil.reentrantLock(lockKey, lockValue, 10, TimeUnit.SECONDS);
            if (reentrant) {
                System.out.println("重入锁成功");
                // 执行业务逻辑
                // ...
                // 释放重入锁（释放一次即可，因为锁的key只有一个）
                redisUtil.unlock(lockKey, lockValue);
                System.out.println("重入锁释放成功");
            }
            // 释放第一次的锁
            redisUtil.unlock(lockKey, lockValue);
            System.out.println("第一次锁释放成功");
        }
    }
}
