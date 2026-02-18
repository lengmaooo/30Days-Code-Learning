package juc;

import java.util.concurrent.Semaphore;

/**
 * Semaphore：控制同时访问的线程数（限流）
 * 实战场景：接口限流、数据库连接池、秒杀接口控制并发
 */
public class SemaphoreDemo {
    public static void main(String[] args) {
        // 初始化信号量：允许3个线程同时访问（公平锁）
        Semaphore semaphore = new Semaphore(3, true);

        // 10个线程模拟请求接口
        for (int i = 1; i <= 10; i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    // 获取许可（没有则等待）
                    semaphore.acquire();
                    System.out.println("请求" + finalI + "：获取到许可，开始处理");
                    Thread.sleep(1000); // 模拟接口处理时间
                    System.out.println("请求" + finalI + "：处理完成，释放许可");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    // 释放许可
                    semaphore.release();
                }
            }).start();
        }
    }
}
