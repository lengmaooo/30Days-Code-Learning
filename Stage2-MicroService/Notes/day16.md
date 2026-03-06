# Day16｜微服务 Demo 打通 + 熔断限流（Sentinel）
## 一、今天目标
1. 把前面所有服务**完整启动、跑通一条链路**
2. 学会**熔断、降级、限流**是什么（面试必问）
3. 简单集成 Sentinel，实现接口限流
4. 不用复杂环境，5 分钟就能会

---

# 二、微服务整套启动顺序（照着启动）
1. **Nacos**（注册中心）
2. **user-service**（用户服务）
3. **order-service**（订单服务）
4. **gateway-service**（网关）

## 整套访问流程（面试口述）
前端 → 网关（8080） → 订单服务 → OpenFeign → 用户服务

---

# 三、熔断、降级、限流（大白话+面试版）
## 1. 限流
- **意思**：接口一秒最多允许多少请求进来，超过直接拒绝
- **作用**：保护服务不被流量冲垮
- **例子**：订单接口每秒只放 100 个请求

## 2. 熔断
- **意思**：发现下游服务一直挂/一直报错，**直接不调用了，快速失败**
- **作用**：防止一个服务挂了拖垮整个调用链
- **例子**：用户服务挂了 → 订单服务熔断，不再调用用户服务

## 3. 降级
- **意思**：熔断后，给用户返回一个**默认结果/友好提示**
- **作用**：服务挂了也不报错，页面不乱

---

# 四、Sentinel 极简集成（order-service 演示）
## 1. pom.xml
```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

## 2. 接口上加限流注解
```java
@RestController
public class OrderController {

    @Autowired
    private UserFeignClient userFeignClient;

    @GetMapping("/order/create/{id}")
    @SentinelResource(
        value = "createOrder",
        fallbackClass = OrderFallback.class,
        fallback = "fallbackCreateOrder" // 降级方法
    )
    public String createOrder(@PathVariable String id) {
        String user = userFeignClient.getUserById(id);
        return "订单创建成功，" + user;
    }
}
```

## 3. 降级类（服务挂了就走这里）
```java
public class OrderFallback {
    public static String fallbackCreateOrder(String id) {
        return "订单服务繁忙，请稍后再试（降级返回）";
    }
}
```

## 4.  application.yml 简单配置
```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8080 # 控制台，可不开
```

---

# 五、面试必背 3 句话
1. **限流**：控制请求量，保护服务。
2. **熔断**：下游故障时停止调用，防止雪崩。
3. **降级**：熔断后返回友好默认值，提高体验。

---

# 六、今天任务
1. 把 Nacos、用户、订单、网关**全部启动成功**
2. 网关访问订单接口能通
3. 知道**熔断、降级、限流**区别，面试能说

---
