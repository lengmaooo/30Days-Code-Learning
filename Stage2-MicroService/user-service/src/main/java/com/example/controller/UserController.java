package com.example.controller;

import com.example.entity.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        System.out.println("getUserById！！！！！");
        User user = new User();
        user.setId(id);
        user.setUsername("测试用户" + id);
        user.setAge(20 + id.intValue());
        user.setPhone("1380013800" + id);
        return user;
    }
}
