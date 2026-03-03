package com.example.controller;

import com.example.entity.User;
import com.example.feign.UserFeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final UserFeignClient userFeignClient;

    public OrderController(UserFeignClient userFeignClient) {
        this.userFeignClient = userFeignClient;
    }

    @GetMapping("/create/{userId}")
    public String createOrder(@PathVariable Long userId) {
        User user = userFeignClient.getUserById(userId);
        return "创建订单成功！\n订单关联的用户信息：" + user.toString();
    }
}
