# API.md — TokenHunter API 文档

本文档随开发同步更新。所有接口默认返回 application/json，时间字段为 ISO-8601 格式。

## 基础信息

- 后端地址：http://127.0.0.1:8080
- 前端地址：http://localhost:5173
- 时区：Asia/Shanghai
- 鉴权：无（本地工具）

## 健康检查

### GET /api/ping

返回字符串 "TokenHunter is running"。

    curl http://127.0.0.1:8080/api/ping

## Dashboard 统计接口

所有统计接口只统计 status=0 的成功记录。

### GET /api/dashboard/summary

今日概况。

响应示例：

    {
      "totalTokens": 1050,
      "requestCount": 3,
      "modelCount": 2
    }

字段说明：
- totalTokens：今日成功请求的 total_tokens 总和
- requestCount：今日成功请求数
- modelCount：今日使用过的不同模型数量

### GET /api/dashboard/trend

按天统计 Token 趋势。某天无记录时由后端补零，保证日期序列连续。

参数：
- range：7d 或 30d，默认 7d

响应示例：

    [
      {"day": "2026-09-08", "totalTokens": 0},
      {"day": "2026-09-09", "totalTokens": 100},
      {"day": "2026-09-10", "totalTokens": 0}
    ]

### GET /api/dashboard/model-usage

按模型统计 Token 占比。全部历史数据，不按时间过滤。

响应示例：

    [
      {"model": "deepseek-chat", "totalTokens": 3524, "percentage": 85.45},
      {"model": "deepseek-reasoner", "totalTokens": 600, "percentage": 14.55}
    ]

### GET /api/dashboard/provider-usage

按 Provider 统计 Token 占比。

响应示例：

    [
      {"provider": "deepseek", "totalTokens": 1350, "percentage": 84.38},
      {"provider": "lmstudio", "totalTokens": 250, "percentage": 15.63}
    ]

## 使用记录接口

### GET /api/usage-records

返回全部使用记录，不分页。

响应示例：

    [
      {
        "id": 50,
        "provider": "deepseek",
        "model": "deepseek-chat",
        "inputTokens": 10,
        "cacheHitTokens": 0,
        "cacheMissTokens": 10,
        "outputTokens": 1,
        "totalTokens": 11,
        "durationMs": 1500,
        "status": 0,
        "httpStatus": 200,
        "errorMessage": null,
        "endpoint": "/v1/chat/completions",
        "stream": 0,
        "createdAt": "2026-09-15T15:48:19"
      }
    ]

## 代理接口

### POST /proxy/deepseek/**

转发到 DeepSeek。客户端把 base_url 从 https://api.deepseek.com 改成 http://127.0.0.1:8080/proxy/deepseek。

请求示例：

    curl -X POST http://127.0.0.1:8080/proxy/deepseek/v1/chat/completions \
      -H "Content-Type: application/json" \
      -d '{"model":"deepseek-chat","stream":false,"messages":[{"role":"user","content":"hi"}]}'

说明：
- TokenHunter 用 application.yml 中配置的 API Key 覆盖客户端的 Authorization 头
- 客户端无需自带 Key
- 只支持 stream: false

### POST /proxy/lmstudio/**

转发到 LM Studio。客户端把 base_url 从 http://localhost:1234 改成 http://127.0.0.1:8080/proxy/lmstudio。

说明：
- LM Studio 不需要 API Key
- 部分模型不返回 usage 字段，此时记录的 token 数为 0
- 只支持 stream: false

## 错误处理

- 上游返回 4xx/5xx：TokenHunter 原样透传状态码和 body，同时记录 status=1 的失败记录，http_status 为上游状态码
- 网络异常（连接失败、超时）：TokenHunter 返回 502 + JSON {"error":"upstream_unreachable","message":"..."}，同时记录 status=1 的失败记录，http_status 为 null

失败记录的 error_message 字段包含错误摘要，截断到 512 字符以内。

## 数据模型

### usage_record 表

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT UNSIGNED | 主键，自增 |
| provider | VARCHAR(32) | deepseek 或 lmstudio |
| model | VARCHAR(64) | 模型名 |
| input_tokens | INT UNSIGNED | 输入 token 总数（含缓存命中部分） |
| cache_hit_tokens | INT UNSIGNED | 缓存命中的输入 token |
| cache_miss_tokens | INT UNSIGNED | 缓存未命中的输入 token |
| output_tokens | INT UNSIGNED | 输出 token |
| total_tokens | INT UNSIGNED | 总 token |
| duration_ms | INT UNSIGNED | 耗时（毫秒） |
| status | TINYINT | 0=成功 1=失败 |
| http_status | INT | 上游 HTTP 状态码，网络异常时为 NULL |
| error_message | VARCHAR(512) | 失败原因摘要 |
| endpoint | VARCHAR(128) | 上游路径，如 /v1/chat/completions |
| stream | TINYINT | 0=非流式（v0.1 只支持 0） |
| created_at | DATETIME | 创建时间，数据库自动填充 |

## 限制

- v0.1 只支持非流式请求（stream: false），stream: true 时上游会返回错误
- GET /api/usage-records 不分页，返回全部记录，数据量大时可能慢
- GET /api/dashboard/trend 返回的日期序列由后端补零
- 统计接口的百分比四舍五入保留 2 位小数，多扇区之和可能为 100.00% ± 0.01%
