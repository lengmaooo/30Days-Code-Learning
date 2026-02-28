# 第10天：Redis 分布式锁 + 第一阶段复盘（超级细版）
今天是第一阶段的收尾，核心有两个重点：① 手写Redis分布式锁（面试必写，解决分布式系统并发问题）；② 复盘前9天核心考点（JWT、JUC、JVM、MySQL、Redis），帮你梳理知识体系、巩固记忆，确保面试能连贯复述，代码能直接落地。

---

## 一、核心目标
1. 彻底理解分布式锁的核心原理，手写Redis分布式锁（含防死锁、重入、释放等细节）
2. 解决分布式锁的常见问题（死锁、误释放、重入性）
3. 复盘前9天核心考点，梳理知识脉络，查漏补缺
4. 背会5道分布式锁+复盘核心八股题

---

## 二、先理清核心概念（先懂原理，再写代码）
### 1. 分布式锁的核心需求（必须满足）
分布式系统中，多台服务器/多线程竞争同一资源（比如秒杀库存、转账），需要一个全局唯一的锁来保证并发安全，必须满足5点：
- 互斥性：同一时刻只有一个线程能拿到锁
- 安全性：不能被其他线程误释放
- 防死锁：即使线程崩溃，锁也能自动释放
- 重入性：同一线程可以多次获取同一把锁（可选，实战常用）
- 高可用：Redis宕机时，锁依然能正常工作（可选，进阶）

### 2. Redis分布式锁 vs 本地锁（synchronized/ReentrantLock）
| 锁类型       | 适用场景                  | 优点                  | 缺点                          |
|--------------|---------------------------|-----------------------|-------------------------------|
| 本地锁       | 单台服务器、单进程多线程  | 实现简单、性能高      | 分布式系统中失效（多服务器无共享锁） |
| Redis分布式锁 | 分布式系统、多服务器      | 全局唯一、跨服务可用  | 实现复杂（需处理死锁、误释放） |

### 3. 第一阶段知识脉络（复盘前置）
前9天我们从「基础登录」到「分布式缓存」，形成了完整的后端基础技术栈，脉络如下：
1. 第1天：SpringBoot基础 + 简单CRUD（项目搭建）
2. 第2天：JWT登录（身份认证，无状态登录）
3. 第3-4天：JUC锁 + 线程池（并发编程，解决多线程安全）
4. 第5-6天：JVM内存 + GC调优（虚拟机底层，解决OOM、性能问题）
5. 第7-8天：MySQL索引 + 事务锁（数据库优化，解决慢SQL、数据一致性）
6. 第9天：Redis缓存三大问题（缓存优化，解决穿透、击穿、雪崩）
7. 第10天：Redis分布式锁（分布式并发，解决跨服务竞争）

---

## 三、代码/实操部分（直接落地到你的项目）
所有代码基于你现有的`java-express-demo`项目，复用之前的Redis配置，无需额外新增依赖。

### 1. 第一步：手写Redis分布式锁（基础版，满足核心需求）
#### ① 完善RedisUtil，新增分布式锁核心方法
修改`com.example.common.RedisUtil.java`，添加分布式锁相关方法（基于SET NX EX实现）：
```java
package com.example.common;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 之前的方法（set、get、delete等）不变，新增以下方法

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
```

#### ② 分布式锁实战（秒杀场景，模拟多线程/多服务竞争）
新建`com.example.redis.DistributedLockDemo.java`，模拟秒杀库存扣减（分布式场景）：
```java
package com.example.redis;

import com.example.common.RedisUtil;
import org.springframework.stereotype.Component;
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
```

#### ③ 测试分布式锁（模拟并发）
新建`com.example.controller.SeckillController.java`，提供秒杀接口，用JMeter模拟并发：
```java
package com.example.controller;

import com.example.redis.DistributedLockDemo;
import com.example.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.Resource;

@RestController
public class SeckillController {
    @Resource
    private DistributedLockDemo distributedLockDemo;

    // 秒杀接口
    @GetMapping("/seckill")
    public Result seckill() {
        String result = distributedLockDemo.seckill();
        return Result.success(result);
    }

    // 测试重入锁
    @GetMapping("/testReentrantLock")
    public Result testReentrantLock() {
        distributedLockDemo.testReentrantLock();
        return Result.success("重入锁测试完成");
    }
}
```
**测试效果**：
1. 用JMeter模拟1000并发请求`/seckill`，库存最终会扣减到0（无超卖）；
2. 未抢到锁的请求会返回“排队过多”，避免并发安全问题；
3. 调用`/testReentrantLock`，控制台会打印重入锁的获取、释放流程。

### 2. 第二步：分布式锁常见问题及解决方案（面试必问）
#### ① 问题1：死锁（线程崩溃，锁未释放）
- 原因：线程获取锁后，未执行解锁代码就崩溃（比如服务宕机）；
- 解决方案：设置锁的过期时间（`SET NX EX`），即使线程崩溃，Redis会自动删除过期的锁。

#### ② 问题2：误释放锁（释放了其他线程的锁）
- 原因：线程A获取锁后，执行时间过长，锁过期自动释放，线程B获取到锁，此时线程A执行完解锁，误删除了线程B的锁；
- 解决方案：用UUID作为锁的value，解锁时校验value是否一致（我们的代码已实现）。

#### ③ 问题3：锁过期时间太短（业务未执行完，锁已释放）
- 原因：设置的过期时间小于业务执行时间，导致锁提前释放，并发安全失效；
- 解决方案：锁续期（比如用定时任务，每隔1秒检查线程是否还持有锁，若持有则延长过期时间），或使用Redisson的分布式锁（自带续期功能）。

#### ④ 问题4：Redis单点故障（锁失效）
- 原因：Redis单点宕机，所有分布式锁都无法获取，服务不可用；
- 解决方案：Redis主从+哨兵集群，即使主节点宕机，从节点切换为主节点，锁依然可用。

### 3. 第三步：第一阶段核心考点复盘（必背）
#### 1. 第1-2天：SpringBoot + JWT（基础登录）
- 核心：JWT的3部分（Header、Payload、Signature），无状态登录的实现；
- 关键代码：JwtUtil（生成token、验证token）、拦截器（校验token）、登录接口；
- 面试题：JWT和Session的区别（JWT无状态、存客户端；Session有状态、存服务器）。

#### 2. 第3-4天：JUC + 线程池（并发编程）
- 核心：volatile（可见性、禁止重排序）、synchronized（原子性）、ReentrantLock（灵活锁）、线程池7大参数；
- 关键代码：三大辅助类（CountDownLatch/CyclicBarrier/Semaphore）、CompletableFuture（异步编程）、自定义线程池；
- 面试题：synchronized和ReentrantLock的区别、线程池拒绝策略、ThreadLocal内存泄漏原因。

#### 3. 第5-6天：JVM（虚拟机底层）
- 核心：JVM内存区域（堆/栈/方法区等）、GC算法（标记-清除/复制/整理）、GC收集器（G1/CMS）；
- 关键代码：复现堆OOM、栈溢出、元空间OOM，GC日志配置与解读；
- 面试题：B+树原理、GC调优核心指标、Full GC触发条件。

#### 4. 第7-8天：MySQL（数据库）
- 核心：索引底层（B+树）、索引类型（聚簇/非聚簇/联合）、事务ACID、隔离级别、锁（行锁/表锁/间隙锁）；
- 关键代码：explain分析SQL、索引优化、事务转账、死锁复现与排查；
- 面试题：索引失效场景、事务隔离级别解决的问题、死锁产生的条件。

#### 5. 第9-10天：Redis（缓存+分布式锁）
- 核心：缓存三大问题（穿透/击穿/雪崩）、缓存更新策略（更DB+删缓存）、分布式锁核心实现；
- 关键代码：Redis工具类、三大问题解决方案、分布式锁手写实现；
- 面试题：缓存三大问题的区别与解决方案、分布式锁的核心需求、Redis分布式锁的缺点。

### 4. 第四步：第一阶段过关自查（确保全部掌握）
1. 能独立写出JWT登录完整流程（拦截器+工具类+登录接口）；
2. 能复现JUC锁、线程池、JVM OOM、MySQL死锁、Redis缓存问题；
3. 能手写Redis分布式锁（基础版），说出常见问题及解决方案；
4. 能连贯复述5大技术栈的核心考点，回答对应的面试题。

---

## 四、第10天必须背的5道核心八股（精简答案）
1. **Redis分布式锁的核心实现原理？**
   基于Redis的`SET NX EX`命令：NX保证互斥性（只有key不存在时才能设置），EX设置过期时间（防死锁）；用UUID作为value，解锁时校验value，防止误释放。

2. **Redis分布式锁的常见问题及解决方案？**
   ① 死锁：设置过期时间；② 误释放：校验锁的value（UUID）；③ 锁过期太短：锁续期；④ 单点故障：Redis主从+哨兵集群。

3. **分布式锁和本地锁的区别？**
   ① 本地锁：单台服务器、单进程可用，实现简单，分布式系统失效；② 分布式锁：跨服务器、跨进程可用，全局唯一，实现复杂（需处理死锁、误释放）。

4. **第一阶段核心技术栈有哪些？各自解决什么问题？**
   ① JWT：身份认证（无状态登录）；② JUC：并发编程（多线程安全）；③ JVM：虚拟机底层（OOM、性能优化）；④ MySQL：数据存储（慢SQL、数据一致性）；⑤ Redis：缓存（性能优化）+ 分布式锁（分布式并发）。

5. **缓存更新为什么选择“更DB+删缓存”？**
   避免并发下数据不一致：更新缓存会导致多个线程同时更新缓存，出现值覆盖错误；删除缓存让下一次查询重新加载最新数据，一致性更高，且实现简单。

---

## 五、过关标准
1. 能手写Redis分布式锁（基础版），解决死锁、误释放问题；
2. 能复现分布式锁的实战场景（比如秒杀），确保并发安全；
3. 能连贯复盘前9天核心考点，查漏补缺；
4. 5道八股题能准确复述（重点是分布式锁原理和问题、第一阶段知识脉络）。

### 第一阶段总结
前10天我们已经掌握了后端开发的「核心基础技术栈」，从项目搭建、登录认证，到并发编程、虚拟机、数据库、缓存，覆盖了面试80%的基础考点，也落地了可运行的实战代码。

接下来第二阶段，我们会聚焦「分布式进阶」（Dubbo、SpringCloud）、「高并发实战」（秒杀系统、分布式事务）、「面试真题演练」，帮你突破进阶考点，轻松应对中高级后端面试。
