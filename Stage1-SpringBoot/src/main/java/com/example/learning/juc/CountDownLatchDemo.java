package com.example.learning.juc;

import java.util.concurrent.CountDownLatch;

public class CountDownLatchDemo {

    public static void main(String[] args) throws InterruptedException {
        // 初始化计数器为3（需要等待3个线程完成）
        CountDownLatch latch = new CountDownLatch(3);

        // 线程1：模拟查询用户信息
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                System.out.println("线程1：用户信息查询完成");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                // 计数器-1
                latch.countDown();
            }
        }).start();

        // 线程2：模拟查询订单信息
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                System.out.println("线程2：订单信息查询完成");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }).start();

        // 线程3：模拟查询商品信息
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                System.out.println("线程3：商品信息查询完成");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }).start();

        // 主线程等待：直到计数器变为0
        System.out.println("主线程：等待所有查询完成...");
        latch.await();
        System.out.println("主线程：所有查询完成，开始聚合返回结果");
    }
}
