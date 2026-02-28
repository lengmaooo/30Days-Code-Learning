package com.example.learning.controller;

import com.example.learning.common.Result;
import com.example.learning.entity.User;
import com.example.learning.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 缓存测试接口 - 用于演示缓存穿透、击穿、雪崩
 */
@RestController
@RequestMapping("/cache-test")
public class CacheTestController {

    @Resource
    private UserService userService;

    /**
     * 测试缓存穿透（无防护）
     * 访问：http://localhost:8080/cache-test/penetration?id=-1
     *
     * 预期结果：查询不存在的用户ID（-1）时，每次都会查DB，导致缓存穿透
     */
    @GetMapping("/penetration")
    public Result<User> testPenetration(@RequestParam Long id) {
        System.out.println("========================================");
        System.out.println("【测试缓存穿透 - 无防护版本】");
        User user = userService.getUserWithCache(id);
        System.out.println("========================================");
        return Result.success(user);
    }

    /**
     * 测试缓存穿透（空值缓存防护）
     * 访问：http://localhost:8080/cache-test/penetration-protected?id=-1
     *
     * 预期结果：第一次查询不存在的用户ID会查DB并写入空值缓存，后续请求直接走缓存
     */
    @GetMapping("/penetration-protected")
    public Result<User> testPenetrationProtected(@RequestParam Long id) {
        System.out.println("========================================");
        System.out.println("【测试缓存穿透 - 空值缓存防护版本】");
        User user = userService.getUserWithCacheAndProtection(id);
        System.out.println("========================================");
        return Result.success(user);
    }

    /**
     * 清空Redis缓存（用于测试）
     * 访问：http://localhost:8080/cache-test/clear-cache?key=user:-1
     */
    @GetMapping("/clear-cache")
    public Result<String> clearCache(@RequestParam String key) {
        userService.clearCacheByKey(key);
        return Result.success("缓存已清空: " + key);
    }
}
