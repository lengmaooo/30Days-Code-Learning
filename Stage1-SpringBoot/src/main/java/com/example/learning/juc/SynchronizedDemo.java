package com.example.learning.juc;

/**
 * synchronized演示：卖票问题（解决多线程原子性）
 * 不加synchronized会出现超卖（票数为负数）
 */
public class SynchronizedDemo {
    // 总票数
    private int ticketNum = 100;

    // 卖票方法（加synchronized保证原子性）
    public synchronized void sellTicket() {
        if (ticketNum > 0) {
            // 模拟出票延迟
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName() + "卖出1张票，剩余：" + --ticketNum);
        }
    }

    public static void main(String[] args) {
        SynchronizedDemo demo = new SynchronizedDemo();

        // 3个窗口卖票
        new Thread(() -> {
            for (int i = 0; i < 40; i++) {
                demo.sellTicket();
            }
        }, "窗口1").start();

        new Thread(() -> {
            for (int i = 0; i < 40; i++) {
                demo.sellTicket();
            }
        }, "窗口2").start();

        new Thread(() -> {
            for (int i = 0; i < 40; i++) {
                demo.sellTicket();
            }
        }, "窗口3").start();
    }
}
