package com.example.learning.juc;

import java.util.concurrent.CyclicBarrier;

/**
 * CyclicBarrier：让多个线程互相等待，直到所有线程都到达屏障点才继续
 * 实战场景：多线程任务分阶段执行（比如先全部准备好，再一起执行）
 */
public class CyclicBarrierDemo {
    public static void main(String[] args) {
        // 初始化屏障：3个线程到达后，执行屏障任务
        CyclicBarrier barrier = new CyclicBarrier(3, () -> {
            System.out.println("=== 所有线程已到达屏障，开始执行核心任务 ===");
        });

        // 3个线程模拟运动员准备
        for (int i = 1; i <= 3; i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    System.out.println("运动员" + finalI + "：准备中...");
                    Thread.sleep(finalI * 1000); // 模拟不同准备时间
                    System.out.println("运动员" + finalI + "：准备完成，等待其他运动员");
                    // 到达屏障，等待其他线程
                    barrier.await();
                    // 屏障后执行
                    System.out.println("运动员" + finalI + "：开始比赛");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}