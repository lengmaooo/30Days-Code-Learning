# 第9天：Redis 缓存三大问题 实战（超级细版）
今天聚焦Redis面试最核心的“缓存三大问题”——穿透、击穿、雪崩，这是后端面试必问的高频考点，我会把每个问题的原理讲透、复现场景、给出落地解决方案，所有代码直接集成到你的项目，确保你既能说清原理，又能手写代码解决问题。

---

## 一、核心目标
1. 彻底理解缓存穿透/击穿/雪崩的本质和区别
2. 手写代码复现三大问题，并用最优方案解决
3. 掌握Redis缓存更新策略（更新DB+删缓存）
4. 背会5道Redis缓存核心八股题

---

## 二、先理清核心概念（先懂原理，再写代码）
### 1. 缓存三大问题对比（必须记死）
| 问题         | 本质                                  | 场景示例                          | 解决方案                          |
|--------------|---------------------------------------|-----------------------------------|-----------------------------------|
| 缓存穿透     | 请求不存在的key，缓存和DB都查不到     | 恶意攻击：查询id=-1的用户         | 空值缓存+布隆过滤器              |
| 缓存击穿     | 热点key过期，大量请求瞬间打到DB       | 秒杀商品key过期，10万请求查DB     | 互斥锁+热点key永不过期           |
| 缓存雪崩     | 大量key同时过期，DB扛不住并发        | 凌晨0点所有缓存key过期            | 过期时间加随机值+Redis集群+熔断  |

### 2. 缓存更新策略（实战必选）
| 策略         | 实现方式                          | 优点                  | 缺点                          |
|--------------|-----------------------------------|-----------------------|-------------------------------|
| Cache Aside  | 查：先缓存后DB；更：更DB+删缓存  | 实现简单              | 可能有短暂数据不一致          |
| Read/Write Through | 读写都走缓存，缓存更DB | 数据一致              | 实现复杂                      |
| Write Back   | 写缓存，异步刷DB                  | 性能极高              | 可能丢数据                    |
**实战首选**：Cache Aside（更新DB后删除缓存，而非更新缓存）。

---

## 三、代码/实操部分（直接落地到你的项目）
### 1. 第一步：集成Redis到SpringBoot项目
#### ① 加Redis依赖（pom.xml）
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- 连接池 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

#### ② 配置Redis（application.yml）
```yaml
spring:
  redis:
    host: localhost # 你的Redis地址
    port: 6379
    password: # 无密码则留空
    lettuce:
      pool:
        max-active: 8 # 最大连接数
        max-idle: 8   # 最大空闲连接
        min-idle: 0   # 最小空闲连接
    timeout: 1000ms # 超时时间
```

#### ③ 封装Redis工具类（com.example.common.RedisUtil.java）
```java
package com.example.common;

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
```

#### ④ 配置RedisTemplate序列化（避免乱码）
```java
package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        
        // key序列化
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        
        // value序列化
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
}
```

### 2. 第二步：复现并解决缓存穿透
#### ① 复现缓存穿透（无防护）
修改UserService，新增查询用户方法（无穿透防护）：
```java
@Service
public class UserService extends ServiceImpl<UserMapper, User> {
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private UserMapper userMapper;

    // 无防护的查询（缓存穿透）
    public User getUserById(Long id) {
        // 1. 查缓存
        String key = "user:" + id;
        User user = (User) redisUtil.get(key);
        if (user != null) {
            return user;
        }
        
        // 2. 缓存没有，查DB
        user = userMapper.selectById(id);
        
        // 3. DB没有，直接返回（穿透！）
        if (user == null) {
            return null;
        }
        
        // 4. DB有，写入缓存（过期时间5分钟）
        redisUtil.set(key, user, 5, TimeUnit.MINUTES);
        return user;
    }
}
```
**测试**：调用`getUserById(-1)`（不存在的ID），每次都会查DB，大量请求会打垮DB。

#### ② 解决缓存穿透（空值缓存+布隆过滤器）
##### 方案1：空值缓存（简单有效，首选）
```java
// 解决穿透：空值缓存
public User getUserById(Long id) {
    String key = "user:" + id;
    User user = (User) redisUtil.get(key);
    
    // 缓存命中（包括空值）
    if (user != null) {
        // 空值返回null
        return user instanceof User ? user : null;
    }
    
    // 查DB
    user = userMapper.selectById(id);
    
    // DB没有，写入空值缓存（过期时间1分钟，避免缓存膨胀）
    if (user == null) {
        redisUtil.set(key, "", 1, TimeUnit.MINUTES);
        return null;
    }
    
    // DB有，写入缓存
    redisUtil.set(key, user, 5, TimeUnit.MINUTES);
    return user;
}
```

##### 方案2：布隆过滤器（海量数据场景）
```java
// 初始化布隆过滤器（项目启动时加载所有用户ID）
@PostConstruct
public void initBloomFilter() {
    // 模拟加载所有用户ID（实际从DB查）
    List<Long> userIds = userMapper.selectList(Wrappers.emptyWrapper()).stream()
            .map(User::getId)
            .collect(Collectors.toList());
    
    // 布隆过滤器（预计数据量10万，误判率0.01）
    BloomFilter<Long> bloomFilter = BloomFilter.create(Funnels.longFunnel(), 100000, 0.01);
    userIds.forEach(bloomFilter::put);
    
    // 存到Redis（实际可使用Redisson的布隆过滤器）
    redisUtil.set("user:bloom:filter", bloomFilter);
}

// 布隆过滤器防护
public User getUserByIdBloom(Long id) {
    // 1. 布隆过滤器判断ID是否存在
    BloomFilter<Long> bloomFilter = (BloomFilter<Long>) redisUtil.get("user:bloom:filter");
    if (!bloomFilter.mightContain(id)) {
        return null; // 一定不存在，直接返回
    }
    
    // 2. 后续逻辑同空值缓存...
    return getUserById(id);
}
```
**测试**：调用`getUserById(-1)`，第一次查DB后写入空值缓存，后续请求直接走缓存，不会查DB。

### 3. 第三步：复现并解决缓存击穿
#### ① 复现缓存击穿（无防护）
```java
// 热点key（比如秒杀商品，模拟10万并发查询）
public User getHotUser() {
    String key = "user:hot:1"; // 假设user_1是热点key
    User user = (User) redisUtil.get(key);
    
    // 缓存过期，大量请求同时查DB（击穿！）
    if (user == null) {
        user = userMapper.selectById(1L);
        redisUtil.set(key, user, 10, TimeUnit.SECONDS); // 短过期时间，模拟过期
    }
    return user;
}
```
**测试**：用JMeter模拟1000并发调用该方法，当key过期时，1000个请求同时打向DB。

#### ② 解决缓存击穿（互斥锁）
```java
// 解决击穿：互斥锁
public User getHotUser() {
    String key = "user:hot:1";
    String lockKey = "lock:user:hot:1";
    User user = (User) redisUtil.get(key);
    
    // 缓存命中，直接返回
    if (user != null) {
        return user;
    }
    
    // 缓存未命中，加锁
    String lockValue = UUID.randomUUID().toString();
    try {
        // 加锁（5秒超时，避免死锁）
        boolean locked = redisUtil.lock(lockKey, lockValue, 5, TimeUnit.SECONDS);
        if (!locked) {
            // 未抢到锁，重试（或返回缓存旧值）
            Thread.sleep(50);
            return getHotUser();
        }
        
        // 抢到锁，查DB
        user = userMapper.selectById(1L);
        // 写入缓存（延长过期时间，比如30分钟）
        redisUtil.set(key, user, 30, TimeUnit.MINUTES);
        return user;
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    } finally {
        // 解锁
        redisUtil.unlock(lockKey, lockValue);
    }
}
```
**测试**：并发请求时，只有一个请求能抢到锁查DB，其他请求等待后从缓存获取，避免DB压力。

### 4. 第四步：复现并解决缓存雪崩
#### ① 复现缓存雪崩（无防护）
```java
// 所有key设置相同过期时间（凌晨0点），模拟雪崩
public void setAllUserCache() {
    List<User> users = userMapper.selectList(Wrappers.emptyWrapper());
    users.forEach(user -> {
        String key = "user:" + user.getId();
        // 所有key都在凌晨0点过期
        long expireAt = getMidnightTimestamp() - System.currentTimeMillis();
        redisUtil.set(key, user, expireAt, TimeUnit.MILLISECONDS);
    });
}

// 获取凌晨0点时间戳
private long getMidnightTimestamp() {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DAY_OF_MONTH, 1);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar.getTimeInMillis();
}
```
**问题**：凌晨0点所有key同时过期，大量请求瞬间打向DB。

#### ② 解决缓存雪崩（过期时间加随机值）
```java
// 解决雪崩：过期时间加随机值
public void setAllUserCache() {
    List<User> users = userMapper.selectList(Wrappers.emptyWrapper());
    users.forEach(user -> {
        String key = "user:" + user.getId();
        // 基础过期时间5分钟 + 随机0-300秒
        long baseExpire = 5 * 60;
        long randomExpire = new Random().nextInt(300);
        redisUtil.set(key, user, baseExpire + randomExpire, TimeUnit.SECONDS);
    });
}
```
**额外防护方案**：
1. **Redis集群**：主从+哨兵，避免Redis单点故障；
2. **服务熔断/限流**：用Sentinel限制对DB的请求数；
3. **多级缓存**：本地缓存（Caffeine）+ Redis缓存，减少Redis依赖。

### 5. 第五步：缓存更新实战（Cache Aside策略）
修改UserService的更新方法，实现“更新DB后删缓存”：
```java
// 缓存更新：更新DB后删除缓存（而非更新缓存）
@Transactional
public boolean updateUser(User user) {
    // 1. 更新DB
    boolean success = updateById(user);
    if (success) {
        // 2. 删除缓存（而非更新，避免并发问题）
        String key = "user:" + user.getId();
        redisUtil.delete(key);
    }
    return success;
}
```
**核心原因**：更新缓存会导致并发下数据不一致，删除缓存让下一次查询重新加载最新数据，是最简单可靠的方案。

---

## 四、第9天必须背的5道Redis缓存八股（精简答案）
1. **缓存穿透、击穿、雪崩的区别？**
   ① 穿透：请求不存在的key，缓存/DB都查不到；② 击穿：热点key过期，大量请求打DB；③ 雪崩：大量key同时过期/Redis宕机，DB扛不住并发。

2. **缓存穿透的解决方案？**
   ① 空值缓存（简单有效，设置短过期时间）；② 布隆过滤器（海量数据场景，过滤不存在的key）；③ 接口参数校验（拦截非法请求）。

3. **缓存击穿的解决方案？**
   ① 互斥锁（只有一个请求查DB，其他等待）；② 热点key永不过期（定时更新缓存）；③ 本地缓存（Caffeine）兜底。

4. **缓存雪崩的解决方案？**
   ① 过期时间加随机值（避免同时过期）；② Redis集群（主从+哨兵，避免单点故障）；③ 服务熔断/限流（保护DB）；④ 多级缓存（本地+Redis）。

5. **缓存更新为什么用“更DB+删缓存”而不是“更DB+更缓存”？**
   ① 更缓存会导致并发下数据不一致（比如两个请求同时更新，缓存值覆盖错误）；② 删缓存让下一次查询重新加载最新数据，一致性更高；③ 实现简单，无需处理缓存更新的并发问题。

---

## 五、过关标准
1. 能复现并解决缓存穿透/击穿/雪崩三大问题；
2. 能写出正确的缓存更新代码（更DB+删缓存）；
3. 能区分三大问题的本质和解决方案差异；
4. 5道八股题能准确复述（重点是三大问题的区别和解决方案）。
