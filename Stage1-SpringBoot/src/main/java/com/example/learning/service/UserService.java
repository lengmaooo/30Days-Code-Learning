package com.example.learning.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.learning.entity.User;

public interface UserService {
    Page<User> page(Integer current, Integer size);  // 分页查询
    User getById(Long id);  // 按ID查询
    boolean save(User user);  // 新增
    boolean updateById(User user);  // 修改
    boolean removeById(Long id);  // 删除
    User getOne(QueryWrapper<User> queryWrapper);  // 条件查询单个对象

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return token 字符串
     */
    String login(String username, String password);
}