package com.example.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.learning.common.JwtUtil;
import com.example.learning.entity.User;
import com.example.learning.mapper.UserMapper;
import com.example.learning.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

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

        // 4. 生成 token 并返回
        return jwtUtil.generateToken(user.getId());
    }
}