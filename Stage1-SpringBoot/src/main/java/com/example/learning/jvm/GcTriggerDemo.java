package com.example.learning.jvm;

import java.util.ArrayList;
import java.util.List;

/**
 * 手动触发GC，生成GC日志，用于分析
 */
public class GcTriggerDemo {
    // 定义一个大对象（占内存）
    static class BigObject {
        // 100KB的字节数组
        private byte[] data = new byte[1024 * 100];
    }

    public static void main(String[] args) throws InterruptedException {
        List<BigObject> list = new ArrayList<>();

        // 循环创建对象，触发Minor GC和Full GC
        for (int i = 0; i < 10000; i++) {
            list.add(new BigObject());
            // 每1000次清空一次，触发GC
            if (i % 1000 == 0) {
                list.clear();
                // 手动建议GC（不是强制，只是触发）
                System.gc();
                Thread.sleep(500);
                System.out.println("第" + i + "次循环，触发GC");
            }
        }
    }
}