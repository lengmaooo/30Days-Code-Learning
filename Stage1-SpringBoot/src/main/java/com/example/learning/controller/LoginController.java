package com.example.learning.controller;

import com.example.learning.common.LoginRequest;
import com.example.learning.common.Result;
import com.example.learning.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 登录控制器
 */
@RestController
@RequiredArgsConstructor
public class LoginController {

    private final UserService userService;

    /**
     * 用户登录
     * @param loginRequest 登录请求参数（username、password）
     * @return token
     */
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginRequest loginRequest) {
        // 调用 Service 层登录方法
        String token = userService.login(loginRequest.getUsername(), loginRequest.getPassword());

        // 返回 token
        return Result.success(token);
    }
}
