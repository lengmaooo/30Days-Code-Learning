# Stage 1：Spring Boot 基础（Day 01-10）

## 📚 学习目标
掌握 Java 后端面试核心考点，能独立写出规范的 Spring Boot 接口。

## 📅 学习内容

### Day 01：Spring Boot 项目规范
- 统一返回封装 Result<T>
- 全局异常处理
- 标准 CRUD 接口实现
- 常用注解、事务 @Transactional

### Day 02：Spring Boot 登录鉴权
- JWT 生成、校验
- 登录接口开发
- 拦截器验证 token

### Day 03：JUC 锁相关
- volatile 关键字
- synchronized 锁
- ReentrantLock 可重入锁

### Day 04：JUC 线程池 + 工具类
- ThreadPoolExecutor 7个参数
- ThreadLocal
- CountDownLatch

### Day 05：JVM 内存区域 + GC
- 堆、栈、方法区（元空间）
- Minor GC / Full GC 触发条件
- CMS、G1 垃圾收集器

### Day 06：JVM 参数 + OOM 思路
- Xms/Xmx 等核心参数
- 内存泄漏排查思路

### Day 07：MySQL 索引 + explain
- B+树、聚簇索引
- 最左前缀原则
- 解读 explain 执行计划

### Day 08：MySQL 事务 + 锁
- ACID、隔离级别
- 脏读、幻读、不可重复读
- 间隙锁、行锁

### Day 09：Redis 缓存三大问题
- 缓存穿透、击穿、雪崩
- 对应解决方案

### Day 10：Redis 分布式锁 + 复盘
- 分布式锁实现方式
- 复盘前10天所有考点

## 🚀 如何运行

```bash
cd Stage1-SpringBoot
mvn spring-boot:run
```

## 📝 学习笔记
详见 [Notes](./Notes/) 目录
