# 第8天：MySQL 事务 + 锁 实战（超级细版）
今天聚焦MySQL事务和锁这两个核心考点——事务是数据一致性的保障，锁是并发控制的核心，面试中这两个点会结合问（比如事务隔离级别和锁的关系），我会把原理讲透、代码写死、实战案例落地到你的项目，确保你既能说清ACID，又能手动复现事务问题和锁冲突。

---

## 一、核心目标
1. 彻底理解事务ACID特性和4大隔离级别
2. 掌握事务失效场景（实战中最容易踩坑）
3. 理解MySQL行锁/表锁/间隙锁，复现锁冲突
4. 背会5道事务+锁核心八股题

---

## 二、先理清核心概念（先懂原理，再写代码）
### 1. 事务ACID特性（面试必背）
| 特性       | 含义                                  | 保障机制                          |
|------------|---------------------------------------|-----------------------------------|
| 原子性（A） | 事务要么全执行，要么全回滚            | undo log（回滚日志）              |
| 一致性（C） | 事务执行前后数据状态一致              | 原子性+隔离性+持久性              |
| 隔离性（I） | 多事务并发执行时互不干扰              | 锁+MVCC（多版本并发控制）         |
| 持久性（D） | 事务提交后数据永久保存                | redo log（重做日志）              |

### 2. 事务4大隔离级别（从低到高）
| 隔离级别               | 脏读 | 不可重复读 | 幻读 | 实战场景                  |
|------------------------|------|------------|------|---------------------------|
| 读未提交（READ UNCOMMITTED） | ✅    | ✅          | ✅    | 几乎不用（数据一致性差）  |
| 读已提交（READ COMMITTED）   | ❌    | ✅          | ✅    | 互联网应用（Oracle默认）  |
| 可重复读（REPEATABLE READ）  | ❌    | ❌          | ❌    | MySQL默认（解决幻读）     |
| 串行化（SERIALIZABLE）       | ❌    | ❌          | ❌    | 金融场景（性能极低）      |

### 3. MySQL锁分类（核心）
| 锁类型       | 粒度       | 适用场景                  | 特点                          |
|--------------|------------|---------------------------|-------------------------------|
| 行锁         | 行         | 高并发更新（where带主键） | 开销大、加锁慢、冲突概率低    |
| 表锁         | 整张表     | 全表更新/查询             | 开销小、加锁快、冲突概率高    |
| 间隙锁       | 索引区间   | 可重复读隔离级别下防幻读  | 导致锁范围扩大，容易死锁      |

---

## 三、代码/实操部分（直接落地到你的项目）
所有操作基于你项目中的`user`表，先打开两个MySQL客户端窗口（窗口A/窗口B），模拟并发事务。

### 1. 第一步：准备测试环境
#### ① 查看当前事务隔离级别
```sql
-- 查看全局/会话隔离级别
SELECT @@global.tx_isolation, @@tx_isolation;
-- MySQL8.0+用这个
SELECT @@global.transaction_isolation, @@transaction_isolation;
```

#### ② 修改隔离级别（测试用）
```sql
-- 设置会话级隔离级别为读未提交（测试脏读）
SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
-- 改回默认（可重复读）
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
```

#### ③ 给user表加余额字段（模拟转账场景）
```sql
ALTER TABLE user ADD COLUMN balance DECIMAL(10,2) DEFAULT 0 COMMENT '余额';
-- 初始化两个用户的余额
UPDATE user SET balance = 1000 WHERE username = 'user_1';
UPDATE user SET balance = 1000 WHERE username = 'user_2';
```

### 2. 第二步：复现事务问题（脏读/不可重复读/幻读）
#### ① 复现脏读（读未提交级别）
| 窗口A（事务1）                          | 窗口B（事务2）                          |
|-----------------------------------------|-----------------------------------------|
| SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED; | SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED; |
| BEGIN;（开启事务）| BEGIN;（开启事务）|
| UPDATE user SET balance = 900 WHERE username = 'user_1';（扣100） | - |
| -                                       | SELECT balance FROM user WHERE username = 'user_1';（查到900，脏读） |
| ROLLBACK;（回滚事务）| - |
| -                                       | SELECT balance FROM user WHERE username = 'user_1';（变回1000） |

**结论**：读未提交级别下，能读到其他事务未提交的数据（脏读），生产环境禁止使用。

#### ② 复现不可重复读（读已提交级别）
| 窗口A（事务1）                          | 窗口B（事务2）                          |
|-----------------------------------------|-----------------------------------------|
| SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED; | SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED; |
| BEGIN;                                  | BEGIN;                                  |
| SELECT balance FROM user WHERE username = 'user_1';（查到1000） | - |
| -                                       | UPDATE user SET balance = 900 WHERE username = 'user_1'; COMMIT;（提交） |
| SELECT balance FROM user WHERE username = 'user_1';（查到900，不可重复读） | - |

**结论**：读已提交级别下，同一事务内多次查询结果不一致（不可重复读），解决了脏读但没解决不可重复读。

#### ③ 复现幻读（可重复读级别下模拟）
MySQL默认的可重复读级别通过MVCC解决了幻读，需手动关闭MVCC才能复现：
```sql
-- 关闭MVCC（测试用，生产环境不要关）
SET SESSION innodb_locks_unsafe_for_binlog = 1;
```
| 窗口A（事务1）                          | 窗口B（事务2）                          |
|-----------------------------------------|-----------------------------------------|
| BEGIN;                                  | BEGIN;                                  |
| SELECT COUNT(*) FROM user WHERE age < 20;（查到100条） | - |
| -                                       | INSERT INTO user (username, age, balance) VALUES ('user_100001', 18, 500); COMMIT; |
| SELECT COUNT(*) FROM user WHERE age < 20;（查到101条，幻读） | - |

**结论**：幻读是指同一事务内，多次查询相同条件的记录数不一致，MySQL可重复读级别通过间隙锁解决了幻读。

### 3. 第三步：事务失效场景（实战踩坑点，必测）
#### ① 场景1：方法未加@Transactional（最常见）
修改你的UserService，新增转账方法（无事务）：
```java
// 无@Transactional，事务失效
public void transfer(String fromUser, String toUser, BigDecimal amount) {
    // 扣减转出方余额
    lambdaUpdate().eq(User::getUsername, fromUser)
            .set(User::getBalance, User::getBalance, (b) -> b.subtract(amount))
            .update();
    
    // 模拟异常（比如除数为0）
    int a = 1 / 0;
    
    // 增加转入方余额（不会执行，数据不一致）
    lambdaUpdate().eq(User::getUsername, toUser)
            .set(User::getBalance, User::getBalance, (b) -> b.add(amount))
            .update();
}
```
**测试**：调用该方法，转出方余额扣了但转入方没加，数据不一致。

#### ② 场景2：加了@Transactional但异常被捕获（失效）
```java
@Transactional
public void transfer(String fromUser, String toUser, BigDecimal amount) {
    try {
        lambdaUpdate().eq(User::getUsername, fromUser)
                .set(User::getBalance, User::getBalance, (b) -> b.subtract(amount))
                .update();
        
        int a = 1 / 0; // 异常
        
        lambdaUpdate().eq(User::getUsername, toUser)
                .set(User::getBalance, User::getBalance, (b) -> b.add(amount))
                .update();
    } catch (Exception e) {
        e.printStackTrace(); // 捕获异常，事务不回滚
    }
}
```
**结论**：Spring事务默认捕获RuntimeException才回滚，异常被手动捕获后事务失效。

#### ③ 场景3：解决事务失效（正确写法）
```java
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {
    // 其他代码...

    // 正确的事务写法：加@Transactional，不捕获异常
    @Transactional(rollbackFor = Exception.class) // 指定回滚所有异常
    public void transfer(String fromUser, String toUser, BigDecimal amount) {
        // 扣减转出方
        User from = lambdaQuery().eq(User::getUsername, fromUser).one();
        if (from.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("余额不足");
        }
        lambdaUpdate().eq(User::getUsername, fromUser)
                .set(User::getBalance, from.getBalance().subtract(amount))
                .update();
        
        // 增加转入方
        User to = lambdaQuery().eq(User::getUsername, toUser).one();
        lambdaUpdate().eq(User::getUsername, toUser)
                .set(User::getBalance, to.getBalance().add(amount))
                .update();
    }
}
```
**测试**：新增TransferController调用该方法，异常时事务回滚，数据一致。

### 4. 第四步：MySQL锁实战（行锁/表锁/间隙锁）
#### ① 行锁（主键查询，只锁一行）
| 窗口A                          | 窗口B                          |
|--------------------------------|--------------------------------|
| BEGIN;                         | BEGIN;                         |
| UPDATE user SET balance = 800 WHERE id = 1;（行锁） | UPDATE user SET balance = 700 WHERE id = 2;（立即执行，不阻塞） |
| -                              | UPDATE user SET balance = 700 WHERE id = 1;（阻塞，直到A提交/回滚） |
| COMMIT;                        | -                              |
| -                              | 立即执行（锁释放）             |

#### ② 表锁（无主键/全表更新，锁整张表）
| 窗口A                          | 窗口B                          |
|--------------------------------|--------------------------------|
| BEGIN;                         | BEGIN;                         |
| UPDATE user SET balance = 800 WHERE age = 20;（无索引，表锁） | UPDATE user SET balance = 700 WHERE id = 1;（阻塞，表锁） |
| COMMIT;                        | -                              |
| -                              | 立即执行                       |

**结论**：更新时where条件不带主键/索引，MySQL会升级为表锁，高并发下性能极差！

#### ③ 间隙锁（可重复读级别下防幻读）
```sql
-- 先给age建索引
CREATE INDEX idx_user_age ON user(age);
```
| 窗口A                          | 窗口B                          |
|--------------------------------|--------------------------------|
| BEGIN;                         | BEGIN;                         |
| SELECT * FROM user WHERE age BETWEEN 18 AND 20 FOR UPDATE;（加间隙锁） | - |
| -                              | INSERT INTO user (username, age, balance) VALUES ('user_100002', 19, 500);（阻塞，间隙锁生效） |
| COMMIT;                        | -                              |
| -                              | 立即执行                       |

**结论**：间隙锁会锁住索引区间（18-20），防止插入该区间的数据，解决幻读但可能导致死锁。

### 5. 第五步：死锁排查（实战必备）
#### ① 复现死锁
| 窗口A                          | 窗口B                          |
|--------------------------------|--------------------------------|
| BEGIN;                         | BEGIN;                         |
| UPDATE user SET balance = 800 WHERE id = 1; | UPDATE user SET balance = 700 WHERE id = 2; |
| UPDATE user SET balance = 700 WHERE id = 2;（阻塞） | UPDATE user SET balance = 800 WHERE id = 1;（死锁） |

#### ② 排查死锁
```sql
-- 查看死锁日志
SHOW ENGINE INNODB STATUS;
```
**关键日志解读**：
```
LATEST DETECTED DEADLOCK
------------------------
WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 123 page no 45 n bits 72 index PRIMARY of table `test`.`user`
trx id 12345 lock_mode X waiting
HOLDING THE LOCK:
RECORD LOCKS space id 123 page no 46 n bits 72 index PRIMARY of table `test`.`user`
trx id 12346 lock_mode X
```

#### ③ 解决死锁
- 统一更新顺序（比如先更id小的，再更id大的）；
- 缩短事务时长（尽快提交）；
- 加锁超时设置（innodb_lock_wait_timeout=5）。

---

## 四、第8天必须背的5道事务+锁八股（精简答案）
1. **事务ACID分别指什么？怎么保障的？**
   ① 原子性：undo log回滚；② 一致性：原子性+隔离性+持久性；③ 隔离性：锁+MVCC；④ 持久性：redo log持久化。

2. **MySQL4大隔离级别？解决了什么问题？**
   ① 读未提交：脏读+不可重复读+幻读；② 读已提交：解决脏读，存在不可重复读+幻读；③ 可重复读（默认）：解决脏读+不可重复读，MVCC解决幻读；④ 串行化：解决所有问题，性能极差。

3. **Spring事务失效的常见场景？**
   ① 方法未加@Transactional；② 异常被手动捕获；③ 方法是private（Spring AOP不支持）；④ 多线程调用；⑤ 数据源未配置事务管理器。

4. **MySQL行锁和表锁的区别？什么时候行锁升级为表锁？**
   ① 行锁锁单行，表锁锁整张表；② 行锁开销大、冲突低，表锁相反；③ 升级场景：where条件无主键/索引、全表更新、间隙锁范围过大。

5. **死锁产生的条件？如何避免？**
   条件：互斥、持有并等待、不可剥夺、循环等待；避免：① 统一更新顺序；② 缩短事务时长；③ 设置锁超时；④ 减少间隙锁使用。

---

## 五、过关标准
1. 能复现脏读/不可重复读/幻读，理解隔离级别差异；
2. 能写出正确的Spring事务代码，避免事务失效；
3. 能复现行锁/表锁/死锁，会用SHOW ENGINE INNODB STATUS排查死锁；
4. 5道八股题能准确复述（重点是ACID、隔离级别、事务失效）。
