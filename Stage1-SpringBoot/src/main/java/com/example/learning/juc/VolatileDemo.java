package com.example.learning.juc;

/**
 * volatile演示：解决多线程下变量可见性问题
 * 不加volatile，线程2永远看不到flag变化；加了之后能立即看到
 */
public class VolatileDemo {
    // 核心：volatile修饰的变量，修改后会立即刷新到主内存，其他线程能看到
    private volatile boolean flag = false;

    public static void main(String[] args) throws InterruptedException {
        VolatileDemo demo = new VolatileDemo();

        // 线程1：循环等待flag变为true
        new Thread(() -> {
            while (!demo.flag) {
                // 空循环，不加volatile会一直卡在这里
            }
            System.out.println("线程1：看到flag变为true，退出循环");
        }).start();

        // 主线程休眠1秒，确保线程1先启动
        Thread.sleep(1000);

        // 线程2：修改flag为true
        new Thread(() -> {
            demo.flag = true;
            System.out.println("线程2：已将flag设置为true");
        }).start();
    }
}
