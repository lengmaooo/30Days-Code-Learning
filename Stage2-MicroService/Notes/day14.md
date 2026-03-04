
---

# Day14｜微服务统一认证（Gateway + JWT）
## 一、今天目标
- 理解：微服务为什么要在**网关做统一登录鉴权**
- 学会：JWT 三部分、怎么生成、怎么校验
- 实现：**网关拦截 → 校验Token → 合法才转发**
- 掌握：面试能口述完整鉴权流程

---

# 二、核心思想（一句话）
**所有请求先进网关，网关统一校验 token，
合法就转发到微服务，不合法直接返回 401，
每个微服务不用再写登录判断。**

---

# 三、JWT 是什么（面试必背）
- JWT = JSON Web Token
- 作用：**前端登录后，后端给一个加密字符串，下次请求带上，证明身份**
- 结构三部分：
    1. Header（头：算法）
    2. Payload（载荷：用户id、用户名）
    3. Signature（签名：防篡改）

---

# 四、网关 + JWT 流程（面试必口述）
1. 用户登录 → 用户名密码正确
2. 后端生成 **JWT Token** 返回给前端
3. 前端以后每次请求在 **请求头** 带上：
   `Authorization: Bearer xxxxx.token.xxxxx`
4. **网关拦截所有请求**
5. 网关校验 token：
    - 合法 → 转发到微服务
    - 不合法/过期 → 直接返回 401，不进微服务
6. 微服务只需要处理业务，不用管登录

---

# 五、网关添加 JWT 校验（实战代码）
## 1. pom.xml 加依赖（gateway-service）
```xml
<!-- JWT 工具类 -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.9.1</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.9.1</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.9.1</version>
</dependency>
```

## 2. 写一个 JWT 工具类（复制即可）
`JwtUtil.java`
```java
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;

public class JwtUtil {
    // 密钥（自己改）
    private static final String SECRET = "abc123456xyz";
    // 过期时间 1天
    private static final long EXPIRE = 1000 * 60 * 60 * 24;

    // 生成 token
    public static String createToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .compact();
    }

    // 校验 token 是否正确
    public static boolean checkToken(String token) {
        try {
            Jwts.parser().setSigningKey(SECRET).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 从 token 取出用户信息
    public static String getUserId(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
}
```

## 3. 网关过滤器（核心：拦截所有请求）
写一个 `AuthFilter.java`
```java
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();

        // 1. 登录接口放行，不校验
        if (path.contains("/login")) {
            return chain.filter(exchange);
        }

        // 2. 获取请求头 token
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (token == null || !token.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 截取 Bearer 后面的真实 token
        String realToken = token.substring(7);

        // 3. 校验 token
        boolean ok = JwtUtil.checkToken(realToken);
        if (!ok) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 4. 合法，放行
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
```

## 4. 写一个登录接口（随便放某个服务）
```java
@PostMapping("/login")
public String login(String username, String password) {
    // 模拟校验账号密码
    if ("admin".equals(username) && "123456".equals(password)) {
        // 生成 token 返回
        return JwtUtil.createToken("1");
    }
    return "登录失败";
}
```

---

# 六、完整流程（面试必说）
1. 请求 `/login` → 网关放行
2. 登录成功 → 返回 token
3. 前端带 token 请求：
   `Header: Authorization: Bearer xxxx`
4. 网关过滤器拦截
5. 校验 token
    - 合法 → 转发到微服务
    - 不合法 → 返回 401

---

# 七、Day14 面试必背 3 句话
1. **微服务统一鉴权放在网关**，不用每个服务都写登录。
2. **JWT 是无状态**，不用存在服务器，适合分布式/微服务。
3. 网关做：**拦截、验token、放行/拒绝**，微服务只关心业务。

---

