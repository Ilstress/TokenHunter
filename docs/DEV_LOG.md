# DEV_LOG.md — TokenHunter 开发日志

---

## Task 1.1：初始化 Spring Boot 项目
日期：2026-09-11

修改文件：
- `backend/pom.xml` — Maven 项目配置，Spring Boot 3.4.4 + MyBatis-Plus 3.5.9 + Lombok 1.18.38
- `backend/src/main/java/com/tokenhunter/TokenHunterApplication.java` — Spring Boot 启动类
- `backend/src/main/resources/application.yml` — 最小配置，仅 spring.application.name=tokenhunter

核心逻辑：
通过 Spring Boot 3.4.4 的 spring-boot-starter-parent 作为父 POM，引入 Spring Web、MySQL 驱动、MyBatis-Plus（使用 spring-boot3-starter 版本）、Lombok 和 Spring Boot Test。启动类 TokenHunterApplication 标注 @SpringBootApplication，包含 main 方法调用 SpringApplication.run()。配置文件 application.yml 仅设置应用名称，不配置任何数据源。

测试方法：
运行 `mvn -q -DskipTests compile`，编译通过无报错。

需要理解的知识点：
- Spring Boot 3.x 对 Java 21 的要求
- MyBatis-Plus 的 mybatis-plus-spring-boot3-starter 与旧版 mybatis-plus-boot-starter 的区别
- Lombok 1.18.38 对 Java 21 的兼容性
- Maven 编译器插件的 annotationProcessorPaths 配置

---

## Task 1.2：配置 MySQL 数据源
日期：2026-09-11

修改文件：
- `backend/src/main/resources/application.yml` — 增加 spring.profiles.active=local
- `backend/src/main/resources/application-local.example.yml` — MySQL 数据源示例配置

核心逻辑：
在 application.yml 中通过 spring.profiles.active=local 激活 local profile。创建 application-local.example.yml 作为示例配置模板，其中数据库用户名使用环境变量 DB_USERNAME（默认 root），密码使用 DB_PASSWORD 环境变量（默认空），避免将真实凭据写入版本控制。连接 URL 指定了时区为 Asia/Shanghai、禁用 SSL、允许获取公钥等参数。

测试方法：
运行 `mvn -q -DskipTests compile`，编译通过无报错。

需要理解的知识点：
- Spring Boot Profile 机制（application.yml + application-{profile}.yml）
- 环境变量注入（${ENV_VAR:default} 语法）
- JDBC 连接字符串参数（useUnicode、characterEncoding、serverTimezone、useSSL、allowPublicKeyRetrieval）

---

## Task 1.3：编写 /ping 接口验证启动
日期：2026-09-11

修改文件：
- `backend/src/main/java/com/tokenhunter/controller/PingController.java` — 新建，@RestController + @GetMapping("/ping")

核心逻辑：
创建一个最简单的 Controller，标注 @RestController（等价于 @Controller + @ResponseBody），定义一个方法用 @GetMapping("/ping") 映射 GET /ping 请求，直接返回字符串 "TokenHunter is running"。Spring MVC 会自动将字符串写入 HTTP 响应体，Content-Type 为 text/plain。

测试方法：
1. 运行 `mvn -q -DskipTests compile`，编译通过无报错。
2. 运行 `mvn spring-boot:run` 启动应用，日志显示 "Started TokenHunterApplication in 0.679 seconds"，端口 8080 正常。
3. 运行 `curl -i http://localhost:8080/ping`，返回 HTTP/1.1 200，响应体为 "TokenHunter is running"。
4. 验证完成后停止应用。

需要理解的知识点：
- @RestController 与 @Controller + @ResponseBody 的关系
- @GetMapping 的作用（HTTP GET 方法映射）
- Spring MVC 返回字符串时的 Content-Type 自动设置为 text/plain
- Spring Boot 自动配置 Tomcat 嵌入式 Web 服务器

---

## Task 1.4：执行建表 SQL
日期：2026-09-11

修改文件：
- `backend/src/main/resources/db/schema.sql` — 新建，usage_record 建表语句

核心逻辑：
根据 PLAN.md 第 6 节的建表语句，创建 usage_record 表，包含 id（主键自增）、provider、model、input_tokens、output_tokens、total_tokens、duration_ms、status、created_at 共 9 个字段。创建两个索引：idx_created_at（按 created_at 单列）和 idx_provider_model（按 provider + model 复合索引）。表引擎为 InnoDB，字符集 utf8mb4。先创建 tokenhunter 数据库，再执行建表 SQL。

测试方法：
1. 运行 `mysql -u root tokenhunter < backend/src/main/resources/db/schema.sql`，无错误输出。
2. 运行 `mysql -u root tokenhunter -e "SHOW TABLES;"`，输出 `usage_record`。
3. 运行 `mysql -u root tokenhunter -e "DESC usage_record;"`，9 个字段与 PLAN.md 完全一致。
4. 运行 `mysql -u root tokenhunter -e "SHOW INDEX FROM usage_record;"`，确认包含 PRIMARY、idx_created_at、idx_provider_model（provider + model 两列）三个索引。

需要理解的知识点：
- MySQL 建表语法：数据类型（BIGINT UNSIGNED、VARCHAR、INT UNSIGNED、TINYINT、DATETIME）、约束（NOT NULL、DEFAULT、AUTO_INCREMENT、PRIMARY KEY）
- MySQL 索引类型：BTREE 索引、单列索引 vs 复合索引
- ENGINE=InnoDB 和 CHARSET=utf8mb4 的含义
- MySQL 客户端命令行执行 SQL 文件的方式（`<` 重定向）

---

## Task 1.5：编写 UsageRecord 实体类
日期：2026-09-11

修改文件：
- `backend/src/main/java/com/tokenhunter/entity/UsageRecord.java` — 新建，MyBatis-Plus 实体类

核心逻辑：
创建 UsageRecord 实体类，包名 com.tokenhunter.entity。使用 Lombok @Data 自动生成 getter/setter/toString/equals/hashCode。标注 @TableName("usage_record") 映射到 usage_record 表。9 个字段与数据库表严格一一对应：id（@TableId(type = IdType.AUTO)）、provider、model、inputTokens（对应 input_tokens）、outputTokens（对应 output_tokens）、totalTokens（对应 total_tokens）、durationMs（对应 duration_ms）、status、createdAt（对应 created_at）。全部使用包装类型（Long/Integer/String/LocalDateTime），MyBatis-Plus 驼峰转下划线自动映射，未加多余 @TableField。未给 createdAt 设 Java 侧默认值，让数据库 CURRENT_TIMESTAMP 生效。未创建 Mapper、Service、Controller。

测试方法：
1. 运行 `mvn -q -DskipTests compile`，编译通过无报错。
2. 运行 `mvn spring-boot:run`，日志显示 "Started TokenHunterApplication in 0.733 seconds"，端口 8080 正常，Tomcat 启动成功，数据库连接正常。
3. 运行 `curl -s http://localhost:8080/ping`，返回 "TokenHunter is running"。
4. 验证完成后停止应用。

需要理解的知识点：
- MyBatis-Plus @TableName 和 @TableId 的作用
- 驼峰转下划线自动映射规则（camelToUnderscore）
- 包装类型 vs 基本类型的选择（null 语义 vs 默认值）
- Lombok @Data 的组成（@Getter + @Setter + @ToString + @EqualsAndHashCode + @RequiredArgsConstructor）
- 数据库 DEFAULT CURRENT_TIMESTAMP 与 Java 侧默认值的区别

字段对照表：
| 数据库字段       | 数据库类型         | Java 字段      | Java 类型  | 映射方式          |
|------------------|--------------------|----------------|------------|-------------------|
| id               | BIGINT UNSIGNED    | id             | Long       | @TableId(AUTO)    |
| provider         | VARCHAR(32)        | provider       | String     | 驼峰转下划线      |
| model            | VARCHAR(64)        | model          | String     | 驼峰转下划线      |
| input_tokens     | INT UNSIGNED       | inputTokens    | Integer    | 驼峰转下划线      |
| output_tokens    | INT UNSIGNED       | outputTokens   | Integer    | 驼峰转下划线      |
| total_tokens     | INT UNSIGNED       | totalTokens    | Integer    | 驼峰转下划线      |
| duration_ms      | INT UNSIGNED       | durationMs     | Integer    | 驼峰转下划线      |
| status           | TINYINT            | status         | Integer    | 驼峰转下划线      |
| created_at       | DATETIME           | createdAt      | LocalDateTime | 驼峰转下划线   |

---

## Task 1.6：编写 UsageRecordMapper
日期：2026-09-11

修改文件：
- `backend/src/main/java/com/tokenhunter/mapper/UsageRecordMapper.java` — 新建，继承 `BaseMapper<UsageRecord>`

核心逻辑：
创建 UsageRecordMapper 接口，包名 com.tokenhunter.mapper。继承 MyBatis-Plus 的 `BaseMapper<UsageRecord>`，不写任何自定义方法（方法体为空）。接口上加 `@Mapper` 注解，让 MyBatis 在编译时生成该 Mapper 的代理实现类。BaseMapper 已提供 insert、selectList、selectById、updateById 等常用 CRUD 方法，后续需要自定义 SQL 时再在此接口中追加方法。

测试方法：
1. 运行 `mvn -q -DskipTests compile`，编译通过无报错。
2. 运行 `mvn spring-boot:run`，日志显示 "Started TokenHunterApplication in 0.858 seconds"，端口 8080 正常。
3. 启动日志中不再出现 "No MyBatis mapper was found" 警告，说明 Mapper 被正确扫描。
4. 验证完成后停止应用。

需要理解的知识点：
- MyBatis-Plus `BaseMapper<T>` 提供的开箱即用的 CRUD 方法（insert、selectList、selectById、updateById、deleteById 等）
- `@Mapper` 注解的作用（标记接口为 MyBatis Mapper，编译时生成代理实现）
- `@Mapper` vs `@MapperScan` 的区别：`@Mapper` 标注每个 Mapper 接口，`@MapperScan` 批量扫描指定包下的接口，多 Mapper 场景推荐 `@MapperScan`
- MyBatis-Plus 通过代理机制将 BaseMapper 方法自动映射为 SQL 操作

---

## Task 1.7：编写 UsageRecordService
日期：2026-09-11

修改文件：
- `backend/src/main/java/com/tokenhunter/service/UsageRecordService.java` — 新建，Service 层封装

核心逻辑：
创建 UsageRecordService 类，包名 com.tokenhunter.service，类上加 @Service 让 Spring 自动注册为 Bean。通过构造器注入 UsageRecordMapper（Spring 4.3+ 支持单构造器自动注入，无需 @Autowired），避免字段注入导致的隐式依赖和测试困难。提供两个方法：1) save(UsageRecord record) — 调用 mapper.insert(record)，利用 MyBatis-Plus BaseMapper 的 insert 方法自动将对象字段映射为 INSERT 语句，驼峰转下划线自动处理；2) listAll() — 调用 mapper.selectList(null)，null 表示无查询条件，返回全表记录。不写 page()、deleteById()、update() 等方法，不添加 @Transactional 注解，因为 Phase 1 只需要最基础的 CRUD 封装。Service 层的作用是将 Mapper 的底层 CRUD 操作向上屏蔽，Controller 只依赖 Service 而不直接操作 Mapper，后续新增业务逻辑时只需修改 Service 而不需要改动 Controller。

测试方法：
1. 运行 `mvn -q -DskipTests compile`，编译通过无报错。

需要理解的知识点：
- @Service 注解的作用（标注业务逻辑层 Bean，属于 @Component 的特化）
- 构造器注入 vs @Autowired 字段注入的区别（显式依赖、不可变、易测试）
- Service 层的作用（为什么不在 Controller 里直接调 Mapper）
- MyBatis-Plus BaseMapper.insert 的自动映射机制
- selectList(null) 中 null 参数的含义（无查询条件，返回全表）

---

## Task 1.8：编写新增记录接口
日期：2026-09-11

修改文件：
- `backend/src/main/java/com/tokenhunter/controller/UsageRecordController.java` — 新建，REST Controller

核心逻辑：
创建 UsageRecordController 类，包名 com.tokenhunter.controller，标注 @RestController（等价于 @Controller + @ResponseBody）和 @RequestMapping("/api/usage-records") 作为统一路径前缀。通过构造器注入 UsageRecordService。定义一个 POST 方法 addRecord()，用 @PostMapping 映射 POST /api/usage-records，用 @RequestBody 将 HTTP 请求体 JSON 自动反序列化为 UsageRecord 对象（Jackson 根据字段名匹配，驼峰转下划线由 MyBatis-Plus 在 insert 时处理），然后调用 usageRecordService.save(record) 将对象存入数据库。Controller 层只做两件事：接收请求、委托 Service，不包含任何业务逻辑。暂时不返回新记录 ID 或成功消息，Phase 1 验收以数据库是否存为准确。

测试方法：
1. 运行 `mvn -q -DskipTests compile`，编译通过无报错。
2. 运行 `mvn spring-boot:run` 启动应用。
3. 运行 `curl -s -X POST http://localhost:8080/api/usage-records -H "Content-Type: application/json" -d '{"provider":"deepseek","model":"deepseek-chat","inputTokens":10,"outputTokens":20,"totalTokens":30,"durationMs":1500,"status":0}'`，返回空响应体（void 方法），HTTP 状态码 200。
4. 运行 `mysql -u root tokenhunter -e "SELECT * FROM usage_record;"`，确认数据库中出现一条记录，字段值与请求体一致。
5. 验证完成后停止应用。

需要理解的知识点：
- @RestController 与 @Controller + @ResponseBody 的关系
- @RequestMapping 作为类级别路径前缀的作用
- @PostMapping 默认映射 POST 方法，不传 path 时继承类级别的 /api/usage-records
- @RequestBody 如何把 JSON 转为 Java 对象（Jackson 反序列化，字段名逐一对应）
- void 返回值的 HTTP 响应：空 body，状态码 200
- Controller 层职责边界（接收请求 → 委托 Service，不写业务逻辑）

---

## Task 1.9：编写查询列表接口
日期：2026-09-11

修改文件：
- `backend/src/main/java/com/tokenhunter/controller/UsageRecordController.java` — 修改，追加 GET 接口

核心逻辑：
在已有的 UsageRecordController 中追加一个 GET 方法 listRecords()，用 @GetMapping 映射 GET /api/usage-records（继承类级别的 @RequestMapping 前缀），返回类型 List<UsageRecord>。方法体内调用 usageRecordService.listAll()，内部执行 mapper.selectList(null) 查询全表。Spring MVC 自动将 List<UsageRecord> 序列化为 JSON 数组，Content-Type 设为 application/json。新增 import @GetMapping 和 java.util.List，不改动已有的 POST 方法、类注解和注入方式。

测试方法：
1. 运行 `mvn -q -DskipTests compile`，编译通过无报错。
2. 运行 `mysql -u root tokenhunter -e "SELECT id, provider, model, total_tokens FROM usage_record ORDER BY id DESC LIMIT 5;"`，确认数据库中有 id=1 的记录（provider=deepseek, total_tokens=300）。
3. 运行 `mvn spring-boot:run` 启动应用，日志显示 "Started TokenHunterApplication in ~0.8s"，端口 8080 正常。
4. 运行 `curl -i http://localhost:8080/api/usage-records`，返回 HTTP/1.1 200，Content-Type: application/json，响应体为：[{"id":1,"provider":"deepseek","model":"deepseek-chat","inputTokens":100,"outputTokens":200,"totalTokens":300,"durationMs":1500,"status":0,"createdAt":"2026-09-11T21:58:40"}]。
5. 验证完成后停止应用。

需要理解的知识点：
- @GetMapping 的作用（等价于 @RequestMapping(method = RequestMethod.GET)）
- Spring MVC 如何将 Java 对象/集合自动序列化为 JSON 响应（Jackson 的 ObjectMapper）
- @RequestMapping 在类级别定义前缀，方法级别 @GetMapping/@PostMapping 补充 HTTP 方法
- List<UsageRecord> 序列化后为 JSON 数组，每个对象的字段名使用 Java 侧驼峰命名（非数据库下划线）
- LocalDateTime 的 JSON 序列化格式（ISO-8601: yyyy-MM-ddTHH:mm:ss）

---

## Task 1.10：Phase 1 缓冲联调
日期：2026-09-11

修改文件：
- 无修改任何代码文件，仅执行启动、验证、停止操作。

核心逻辑：
启动 Spring Boot 应用（`mvn spring-boot:run`），依次验证 /ping 接口、POST 插入一条 usage_record 记录并核对数据库行数与字段值、GET 查询列表返回完整 JSON 数组，最后停止应用并确认 8080 端口释放。整个过程不修改任何代码，纯联调验证。

测试方法：
1. 运行 `mvn spring-boot:run`，日志显示 "Started TokenHunterApplication in 0.761 seconds"，Tomcat 启动在 8080 端口，无 ERROR/Exception，无 "No MyBatis mapper was found" 警告，Profile "local" 正确激活。
2. 运行 `curl -i http://localhost:8080/ping`，返回 HTTP/1.1 200，响应体 "TokenHunter is running"。
3. 运行 `mysql -u root tokenhunter -e "SELECT COUNT(*) AS before_count FROM usage_record;"`，返回 before_count = 2。
4. 运行 `curl -i -X POST http://localhost:8080/api/usage-records -H "Content-Type: application/json" -d '{"provider":"deepseek","model":"deepseek-chat","inputTokens":111,"outputTokens":222,"totalTokens":333,"durationMs":1234,"status":0}'`，返回 HTTP/1.1 200（响应体为空，void 方法）。
5. 运行 `mysql -u root tokenhunter -e "SELECT COUNT(*) AS after_count FROM usage_record;"`，返回 after_count = 3（= before_count + 1）。
6. 运行 `mysql -u root tokenhunter -e "SELECT id, provider, model, input_tokens, output_tokens, total_tokens, duration_ms, status, created_at FROM usage_record ORDER BY id DESC LIMIT 1;"`，返回 id=3, provider=deepseek, model=deepseek-chat, input_tokens=111, output_tokens=222, total_tokens=333, duration_ms=1234, status=0, created_at=2026-09-11 22:31:27，字段值与 POST 请求完全一致，created_at 自动填充。
7. 运行 `curl -i http://localhost:8080/api/usage-records`，返回 HTTP/1.1 200，Content-Type: application/json，响应体为包含 3 条记录的 JSON 数组，id=3 的记录在其中。
8. 运行 `kill 67499` 停止应用，随后 `lsof -i :8080` 返回 exit code 1（无进程占用），端口释放。

遇到的问题及处理方式：
- HikariPool 连接日志未在启动日志中出现：日志中没有 "HikariPool-1 - Start completed" 字样，但应用启动后数据库查询（MySQL 命令）和 MyBatis-Plus 的 CRUD 操作均正常工作，说明连接池已正常初始化，可能是日志级别或连接懒加载导致未打印。
- POST 接口返回空响应体（Content-Length: 0）：UsageRecordController.addRecord() 返回 void，无响应体，属预期行为，不影响功能。

Phase 1 整体结论：
- ✅ Spring Boot 项目启动成功，端口 8080 正常监听
- ✅ MySQL 数据库连接成功
- ✅ 无 MyBatis mapper 扫描警告
- ✅ /ping 接口返回 HTTP 200，响应体正确
- ✅ POST /api/usage-records 成功插入记录，数据库行数 +1
- ✅ 数据库字段值与请求体一致，created_at 自动填充
- ✅ GET /api/usage-records 返回 HTTP 200，Content-Type application/json，JSON 数组包含刚插入的记录
- ✅ 应用停止后 8080 端口释放
- 验收标准：整条链路顺利跑通，无遗留报错。全部通过。

阻塞事项：
- 无。Phase 1 整体验收完成，等待确认后再决定是否进入 Phase 2。

## Phase 1 总结
日期：2026-09-11

已完成：
- Task 1.1 - 1.10 全部完成并验收通过
- 后端基础链路：/ping、POST /api/usage-records、GET /api/usage-records
- MySQL 表 usage_record 已建，字段与 PLAN.md 第 6 节一致
- JDK 21 + Spring Boot 3.4.4 + MyBatis-Plus 3.5.9

遗留问题：
- 数据库中有若干测试数据（id 1~4），Phase 3 核对统计时需注意
- PLAN.md 第 6 节建表 SQL 未同步新增字段，与 schema.sql 存在不一致，待后续统一
- 已新增错误排查字段（http_status / error_message / endpoint / stream），见"错误字段补丁"章节

下一步：Phase 2 前先冻结几项设计决策，见 DEV_LOG 后续章节。

---

## Task 2.1：添加 DeepSeek 配置
日期：2026-09-11

修改文件：
- `backend/src/main/resources/application.yml` — 修改，增加 server.address、spring.application/profiles、tokenhunter.providers.deepseek
- `backend/src/main/resources/application-local.example.yml` — 修改，追加 tokenhunter.providers.deepseek 占位配置
- `backend/src/main/resources/application-local.yml` — 修改，追加 tokenhunter.providers.deepseek 真实配置（api-key 用环境变量引用）

核心逻辑：
在 application.yml 中增加 server.address=127.0.0.1 使 Tomcat 仅绑定本机回环地址，防止代理接口被局域网滥用；同时补上 spring.application.name 和 spring.profiles.active=local（之前 Task 1.3 后已存在但被 Task 1.10 联调时的版本覆盖，以当前实际文件为准）。在三个配置文件中统一添加 tokenhunter.providers.deepseek 结构：base-url 固定为 https://api.deepseek.com，api-key 在 application.yml 中用 ${DEEPSEEK_API_KEY:}（空默认值作为占位），在 application-local.example.yml 中同样用 ${DEEPSEEK_API_KEY:} 占位，在 application-local.yml 中用 ${DEEPSEEK_API_KEY} 引用环境变量，不硬编码真实 Key。

测试方法：
1. 运行 `mvn -q -DskipTests compile`，编译通过无报错。
2. 运行 `mvn spring-boot:run`，日志显示 "The following 1 profile is active: local"，"Tomcat started on port 8080 (http)"，lsof 显示绑定在 localhost，无 ERROR。
3. 启动后 lsof -i :8080 显示 TCP localhost:http-alt (LISTEN)，确认绑定 127.0.0.1。
4. 验证完成后 kill 进程，lsof -i :8080 返回 exit code 1（端口释放）。

遇到的问题及处理方式：
- 无。三个配置文件一次性修改成功，编译启动均无报错。

阻塞事项：
- 无。Task 2.1 已完成，等待确认后再进入 Task 2.2。

---

## 错误字段补丁
日期：2026-09-12

修改文件：
- `backend/src/main/resources/db/schema.sql` — 修改，在 status 和 created_at 之间新增 4 个字段
- `backend/src/main/java/com/tokenhunter/entity/UsageRecord.java` — 修改，在 status 和 createdAt 之间新增 4 个字段
- `docs/PHASES.md` — 修改，Task 2.9 和 Task 2.10 正文补充新字段写入规则

核心逻辑：
为 usage_record 表新增 4 个排查失败请求的字段：http_status（上游 HTTP 状态码）、error_message（错误摘要，VARCHAR(512)）、endpoint（上游路径）、stream（0=非流式/1=流式）。改动分三步：1) schema.sql 建表 SQL 同步新增 4 个字段定义；2) 通过 ALTER TABLE 对已存在数据库执行 ADD COLUMN，按 status → http_status → error_message → endpoint → stream → created_at 顺序排列；3) Java 实体类 UsageRecord.java 在 status 和 createdAt 之间新增对应 4 个字段（Integer/String 类型），保持 @Data 和 @TableName 不变，利用 MyBatis-Plus 驼峰转下划线自动映射，不加多余 @TableField。4) PHASES.md Task 2.9 补充"失败记录写入 http_status、error_message（截断 512 字符）、endpoint、stream=0"，Task 2.10 补充"超时/连接异常 http_status 为 NULL、error_message 写异常类型"。PLAN.md 第 6 节建表 SQL 未同步修改，避免与 schema.sql 重复维护，后续统一处理。

测试方法：
1. 运行 `mysql -u root tokenhunter -e "SELECT COUNT(*) FROM usage_record;"`，返回 4 条，MySQL 连接正常。
2. 执行 ALTER TABLE 添加 4 个字段，无错误输出。
3. 运行 `mysql -u root tokenhunter -e "DESC usage_record;"`，确认 http_status、error_message、endpoint、stream 四个新字段已存在，类型和位置正确。
4. 运行 `mvn -q -DskipTests compile`，编译通过无报错。
5. 运行 `mvn spring-boot:run` 启动应用，日志显示 "Tomcat started on port 8080 (http) with context path '/'" 和 "Started TokenHunterApplication in 0.791 seconds"。
6. 运行 `curl -s http://localhost:8080/api/usage-records`，返回 JSON 数组，每条记录包含 httpStatus（null）、errorMessage（null）、endpoint（null）、stream（0）四个新字段。
7. 运行 kill 停止应用，确认 8080 端口释放。

需要理解的知识点：
- MySQL ALTER TABLE ADD COLUMN AFTER 的语法，字段在表中的物理排列顺序
- MyBatis-Plus 驼峰转下划线映射（httpStatus → http_status）对新增字段的自动处理
- JSON 序列化中包装类型 Integer 为 null 时输出 JSON null 而非缺失字段
- schema.sql 与 ALTER TABLE 的区别：schema.sql 是新库建表模板，ALTER TABLE 是对已有库的增量变更
- PLAN.md 第 6 节建表 SQL 未同步新增字段，后续需在合适时机统一更新

回退 SQL（如需恢复）：
```sql
ALTER TABLE usage_record DROP COLUMN http_status, DROP COLUMN error_message, DROP COLUMN endpoint, DROP COLUMN stream;
```

阻塞事项：
- PLAN.md 第 6 节建表 SQL 未同步新增 4 个字段，与 schema.sql 存在文档不一致，待后续统一。

错误字段补丁完成，等待你确认后再进入 Task 2.1。

