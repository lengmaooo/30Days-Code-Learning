
---

# Day13｜Gateway 网关（实战极简版）
## 一、今天目标
- 新建网关服务：`gateway-service`
- 配置路由，转发到 order-service、user-service
- 理解网关作用：**统一入口、路由、过滤、鉴权**
- 跑通：**前端 → 网关 → 微服务**

---

## 二、新建模块：gateway-service
### 1. pom.xml（只复制这一段）
```xml
<dependencies>
    <!-- 网关核心依赖 -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>

    <!-- Nacos 注册发现 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>
</dependencies>
```

### 2. application.yml
```yaml
server:
  port: 8080

spring:
  application:
    name: gateway-service

  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848

    gateway:
      routes:
        # 路由到订单服务
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/order/**

        # 路由到用户服务
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/user/**
```

### 3. 启动类
```java
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
```

---

## 三、测试网关（最关键）
启动：
1. Nacos
2. user-service
3. order-service
4. gateway-service

访问：
- 原来：`http://localhost:8082/order/create/1`
- 现在走网关：`http://localhost:8080/order/create/1`

能通 = 网关成功。

---

## 四、网关核心作用（面试必背）
1. **统一入口**：所有请求走网关，不用记每个服务端口
2. **路由转发**：根据路径自动转到对应微服务
3. **过滤、鉴权**：统一做登录、权限校验（明天 JWT 用）
4. **限流、跨域、日志**：统一处理，不用每个服务写

---

## 五、今天必须掌握
- 能搭建网关
- 能写路由配置
- 能讲清：**请求 → 网关 → 微服务**
- 知道网关是微服务的**大门**

---
