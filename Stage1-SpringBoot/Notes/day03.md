# 第3天：JUC锁 + 线程池（超级细版）
---

## 一、先明确今天的核心目标
1. 写3个JUC锁的演示代码（volatile/synchronized/ReentrantLock）
2. 手写标准线程池 + ThreadLocal存用户ID
3. 背会5道核心八股题（面试必问）

---

## 二、代码部分（直接复制到项目里）
### 1. 新建包和类结构
在项目里新增包：`com.example.learning.juc`，所有JUC代码都放这里。

### 2. 演示1：volatile关键字（解决可见性）
新建 `VolatileDemo.java`
```java
package com.example.learning.juc;

/**
 * volatile演示：解决多线程下变量可见性问题
 * 不加volatile，线程2永远看不到flag变化；加了之后能立即看到
 */
public class VolatileDemo {
    // 核心：volatile修饰的变量，修改后会立即刷新到主内存，其他线程能看到
    private volatile boolean flag = false;

    public static void main(String[] args) throws InterruptedException {
        VolatileDemo demo = new VolatileDemo();

        // 线程1：循环等待flag变为true
        new Thread(() -> {
            while (!demo.flag) {
                // 空循环，不加volatile会一直卡在这里
            }
            System.out.println("线程1：看到flag变为true，退出循环");
        }).start();

        // 主线程休眠1秒，确保线程1先启动
        Thread.sleep(1000);

        // 线程2：修改flag为true
        new Thread(() -> {
            demo.flag = true;
            System.out.println("线程2：已将flag设置为true");
        }).start();
    }
}
```
**运行效果**：加了`volatile`后，线程1会立即退出循环；去掉`volatile`，线程1会一直死循环。

### 3. 演示2：synchronized（卖票Demo，解决原子性）
新建 `SynchronizedDemo.java`
```java
package com.example.learning.juc;

/**
 * synchronized演示：卖票问题（解决多线程原子性）
 * 不加synchronized会出现超卖（票数为负数）
 */
public class SynchronizedDemo {
    // 总票数
    private int ticketNum = 100;

    // 卖票方法（加synchronized保证原子性）
    public synchronized void sellTicket() {
        if (ticketNum > 0) {
            // 模拟出票延迟
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName() + "卖出1张票，剩余：" + --ticketNum);
        }
    }

    public static void main(String[] args) {
        SynchronizedDemo demo = new SynchronizedDemo();

        // 3个窗口卖票
        new Thread(() -> {
            for (int i = 0; i < 40; i++) {
                demo.sellTicket();
            }
        }, "窗口1").start();

        new Thread(() -> {
            for (int i = 0; i < 40; i++) {
                demo.sellTicket();
            }
        }, "窗口2").start();

        new Thread(() -> {
            for (int i = 0; i < 40; i++) {
                demo.sellTicket();
            }
        }, "窗口3").start();
    }
}
```
**运行效果**：加了`synchronized`后，票数不会出现负数；去掉后会出现超卖（比如剩余-1）。

### 4. 演示3：ReentrantLock（可重入锁，手动控制锁）
新建 `ReentrantLockDemo.java`
```java
package com.example.learning.juc;

import java.util.concurrent.locks.ReentrantLock;

/**
 * ReentrantLock演示：手动加锁/解锁，比synchronized更灵活
 */
public class ReentrantLockDemo {
    private int count = 0;
    // 创建可重入锁（默认非公平锁）
    private final ReentrantLock lock = new ReentrantLock();

    // 累加方法
    public void increment() {
        // 手动加锁
        lock.lock();
        try {
            count++;
            System.out.println(Thread.currentThread().getName() + "：count=" + count);
        } finally {
            // 必须在finally里解锁，防止死锁
            lock.unlock();
        }
    }

    public static void main(String[] args) {
        ReentrantLockDemo demo = new ReentrantLockDemo();

        // 5个线程各累加10次
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    demo.increment();
                }
            }, "线程" + i).start();
        }
    }
}
```
**核心点**：`lock()`加锁、`unlock()`解锁，必须放`finally`里，避免异常导致锁无法释放。

### 5. 演示4：手写ThreadPoolExecutor（线程池核心）
新建 `ThreadPoolDemo.java`
```java
package com.example.learning.juc;

import java.util.concurrent.*;

/**
 * 手写线程池：重点掌握7大参数，面试必问
 */
public class ThreadPoolDemo {
    public static void main(String[] args) {
        // 核心：ThreadPoolExecutor的7个参数
        ExecutorService executor = new ThreadPoolExecutor(
                2, // 核心线程数（常驻线程）
                5, // 最大线程数（核心+临时）
                60L, // 临时线程空闲时间（超过就销毁）
                TimeUnit.SECONDS, // 时间单位
                new ArrayBlockingQueue<>(3), // 任务队列（核心线程满了放这里）
                Executors.defaultThreadFactory(), // 线程工厂（创建线程）
                // 拒绝策略（队列满+最大线程满，触发拒绝）
                new ThreadPoolExecutor.AbortPolicy() 
        );

        // 提交10个任务（核心2 + 队列3 + 临时3 = 8，剩下2个触发拒绝）
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + "执行任务：" + finalI);
            });
        }

        // 关闭线程池
        executor.shutdown();
    }
}
```
**运行效果**：前8个任务正常执行，后2个任务抛出`RejectedExecutionException`（拒绝策略生效）。

### 6. 演示5：ThreadLocal存用户ID（实战常用）
新建 `ThreadLocalDemo.java`
```java
package com.example.learning.juc;

/**
 * ThreadLocal演示：每个线程独立存储数据，解决线程安全问题
 * 实战中常用：存登录用户ID、请求ID等
 */
public class ThreadLocalDemo {
    // 创建ThreadLocal，存储用户ID
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    // 设置用户ID
    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    // 获取用户ID
    public static Long getUserId() {
        return USER_ID.get();
    }

    // 移除用户ID（必须手动移除，否则内存泄漏）
    public static void removeUserId() {
        USER_ID.remove();
    }

    public static void main(String[] args) {
        // 线程1：存用户ID=1001
        new Thread(() -> {
            setUserId(1001L);
            System.out.println("线程1的用户ID：" + getUserId());
            removeUserId(); // 用完必须移除
        }).start();

        // 线程2：存用户ID=1002
        new Thread(() -> {
            setUserId(1002L);
            System.out.println("线程2的用户ID：" + getUserId());
            removeUserId(); // 用完必须移除
        }).start();
    }
}
```
**运行效果**：线程1输出1001，线程2输出1002，互相不干扰。

---

## 三、把ThreadLocal集成到JWT登录（实战化）
修改第二天的`JwtInterceptor.java`，在拦截器里存用户ID：
```java
package com.example.learning.common;

import com.example.learning.juc.ThreadLocalDemo; // 新增导入
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class JwtInterceptor implements HandlerInterceptor {
    private final JwtUtil jwtUtil;

    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");

        if (token == null || token.isBlank()) {
            throw new RuntimeException("请先登录");
        }
        if (!jwtUtil.verifyToken(token)) {
            throw new RuntimeException("token无效或已过期");
        }

        // 新增：解析用户ID并存入ThreadLocal
        Long userId = jwtUtil.getUserIdFromToken(token);
        ThreadLocalDemo.setUserId(userId);

        return true;
    }

    // 新增：请求结束后移除ThreadLocal，防止内存泄漏
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ThreadLocalDemo.removeUserId();
    }
}
```
**测试**：在`UserController`的`page`方法里加一行：
```java
package com.example.learning.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.learning.common.Result;
import com.example.learning.entity.User;
import com.example.learning.juc.ThreadLocalDemo; // 新增导入
import com.example.learning.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/page")
    public Result<Page<User>> page(@RequestParam(defaultValue = "1") Integer current,
                                    @RequestParam(defaultValue = "10") Integer size) {
        // 新增：获取当前登录用户ID
        Long userId = ThreadLocalDemo.getUserId();
        System.out.println("当前登录用户ID：" + userId);

        Page<User> page = userService.page(new Page<>(current, size));
        return Result.success(page);
    }

    // 其他方法保持不变...
}
```
调用`/users/page`时，控制台会打印当前登录用户的ID，实战中这个用法非常高频。

---

## 四、第3天必须背的5道八股
1. **volatile的作用？**
   ① 保证可见性（一个线程修改，其他线程立即看到）；② 禁止指令重排序（比如单例模式的DCL）；③ 不保证原子性（比如i++不行）。

   **📝 面试标准回答：**
   > volatile 有三个作用：
   > ① 保证可见性：当一个线程修改了 volatile 变量，会立即刷新到主内存，其他线程读取时从主内存获取最新值，而不是使用缓存。
   > ② 禁止指令重排序：通过内存屏障禁止特定类型的指令重排序，典型应用是单例模式的 DCL，防止返回未初始化的对象。
   > ③ 不保证原子性：比如 i++ 操作，即使加了 volatile，多线程并发时仍可能出现数据丢失，需要配合 synchronized 或 AtomicInteger 使用。
   

2. **synchronized的锁升级过程？**
   无锁 → 偏向锁（第一次获取） → 轻量级锁（多线程竞争） → 重量级锁（竞争激烈）；目的是提高性能。

   **📝 面试标准回答：**
   > synchronized 有4种锁状态，会根据竞争情况自动升级，目的是提高性能：
   > ① 无锁 → 偏向锁：当一个线程首次获取锁时，对象头记录该线程ID，后续该线程再次访问无需加锁，偏向锁开销最小。
   > ② 偏向锁 → 轻量级锁：当第二个线程尝试获取锁时，升级为轻量级锁，线程通过 CAS 自旋尝试获取锁，而不是立即阻塞，减少上下文切换。
   > ③ 轻量级锁 → 重量级锁：当自旋超过一定次数或竞争激烈时，升级为重量级锁，线程阻塞，依赖操作系统的互斥量实现。
   > **注意**：锁升级是单向的，不能降级！
   


3. **ReentrantLock和synchronized的区别？**
   ① ReentrantLock手动加锁/解锁，synchronized自动；② ReentrantLock支持公平锁/非公平锁，synchronized只有非公平；③ ReentrantLock支持条件变量、可中断，synchronized不行。

   **📝 面试标准回答：**
   > ReentrantLock 和 synchronized 有5个核心区别：
   > ① 锁释放方式：ReentrantLock 需要手动在 finally 中调用 unlock() 释放锁，synchronized 由 JVM 自动释放。
   > ② 锁类型：ReentrantLock 支持公平锁和非公平锁（构造函数可选），synchronized 只能是非公平锁。
   > ③ 条件变量：ReentrantLock 支持多个 Condition，可以精确唤醒特定等待线程（如生产者-消费者），synchronized 只能用 wait/notify，随机唤醒或全部唤醒。
   > ④ 可中断性：ReentrantLock 支持 lockInterruptibly() 可中断获取锁，synchronized 不可中断，只能死等。
   > ⑤ 实现原理：synchronized 是 JVM 内置的关键字，基于对象头实现；ReentrantLock 是 JDK 提供的类，基于 AQS（AbstractQueuedSynchronizer）实现。
   >
   > **使用建议**：优先使用 synchronized（代码简洁），需要公平锁/可中断/多条件时用 ReentrantLock。
   

4. **线程池的7大参数？**
   核心线程数、最大线程数、临时线程空闲时间、时间单位、任务队列、线程工厂、拒绝策略（面试必背，一个都不能漏）。

   **📝 面试标准回答：**
   > 线程池有7个核心参数，按顺序是：
   > ① **corePoolSize（核心线程数）**：常驻线程数，即使空闲也不会销毁。类比：公司正式员工。
   > ② **maximumPoolSize（最大线程数）**：线程池允许的最大线程数，等于核心线程数加临时线程数。类比：正式员工+临时工总数。
   > ③ **keepAliveTime（临时线程空闲时间）**：临时线程空闲多长时间后销毁。类比：临时工没活干多久被辞退。
   > ④ **unit（时间单位）**：keepAliveTime 的时间单位，如 TimeUnit.SECONDS。
   > ⑤ **workQueue（任务队列）**：核心线程忙碌时，任务存入的队列。常用 ArrayBlockingQueue（有界）和 LinkedBlockingQueue（无界）。类比：任务等候区。
   > ⑥ **threadFactory（线程工厂）**：创建线程的工厂，用于给线程命名，方便日志排查问题。
   > ⑦ **handler（拒绝策略）**：队列满且最大线程满时的处理策略。有4种：AbortPolicy（抛异常，默认）、CallerRunsPolicy（调用者自己执行）、DiscardPolicy（静默丢弃）、DiscardOldestPolicy（丢弃最老任务）。
   

5. **ThreadLocal为什么会内存泄漏？怎么解决？**
   原因：ThreadLocalMap的key是弱引用，GC时key被回收，value还在，导致内存泄漏；解决：用完后手动调用`remove()`。

   **📝 面试标准回答：**
   > **内存泄漏的原因**：
   > ThreadLocal 的内部结构是 Thread 持有 ThreadLocalMap，Map 中的 Entry 继承 WeakReference，key 是弱引用指向 ThreadLocal 对象，value 是强引用。
   >
   > 当外部 ThreadLocal 引用被置空时，key 作为弱引用会被 GC 回收变成 null。但 value 仍然是强引用，且 Thread → ThreadLocalMap → Entry → value 这个引用链一直存在，导致 value 无法被 GC 回收，造成内存泄漏。
   >
   > 在 Web 应用中更危险，因为线程通常是线程池中的线程，会一直存活，如果不手动清理，会累积大量泄漏对象，最终导致 OOM。
   >
   > **解决方案**：
   > 必须在使用完 ThreadLocal 后手动调用 `remove()` 方法清理。最佳实践是在 finally 块中清理，或者在 Web 应用的拦截器中请求结束时统一清理。



---

## 五、过关标准
1. 5个JUC代码都能运行，看懂输出结果；
2. ThreadLocal集成到JWT登录后，能正确打印用户ID；
3. 5道八股题能一字不差背下来；

