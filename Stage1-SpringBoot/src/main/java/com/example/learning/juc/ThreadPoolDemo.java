package com.example.learning.juc;

import java.util.concurrent.*;

/**
 * 手写线程池：重点掌握7大参数，面试必问
 */
public class ThreadPoolDemo {
    public static void main(String[] args) {
        // 核心：ThreadPoolExecutor的7个参数
        ExecutorService executor = new ThreadPoolExecutor(
                2, // 核心线程数（常驻线程）
                5, // 最大线程数（核心+临时）
                60L, // 临时线程空闲时间（超过就销毁）
                TimeUnit.SECONDS, // 时间单位
                new ArrayBlockingQueue<>(3), // 任务队列（核心线程满了放这里）
                Executors.defaultThreadFactory(), // 线程工厂（创建线程）
                // 拒绝策略（队列满+最大线程满，触发拒绝）
                new ThreadPoolExecutor.AbortPolicy()
        );

        // 提交10个任务（核心2 + 队列3 + 临时3 = 8，剩下2个触发拒绝）
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + "执行任务：" + finalI);
            });
        }

        // 关闭线程池
        executor.shutdown();
    }
}
