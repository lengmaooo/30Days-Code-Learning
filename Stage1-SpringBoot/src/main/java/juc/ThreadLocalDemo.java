package juc;

/**
 * ThreadLocal演示：每个线程独立存储数据，解决线程安全问题
 * 实战中常用：存登录用户ID、请求ID等
 */
public class ThreadLocalDemo {
    // 创建ThreadLocal，存储用户ID
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    // 设置用户ID
    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    // 获取用户ID
    public static Long getUserId() {
        return USER_ID.get();
    }

    // 移除用户ID（必须手动移除，否则内存泄漏）
    public static void removeUserId() {
        USER_ID.remove();
    }

    public static void main(String[] args) {
        // 线程1：存用户ID=1001
        new Thread(() -> {
            setUserId(1001L);
            System.out.println("线程1的用户ID：" + getUserId());
            removeUserId(); // 用完必须移除
        }).start();

        // 线程2：存用户ID=1002
        new Thread(() -> {
            setUserId(1002L);
            System.out.println("线程2的用户ID：" + getUserId());
            removeUserId(); // 用完必须移除
        }).start();
    }
}