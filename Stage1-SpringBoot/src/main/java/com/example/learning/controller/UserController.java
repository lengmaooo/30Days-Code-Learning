package com.example.learning.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.learning.common.Result;
import com.example.learning.entity.User;
import com.example.learning.service.UserService;
import juc.ThreadLocalDemo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    // 分页查询
    @GetMapping("/page")
    public Result<Page<User>> page(@RequestParam(defaultValue = "1") Integer current,
                                    @RequestParam(defaultValue = "10") Integer size) {
        // 新增：获取当前登录用户ID
        Long userId = ThreadLocalDemo.getUserId();
        System.out.println("当前登录用户ID：" + userId);
        System.out.println("线程：" + Thread.currentThread().getName() + " 取出用户ID：" + userId);

        Page<User> page = userService.page(current, size);
        return Result.success(page);
    }

    // 按 ID 查询
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        User user = userService.getById(id);
        return Result.success(user);
    }

    // 新增
    @PostMapping
    public Result<Boolean> save(@RequestBody User user) {
        boolean success = userService.save(user);
        return Result.success(success);
    }

    // 修改
    @PutMapping
    public Result<Boolean> updateById(@RequestBody User user) {
        boolean success = userService.updateById(user);
        return Result.success(success);
    }

    // 删除
    @DeleteMapping("/{id}")
    public Result<Boolean> removeById(@PathVariable Long id) {
        boolean success = userService.removeById(id);
        return Result.success(success);
    }
}