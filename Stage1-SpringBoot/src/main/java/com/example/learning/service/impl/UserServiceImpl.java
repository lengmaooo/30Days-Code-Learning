package com.example.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.learning.common.JwtUtil;
import com.example.learning.common.RedisUtil;
import com.example.learning.entity.User;
import com.example.learning.mapper.UserMapper;
import com.example.learning.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Resource
    private RedisUtil redisUtil;

    // 注入自定义线程池
    @Resource(name = "userThreadPool")
    private ExecutorService userThreadPool;

    @Override
    public Page<User> page(Integer current, Integer size) {
        Page<User> page = new Page<>(current, size);
        return userMapper.selectPage(page, null);
    }

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public boolean save(User user) {
        return userMapper.insert(user) > 0;
    }

    @Override
    public boolean updateById(User user) {
        return userMapper.updateById(user) > 0;
    }

    @Override
    public boolean removeById(Long id) {
        return userMapper.deleteById(id) > 0;
    }

    @Override
    public User getOne(QueryWrapper<User> queryWrapper) {
        return userMapper.selectOne(queryWrapper);
    }

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return token
     */
    @Override
    public String login(String username, String password) {
        // 1. 根据用户名查询用户
        User user = userMapper.selectByUsername(username);

        // 2. 用户不存在
        if (user == null) {
            throw new RuntimeException("用户名不存在");
        }

        // 3. 验证密码
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 异步记录登录日志（不阻塞登录接口响应）
        userThreadPool.submit(() -> {
            try {
                // 模拟日志记录耗时操作
                Thread.sleep(500);
                System.out.println("【登录日志】用户：" + username + "，ID：" + user.getId() + "，登录时间：" + System.currentTimeMillis());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // 4. 生成 token 并返回
        return jwtUtil.generateToken(user.getId());
    }

    /**
     * 缓存穿透测试 - 无防护版本
     * 问题：查询不存在的用户ID时，每次都会查DB，导致缓存穿透
     */
    @Override
    public User getUserWithCache(Long id) {
        String key = "user:" + id;

        // 1. 查缓存
        User user = (User) redisUtil.get(key);
        if (user != null) {
            System.out.println("【缓存命中】用户ID: " + id);
            return user;
        }

        // 2. 缓存没有，查DB
        System.out.println("【缓存未命中，查询DB】用户ID: " + id);
        user = userMapper.selectById(id);

        // 3. DB没有，直接返回（穿透！）
        if (user == null) {
            System.out.println("【DB查询为空，缓存穿透】用户ID: " + id);
            return null;
        }

        // 4. DB有，写入缓存（过期时间5分钟）
        System.out.println("【DB查询成功，写入缓存】用户ID: " + id);
        redisUtil.set(key, user, 5, TimeUnit.MINUTES);
        return user;
    }

    /**
     * 缓存穿透测试 - 空值缓存防护版本
     * 解决：DB查询为空时，写入空值缓存，避免每次都查DB
     */
    @Override
    public User getUserWithCacheAndProtection(Long id) {
        String key = "user:" + id;

        // 1. 查缓存（包括空值）
        Object cached = redisUtil.get(key);
        if (cached != null) {
            // 判断是否是空值
            if (cached instanceof String && ((String) cached).isEmpty()) {
                System.out.println("【空值缓存命中】用户ID: " + id);
                return null;
            }
            System.out.println("【缓存命中】用户ID: " + id);
            return (User) cached;
        }

        // 2. 缓存没有，查DB
        System.out.println("【缓存未命中，查询DB】用户ID: " + id);
        User user = userMapper.selectById(id);

        // 3. DB没有，写入空值缓存（过期时间1分钟，避免缓存膨胀）
        if (user == null) {
            System.out.println("【DB查询为空，写入空值缓存】用户ID: " + id);
            redisUtil.set(key, "", 1, TimeUnit.MINUTES);
            return null;
        }

        // 4. DB有，写入缓存（过期时间5分钟）
        System.out.println("【DB查询成功，写入缓存】用户ID: " + id);
        redisUtil.set(key, user, 5, TimeUnit.MINUTES);
        return user;
    }

    /**
     * 清空指定缓存（用于测试）
     */
    @Override
    public void clearCacheByKey(String key) {
        redisUtil.delete(key);
        System.out.println("【缓存已清空】key: " + key);
    }
}