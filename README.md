# TokenHunter

> 本地优先、数据完全自持的个人 AI 使用分析工具

TokenHunter 是一个通过代理转发 AI 请求、自动记录 Token 消耗、并用 Dashboard 展示统计结果的本地工具。支持 DeepSeek API 和 LM Studio 两个 Provider。

## 功能特性

- **代理转发**：DeepSeek API + LM Studio，自动记录每次调用的 Token 消耗
- **数据统计**：今日概况、7/30 天趋势、模型占比、Provider 占比
- **可视化 Dashboard**：Vue 3 + Element Plus + ECharts
- **数据自持**：所有数据存本地 MySQL，不上传云端

## 技术栈

| 层 | 技术 |
|---|---|
| Frontend | Vue 3 + TypeScript + Vite + Element Plus + ECharts |
| Backend | Spring Boot 3.4.4 + Java 21 + MyBatis-Plus 3.5.9 |
| Database | MySQL 8 |

## 快速开始

### 前置要求

Java 21+、Maven 3.9+、Node.js 20+、pnpm 9+、MySQL 8

### 1. 准备数据库

    mysql -u root -e "CREATE DATABASE IF NOT EXISTS tokenhunter DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    mysql -u root tokenhunter < backend/src/main/resources/db/schema.sql

### 2. 配置环境变量

    export DEEPSEEK_API_KEY=sk-xxxxxxxxxxxx
    export DB_USERNAME=root
    export DB_PASSWORD=your-password
    export LMSTUDIO_BASE_URL=http://localhost:1234

### 3. 启动后端

    cd backend
    mvn spring-boot:run

后端监听 http://127.0.0.1:8080。

### 4. 启动前端

    cd frontend
    pnpm install
    pnpm dev

前端监听 http://localhost:5173。

### 5. 使用代理

把 AI 客户端的 base_url 指向 TokenHunter：

- DeepSeek：把 base_url 从 https://api.deepseek.com 改成 http://127.0.0.1:8080/proxy/deepseek
- LM Studio：把 base_url 从 http://localhost:1234 改成 http://127.0.0.1:8080/proxy/lmstudio

示例：

    curl -X POST http://127.0.0.1:8080/proxy/deepseek/v1/chat/completions \
      -H "Content-Type: application/json" \
      -d '{"model":"deepseek-chat","stream":false,"messages":[{"role":"user","content":"hi"}]}'

### 6. 打开 Dashboard

浏览器访问 http://localhost:5173/

## API 一览

| Method | Path | 说明 |
|---|---|---|
| GET | /api/ping | 健康检查 |
| GET | /api/dashboard/summary | 今日 Token / 请求数 / 模型数 |
| GET | /api/dashboard/trend?range=7d或30d | Token 使用趋势 |
| GET | /api/dashboard/model-usage | 按模型聚合占比 |
| GET | /api/dashboard/provider-usage | 按 Provider 聚合占比 |
| GET | /api/usage-records | 最近请求列表 |
| POST | /proxy/deepseek/** | 转发到 DeepSeek，落库统计 |
| POST | /proxy/lmstudio/** | 转发到 LM Studio，落库统计 |

详细 API 文档见 docs/API.md。

## 配置说明

| 环境变量 | 用途 | 必填 |
|---|---|---|
| DEEPSEEK_API_KEY | DeepSeek API 密钥 | 是 |
| DB_USERNAME | MySQL 用户名 | 是 |
| DB_PASSWORD | MySQL 密码 | 是 |
| LMSTUDIO_BASE_URL | LM Studio 地址 | 否，默认 http://localhost:1234 |

## 已知限制（v0.1）

- 只支持非流式请求（stream: false）
- 不支持数据导出、清空、删除
- 无用户系统、无鉴权（本地工具定位）
- 代理只绑定 127.0.0.1，仅本机可访问

## 开发文档

- docs/PLAN.md — 项目计划与设计
- docs/PHASES.md — 详细任务拆解
- docs/DEV_LOG.md — 开发日志
- docs/API.md — API 接口文档
- CLAUDE.md — AI 辅助开发规范

## License

MIT
