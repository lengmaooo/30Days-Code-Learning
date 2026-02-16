package com.example.learning;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt 密码生成工具
 * 运行这个类来生成加密后的密码
 */
public class BCryptTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 生成密码 123456 的加密字符串
        String password = "123456";
        String encodedPassword = encoder.encode(password);

        System.out.println("原始密码: " + password);
        System.out.println("加密后的密码: " + encodedPassword);

        // 验证密码是否正确
        boolean matches = encoder.matches(password, encodedPassword);
        System.out.println("密码验证结果: " + matches);
    }
}
