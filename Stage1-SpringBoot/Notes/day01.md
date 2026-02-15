# Day 01：Spring Boot 项目规范

## 🎯 学习目标
写出 "像正规公司后端" 的标准架子

## 📋 核心内容

### 1. 新建 Spring Boot 项目
**依赖清单：**
- spring-web
- lombok
- mysql-connector-java
- mybatis-plus

### 2. 写统一返回体

```java
@Data
public class Result<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> fail(int code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}
```

### 3. 写全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        return Result.fail(500, e.getMessage());
    }
}
```

### 4. 写一张表的标准 CRUD

**功能清单：**
- [ ] 分页查询
- [ ] 按 id 查
- [ ] 新增
- [ ] 修改
- [ ] 删除
- [ ] Postman 全跑通

**标准分层结构：**
```
com.example.learning
├── entity          # 实体类
├── mapper          # MyBatis-Plus Mapper
├── service         # 业务接口
│   └── impl        # 业务实现
└── controller      # 控制器
```

---

## ✅ 过关标准（必须做到）

- 项目能启动
- 5 个接口全部跑通
- 返回格式统一：`{"code": 200, "msg": "success", "data": ...}`
- 报错走全局异常，不乱码

**做到这 4 条，今天代码任务完成！**

---

## 📝 必背面试题（今天就背这 5 道）

### 1️⃣ SpringBoot 自动配置原理是什么？

**核心流程：**

```
启动 → @EnableAutoConfiguration → 扫描 spring.factories → 条件过滤 → 创建 Bean
```

**详细步骤：**

1. **开启自动配置**
   - `@SpringBootApplication` 包含 `@EnableAutoConfiguration`
   - 启动时触发自动配置机制

2. **加载配置类**
   - 扫描所有 jar 包的 `META-INF/spring.factories`
   - 读取里面注册的自动配置类（如 MybatisPlusAutoConfiguration）

3. **按需过滤（关键）**
   - 条件注解判断是否生效：
     - `@ConditionalOnClass`：classpath 存在指定类才配置
     - `@ConditionalOnMissingBean`：容器没有该 Bean 才创建（避免覆盖）
   - 例：有 MyBatis 依赖 → 配置数据库；没有 → 跳过

4. **注册 Bean**
   - 读取 `application.yml` 配置
   - 创建 Bean 并放入 Spring 容器

**面试一句话总结：**
> 通过 `@EnableAutoConfiguration` 启动，读取 `spring.factories` 加载配置类，根据条件注解按需装配 Bean。

**举例：**
> 引入 MyBatis-Plus 依赖 → SpringBoot 发现 → 自动配置数据源、SqlSessionFactory → 无需手动配置

---

### 2️⃣ @SpringBootApplication 包含哪三个注解？

**答案：**

```
@SpringBootApplication = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
```

**三个注解详解：**

| 注解 | 作用 | 说明 |
|------|------|------|
| **@SpringBootConfiguration** | 标识配置类 | 本质就是 `@Configuration`，启动类可以作为配置类定义 Bean |
| **@EnableAutoConfiguration** | 开启自动配置 | 核心开关，扫描 `spring.factories` 实现自动装配 |
| **@ComponentScan** | 组件扫描 | 默认扫描启动类所在包及子包，自动注册 `@Controller`、`@Service` 等 |

**关键点：**

1. **@SpringBootConfiguration**
   - 标注当前类是配置类
   - 可以在启动类中用 `@Bean` 定义 Bean

2. **@EnableAutoConfiguration**
   - 自动配置的开关（第1题的核心）
   - 扫描 jar 包的 `spring.factories` 按需配置

3. **@ComponentScan**
   - 默认扫描：启动类所在包及其子包
   - 如果 Bean 在包外，需要手动指定：`@ComponentScan(basePackages = "com.example")`

**记忆技巧：**
> 配置 + 自动配置 + 扫描 = 启动注解（三大功能合体）

**设计思想：**
- 职责分离，可单独使用
- 组合使用更方便

---

### 3️⃣ SpringBoot 启动流程简单说一下？

**启动入口：**
```java
public static void main(String[] args) {
    SpringApplication.run(LearningApplication.class, args);
}
```

**5 个核心步骤：**

1. **创建 SpringApplication**
   - 判断应用类型：检测 classpath 下是否存在 Servlet 类
     - 有 → Web 应用（启动 Tomcat）
     - 没有 → 普通应用
   - 加载初始化器

2. **准备环境（prepareEnvironment）**
   - 创建 Environment 对象
   - 加载配置：application.yml、环境变量、JVM 参数

3. **创建 ApplicationContext**
   - 创建 Spring 容器
   - 用于存放所有 Bean

4. **刷新上下文（refreshContext）⭐ 核心步骤**
   - 执行自动配置（扫描 spring.factories）
   - 扫描组件（@Component、@Service、@Controller），创建 Bean 并放入容器
   - 启动 Web 服务器（内嵌 Tomcat）

5. **调用 Runner**
   - 执行 CommandLineRunner 和 ApplicationRunner
   - 用于启动完成后的初始化操作

**总结：**
> 启动 = 准备环境 → 创建容器 → 自动配置 + 扫描组件 → 启动服务器 → 执行回调

**关键点：**
- **核心方法**：`SpringApplication.run()`
- **最重要步骤**：刷新上下文（自动配置 + 扫描组件 + 启动 Tomcat）

---

### 4️⃣ @RestController 和 @Controller 区别？

**核心区别：**
```
@RestController = @Controller + @ResponseBody
```

**@Controller：**
- 标注控制器类
- 返回 String → 解析为视图名（跳转页面）
- 返回 JSON → 需要加 `@ResponseBody`

**@RestController：**
- 组合注解（`@Controller` + `@ResponseBody`）
- 所有方法返回值自动转 JSON
- 专用于前后端分离项目

**使用场景对比：**

| 注解 | 适用场景 | 返回内容 |
|------|---------|---------|
| **@Controller** | 传统 MVC + 页面跳转 | 视图名（HTML/JSP） |
| **@RestController** | 前后端分离 + RESTful API | JSON 数据 |

**代码示例：**

```java
// 返回页面（传统 MVC）
@Controller
public class PageController {
    @GetMapping("/index")
    public String index() {
        return "index";  // 跳转到 index.html
    }

    @ResponseBody  // 需要 JSON 时加这个
    @GetMapping("/api/data")
    public Result getData() {
        return Result.success(data);
    }
}

// 返回 JSON（前后端分离）
@RestController
public class ApiController {
    @GetMapping("/user")
    public User getUser() {
        return new User();  // 自动转 JSON：{"username":"张三","age":25}
    }
}
```

**记忆技巧：**
> - 要跳转页面 → `@Controller`
> - 只返回数据 → `@RestController`

---

### 5️⃣ 统一返回值、全局异常的作用是什么？

**统一返回值的作用：**

1. **规范接口格式**
   - 所有接口返回结构一致：`{code, msg, data}`
   - 前端只需写一套解析逻辑

2. **明确请求状态**
   - `code = 200`：成功
   - `code = 500`：失败
   - 前端根据 code 判断请求结果

3. **便于扩展**
   - 统一处理分页、时间格式、数据加密等

4. **前端友好**
   - 不用为每个接口写不同的解析代码

**全局异常处理的作用：**

1. **避免代码冗余**
   - 不用每个方法都写 try-catch
   - 只在全局处理器写一次即可

2. **统一错误处理**
   - 所有异常自动捕获，返回统一格式
   - 不会把异常堆栈暴露给前端（安全性）

3. **代码更清晰**
   - 业务逻辑和异常处理分离
   - 专注于业务代码

4. **更规范**
   - 错误信息统一返回：`{code: 500, msg: "错误信息", data: null}`

**对比示例：**

```java
// ❌ 没有：每个方法都要 try-catch
@PostMapping("/user")
public Result save(@RequestBody User user) {
    try {
        userService.save(user);
        return Result.success(true);
    } catch (Exception e) {
        return Result.fail(500, e.getMessage());
    }
}

// ✅ 有全局异常处理：直接写业务逻辑
@PostMapping("/user")
public Result save(@RequestBody User user) {
    userService.save(user);
    return Result.success(true);
    // 异常会被 GlobalExceptionHandler 自动捕获
}
```

**总结：**
> 统一返回值让前端解析更简单，全局异常处理让后端代码更简洁

---