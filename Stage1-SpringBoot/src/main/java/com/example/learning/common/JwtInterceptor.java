package com.example.learning.common;

import juc.ThreadLocalDemo;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 拦截器
 * 验证请求是否携带有效的 token
 */
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * 在 Controller 执行前拦截
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从请求头获取 token
        String token = request.getHeader("Authorization");

        // 检查 token 是否为空
        if (token == null || token.isBlank()) {
            throw new RuntimeException("请先登录");
        }

        // 验证 token 是否有效
        if (!jwtUtil.verifyToken(token)) {
            throw new RuntimeException("token无效或已过期");
        }

        Long userId = jwtUtil.getUserIdFromToken(token);
        ThreadLocalDemo.setUserId(userId);
        System.out.println("线程：" + Thread.currentThread().getName() + " 存入用户ID：" + userId);

        // 验证通过，放行
        return true;
    }

    // 新增：请求结束后移除ThreadLocal，防止内存泄漏
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ThreadLocalDemo.removeUserId();
    }
}
