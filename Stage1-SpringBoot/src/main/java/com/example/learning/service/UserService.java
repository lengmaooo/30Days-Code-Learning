package com.example.learning.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.learning.entity.User;

public interface UserService {
    Page<User> page(Integer current, Integer size);  // 分页查询
    User getById(Long id);  // 按ID查询
    boolean save(User user);  // 新增
    boolean updateById(User user);  // 修改
    boolean removeById(Long id);  // 删除
}