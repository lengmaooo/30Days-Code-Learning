package com.example.learning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * 自定义线程池配置：实战中禁止用Executors创建（避免OOM）
 * 核心：根据业务场景设置参数，自定义线程名+拒绝策略
 */
@Configuration
public class ThreadPoolConfig {

    @Bean("userThreadPool") // 命名线程池，方便区分
    public ExecutorService userThreadPool() {
        // 核心参数计算：CPU密集型=CPU核心数+1；IO密集型=CPU核心数*2
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
                corePoolSize, // 核心线程数（CPU核心数）
                corePoolSize * 2, // 最大线程数
                60L, // 临时线程空闲时间
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100), // 任务队列（容量100）
                // 自定义线程工厂（设置线程名，方便排查问题）
                new ThreadFactory() {
                    private int count = 1;

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("user-thread-" + count++);
                        return thread;
                    }
                },
                // 自定义拒绝策略（打印日志+重试，比默认AbortPolicy更友好）
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
