# 第2天：JWT 登录完整版（超级细版）

## 🛠️ 代码实现步骤

## 1. 先加依赖（pom.xml）
```xml
<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>

<!-- 密码加密 BCrypt -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>
```

---

## 2. 执行 SQL（加密码字段）
```sql
ALTER TABLE user ADD COLUMN password VARCHAR(100) NOT NULL COMMENT '密码';

-- 测试账号：test / 123456
INSERT INTO user (username, age, password)
VALUES ('test', 20, '$2a$10$EixZaYb4xU58Gpq1R0yWbeb00LU5qUaK6x8h8y0xU58Gpq1R0yWbeb');
```

---

## 3. 修改 User 实体类
```java
@Data
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private Integer age;
    private String password; // 加这行
}
```

---

## 4. common 包新建 4 个类

### ① JwtUtil.java（工具类）
```java
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    private static final String SECRET = "my-secret-key-1234567890123456";
    private static final long EXPIRATION = 1000 * 60 * 60 * 2;

    private Key getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    public String generateToken(Long userId) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean verifyToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return Long.parseLong(claims.getSubject());
    }
}
```

### ② LoginRequest.java（登录参数）
```java
@Data
public class LoginRequest {
    private String username;
    private String password;
}
```

### ③ JwtInterceptor.java（拦截器）
```java
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
        return true;
    }
}
```

### ④ WebConfig.java（配置拦截器）
```java
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final JwtUtil jwtUtil;

    public WebConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtInterceptor(jwtUtil))
                .addPathPatterns("/user/**")
                .excludePathPatterns("/login");
    }
}
```

---

## 5. UserMapper 加方法
```java
public interface UserMapper extends BaseMapper<User> {
    User selectByUsername(@Param("username") String username);
}
```

### resources/mapper/UserMapper.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
"http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.mapper.UserMapper">
    <select id="selectByUsername" resultType="com.example.entity.User">
        SELECT id,username,password,age FROM user WHERE username = #{username}
    </select>
</mapper>
```

### application.yml 加上
```yaml
mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
```

---

## 6. UserService 加登录方法
```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import javax.annotation.Resource;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {
    @Resource
    private UserMapper userMapper;
    @Resource
    private JwtUtil jwtUtil;

    public String login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户名不存在");
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        return jwtUtil.generateToken(user.getId());
    }
}
```

---

## 7. 新建 LoginController
```java
@RestController
@RequiredArgsConstructor
public class LoginController {
    private final UserService userService;

    @PostMapping("/login")
    public Result login(@RequestBody LoginRequest loginRequest) {
        String token = userService.login(loginRequest.getUsername(), loginRequest.getPassword());
        return Result.success(token);
    }
}
```

---

# 测试流程（你直接照着测）
1. POST `/login`
   body:
   ```json
   {"username":"test","password":"123456"}
   ```
   得到 token

2. GET `/user/page`
   请求头加：
   ```
   Authorization: 你刚才的token
   ```
   能访问就是成功

3. 不带 token 访问 `/user/page` → 报错“请先登录”

---

## 📝 必背面试题

### 1️⃣ JWT 由什么组成？

**答案：**

JWT 由三部分组成，用 `.` 分隔：

**1. Header（头部）**
- 描述 token 的基本信息
- 包含：算法类型（如 HS256）、token 类型（JWT）
- Base64 编码，可解码查看

**2. Payload（载荷）**
- 存放实际数据
- 包含：用户 ID、签发时间、过期时间等
- Base64 编码，可解码查看
- 注意：不是加密，不要存敏感信息

**3. Signature（签名）**
- 防止 token 被篡改
- 使用密钥对 Header 和 Payload 进行签名
- 不可解码，用于验证

**完整格式：**
```
Header.Payload.Signature
```

**举例：**
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

**记忆技巧：**
> 头部（类型+算法）+ 载荷（用户数据）+ 签名（防篡改）

---

### 2️⃣ JWT 和 Session 区别？

**答案：**

**核心区别：**
- **Session**：有状态，数据存在服务器
- **JWT**：无状态，数据存在客户端

**详细对比：**

| 对比项 | Session | JWT |
|--------|---------|-----|
| **存储位置** | 服务器（内存/Redis） | 客户端（localStorage/Cookie） |
| **工作方式** | 服务端存 Session，返回 SessionID | 服务端生成 token，客户端存储 |
| **验证方式** | 根据 SessionID 查询 Session | 验证 token 签名 |
| **服务器压力** | 每个用户占内存 | 不占内存（无状态） |
| **扩展性** | 差（多服务器需要共享 Session） | 好（每台服务器都能验证） |

**Session 的流程：**
```
1. 用户登录 → 服务器创建 Session（内存）
2. 返回 SessionID → 存客户端 Cookie
3. 下次请求 → 带 SessionID → 服务器查询 Session
4. 验证通过 → 放行
```

**JWT 的流程：**
```
1. 用户登录 → 服务器生成 token
2. 返回 token → 存客户端 localStorage
3. 下次请求 → 带 token → 服务器验证签名
4. 验证通过 → 放行（不需要查数据库）
```

**适用场景：**
- **Session**：传统 Web、单机应用
- **JWT**：前后端分离、分布式系统、移动端 App

**一句话总结：**
> Session 占服务器内存但有状态，JWT 不占内存但无状态，适合分布式和前后端分离。

---

### 3️⃣ 为什么用 BCrypt 加密？

**答案：**

**核心原因：自动加盐，相同密码每次加密结果不同，防止彩虹表攻击。**

**BCrypt 的特点：**

1. **自动加盐**
   - 每次加密自动生成随机盐值
   - 相同密码（如 `123456`）每次加密结果都不同
   - 不需要手动存储和管理盐值

2. **防止彩虹表攻击**
   - MD5：`123456` 永远加密成 `e10adc3949ba59abbe56e057f20f883e`
   - BCrypt：`123456` 每次加密结果都不同
   - 攻击者无法通过预先计算的密码表破解

3. **安全验证**
   - `matches(明文密码, 加密密码)` 能正确验证
   - 自动从密文中提取盐值进行比对

**与其他加密对比：**

| 方式 | 相同密码结果 | 安全性 |
|------|------------|-------|
| 明文 | 相同 | ❌ 极低 |
| MD5/SHA-1 | 相同 | ⚠️ 低（可被彩虹表攻击） |
| BCrypt | 不同 | ✅ 高（自动加盐） |

**一句话总结：**
> BCrypt 自动加盐，相同密码每次加密结果不同，防止彩虹表攻击，比 MD5 更安全。

---

### 4️⃣ Spring 拦截器执行流程？

**答案：**

**执行流程：**
```
请求 → preHandle → Controller → postHandle → 视图渲染 → afterCompletion → 响应
```

**详细步骤：**

**1. preHandle（Controller 执行前）**
- 作用：拦截请求、验证权限、预处理
- 返回值：`true` = 放行，`false` = 拦截
- 常见用途：登录验证、权限检查、日志记录

**2. postHandle（Controller 执行后）**
- 作用：修改 ModelAndView
- 执行时机：Controller 方法执行完毕，视图渲染之前
- 常见用途：统一处理返回数据

**3. afterCompletion（请求完成后）**
- 作用：清理资源、记录日志
- 执行时机：视图渲染完成后
- 常见用途：释放资源、记录请求耗时

**对比表格：**

| 方法 | 执行时机 | 作用 | 返回值 |
|------|---------|------|-------|
| **preHandle** | Controller 之前 | 验证、拦截 | boolean |
| **postHandle** | Controller 之后 | 处理数据 | void |
| **afterCompletion** | 请求完成后 | 清理资源 | void |

**实际例子（今天的 JWT 验证）：**
- preHandle 验证 token
- 返回 true → 放行到 Controller
- 返回 false 或抛异常 → 拦截，返回错误

**一句话总结：**
> 拦截器在 Controller 前后执行，preHandle 最常用，返回 true 放行，false 拦截。

---

### 5️⃣ Token 过期怎么办？

**答案：**

**有两种解决方案：**

**方案1：前端重新登录（简单方式）**
- token 过期后，前端跳转到登录页
- 重新输入账号密码登录
- 获取新的 token
- **优点**：实现简单
- **缺点**：用户体验差，频繁登录

**方案2：使用 RefreshToken（推荐方式）**
- 登录时返回两个 token：
  - **AccessToken**：短期有效（2小时），用于访问 API
  - **RefreshToken**：长期有效（7天），用于刷新 accessToken
- accessToken 过期后，前端用 refreshToken 调用刷新接口
- 后端验证 refreshToken，返回新的 accessToken
- **优点**：用户体验好，不需要频繁登录
- **缺点**：实现稍复杂，需要存储 refreshToken

**RefreshToken 流程：**
```
登录 → 获取 accessToken + refreshToken
  ↓
accessToken 过期
  ↓
用 refreshToken 换取新的 accessToken
  ↓
继续访问，无需重新登录
```

**对比表格：**

| 方案 | 用户体验 | 实现难度 | 适用场景 |
|------|---------|---------|---------|
| **重新登录** | 差 | 简单 | 内部系统 |
| **RefreshToken** | 好 | 中等 | 移动端 App、对外系统 |

**一句话总结：**
> Token 过期有两种处理方式：简单方案是重新登录，推荐方案是用 RefreshToken 自动刷新，提升用户体验。

