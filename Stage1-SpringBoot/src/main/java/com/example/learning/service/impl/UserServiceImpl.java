package com.example.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.learning.common.JwtUtil;
import com.example.learning.entity.User;
import com.example.learning.mapper.UserMapper;
import com.example.learning.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

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
}