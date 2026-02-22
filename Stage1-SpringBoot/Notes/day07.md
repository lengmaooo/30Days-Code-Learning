# 第7天：MySQL 索引 + 优化实战（超级细版）
今天聚焦MySQL面试核心——索引原理和SQL优化，这是后端面试“必问必答”的考点，我会把索引原理讲透、SQL优化步骤写死、实战案例落地到你的项目，确保你既能说清B+树原理，又能手动优化慢SQL。

---

## 一、核心目标
1. 彻底理解索引底层（B+树）和索引类型（聚簇/非聚簇）
2. 掌握explain分析SQL，落地4大索引优化技巧
3. 修复你的项目中低效SQL，提升查询性能
4. 背会5道索引核心八股题

---

## 二、先理清索引核心原理（先懂原理，再写代码）
### 1. 索引底层：B+树（面试必画必说）
B+树是MySQL索引的底层结构，核心特点：
- 非叶子节点只存索引键，叶子节点存数据（聚簇索引）或主键（非聚簇索引）；
- 叶子节点通过双向链表连接，支持范围查询；
- 高度一般是3-4层（百万级数据），查询效率极高（O(logn)）。

### 2. 索引类型（必须区分）
| 索引类型       | 特点                                  | 实战场景                  |
|----------------|---------------------------------------|---------------------------|
| 聚簇索引（主键） | 叶子节点存整行数据，一张表只有一个    | 按主键查询（select * from user where id=1） |
| 非聚簇索引（普通索引） | 叶子节点存主键值，需回表查询          | 按用户名查询（select * from user where username='test'） |
| 联合索引       | 多个字段组成的索引，遵循最左前缀原则  | 按用户名+年龄查询（where username='test' and age=20） |
| 覆盖索引       | 查询字段都在索引中，无需回表          | select id,username from user where username='test' |

---

## 三、代码/实操部分（直接落地到你的项目）
所有操作基于你项目中的`user`表，先备份表数据，再一步步操作。

### 1. 第一步：准备测试数据（造大量数据，模拟慢查询）
执行SQL脚本，插入10万条测试数据：
```sql
-- 清空原有数据
TRUNCATE TABLE user;

-- 插入10万条测试数据（执行可能需要1-2分钟）
DELIMITER //
CREATE PROCEDURE insert_test_data()
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= 100000 DO
        INSERT INTO user (username, age, password) 
        VALUES (CONCAT('user_', i), FLOOR(RAND()*50), '$2a$10$EixZaYb4xU58Gpq1R0yWbeb00LU5qUaK6x8h8y0xU58Gpq1R0yWbeb');
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

-- 调用存储过程
CALL insert_test_data();
-- 删除存储过程
DROP PROCEDURE insert_test_data;
```

### 2. 第二步：用explain分析低效SQL（核心工具）
#### ① 先执行低效SQL（无索引）
```sql
-- 无索引时查询，耗时约0.5秒（10万条数据）
SELECT * FROM user WHERE username = 'user_50000';
```

#### ② 用explain分析SQL
```sql
EXPLAIN SELECT * FROM user WHERE username = 'user_50000';
```
**explain结果解读（重点字段）**：
| 字段   | 值          | 含义                                  |
|--------|-------------|---------------------------------------|
| type   | ALL         | 全表扫描（最差，必须优化）            |
| key    | NULL        | 未使用索引                            |
| rows   | 100000      | 扫描10万行（全表）                    |
| Extra  | Using where | 过滤条件，但仍需全表扫描              |

### 3. 第三步：创建索引并优化SQL（核心实战）
#### ① 创建普通索引（优化按用户名查询）
```sql
-- 创建username索引
CREATE INDEX idx_user_username ON user(username);

-- 再次分析SQL
EXPLAIN SELECT * FROM user WHERE username = 'user_50000';
```
**优化后explain结果**：
| 字段   | 值                | 含义                                  |
|--------|-------------------|---------------------------------------|
| type   | ref               | 非唯一索引扫描（优秀）                |
| key    | idx_user_username | 使用了创建的索引                      |
| rows   | 1                 | 仅扫描1行（效率提升10万倍）           |
| Extra  | NULL              | 无额外操作                            |

**执行SQL耗时**：约0.001秒（从0.5秒降到0.001秒）。

#### ② 创建联合索引（遵循最左前缀原则）
```sql
-- 创建username+age联合索引
CREATE INDEX idx_user_username_age ON user(username, age);

-- 分析符合最左前缀的SQL（命中索引）
EXPLAIN SELECT * FROM user WHERE username = 'user_50000' AND age = 20;

-- 分析不符合最左前缀的SQL（未命中索引）
EXPLAIN SELECT * FROM user WHERE age = 20;
```
**核心结论**：联合索引`(a,b,c)`只能命中`a`、`a,b`、`a,b,c`，无法命中`b`、`c`、`b,c`。

#### ③ 创建覆盖索引（避免回表）
```sql
-- 原SQL（需要回表，因为查询*，索引只存username和主键）
EXPLAIN SELECT * FROM user WHERE username = 'user_50000';

-- 优化为覆盖索引SQL（查询字段都在索引中）
EXPLAIN SELECT id, username FROM user WHERE username = 'user_50000';
```
**优化后Extra字段**：`Using index`（使用覆盖索引，无需回表）。

#### ④ 索引失效场景（面试必问，手动验证）
```sql
-- 1. 模糊查询以%开头（失效）
EXPLAIN SELECT * FROM user WHERE username LIKE '%50000';

-- 2. 索引字段做函数运算（失效）
EXPLAIN SELECT * FROM user WHERE SUBSTR(username, 1, 5) = 'user_';

-- 3. 索引字段用or（一侧无索引则失效）
EXPLAIN SELECT * FROM user WHERE username = 'user_50000' OR age = 20;

-- 4. 隐式类型转换（失效，比如age是int，传字符串）
EXPLAIN SELECT * FROM user WHERE age = '20';
```

### 4. 第四步：集成到SpringBoot项目（实战落地）
#### ① 修改UserController，添加慢查询接口（测试）
```java
@GetMapping("/slow")
public Result slowQuery(@RequestParam String username) {
    // 模拟低效查询（无索引时慢，有索引时快）
    User user = userService.lambdaQuery().eq(User::getUsername, username).one();
    return Result.success(user);
}
```

#### ② 开启MySQL慢查询日志（生产环境必备）

**方法1：使用SQL命令动态修改（推荐，Docker适用）**
```sql
-- 1. 开启慢查询日志
SET GLOBAL slow_query_log = 'ON';

-- 2. 设置慢查询日志文件路径（Docker容器内路径）
SET GLOBAL slow_query_log_file = '/var/lib/mysql/mysql-slow.log';

-- 3. 设置慢查询阈值（单位：秒，超过此时间的SQL会被记录）
SET GLOBAL long_query_time = 1;

-- 4. 记录未使用索引的查询（帮助发现需要优化的SQL）
SET GLOBAL log_queries_not_using_indexes = 'ON';

-- 5. 查看当前配置是否生效
SHOW VARIABLES LIKE '%slow_query%';
SHOW VARIABLES LIKE 'long_query_time';
SHOW VARIABLES LIKE 'log_queries_not_using_indexes';
```

**方法2：Docker 容器启动时配置（一次性生效）**
```bash
# 停止容器
docker stop <mysql容器名或ID>

# 重新启动容器，挂载配置目录
docker run -d \
  --name mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  -v $(pwd)/mysql-data:/var/lib/mysql \
  mysql:8.0 \
  --slow_query_log=1 \
  --slow_query_log_file=/var/lib/mysql/mysql-slow.log \
  --long_query_time=1 \
  --log_queries_not_using_indexes=1
```

**查看慢查询日志**：
```bash
# 如果使用Docker，进入容器查看
docker exec -it <mysql容器名> tail -f /var/lib/mysql/mysql-slow.log

# 或者复制日志到宿主机
docker cp <mysql容器名>:/var/lib/mysql/mysql-slow.log ./
tail -f mysql-slow.log
```

#### ③ 用工具分析慢查询（mysqldumpslow）

**方法1：在Docker容器内分析**
```bash
# 进入容器
docker exec -it <mysql容器名> bash

# 查看最慢的10条SQL（按执行时间排序）
mysqldumpslow -s t -t 10 /var/lib/mysql/mysql-slow.log

# 查看访问次数最多的10条SQL
mysqldumpslow -s c -t 10 /var/lib/mysql/mysql-slow.log
```

**方法2：复制日志到宿主机分析**
```bash
# 复制日志到宿主机
docker cp <mysql容器名>:/var/lib/mysql/mysql-slow.log ./

# 在宿主机分析
mysqldumpslow -s t -t 10 mysql-slow.log
```

**参数说明**：
- `-s t`：按查询时间排序（t=时间，c=次数，l=锁定时间）
- `-t 10`：显示前10条
- `-g "SELECT"`：过滤包含"SELECT"的语句

### 5. 第五步：索引最佳实践（落地到你的项目）
#### ① 哪些字段适合建索引？
- 查询条件字段（where后的字段）；
- 排序字段（order by）；
- 分组字段（group by）；
- 关联字段（join on）。

#### ② 哪些字段不适合建索引？
- 高频更新的字段（比如订单状态）；
- 基数低的字段（比如性别、状态）；
- 数据量小的表（全表扫描更快）；
- 重复值多的字段（比如年龄）。

#### ③ 你的项目索引优化建议
```sql
-- 1. 主键索引（已存在）
ALTER TABLE user ADD PRIMARY KEY (id);

-- 2. 用户名普通索引（必建）
CREATE INDEX idx_user_username ON user(username);

-- 3. 若有按用户名+年龄查询，建联合索引
CREATE INDEX idx_user_username_age ON user(username, age);

-- 4. 定期删除无用索引（避免索引过多影响插入/更新）
DROP INDEX idx_user_age ON user; -- 假设这个索引没用
```

---

## 四、第7天必须背的5道索引八股（精简答案）
1. **MySQL索引底层为什么用B+树而不是B树/红黑树？**
   ① B+树非叶子节点只存索引，内存占用少，磁盘IO少；② 叶子节点链表连接，支持范围查询；③ B树叶子节点存数据，磁盘IO多；④ 红黑树高度高（百万数据约20层），IO次数多。

2. **最左前缀原则是什么？**
   联合索引`(a,b,c)`会按`a`、`a+b`、`a+b+c`建立索引，查询时必须从最左字段开始，否则索引失效（比如只查b+c不命中）。

3. **索引失效的常见场景？**
   ① 模糊查询以%开头；② 索引字段做函数运算；③ 索引字段用or（一侧无索引）；④ 隐式类型转换；⑤ 使用!=/<>/not in。

4. **聚簇索引和非聚簇索引的区别？**
   ① 聚簇索引（主键）：叶子节点存整行数据，一张表只有一个；② 非聚簇索引：叶子节点存主键，需回表查询，可建多个；③ 聚簇索引查询更快，无需回表。

5. **如何优化慢SQL？**
   ① 用explain分析SQL；② 建合适的索引（普通/联合/覆盖）；③ 避免索引失效；④ 分页查询（limit）；⑤ 开启慢查询日志，定期分析。

---

## 五、过关标准
1. 能在你的项目中用explain分析并优化SQL；
2. 能说出索引失效的5个场景并手动验证；
3. 能解释B+树原理和最左前缀原则；
4. 5道八股题能准确复述（重点是B+树、索引失效、最左前缀）。
