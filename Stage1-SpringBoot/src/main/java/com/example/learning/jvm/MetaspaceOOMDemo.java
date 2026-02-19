package com.example.learning.jvm;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * 复现元空间OOM：动态生成大量类，占满元空间
 * VM参数：-XX:MetaspaceSize=10m -XX:MaxMetaspaceSize=10m（限制元空间大小）
 * 需加依赖：cglib（动态生成类）
 */
public class MetaspaceOOMDemo implements MethodInterceptor {
    public static void main(String[] args) {
        // 动态生成大量类
        while (true) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(MetaspaceOOMDemo.class);
            enhancer.setCallback(new MetaspaceOOMDemo());
            enhancer.create(); // 生成新的子类
        }
    }

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        return methodProxy.invokeSuper(o, objects);
    }
}
