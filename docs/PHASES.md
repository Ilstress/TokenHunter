# PHASES.md — TokenHunter v0.1 详细任务拆解

> 每个 Task 遵循 DEVELOPMENT_RULES.md 的拆分标准：只做一件事、改动范围可控、能在一次专注时间内完成并验证。按顺序做，不要跳跃。

---

## Phase 1：后端基础

**目标**：Spring Boot 项目能跑起来，MySQL 中能通过接口手动新增和查询一条 `usage_record`。

### Task 1.1 初始化 Spring Boot 项目
- 用 Spring Initializr 创建项目，勾选依赖：Spring Web、MySQL Driver、Lombok（可选）。
- 手动加入 MyBatis-Plus 依赖（Initializr 里通常没有，需要自己在 `pom.xml`/`build.gradle` 里补）。
- 涉及文件：项目根目录（新建）
- 验收标准：项目能在 IDE 里成功导入，`mvn compile` 或 `./gradlew build` 不报错。
- 知识点：Spring Boot 项目结构、Maven/Gradle 依赖管理

### Task 1.2 配置 MySQL 数据源
- 在本地安装/确认 MySQL 已运行，创建数据库 `tokenhunter`。
- 在 `application-local.yml` 中配置数据库连接（URL、用户名、密码）。
- 在 `application.yml` 中通过 `spring.profiles.active` 引用 local 配置。
- 涉及文件：`application.yml`、`application-local.yml`
- 验收标准：项目启动时日志显示数据库连接成功，无连接异常。
- 知识点：Spring Boot 多环境配置（profile）、JDBC 连接字符串

### Task 1.3 编写 /ping 接口验证启动
- 新建一个最简单的 Controller，只返回一个固定字符串。
- 涉及文件：`controller/PingController.java`
- 验收标准：浏览器或 Postman 访问该接口，能拿到预期返回值。
- 知识点：`@RestController`、`@GetMapping` 基本用法

### Task 1.4 执行建表 SQL
- 编写 `db/schema.sql`，包含 `usage_record` 建表语句（见 PLAN.md 第 6 节）。
- 用 MySQL 客户端（命令行或图形工具）手动执行这个脚本。
- 涉及文件：`db/schema.sql`
- 验收标准：数据库里能看到 `usage_record` 表，字段和索引与设计一致。
- 知识点：MySQL 建表语法、索引的作用

### Task 1.5 编写 UsageRecord 实体类
- 创建与表结构一一对应的实体类，字段名、类型与表保持一致。
- 涉及文件：`entity/UsageRecord.java`
- 验收标准：实体类字段与 `usage_record` 表字段能对应上（自己对照检查一遍）。
- 知识点：Java 实体类（POJO）的写法、MyBatis-Plus 的 `@TableName`/`@TableId` 注解

### Task 1.6 编写 UsageRecordMapper
- 继承 MyBatis-Plus 的 `BaseMapper<UsageRecord>`，暂时不写任何自定义方法。
- 涉及文件：`mapper/UsageRecordMapper.java`
- 验收标准：项目能正常启动，Mapper 被正确扫描（可以先写一个简单的启动测试确认没有报错）。
- 知识点：MyBatis-Plus 的 `BaseMapper` 提供了哪些开箱即用的方法（insert/selectList 等）

### Task 1.7 编写 UsageRecordService
- 编写 Service 类，提供"新增一条记录"和"查询记录列表"两个方法，内部调用 Mapper。
- 涉及文件：`service/UsageRecordService.java`
- 验收标准：方法编译通过（暂时还没有接口调用它，先保证代码本身没问题）。
- 知识点：Service 层的作用（为什么不直接在 Controller 里调 Mapper）

### Task 1.8 编写新增记录接口
- `UsageRecordController` 中实现一个 POST 接口，接收请求体，调用 Service 的新增方法。
- 涉及文件：`controller/UsageRecordController.java`
- 验收标准：用 Postman 发送一条 JSON 数据，接口返回成功，数据库里能查到这条记录。
- 知识点：`@PostMapping`、`@RequestBody`、HTTP 请求体与 Java 对象的自动转换

### Task 1.9 编写查询列表接口
- 在同一个 Controller 中实现一个 GET 接口，返回 `usage_record` 表里的所有记录（暂时不做分页）。
- 涉及文件：`controller/UsageRecordController.java`
- 验收标准：Postman 访问该接口，能看到 Task 1.8 插入的数据。
- 知识点：`@GetMapping`、如何把查询结果序列化成 JSON 返回

### Task 1.10（缓冲）联调与问题处理
- 完整跑一遍：启动项目 → POST 插入一条数据 → GET 查询出来，确认链路无误。
- 记录并解决过程中遇到的任何报错（连接失败、字段类型不匹配、序列化异常等）。
- 验收标准：整条链路顺利跑通，没有遗留的报错。

---

## Phase 2：DeepSeek Proxy（非流式）

**目标**：请求经 TokenHunter 转发给 DeepSeek，正确返回结果的同时把 usage 存入数据库。

### Task 2.1 添加 DeepSeek 配置
- 在 `application.yml` 中加入 DeepSeek 的 `base-url` 结构；`api-key` 通过 `application-local.yml` 引用环境变量 `${DEEPSEEK_API_KEY}`，不写明文。
- 涉及文件：`application-local.yml`
- 验收标准：配置文件格式正确，无 YAML 语法错误。
- 知识点：YAML 语法基础

### Task 2.2 编写配置读取类
- 编写 `ProviderProperties` 类，用 `@ConfigurationProperties` 把 yml 里的配置绑定成 Java 对象。
- 涉及文件：`config/ProviderProperties.java`
- 验收标准：在一个简单方法里打印读取到的配置值，确认和 yml 里写的一致。
- 知识点：`@ConfigurationProperties` 的用法

### Task 2.3 编写最基础的转发接口
- 在 `ProxyController` 中实现一个接口，接收客户端请求体，用 `RestTemplate` 或 `WebClient` 转发到 DeepSeek 的对应地址，把响应原样返回。暂时不涉及数据库。
- 涉及文件：`controller/ProxyController.java`、`service/proxy/DeepSeekProxyService.java`
- 验收标准：用 curl 发一个真实的非流式请求（`"stream": false`），能拿到和直接请求 DeepSeek 一样的返回内容。
- 知识点：`RestTemplate`/`WebClient` 的基本用法、HTTP 请求头如何透传（尤其是 Authorization）
- 知识点补充：TokenHunter 用自己的 Provider Key 覆盖客户端 Authorization，客户端不需要传 Key；LM Studio 不传 Authorization。

### Task 2.4 验证转发链路（无统计）
- 多测几种不同的请求内容，确认转发本身稳定可靠，没有丢数据或格式错乱的问题。
- 验收标准：至少 3 次不同内容的请求都能正确转发和返回。

### Task 2.5 解析 usage 字段
- 在拿到 DeepSeek 响应后，从 JSON 中取出 `usage.prompt_tokens`、`usage.completion_tokens`、`usage.total_tokens`。
- 涉及文件：`service/proxy/DeepSeekProxyService.java`
- 验收标准：能打印出正确解析到的三个数字（先用日志确认，不着急存库）。
- 知识点：JSON 解析（Jackson 的 `ObjectMapper` 或类似方式）

### Task 2.6 计算耗时并组装记录对象
- 在请求开始和结束时记录时间戳，算出 `duration_ms`；把 provider、model、token 数、耗时组装成一个 `UsageRecord` 对象。
- 涉及文件：`service/proxy/DeepSeekProxyService.java`
- 验收标准：组装出的对象字段值符合预期（用日志打印确认）。
- 知识点：Java 中获取当前时间戳的方式

### Task 2.7 保存记录到数据库
- 调用 Phase 1 写好的 `UsageRecordService`，把组装好的对象存入数据库。
- 涉及文件：`service/proxy/DeepSeekProxyService.java`
- 验收标准：发一个请求后，数据库里出现一条对应的记录，字段值正确。
- 知识点：Service 之间如何互相调用（依赖注入）

### Task 2.8 验证成功路径完整链路
- 完整测试：发请求 → 拿到正确响应 → 数据库记录正确，跑 3-5 次不同请求确认稳定。
- 验收标准：多次测试均无异常，token 数与手动直接调用 DeepSeek API 得到的数字一致。

### Task 2.9 处理 DeepSeek 错误响应
- 当 DeepSeek 返回 4xx/5xx 状态码时，捕获这个情况，仍然要把错误信息返回给客户端，同时存一条 `status=1` 的记录（token 数记 0）。失败记录除了 status=1，还要写入 http_status（上游返回码）、error_message（错误摘要，截断到 512 字符以内）、endpoint（上游路径）、stream=0。
- 涉及文件：`service/proxy/DeepSeekProxyService.java`
- 验收标准：故意发一个错误请求（比如错误的 API Key），确认返回了错误信息且数据库记录了这次失败。
- 知识点：HTTP 状态码分类（2xx/4xx/5xx 分别代表什么）

### Task 2.10 处理网络超时/连接异常
- 设置合理的超时时间，捕获超时或连接失败异常，同样存一条 `status=1` 的记录，不能让程序直接崩溃。超时/连接异常同样写 status=1，http_status 为 NULL，error_message 写明异常类型（如 SocketTimeoutException / ConnectException），endpoint 和 stream 照常写。
- 涉及文件：`service/proxy/DeepSeekProxyService.java`
- 验收标准：可以通过临时改错 base-url 或断网模拟异常，确认程序能优雅处理不崩溃。
- 知识点：Java 异常处理（try-catch）、超时的概念

### Task 2.11（缓冲）真实场景多次验证
- 用真实 DeepSeek Key，正常使用几次，核对 TokenHunter 记录的 token 数与 DeepSeek 官方后台一致。
- 验收标准：至少 5 次真实调用，数字全部核对一致。

---

## Phase 3：Dashboard 统计接口

**目标**：为前端提供计算好的统计数据接口。

### Task 3.1 编写"今日概况"SQL
- 写一条 SQL：统计当天的 `total_tokens` 求和、记录数、`DISTINCT model` 数量，时间范围用 `created_at >= CURDATE() AND created_at < CURDATE() + INTERVAL 1 DAY`。
- 涉及文件：`mapper/UsageRecordMapper.xml`（或用 MyBatis-Plus 的 QueryWrapper 实现）
- 验收标准：直接在 MySQL 客户端里跑这条 SQL，结果符合预期（自己插入几条测试数据核对）。
- 知识点：SQL 的 `SUM`、`COUNT`、`COUNT(DISTINCT ...)`、`CURDATE()`
- 注意：`SUM` 在没有匹配数据时会返回 `NULL`，需要用 `COALESCE(SUM(total_tokens), 0)` 处理，避免接口返回 null 导致前端报错。

### Task 3.2 封装 summary 接口
- 在 `DashboardService` 里调用 Task 3.1 的查询，组装成一个包含三个字段的返回结果；在 `DashboardController` 里暴露 GET 接口。
- 涉及文件：`service/DashboardService.java`、`controller/DashboardController.java`
- 验收标准：Postman 访问接口，返回的数字与数据库里的真实数据一致。
- 知识点：如何把多个统计结果组装成一个 JSON 返回（可以用 Map 或简单的内部类）

### Task 3.3 编写趋势查询 SQL
- 写一条按天分组的 SQL：`GROUP BY DATE(created_at)`，统计每天的 `total_tokens` 总和，支持传入天数范围（7 或 30）。
- 涉及文件：`mapper/UsageRecordMapper.xml`
- 验收标准：手动跑 SQL，能看到按日期分组的正确结果。
- 知识点：SQL 的 `GROUP BY`、日期范围过滤（`WHERE created_at >= ...`）
- 注意：某一天没有记录时，后端需要补零，保证返回的日期序列连续；时间范围过滤建议用 `created_at >= ? AND created_at < ?` 的范围查询，避免对 `created_at` 加函数（如 `DATE(created_at) = ?`）导致索引失效。

### Task 3.4 封装 trend 接口
- Service 中调用 Task 3.3 的查询，Controller 暴露接口，支持 `range=7d` 或 `range=30d` 参数。
- 涉及文件：`service/DashboardService.java`、`controller/DashboardController.java`
- 验收标准：分别用 `range=7d` 和 `range=30d` 请求，返回天数正确、数字正确。
- 知识点：`@RequestParam` 接收 URL 参数

### Task 3.5 编写模型占比查询
- 写 SQL 按 `model` 分组统计 `total_tokens` 总和，用于计算占比。
- 涉及文件：`mapper/UsageRecordMapper.xml`
- 验收标准：手动跑 SQL 结果正确。

### Task 3.6 封装 model-usage 接口
- Service + Controller 封装成接口，返回每个模型的 token 数（占比可以在前端算，也可以后端直接算好百分比）。
- 涉及文件：`service/DashboardService.java`、`controller/DashboardController.java`
- 验收标准：接口返回结果与手工核算一致。
- 说明：v0.1 不做模型 ID 归一化，`model` 字段值相同才归为一组；不同名称但实际同一模型的归一化处理留到 v0.5+（见 PLAN.md 第 12 节）。

### Task 3.7 编写 Provider 占比查询与接口
- 和 Task 3.5/3.6 逻辑一样，把 `model` 换成 `provider` 分组。
- 涉及文件：`mapper/UsageRecordMapper.xml`、`service/DashboardService.java`、`controller/DashboardController.java`
- 验收标准：接口返回结果正确。
- 说明：这个 Task 和前两个高度相似，可以直接复用/照抄写法，不需要重新设计。

### Task 3.8 处理空数据边界情况
- 测试数据库完全没有记录时，四个接口分别调用一次，确认不会报错，而是返回合理的空结构（比如 0、空数组）。
- 涉及文件：`service/DashboardService.java`
- 验收标准：清空表数据后调用所有接口，均正常返回而不是 500 错误。
- 知识点：SQL 聚合函数在无数据时的返回值（比如 `SUM` 返回 `NULL` 而不是 0，需要处理）

### Task 3.9（缓冲）全面核对
- 插入一批覆盖多天、多模型、多 Provider 的测试数据，逐个接口手工核对数字。
- 验收标准：所有接口的数字都能通过手工计算验证无误。

---

## Phase 4：前端 Dashboard

**目标**：Dashboard 页面直接对接真实后端数据，能完整展示统计信息。

### Task 4.1 初始化 Vue3 + Vite 项目
- 用 `npm create vite@latest` 创建 Vue3 + TypeScript 项目。
- 涉及文件：项目根目录（新建）
- 验收标准：`npm run dev` 能启动，浏览器看到默认页面。
- 知识点：Vite 项目结构、`npm run dev` 的作用

### Task 4.2 安装并配置 Element Plus
- 安装依赖，在 `main.ts` 里全局引入。
- 涉及文件：`main.ts`
- 验收标准：页面上放一个 Element Plus 的按钮组件能正常显示样式。
- 知识点：Vue 插件的注册方式（`app.use()`）

### Task 4.3 安装并配置 ECharts
- 安装 ECharts，确认能在一个组件里渲染出一个最简单的示例图表。
- 涉及文件：任意一个测试用的 `.vue` 文件（可以之后删除）
- 验收标准：页面上能看到一个 ECharts 渲染出来的图表（哪怕是官方示例数据）。
- 知识点：ECharts 在 Vue 里的挂载方式（通常需要一个 DOM 容器 + `echarts.init()`）

### Task 4.4 搭建路由和基础布局
- 配置 Vue Router，创建 `DashboardView.vue` 作为首页；搭好 `App.vue` 的基本布局（顶部标题 + 内容区）。
- 涉及文件：`router/index.ts`、`App.vue`、`views/DashboardView.vue`
- 验收标准：访问首页能看到基础布局框架，无路由报错。
- 知识点：Vue Router 基本配置

### Task 4.5 封装接口请求方法
- 用 `fetch` 或 `axios` 封装对后端各个统计接口的请求方法。
- 涉及文件：`api/dashboard.ts`
- 验收标准：在浏览器控制台手动调用这些方法，能拿到后端真实返回的数据。
- 知识点：`fetch`/`axios` 基本用法、异步请求（`async/await`）

### Task 4.6 SummaryCards 静态布局
- 先用固定假数据搭好四个卡片的 UI（今日 Token/请求数/模型数），不接接口。
- 涉及文件：`components/SummaryCards.vue`
- 验收标准：页面上能看到四个卡片样式正确。

### Task 4.7 SummaryCards 接入真实数据
- 用 Task 4.5 封装的方法请求 summary 接口，把返回数据渲染到卡片里。
- 涉及文件：`components/SummaryCards.vue`
- 验收标准：卡片上显示的数字和后端接口返回的数字一致。
- 知识点：Vue 的响应式数据（`ref`/`reactive`）、组件挂载时发请求（`onMounted`）

### Task 4.8 TokenTrendChart 静态图表
- 先用假数据画出一个折线图的样式。
- 涉及文件：`components/TokenTrendChart.vue`
- 验收标准：能看到折线图，样式基本符合预期。

### Task 4.9 TokenTrendChart 接入真实数据
- 请求 trend 接口，把返回数据传给图表渲染；实现 7 天/30 天切换（可以先做一个简单的按钮或下拉框）。
- 涉及文件：`components/TokenTrendChart.vue`
- 验收标准：切换时间范围，图表数据能正确变化。

### Task 4.10 ModelUsagePie 组件
- 请求 model-usage 接口，渲染成饼图。
- 涉及文件：`components/ModelUsagePie.vue`
- 验收标准：饼图各分片占比与接口返回数据一致。

### Task 4.11 ProviderUsagePie 组件
- 和 Task 4.10 逻辑一致，换成 provider-usage 接口。
- 涉及文件：`components/ProviderUsagePie.vue`
- 验收标准：饼图数据正确。

### Task 4.12 RecentRequestsTable 组件
- 请求 usage-records 列表接口，用 Element Plus 的表格组件展示（时间、Provider、模型、Token 数等字段）。
- 涉及文件：`components/RecentRequestsTable.vue`
- 验收标准：表格内容与数据库真实记录一致，字段对齐正确。

### Task 4.13 处理 loading / 空数据 / 错误状态
- 给每个组件加上：请求中的加载提示、没有数据时的友好提示、请求失败时的错误提示，不能白屏或直接报错。
- 涉及文件：`components/*.vue`（逐个检查）
- 验收标准：手动模拟没有数据的情况（清空数据库）和后端未启动的情况，页面都能给出合理提示而不是崩溃。
- 知识点：Vue 中的条件渲染（`v-if`/`v-else`）

### Task 4.14（缓冲）整体样式统一与联调
- 统一间距、字体、颜色风格；完整走一遍从打开页面到看到所有数据的流程。
- 验收标准：页面整体观感一致，没有明显的样式错乱。

---

## Phase 5（可选）：LM Studio Proxy

**目标**：为 LM Studio 实现和 DeepSeek 类似的转发与统计能力。

### Task 5.1 添加 LM Studio 配置
- 在 `application-local.yml` 中加入 LM Studio 的 `base-url`（本地地址，如 `http://localhost:1234`）。
- 涉及文件：`application-local.yml`
- 验收标准：配置格式正确。

### Task 5.2 编写 LM Studio 转发服务
- 参照 `DeepSeekProxyService` 的写法，新建 `LmStudioProxyService`，允许代码有重复，不用刻意复用。
- 涉及文件：`service/proxy/LmStudioProxyService.java`、`controller/ProxyController.java`
- 验收标准：能成功转发一个请求到本地 LM Studio 并拿到响应。

### Task 5.3 处理 usage 字段缺失的情况
- 部分本地模型不返回 `usage` 字段，这种情况下 token 数存 0，但依然要记录这次调用发生过（provider、model、耗时正常记录）。
- 涉及文件：`service/proxy/LmStudioProxyService.java`
- 验收标准：用一个不返回 usage 的本地模型测试，确认记录能正常存入（token 数为 0，不报错）。

### Task 5.4 验证完整链路
- 多次测试不同的本地模型请求，确认转发稳定、记录准确、对本地推理速度的影响可以接受。
- 验收标准：至少 3 次不同请求测试通过，Dashboard 上能看到 LM Studio 的数据正常展示。

---

## 使用说明

- 严格按 Task 编号顺序执行，不要因为"看起来简单"就跳过验收标准。
- 每完成一个 Task，按 DEVELOPMENT_RULES.md 第七节的四项模板（修改文件 / 核心逻辑 / 测试方法 / 需要理解的知识点）写进 `docs/DEV_LOG.md`。
- 标记了"（缓冲）"的 Task 是留给踩坑和补测的时间，不要提前跳过，也不要因为顺利就删掉这个环节——第一次踩坑通常发生在你意想不到的地方。
