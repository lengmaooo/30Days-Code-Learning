package com.example.learning.controller;

import com.example.learning.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * GC测试专用接口 - 用于产生大量对象触发GC
 */
@RestController
@RequestMapping("/gc-test")
public class GcTestController {

    /**
     * 创建大量对象，触发GC
     * 访问：http://localhost:8080/gc-test/create-objects?count=10000
     */
    @GetMapping("/create-objects")
    public Result<String> createObjects(@RequestParam(defaultValue = "10000") int count) {
        List<byte[]> list = new ArrayList<>();

        // 创建大量对象（每个1MB）
        for (int i = 0; i < count; i++) {
            list.add(new byte[1024 * 1024]); // 1MB对象

            // 每1000个对象打印一次
            if (i % 1000 == 0 && i > 0) {
                System.out.println("已创建 " + i + " 个对象，当前内存使用：" +
                        (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024 + "MB");
            }
        }

        long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        String result = "成功创建 " + count + " 个对象（每个1MB），当前使用内存：" + usedMemory + "MB";
        System.out.println(result);

        // 不返回list，让这些对象成为垃圾，触发GC
        return Result.success(result);
    }

    /**
     * 清空内存并建议GC
     * 访问：http://localhost:8080/gc-test/clear-memory
     */
    @GetMapping("/clear-memory")
    public Result<String> clearMemory() {
        System.gc();
        long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        return Result.success("已触发GC，当前使用内存：" + usedMemory + "MB");
    }
}
