package com.example.learning.common;

import lombok.Data;

/**
 * 登录请求参数
 */
@Data
public class LoginRequest {
    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;
}