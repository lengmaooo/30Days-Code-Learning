# 第4天：JUC进阶 + 线程池实战（超级细版）
今天聚焦JUC高频进阶考点+线程池实战落地，所有代码直接在你现有项目`Stage1-SpringBoot`里新增/修改，八股答案精简到能直接背，确保面试能说清、代码能写对。

---

## 一、核心目标
1. 掌握CountDownLatch/CyclicBarrier/Semaphore（JUC三大辅助类）
2. 实现线程池实战场景（异步处理用户操作、自定义拒绝策略）
3. 理解CompletableFuture（异步编程核心）
4. 背会5道进阶八股题

---

## 二、代码部分（直接复制到项目）
### 1. 新增包结构
继续在`com.example.learning.juc`包下新增类，所有代码统一放在这里。

### 2. JUC三大辅助类（面试高频）
#### ① CountDownLatch（倒计时门闩：等待多线程完成）
新建`CountDownLatchDemo.java`
```java
package com.example.learning.juc;

import java.util.concurrent.CountDownLatch;

/**
 * CountDownLatch：让主线程等待多个子线程执行完再继续
 * 实战场景：接口聚合（等待多个微服务接口返回结果）、批量任务执行
 */
public class CountDownLatchDemo {
    public static void main(String[] args) throws InterruptedException {
        // 初始化计数器为3（需要等待3个线程完成）
        CountDownLatch latch = new CountDownLatch(3);

        // 线程1：模拟查询用户信息
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                System.out.println("线程1：用户信息查询完成");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                // 计数器-1
                latch.countDown();
            }
        }).start();

        // 线程2：模拟查询订单信息
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                System.out.println("线程2：订单信息查询完成");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }).start();

        // 线程3：模拟查询商品信息
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                System.out.println("线程3：商品信息查询完成");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }).start();

        // 主线程等待：直到计数器变为0
        System.out.println("主线程：等待所有查询完成...");
        latch.await();
        System.out.println("主线程：所有查询完成，开始聚合返回结果");
    }
}
```
**运行效果**：主线程会等3个子线程都执行完，才会打印“开始聚合返回结果”。

#### ② CyclicBarrier（循环屏障：等待多线程达到同一节点）
新建`CyclicBarrierDemo.java`
```java
package com.example.learning.juc;

import java.util.concurrent.CyclicBarrier;

/**
 * CyclicBarrier：让多个线程互相等待，直到所有线程都到达屏障点才继续
 * 实战场景：多线程任务分阶段执行（比如先全部准备好，再一起执行）
 */
public class CyclicBarrierDemo {
    public static void main(String[] args) {
        // 初始化屏障：3个线程到达后，执行屏障任务
        CyclicBarrier barrier = new CyclicBarrier(3, () -> {
            System.out.println("=== 所有线程已到达屏障，开始执行核心任务 ===");
        });

        // 3个线程模拟运动员准备
        for (int i = 1; i <= 3; i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    System.out.println("运动员" + finalI + "：准备中...");
                    Thread.sleep(finalI * 1000); // 模拟不同准备时间
                    System.out.println("运动员" + finalI + "：准备完成，等待其他运动员");
                    // 到达屏障，等待其他线程
                    barrier.await();
                    // 屏障后执行
                    System.out.println("运动员" + finalI + "：开始比赛");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
```
**运行效果**：3个运动员都准备完成后，才会执行屏障任务，然后各自开始比赛（可循环使用）。

#### ③ Semaphore（信号量：控制并发数）
新建`SemaphoreDemo.java`
```java
package com.example.learning.juc;

import java.util.concurrent.Semaphore;

/**
 * Semaphore：控制同时访问的线程数（限流）
 * 实战场景：接口限流、数据库连接池、秒杀接口控制并发
 */
public class SemaphoreDemo {
    public static void main(String[] args) {
        // 初始化信号量：允许3个线程同时访问（公平锁）
        Semaphore semaphore = new Semaphore(3, true);

        // 10个线程模拟请求接口
        for (int i = 1; i <= 10; i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    // 获取许可（没有则等待）
                    semaphore.acquire();
                    System.out.println("请求" + finalI + "：获取到许可，开始处理");
                    Thread.sleep(1000); // 模拟接口处理时间
                    System.out.println("请求" + finalI + "：处理完成，释放许可");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    // 释放许可
                    semaphore.release();
                }
            }).start();
        }
    }
}
```
**运行效果**：同一时间只有3个请求在处理，其他请求等待，释放一个许可才会有一个新请求进来。

### 3. 线程池实战（落地到业务）
#### ① 自定义线程池配置（替代Executors，面试推荐写法）
新建`ThreadPoolConfig.java`（放到`com.example.learning.config`包）
```java
package com.example.learning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.*;

/**
 * 自定义线程池配置：实战中禁止用Executors创建（避免OOM）
 * 核心：根据业务场景设置参数，自定义线程名+拒绝策略
 */
@Configuration
public class ThreadPoolConfig {

    @Bean("userThreadPool") // 命名线程池，方便区分
    public ExecutorService userThreadPool() {
        // 核心参数计算：CPU密集型=CPU核心数+1；IO密集型=CPU核心数*2
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
                corePoolSize, // 核心线程数（CPU核心数）
                corePoolSize * 2, // 最大线程数
                60L, // 临时线程空闲时间
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100), // 任务队列（容量100）
                // 自定义线程工厂（设置线程名，方便排查问题）
                new ThreadFactory() {
                    private int count = 1;

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("user-thread-" + count++);
                        return thread;
                    }
                },
                // 自定义拒绝策略（打印日志+重试，比默认AbortPolicy更友好）
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
```

#### ② 线程池实战：异步处理用户操作（比如登录后记录日志）
修改`UserServiceImpl.java`，注入自定义线程池：
```java
package com.example.learning.service.impl;

import com.example.learning.common.JwtUtil;
import com.example.learning.entity.User;
import com.example.learning.mapper.UserMapper;
import com.example.learning.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    // 注入自定义线程池
    @Resource(name = "userThreadPool")
    private ExecutorService userThreadPool;

    @Override
    public String login(String username, String password) {
        // 1. 根据用户名查询用户
        User user = userMapper.selectByUsername(username);

        // 2. 用户不存在
        if (user == null) {
            throw new RuntimeException("用户名不存在");
        }

        // 3. 验证密码
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 异步记录登录日志（不阻塞登录接口响应）
        userThreadPool.submit(() -> {
            try {
                // 模拟日志记录耗时操作
                Thread.sleep(500);
                System.out.println("【登录日志】用户：" + username + "，ID：" + user.getId() + "，登录时间：" + System.currentTimeMillis());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // 4. 生成 token 并返回
        return jwtUtil.generateToken(user.getId());
    }

    // 其他方法保持不变...
}
```
**测试效果**：调用`/login`接口时，登录响应立即返回，日志记录在后台异步执行（不影响接口响应速度）。

### 4. CompletableFuture（异步编程核心，面试必问）
新建`CompletableFutureDemo.java`
```java
package com.example.learning.juc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * CompletableFuture：异步编程神器，比Future更强大
 * 实战场景：异步调用多个接口、结果聚合、异常处理
 */
public class CompletableFutureDemo {
    public static void main(String[] args) throws Exception {
        // 1. 异步执行无返回值任务
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000);
                System.out.println("异步任务1执行完成");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // 2. 异步执行有返回值任务
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1500);
                return "异步任务2返回结果";
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        // 3. 等待所有任务完成
        CompletableFuture.allOf(future1, future2).get();
        System.out.println("所有异步任务完成，future2结果：" + future2.get());

        // 4. 异常处理（实战必备）
        CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
            // 模拟异常
            int a = 1 / 0;
            return "正常返回";
        }).exceptionally(e -> {
            // 捕获异常并返回默认值
            System.out.println("任务3异常：" + e.getMessage());
            return "异常默认值";
        });

        System.out.println("future3结果：" + future3.get());
    }
}
```
**运行效果**：异步执行多个任务，自动处理异常，结果聚合后返回。

---

## 三、第4天必须背的5道进阶八股（精简答案）
1. **CountDownLatch和CyclicBarrier的区别？**
   ① CountDownLatch是“等待多线程完成”，计数器只能用一次；② CyclicBarrier是“多线程互相等待”，计数器可重置（循环使用）；③ CountDownLatch基于AQS共享模式，CyclicBarrier基于ReentrantLock。

2. **Semaphore的作用？实战场景？**
   作用：控制并发访问的线程数（限流）；场景：接口限流、数据库连接池、秒杀接口控制并发数。

3. **为什么禁止用Executors创建线程池？**
   ① FixedThreadPool/SingleThreadPool：队列是无界的，任务过多会OOM；② CachedThreadPool/ScheduledThreadPool：最大线程数是Integer.MAX_VALUE，会创建大量线程导致OOM；推荐手动创建ThreadPoolExecutor，指定核心参数。

4. **线程池的拒绝策略有哪些？**
   ① AbortPolicy（默认）：抛异常；② CallerRunsPolicy：由调用线程执行；③ DiscardPolicy：直接丢弃任务；④ DiscardOldestPolicy：丢弃队列最老的任务。

5. **CompletableFuture的核心优势？**
   ① 异步执行+结果回调；② 支持多个异步任务组合（allOf/anyOf）；③ 内置异常处理（exceptionally）；④ 无需手动管理线程池（可自定义）。

---

## 四、过关标准
1. 三大辅助类代码能运行，理解各自的实战场景；
2. 自定义线程池集成到登录业务，异步日志正常打印；
3. CompletableFuture代码能运行，理解异常处理逻辑；
4. 5道八股题能准确复述（重点区分CountDownLatch和CyclicBarrier）。
