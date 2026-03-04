package com.example.controller;

import com.example.entity.User;
import com.example.util.JwtUtil;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 登录接口（网关放行，不校验 token）
     */
    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password) {
        // 模拟校验账号密码
        if ("admin".equals(username) && "123456".equals(password)) {
            // 生成 token 返回
            return JwtUtil.createToken("1");
        }
        return "登录失败";
    }
}
