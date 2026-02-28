package com.example.learning.controller;

import com.example.learning.common.Result;
import com.example.learning.redis.DistributedLockDemo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class SeckillController {
    @Resource
    private DistributedLockDemo distributedLockDemo;

    // 秒杀接口
    @GetMapping("/seckill")
    public Result seckill() {
        String result = distributedLockDemo.seckill();
        return Result.success(result);
    }

    // 测试重入锁
    @GetMapping("/testReentrantLock")
    public Result testReentrantLock() {
        distributedLockDemo.testReentrantLock();
        return Result.success("重入锁测试完成");
    }
}
