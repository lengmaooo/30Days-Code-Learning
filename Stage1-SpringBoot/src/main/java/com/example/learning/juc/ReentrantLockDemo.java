package com.example.learning.juc;

import java.util.concurrent.locks.ReentrantLock;

/**
 * ReentrantLock演示：手动加锁/解锁，比synchronized更灵活
 */
public class ReentrantLockDemo {
    private int count = 0;
    // 创建可重入锁（默认非公平锁）
    private final ReentrantLock lock = new ReentrantLock();

    // 累加方法
    public void increment() {
        // 手动加锁
        lock.lock();
        try {
            count++;
            System.out.println(Thread.currentThread().getName() + "：count=" + count);
        } finally {
            // 必须在finally里解锁，防止死锁
            lock.unlock();
        }
    }

    public static void main(String[] args) {
        ReentrantLockDemo demo = new ReentrantLockDemo();

        // 5个线程各累加10次
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    demo.increment();
                }
            }, "线程" + i).start();
        }
    }
}