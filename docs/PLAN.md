# PLAN.md — TokenHunter v0.1 项目开发计划

## 1. 项目背景

个人同时使用 DeepSeek API、LM Studio 等多个 AI 服务，缺乏统一的 Token 使用统计入口。TokenHunter 是一个**本地优先、数据完全自持**的个人 AI 使用分析工具：通过代理转发 AI 请求，自动记录每次调用的 Token 消耗，并用 Dashboard 展示统计结果。

**产品定位**：个人使用监控/分析工具。不是聊天客户端，不是 Agent 平台，不是云端 SaaS。所有数据保存在本机 MySQL，不上传云端，不做用户系统。

---

## 2. 项目目标

用户无需改变自己的 AI 使用习惯，通过 Proxy 自动记录 AI 请求，Dashboard 展示：

- 今日 Token 使用量、请求次数、使用的模型数量
- 最近 7/30 天的 Token 使用趋势
- 不同模型、不同 Provider 的 Token 消耗占比
- 最近请求的详细记录列表

---

## 3. 技术栈

| 层 | 技术 |
|---|---|
| Frontend | Vue 3 + TypeScript + Vite + Element Plus + ECharts |
| Backend | Spring Boot 3 + Java 21 + MyBatis-Plus |
| Database | MySQL 8（单机本地运行） |
| 部署形态 | 开发期前后端分离；最终打包为 Spring Boot 内嵌 Vue 静态资源，单进程运行 |

---

## 4. 系统架构

```
        Vue Dashboard (只读展示)
               │  REST API
        Spring Boot 单体应用
     ┌──────────┬──────────────┐
     │ Dashboard │  AI Proxy    │
     │  接口      │  (转发+统计)  │
     └──────────┴──────────────┘
               │
             MySQL
       （一张表：usage_record）
               │
      ┌────────┴────────┐
   DeepSeek API      LM Studio
```

Proxy 部分直接为 DeepSeek 和 LM Studio 各写一个转发方法，逻辑上允许有少量重复，不做提前的接口抽象。Provider 的 Base URL、API Key 等配置直接写在后端配置文件里，不建配置管理表，不做配置管理页面。

v0.1 只支持**非流式**请求的转发与统计（客户端请求体中 `stream: false`）。流式（SSE）请求的转发与 usage 解析留待后续版本。

---

## 5. 目录结构

### 5.1 后端（Spring Boot）

```
tokenhunter-backend/
├── src/main/java/com/tokenhunter/
│   ├── TokenHunterApplication.java
│   ├── config/
│   │   ├── MybatisPlusConfig.java
│   │   ├── CorsConfig.java
│   │   └── ProviderProperties.java   # 从 application.yml 读取 Provider 配置
│   ├── controller/
│   │   ├── DashboardController.java
│   │   ├── UsageRecordController.java
│   │   └── ProxyController.java      # /proxy/deepseek/**、/proxy/lmstudio/**
│   ├── service/
│   │   ├── DashboardService.java
│   │   ├── UsageRecordService.java
│   │   └── proxy/
│   │       ├── DeepSeekProxyService.java
│   │       └── LmStudioProxyService.java
│   ├── mapper/
│   │   └── UsageRecordMapper.java
│   ├── entity/
│   │   └── UsageRecord.java
│   └── exception/
│       └── GlobalExceptionHandler.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml         # 不进 git，存放本机 MySQL 连接与 API Key
│   └── db/schema.sql                 # 手动执行一次的建表脚本
└── src/test/java/...
```

### 5.2 前端（Vue 3）

```
tokenhunter-frontend/
├── src/
│   ├── views/
│   │   └── DashboardView.vue
│   ├── components/
│   │   ├── SummaryCards.vue
│   │   ├── TokenTrendChart.vue
│   │   ├── ModelUsagePie.vue
│   │   ├── ProviderUsagePie.vue
│   │   └── RecentRequestsTable.vue
│   ├── api/
│   │   ├── dashboard.ts
│   │   └── usageRecord.ts
│   ├── types/
│   │   └── index.ts
│   ├── stores/
│   │   └── dashboard.ts              # Pinia
│   ├── router/
│   │   └── index.ts
│   └── App.vue
└── vite.config.ts
```

---

## 6. 数据库设计

只使用一张业务表：

```sql
CREATE TABLE usage_record (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    provider        VARCHAR(32)  NOT NULL,   -- deepseek / lmstudio
    model           VARCHAR(64)  NOT NULL,
    input_tokens    INT UNSIGNED DEFAULT 0,
    cache_hit_tokens  INT UNSIGNED DEFAULT 0,   -- 命中缓存的输入 token，无缓存概念时存 0
    cache_miss_tokens INT UNSIGNED DEFAULT 0,   -- 未命中缓存的输入 token，无缓存概念时存 0
    output_tokens   INT UNSIGNED DEFAULT 0,
    total_tokens    INT UNSIGNED DEFAULT 0,
    duration_ms     INT UNSIGNED DEFAULT 0,
    status          TINYINT      NOT NULL DEFAULT 0,  -- 0=成功 1=失败
    http_status     INT NULL,                    -- 上游返回的 HTTP 状态码，成功路径也记录
    error_message   VARCHAR(512) NULL,           -- 失败时的错误摘要，成功时为 NULL
    endpoint        VARCHAR(128) NULL,           -- 请求的上游路径，如 /v1/chat/completions
    stream          TINYINT NOT NULL DEFAULT 0,  -- 0=非流式 1=流式
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    KEY idx_created_at (created_at),
    KEY idx_provider_model (provider, model)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='每次 AI 调用的使用记录，不保存 prompt/response 内容';
```

**索引说明**：
- `idx_created_at`：Dashboard 趋势图、最近请求列表都按时间过滤/排序，是最高频查询。
- `idx_provider_model`：模型占比、Provider 占比统计按此聚合。

**建表方式**：手动执行一次 `db/schema.sql`，不引入数据库迁移工具。

**Provider 配置**（不建表，写在 `application.yml`）：

```yaml
tokenhunter:
  providers:
    deepseek:
      base-url: https://api.deepseek.com
      api-key: ${DEEPSEEK_API_KEY:}
    lmstudio:
      base-url: http://localhost:1234
      api-key: not-needed
```

`api-key` 不写明文，通过环境变量注入，真实值放在 `application-local.yml`。

---

### 6.5 统计口径与数据来源

- **口径定义**：v0.1 的"Token 消耗"以 `input_tokens + output_tokens` 为基础口径，`total_tokens` 取 Provider 返回值或用两者相加校验。
- **缓存 token 的采集**：DeepSeek 等支持 Prompt Caching 的 Provider 会返回 `prompt_cache_hit_tokens` 和 `prompt_cache_miss_tokens`，TokenHunter 分别存入 `cache_hit_tokens` 和 `cache_miss_tokens` 字段。无缓存概念的 Provider（如 LM Studio）两个字段存 0。
- **缓存 Token 的预留**：部分 Provider（尤其是支持 Prompt Caching 的模型）会额外返回 `cache_hit`/`cache_miss`/`cache_creation`/`cache_read` 等 token 字段，v0.1 已建列、已采集，但不做费用计算。未来若要按缓存价格分别计费，需要在 v0.2 引入定价配置。
- **v0.1 范围**：采集 `input_tokens`、`cache_hit_tokens`、`cache_miss_tokens`、`output_tokens`、`total_tokens`，其中 `input_tokens = cache_hit_tokens + cache_miss_tokens`（有缓存时）；无缓存时后两者为 0。
- **数据来源**：v0.1 只统计经过 TokenHunter Proxy 转发的请求，不扫描本地 CLI 会话文件，不导入任何历史数据。
- **单位与时区**：所有 Token 数均为整数；时间统一按 `Asia/Shanghai` 存储与展示。

---

## 7. API 设计

| Method | Path | 说明 |
|---|---|---|
| GET | `/api/dashboard/summary` | 今日 Token / 请求数 / 模型数 |
| GET | `/api/dashboard/trend?range=7d\|30d` | Token 使用趋势 |
| GET | `/api/dashboard/model-usage` | 按模型聚合占比 |
| GET | `/api/dashboard/provider-usage` | 按 Provider 聚合占比 |
| GET | `/api/usage-records?page=&size=` | 最近请求分页列表 |
| POST | `/proxy/deepseek/**` | 转发到 DeepSeek，落库统计 |
| POST | `/proxy/lmstudio/**` | 转发到 LM Studio，落库统计 |

**trend 接口粒度规则**：
- `range=7d`、`range=30d` 时按天返回一个数据点；未来若支持 `range=today`，按小时返回。
- 某天（或某小时）没有记录时，由**后端补零**，保证返回的时间序列连续，避免前端折线图出现断点。
- 时间字段统一使用 ISO-8601 格式，例如 `2026-09-11T21:58:40`。

---

## 8. 开发阶段与任务拆解

### Phase 1：后端基础（预计 3-4 天）

**目标**：Spring Boot 项目跑通，MySQL 中能手动插入/查询一条 `usage_record`。

- Day 1：搭建 Spring Boot 3 项目（Web + MyBatis-Plus 依赖），配置 MySQL 连接，跑通一个最简单的 `/ping` 接口。
- Day 2：执行 `schema.sql` 建表，写好 `UsageRecord` 实体类和 Mapper。
- Day 3：写 `UsageRecordController`，实现新增记录、查询列表两个接口，用 Postman 测试通过。
- Day 4（缓冲）：处理环境问题（连接失败、字段类型报错等）。

**验收标准**：Postman 手动 POST 一条数据能存入数据库；GET 接口能把这条数据列出来。

---

### Phase 2：DeepSeek Proxy（非流式，预计 3-4 天）

**目标**：客户端请求经 TokenHunter 转发给 DeepSeek，正确返回结果的同时把 usage 存入数据库。

- Day 1：写最基础的转发接口——接收请求体，用 `RestTemplate`/`WebClient` 转发到 DeepSeek，原样返回响应。先不管统计，确认转发本身工作正常。
- Day 2：解析 DeepSeek 响应中的 `usage` 字段（`prompt_tokens`/`completion_tokens`/`total_tokens`），存入 `usage_record`。
- Day 3：处理异常情况——DeepSeek 返回错误状态码、网络超时，落一条 `status=1` 的记录，不能让程序崩溃。
- Day 4（缓冲）：用真实 DeepSeek Key 多次测试，核对统计数字与 DeepSeek 官方后台一致。

**验收标准**：发一个 `stream: false` 的请求，走完整个链路后数据库出现一条准确记录，token 数与官方一致。

---

### Phase 3：Dashboard 统计接口（预计 2-3 天）

**目标**：为前端提供计算好的统计数据。

- Day 1：写"今日 Token 总量 / 请求数 / 模型数"接口（用 `created_at >= CURDATE() AND created_at < CURDATE() + INTERVAL 1 DAY` 范围查询，避免 `DATE(created_at)` 导致索引失效）。
- Day 2：写"最近 7/30 天趋势"接口和"按模型/Provider 聚合占比"接口。
- Day 3（缓冲）：处理边界情况——没有任何记录时，接口要返回空数据结构而不是报错。

**验收标准**：手动插入不同日期、不同模型的记录后，各统计接口返回结果与手工核算一致。

---

### Phase 4：前端 Dashboard（预计 4-5 天）

**目标**：Dashboard 页面直接对接真实后端数据。

- Day 1：搭建 Vue3 + Vite + Element Plus 项目骨架。
- Day 2：SummaryCards 组件，对接 summary 接口。
- Day 3：TokenTrendChart（ECharts 折线图），对接 trend 接口。
- Day 4：ModelUsagePie / ProviderUsagePie（饼图）+ RecentRequestsTable，对接对应接口。
- Day 5（缓冲）：样式调整、空数据状态处理。

**验收标准**：打开页面能看到今日统计卡片、趋势图、占比图、最近请求列表，数据全部来自真实后端。

---

### Phase 5（可选）：LM Studio Proxy（预计 1-2 天）

**目标**：复用 Phase 2 的写法，为 LM Studio 实现转发逻辑。

- LM Studio 走 OpenAI 兼容协议，转发逻辑与 DeepSeek 类似，主要差异是 base-url 不同。
- 部分本地模型/推理框架可能不返回 `usage` 字段，遇到这种情况该条记录的 token 数存 0，不需要额外设计新字段。

**验收标准**：LM Studio 请求经代理后延迟增量可接受，能记录到请求次数，即便部分记录没有 token 数据。

---

## 9. 总体时间预估

| Phase | 内容 | 预计天数 |
|---|---|---|
| Phase 1 | 后端基础 | 3-4 天 |
| Phase 2 | DeepSeek Proxy（非流式） | 3-4 天 |
| Phase 3 | Dashboard 统计接口 | 2-3 天 |
| Phase 4 | 前端 Dashboard | 4-5 天 |
| Phase 5（可选） | LM Studio Proxy | 1-2 天 |
| **合计** | | **约 13-18 天**（业余时间） |

---

## 10. 工程规范

### Git / Commit
- 分支：`main`（稳定）+ `dev`（日常开发），每个 Phase 结束后合并。
- Commit 格式：`feat(proxy): 实现 DeepSeek 转发`、`fix(dashboard): 修复趋势图空数据报错`（type: feat/fix/refactor/docs/test/chore）。
- `.gitignore` 必须排除：`application-local.yml`、任何含 API Key 的配置文件、MySQL dump 文件。

### 项目文档结构
```
docs/
├── PLAN.md              # 本文档
├── API.md               # 接口文档，随开发同步更新
├── DEV_LOG.md            # 开发过程记录（每个 Task 完成后的总结）
└── DEVELOPMENT_RULES.md  # AI 辅助开发规范
```

### 测试策略
- 每个接口开发完成后，用 Postman 手动验证一次，确认输入输出符合预期。
- Proxy 转发逻辑重点验证异常路径（超时、上游报错），不能只测成功路径。
- 前端不追求测试覆盖率，核心图表组件能正确渲染真实数据即可。

---

## 11. v0.1 不做的功能

以下功能不在 v0.1 范围内：

- 流式（streaming）请求的转发与 usage 解析
- Provider 抽象接口层（多 Provider 通用接口设计）
- Provider 配置数据库表与管理页面（配置写在 `application.yml`）
- 数据导出（CSV/JSON）
- 数据删除功能（清空/按时间删除）
- 数据库迁移工具（Flyway 等）
- Tauri 打包为桌面 App
- Average Speed（tokens/s）等衍生指标
- 用户系统、登录鉴权、云同步
- Prompt/完整对话内容保存
- 复杂 AI 分析（Prompt 质量评分、Agent Trace 等）

---

## 12. 后续版本规划（v0.1 完成后再考虑）

- **v0.2**：流式请求支持（SSE 透传 + usage 旁路解析），这是唯一值得专门攻克的技术难点。
  - **费用统计**：引入定价配置（provider + model → input / output / cache_hit / cache_miss 单价），在查询时计算费用；Dashboard 增加"今日费用""费用趋势""按模型费用占比"卡片。定价会随时间变化，v0.2 设计时需要决定是否记录 price_snapshot。
- **v0.3**：数据导出、清空数据的前端功能。
- **v0.4**：Provider 配置从 `application.yml` 迁移到数据库 + 管理页面。
- **v0.5+**：接入更多 Provider（Ollama、OpenAI 等），此时再考虑做 Provider 抽象层。
  - **模型 ID 归一化**：接入多个 Provider 后，同一模型在不同来源下可能出现不同名称（如 `deepseek-chat` 与 `deepseek-chat-v2`），需要在统计层做归一化映射，确保模型占比统计准确；v0.1 不做。
- **打包**：评估 Tauri 封装为本地桌面应用的可行性。

---

## 13. 风险与待确认事项

1. DeepSeek 是否对本地代理转发有 IP/来源限制，建议 Phase 2 开始前先用 curl 验证直连可行性。
2. LM Studio 的 usage 字段支持情况因加载的模型/推理引擎而异，需要在 Phase 5 前实测确认。
3. `application-local.yml` 中的 API Key 明文存储是本地单机工具下的合理简化，但需确保该文件不会被误提交到 git 或分享给他人。
