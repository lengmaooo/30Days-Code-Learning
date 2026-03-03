# 第二阶段第2天：OpenFeign 进阶（负载均衡 + 超时重试）
今天在第1天的**订单+用户服务Demo**基础上，只加配置、不加复杂代码，学会：
- 服务集群（多实例）
- OpenFeign 自带负载均衡
- 超时、重试配置（面试高频 + 实战必用）

---

## 一、今天目标
1. 启动 **2 个 user-service 实例**（集群）
2. 看到 OpenFeign **默认轮询负载均衡**
3. 学会配置 **超时时间**
4. 学会配置 **重试机制**
5. 知道这些配置在面试里怎么说

---

## 二、第一步：启动 user-service 多实例（集群）
### 1. IDEA 允许并行运行
- 打开 `UserServiceApplication`
- 右上角 → 编辑配置(Edit Configurations)
- 勾选 **Allow parallel run**

### 2. 启动第二个实例
直接再运行一次 `UserServiceApplication`
- 第一个：端口 8081
- 第二个：会自动用 8083/8084 之类

去 Nacos 服务列表 → user-service 实例数变成 **2**，就算成功。

---

## 三、第二步：OpenFeign 负载均衡（默认就有）
### 你不用写任何代码！
OpenFeign 底层集成 Ribbon / Spring Cloud LoadBalancer，**默认轮询（round-robin）**。

### 验证方法
反复访问：
http://localhost:8082/order/create/1

你在两个 user-service 控制台会**交替打印请求**，这就是负载均衡。

---

## 四、第三步：超时配置（最实用）
在 **order-service** 的 `application.yml` 加：
```yaml
# OpenFeign 超时配置
feign:
  client:
    config:
      # 全局默认配置
      default:
        # 连接超时
        connectTimeout: 3000
        # 读取超时（业务处理超时）
        readTimeout: 3000
```

意义：
- 调用超过 3 秒直接报错，不无限等待
- 保护服务不被拖死

---

## 五、第四步：重试配置（面试高频）
### 1. 加依赖（重试需要）
order-service pom.xml 加：
```xml
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
```

### 2. 开启重试
```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 3000
        readTimeout: 3000
        # 重试次数（失败后重试2次，总共最多调3次）
        maxAutoRetries: 2
        # 下一个实例重试（多实例集群才有用）
        maxAutoRetriesNextServer: 1
  # 开启 Feign 重试
  retryer: feign.Retryer.Default
```

### 3. 启动类加开启重试（可选但保险）
```java
@EnableRetry // 加这一句
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

---

## 六、今天必须会的面试题（精简答案）
1. **OpenFeign 自带负载均衡吗？**
   带，默认轮询，底层是 Spring Cloud LoadBalancer / Ribbon。

2. **超时配置作用？**
   防止调用方一直等待，避免线程堆积、服务雪崩。

3. **重试什么场景用？**
   网络抖动、瞬时故障、实例宕机，自动重试下一个实例，提高成功率。
   **注意：查询接口可以重试，增删改接口不要随便重试！**

---

## 七、第2天过关标准
- 能启动 2 个 user-service
- Nacos 能看到 2 个实例
- 多次调用订单接口，请求轮询到两个用户服务
- 超时 + 重试配置能看懂、会复制、会说作用

---
