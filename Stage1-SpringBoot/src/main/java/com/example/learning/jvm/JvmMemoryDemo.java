package com.example.learning.jvm;

/**
 * 直观理解JVM内存区域：
 * - 栈：存储方法栈帧、局部变量（userName）
 * - 堆：存储User对象实例（new User()）
 * - 方法区：存储User类的Class信息、静态变量（count）
 */
public class JvmMemoryDemo {
    // 静态变量（存在方法区/元空间）
    private static int count = 0;

    // 内部类（Class信息存在方法区）
    static class User {
        private String name; // 实例变量（存在堆）

        public User(String name) {
            this.name = name;
        }
    }

    public static void main(String[] args) {
        // 局部变量（存在栈）
        String userName = "test";
        // new User()对象实例存在堆
        User user = new User(userName);
        // 修改静态变量（方法区）
        count++;

        System.out.println("栈变量：" + userName);
        System.out.println("堆对象：" + user.name);
        System.out.println("方法区静态变量：" + count);
    }
}
