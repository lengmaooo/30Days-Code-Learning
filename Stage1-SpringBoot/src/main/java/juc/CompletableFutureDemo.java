package juc;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

/**
 * CompletableFuture：异步编程神器，比Future更强大
 * 实战场景：异步调用多个接口、结果聚合、异常处理
 */
public class CompletableFutureDemo {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 1. 异步执行无返回值任务
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000);
                System.out.println("异步任务1执行完成");
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        });

        // 2. 异步执行有返回值任务
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
                return "异步任务2返回结果";
            }catch (InterruptedException e){
                throw new RuntimeException(e);
            }
        });

        // 3. 等待所有任务完成
        CompletableFuture.allOf(future1, future2).get();
        System.out.println("所有异步任务完成，future2结果：" + future2.get());

        CompletableFuture<String> future3 = CompletableFuture.supplyAsync(() -> {
            int a= 1/0;
            return "正常返回";
        }).exceptionally(e -> {
            // 捕获异常并返回默认值
            System.out.println("任务3异常：" + e.getMessage());
            return "异常默认值";
        });

        System.out.println("future3结果：" + future3.get());


    }

}
