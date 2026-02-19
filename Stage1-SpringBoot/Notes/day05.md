# 第5天：JVM 内存区域 + OOM 实战（超级细版）
今天聚焦JVM核心考点——内存区域划分和OOM实战，这是面试必问的“硬八股”，我会把代码写死、原理讲透，确保你既能手写代码复现OOM，又能把内存模型说清楚。

---

## 一、核心目标
1. 彻底理解JVM内存区域划分（堆/栈/方法区/程序计数器/本地方法栈）
2. 手写代码复现堆OOM、栈溢出、方法区OOM
3. 掌握JVM常用参数和OOM排查思路
4. 背会5道核心JVM八股题

---

## 二、JVM内存区域先理清（先懂原理，再写代码）
先记住JVM运行时数据区的5个核心区域，用表格帮你快速记：

| 区域名称         | 作用                                  | 是否线程私有 | 可能出现的异常       |
|------------------|---------------------------------------|--------------|----------------------|
| 程序计数器       | 记录当前线程执行的字节码行号          | 是           | 无                   |
| Java虚拟机栈     | 存储方法调用栈帧（局部变量、操作数）  | 是           | StackOverflowError   |
| 本地方法栈       | 为Native方法服务                      | 是           | StackOverflowError   |
| 堆               | 存储对象实例（new出来的对象）         | 否           | OutOfMemoryError     |
| 方法区（元空间） | 存储类信息、常量、静态变量、编译后的代码 | 否           | OutOfMemoryError     |

---

## 三、代码部分（直接复制运行，复现OOM）
所有代码新建包`com.example.learning.jvm`，统一放在这个包下。

### 1. 演示1：理解堆、栈、方法区（基础代码）
新建`JvmMemoryDemo.java`
```java
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
```
**运行效果**：无异常，重点是理解变量分别存在哪个区域，面试能说清即可。

### 2. 演示2：堆OOM（最常见，面试必问）
新建`HeapOOMDemo.java`
```java
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
```
#### 运行步骤（关键！必须设置JVM参数）：
1. IDEA中右键该类 → Run/Debug Configurations
2. 在VM options中输入：`-Xms20m -Xmx20m`（限制堆大小为20M）
3. 运行程序，会快速抛出`java.lang.OutOfMemoryError: Java heap space`

**核心说明**：
- `-Xms`：堆初始内存（默认为物理内存1/64）
- `-Xmx`：堆最大内存（默认为物理内存1/4）
- 堆OOM是生产环境最常见的OOM，原因通常是内存泄漏（对象无法回收）或堆内存设置过小。

### 3. 演示3：栈溢出（StackOverflowError）
新建`StackOverflowDemo.java`
```java
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
```
#### 运行步骤：
1. VM options设置：`-Xss128k`（栈大小设小，更快溢出）
2. 运行程序，抛出`java.lang.StackOverflowError`

**核心说明**：
- `-Xss`：设置每个线程的栈大小（默认1M左右）
- 栈溢出常见原因：无限递归、方法调用层级过深（比如递归遍历深度极深的树）。

### 4. 演示4：方法区（元空间）OOM（JDK8+）
JDK8后方法区被元空间（Metaspace）替代，元空间使用本地内存，默认不限制，需手动设置参数触发OOM。
新建`MetaspaceOOMDemo.java`
```java
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
```
#### 前置准备：
1. 在pom.xml加cglib依赖：
```xml
<dependency>
    <groupId>cglib</groupId>
    <artifactId>cglib</artifactId>
    <version>3.3.0</version>
</dependency>
```
2. VM options设置：`-XX:MetaspaceSize=10m -XX:MaxMetaspaceSize=10m`
3. 运行程序，抛出`java.lang.OutOfMemoryError: Metaspace`

**核心说明**：
- 元空间存储类的元数据（类名、方法信息、字段信息），动态生成大量类（比如动态代理、热部署）会导致元空间OOM。

### 5. 实战：JVM参数配置到SpringBoot项目
修改你的`Stage1-SpringBoot`项目启动配置，添加常用JVM参数（模拟生产环境配置）：
1. IDEA中找到项目启动类（LearningApplication）→ Run/Debug Configurations
2. VM options输入：
```
-Xms512m
-Xmx512m
-Xss1m
-XX:MetaspaceSize=128m
-XX:MaxMetaspaceSize=256m
-XX:+PrintGCDetails
-XX:+PrintGCTimeStamps
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=./heap-dump.hprof
```
**参数说明**（面试必背）：
| 参数                          | 作用                                  |
|-------------------------------|---------------------------------------|
| -Xms512m/-Xmx512m             | 堆初始/最大512M（设置为相同值避免堆扩容） |
| -Xss1m                        | 每个线程栈大小1M                      |
| -XX:MetaspaceSize/MaxMetaspaceSize | 元空间初始128M，最大256M            |
| -XX:+PrintGCDetails/+PrintGCTimeStamps | 打印GC详细日志和时间戳           |
| -XX:+HeapDumpOnOutOfMemoryError | OOM时自动生成堆转储文件（hprof）     |
| -XX:HeapDumpPath              | 堆转储文件保存路径                    |

---

## 四、OOM排查思路（实战必备，面试必说）
### 1. 堆OOM排查步骤（核心）
1. 找到OOM时生成的`heap-dump.hprof`文件（上面配置的路径）；
2. 使用工具分析：JDK自带的`jvisualvm`（可视化）或`MAT`（Eclipse Memory Analyzer）；
3. 分析步骤：
    - 查看哪个对象占用内存最多（比如ArrayList持有大量对象）；
    - 查看对象的引用链，找到内存泄漏的根源（比如静态集合未清理）；
    - 调整JVM参数（增大堆内存）或修复代码（清理无用引用）。

### 2. 常用JVM排查命令（面试必记）
| 命令   | 作用                                  |
|--------|---------------------------------------|
| jps    | 查看运行中的Java进程ID                |
| jstat  | 查看GC统计信息（jstat -gc 进程ID）    |
| jmap   | 生成堆转储文件（jmap -dump:format=b,file=heap.hprof 进程ID） |
| jstack | 查看线程栈信息（排查死锁、线程阻塞）  |
| jvisualvm | 可视化工具（集成jps/jstat/jmap/jstack） |

---

## 五、第5天必须背的5道JVM八股（精简答案）
1. **JVM运行时数据区分为哪几个部分？**
   ① 程序计数器（线程私有，记录字节码行号）；② Java虚拟机栈（线程私有，存储方法栈帧）；③ 本地方法栈（线程私有，为Native方法服务）；④ 堆（线程共享，存储对象实例）；⑤ 方法区（线程共享，存储类信息、常量、静态变量）。

2. **堆和栈的区别？**
   ① 堆：线程共享，存储对象实例，GC回收，可能OOM；② 栈：线程私有，存储栈帧/局部变量，方法执行完自动释放，可能栈溢出；③ 堆内存大小远大于栈。

3. **Minor GC和Full GC的区别？**
   ① Minor GC：发生在新生代，回收年轻代对象，频率高、速度快；② Full GC：发生在老年代+新生代，回收整个堆，频率低、速度慢（要尽量避免）。

4. **OOM的常见类型及原因？**
   ① 堆OOM：对象过多/内存泄漏，堆内存不足；② 栈溢出：递归过深/栈太小；③ 元空间OOM：动态生成大量类，元空间不足；④ 直接内存OOM：NIO直接内存使用过多。

5. **生产环境如何排查OOM？**
   ① 配置`-XX:+HeapDumpOnOutOfMemoryError`生成堆转储文件；② 用jmap/jvisualvm/MAT分析堆文件；③ 定位内存泄漏的对象和引用链；④ 调整JVM参数或修复代码。

---

## 六、过关标准
1. 能复现堆OOM、栈溢出、元空间OOM三种异常；
2. 能说出SpringBoot项目中配置的JVM参数含义；
3. 能复述OOM排查步骤和常用JVM命令；
4. 5道八股题能准确背下来（重点是内存区域划分和OOM排查）。

你只要说一句：“第五天搞定”，我就给你发**第六天 JVM GC 算法 + 调优实战**的超细版。