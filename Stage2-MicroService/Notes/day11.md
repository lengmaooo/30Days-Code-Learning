# 第二阶段第1天：分布式基础 + Nacos + OpenFeign 实战（超细简化版）
今天核心是搭建「微服务最小闭环」—— 2个服务（用户服务+订单服务）+ Nacos注册中心 + OpenFeign远程调用，全程代码可直接复制，新手0踩坑，先跑通“服务注册→远程调用”的核心流程。

## 一、前置准备（5分钟搞定）
### 1. 环境要求
- JDK 8+（必须8/11，避免版本兼容问题）
- Maven 3.6+
- 开发工具：IDEA（推荐）
- 中间件：Nacos 2.3.2（单机版）

### 2. 安装启动Nacos（关键第一步）
#### 步骤1：下载Nacos
官网下载地址：https://github.com/alibaba/nacos/releases/download/2.3.2/nacos-server-2.3.2.zip
（或直接用网盘：链接：https://pan.baidu.com/s/1ZbFZ67p487G8G9t0Q787hQ 提取码：nacos）

#### 步骤2：解压并启动
- 解压到任意目录（比如 `D:\nacos-server-2.3.2`，路径不要有中文/空格）
- 进入 `bin` 目录，执行启动命令（单机模式，避免集群复杂度）：
  ```bash
  # Windows系统（双击执行或cmd运行）
  startup.cmd -m standalone
  # Linux/Mac系统
  sh startup.sh -m standalone
  ```

#### 步骤3：验证启动成功
访问Nacos控制台：http://localhost:8848/nacos
默认账号/密码：nacos/nacos → 能登录即启动成功。

## 二、第一步：搭建用户服务（user-service，服务提供者）
### 1. 新建Maven工程（IDEA操作）
- 打开IDEA → 新建Project → Maven → 勾选「Create from archetype」→ 选 `maven-archetype-quickstart`
- 配置信息：
    - GroupId：com.example
    - ArtifactId：user-service
    - Version：1.0-SNAPSHOT
- 点击Finish，等待工程创建完成。

### 2. 配置pom.xml（核心依赖，直接复制）
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 父工程：Spring Boot 2.7.10（和Spring Cloud Alibaba适配） -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.10</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>user-service</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
        <!-- Spring Cloud Alibaba版本（和Spring Boot 2.7.10适配） -->
        <spring-cloud-alibaba.version>2021.0.5.0</spring-cloud-alibaba.version>
    </properties>

    <!-- 依赖版本管理：避免版本冲突 -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Spring Boot Web核心依赖（必须） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Nacos注册中心依赖（服务注册/发现） -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>

        <!-- Lombok（简化实体类代码，可选但推荐） -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试依赖（可选） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <!-- 打包插件（Spring Boot工程必须） -->
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 3. 编写配置文件（application.yml）
在 `src/main/resources` 下新建 `application.yml`（删除默认的application.properties），内容：
```yaml
# 服务端口（避免冲突，选8081）
server:
  port: 8081

# 服务名称（Nacos注册中心识别的服务名，必须唯一）
spring:
  application:
    name: user-service
  # Nacos注册中心配置
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848 # Nacos地址（默认8848）
        username: nacos # 用户名（默认nacos）
        password: nacos # 密码（默认nacos）

# 日志配置（可选，方便看调用日志）
logging:
  level:
    org.springframework.web: INFO
    com.example: DEBUG
```

### 4. 编写核心代码
#### 步骤1：实体类（User）
在 `src/main/java/com/example` 下新建包 `entity`，新建 `User.java`：
```java
package com.example.entity;

import lombok.Data;

/**
 * 用户实体类（和数据库字段对应，这里简化，无数据库也能测试）
 */
@Data // Lombok注解：自动生成get/set/toString等方法
public class User {
    private Long id; // 用户ID
    private String username; // 用户名
    private Integer age; // 年龄
    private String phone; // 手机号
}
```

#### 步骤2：Controller（提供HTTP接口）
在 `src/main/java/com/example` 下新建包 `controller`，新建 `UserController.java`：
```java
package com.example.controller;

import com.example.entity.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户服务接口（提供者）
 */
@RestController
@RequestMapping("/user")
public class UserController {

    /**
     * 根据ID查询用户（核心接口，供订单服务调用）
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        // 模拟数据库查询（不用真实数据库，先跑通调用）
        User user = new User();
        user.setId(id);
        user.setUsername("测试用户" + id);
        user.setAge(20 + id.intValue());
        user.setPhone("1380013800" + id);
        return user;
    }
}
```

#### 步骤3：启动类（核心，必须）
在 `src/main/java/com/example` 下新建 `UserServiceApplication.java`：
```java
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 用户服务启动类
 * @EnableDiscoveryClient：开启Nacos服务注册/发现（必须加）
 */
@EnableDiscoveryClient // 核心注解：让服务注册到Nacos
@SpringBootApplication
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
        System.out.println("=====用户服务启动成功=====");
    }
}
```

### 5. 启动并验证用户服务
#### 步骤1：启动UserServiceApplication
- 右键点击启动类 → Run 'UserServiceApplication'
- 控制台打印 `=====用户服务启动成功=====` 且无报错 → 启动成功。

#### 步骤2：验证服务注册到Nacos
- 刷新Nacos控制台 → 服务管理 → 服务列表 → 能看到 `user-service` → 注册成功。

#### 步骤3：验证接口可用
访问：http://localhost:8081/user/1
返回如下JSON → 接口正常：
```json
{
  "id": 1,
  "username": "测试用户1",
  "age": 21,
  "phone": "13800138001"
}
```

## 三、第二步：搭建订单服务（order-service，服务消费者）
### 1. 新建Maven工程（和user-service步骤一致）
- ArtifactId：order-service
- 其他配置和user-service完全一致。

### 2. 配置pom.xml（比user-service多OpenFeign依赖）
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.10</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>order-service</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
        <spring-cloud-alibaba.version>2021.0.5.0</spring-cloud-alibaba.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Spring Boot Web核心依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Nacos注册中心依赖 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>

        <!-- OpenFeign核心依赖（远程调用） -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 3. 编写配置文件（application.yml）
```yaml
# 服务端口（和user-service的8081区分，选8082）
server:
  port: 8082

# 服务名称
spring:
  application:
    name: order-service
  # Nacos注册中心配置（和user-service一致）
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        username: nacos
        password: nacos

# 日志配置
logging:
  level:
    org.springframework.web: INFO
    com.example: DEBUG
```

### 4. 编写核心代码
#### 步骤1：复制实体类（User）
把user-service中的 `com.example.entity.User` 完整复制到order-service的同包路径下（包名、类名、字段必须完全一致，否则序列化失败）。

#### 步骤2：编写OpenFeign客户端（核心，远程调用用户服务）
在 `src/main/java/com/example` 下新建包 `feign`，新建 `UserFeignClient.java`：
```java
package com.example.feign;

import com.example.entity.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * OpenFeign客户端：调用user-service的接口
 * @FeignClient(name = "user-service")：指定要调用的服务名（和Nacos中的服务名一致）
 */
@FeignClient(name = "user-service") // 核心注解：绑定用户服务
public interface UserFeignClient {

    /**
     * 调用user-service的/user/{id}接口
     * 注解和参数必须和提供者的接口完全一致
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/user/{id}")
    User getUserById(@PathVariable("id") Long id);
}
```

#### 步骤3：编写Controller（订单服务接口，调用用户服务）
在 `src/main/java/com/example` 下新建包 `controller`，新建 `OrderController.java`：
```java
package com.example.controller;

import com.example.entity.User;
import com.example.feign.UserFeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 订单服务接口（消费者）
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    /**
     * 注入OpenFeign客户端（像调用本地接口一样调用远程服务）
     */
    @Resource
    private UserFeignClient userFeignClient;

    /**
     * 创建订单（模拟）：调用用户服务查询用户信息
     * @param userId 用户ID
     * @return 订单信息（包含用户信息）
     */
    @GetMapping("/create/{userId}")
    public String createOrder(@PathVariable Long userId) {
        // 核心：通过OpenFeign调用user-service的接口
        User user = userFeignClient.getUserById(userId);
        
        // 模拟创建订单
        return "创建订单成功！\n订单关联的用户信息：" + user.toString();
    }
}
```

#### 步骤4：启动类（必须加@EnableFeignClients）
在 `src/main/java/com/example` 下新建 `OrderServiceApplication.java`：
```java
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 订单服务启动类
 * @EnableFeignClients：开启OpenFeign（必须加，否则无法扫描Feign客户端）
 */
@EnableFeignClients // 核心注解：开启OpenFeign
@EnableDiscoveryClient
@SpringBootApplication
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
        System.out.println("=====订单服务启动成功=====");
    }
}
```

### 5. 启动并验证完整调用链路
#### 步骤1：启动OrderServiceApplication
- 右键启动 → 控制台打印 `=====订单服务启动成功=====` 且无报错。
- 刷新Nacos控制台 → 服务列表能看到 `order-service` → 注册成功。

#### 步骤2：验证远程调用（核心！）
访问订单服务接口：http://localhost:8082/order/create/1
返回如下内容 → 调用成功：
```
创建订单成功！
订单关联的用户信息：User(id=1, username=测试用户1, age=21, phone=13800138001)
```

## 四、新手常见问题排查（必看）
### 1. 服务注册失败（Nacos看不到服务）
- 原因1：Nacos未启动 → 先启动Nacos，确保http://localhost:8848/nacos能访问；
- 原因2：启动类没加 `@EnableDiscoveryClient` → 补上注解；
- 原因3：Nacos地址配置错误 → 检查 `spring.cloud.nacos.discovery.server-addr` 是否为 `localhost:8848`；
- 原因4：依赖缺失 → 检查是否引入 `spring-cloud-starter-alibaba-nacos-discovery`。

### 2. OpenFeign调用失败（报错“No available instance for user-service”）
- 原因1：user-service未启动 → 先启动用户服务；
- 原因2：FeignClient的name和Nacos服务名不一致 → 确保 `@FeignClient(name = "user-service")` 和用户服务的 `spring.application.name` 一致；
- 原因3：接口路径/参数不一致 → 确保Feign客户端的注解、参数和提供者完全一致（比如 `@PathVariable("id")` 不能少括号里的id）。

### 3. 实体类序列化失败（返回null或字段缺失）
- 原因：消费者和提供者的实体类不一致（包名、类名、字段名、Lombok注解）→ 完全复制提供者的实体类。

## 五、今天的核心收获（面试能说的知识点）
### 1. 核心概念
- 微服务：把大项目拆成独立的小服务（user-service/order-service），各自部署、通过网络调用；
- Nacos注册中心：服务启动后自动注册，消费者通过服务名找到提供者，不用写死IP；
- OpenFeign：声明式HTTP客户端，用SpringMVC注解就能实现远程调用，不用手动写HTTP请求。

### 2. 核心流程
```
1. user-service启动 → 注册到Nacos；
2. order-service启动 → 注册到Nacos；
3. 访问order-service的/create/1接口 → order-service通过OpenFeign（服务名user-service）从Nacos获取user-service的地址；
4. order-service调用user-service的/user/1接口 → 返回用户信息 → 拼接订单信息返回。
```

### 总结
1. 今天完成了微服务最核心的「服务注册+远程调用」闭环，跑通了2个服务的跨服务调用；
2. 核心注解要记牢：`@EnableDiscoveryClient`（注册到Nacos）、`@FeignClient`（OpenFeign客户端）、`@EnableFeignClients`（开启OpenFeign）；
3. 新手踩坑点：服务名一致、实体类一致、接口路径/参数一致，这三点是调用成功的关键。
