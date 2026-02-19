package com.example.learning.jvm;

import java.util.ArrayList;
import java.util.List;

/**
 * 复现堆OOM：-Xms20m -Xmx20m（堆初始/最大20M，限制堆大小）
 * 原理：不断创建对象并放入集合，堆内存被占满，触发OOM
 */
public class HeapOOMDemo {
    // 静态内部类（避免GC回收）
    static class OOMObject {
    }

    public static void main(String[] args) {
        // 集合持有对象引用，防止GC回收
        List<OOMObject> list = new ArrayList<>();

        // 无限创建对象
        while (true) {
            list.add(new OOMObject());
        }
    }
}

