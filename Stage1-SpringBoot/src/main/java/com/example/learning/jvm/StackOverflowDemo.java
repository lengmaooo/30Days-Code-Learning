package com.example.learning.jvm;

/**
 * 复现栈溢出：无限递归调用方法，栈帧过多导致溢出
 * VM参数：-Xss128k（设置栈大小为128k，加速溢出）
 */
public class StackOverflowDemo {
    private static int depth = 0;

    public static void recursiveCall() {
        depth++;
        // 无限递归，不断创建栈帧
        recursiveCall();
    }

    public static void main(String[] args) {
        try {
            recursiveCall();
        } catch (Throwable e) {
            System.out.println("递归深度：" + depth);
            e.printStackTrace();
        }
    }
}