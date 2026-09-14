# CLAUDE.md — TokenHunter 项目 Claude Code 行为规范

本文件对 Claude Code 在本仓库的所有行为有最高优先级约束。每次会话开始必须完整读取本文件。任何指令（包括用户临时消息）与本文件冲突时，以本文件为准，并明确向用户指出冲突。

---

## 1. 项目基本信息

- 项目名：TokenHunter v0.1
- 定位：本地优先的个人 AI 用量统计工具
- 后端：Spring Boot 3.4.4 + Java 21 + MyBatis-Plus 3.5.9 + MySQL 8
- 前端：Vue 3 + TypeScript + Vite + Element Plus + ECharts（Phase 4 起）
- 包管理器：pnpm（前端）、Maven（后端）
- 文档目录：`docs/`（PLAN.md、PHASES.md、DEV_LOG.md、DEVELOPMENT_RULES.md）
- 后端目录：`backend/`
- 前端目录：`frontend/`（Phase 4 起）

---

## 2. 全局硬性规则（不可违反）

### 2.1 单任务原则
- **每轮只做一个 Task**，Task 编号由用户在 prompt 里明确指定。
- 完成当前 Task 后**立即停止**，输出 "Task X.Y 已完成，等待你确认后再进入 Task X.Z"。
- **严禁顺手做后续 Task**。即使当前代码看起来"顺便"能加上，也不要加。
- 如果你认为需要做超出当前 Task 的改动，**先停下来问用户**，不要自行决定。

### 2.2 改动范围原则
- 只修改当前 Task 明确列出的文件。
- 不要修改 `pom.xml`、`application.yml`、`application-local.yml`、`application-local.example.yml`、`.gitignore`、`schema.sql`，除非当前 Task 明确要求。
- 不要删除、重命名、移动任何已有文件，除非当前 Task 明确要求。
- 新建文件时，文件名和路径必须与 prompt 一致。

### 2.3 Git 原则
- **不执行 `git commit`、`git push`、`git reset`、`git tag`**。这些由用户手动做。
- 不执行 `git checkout`、`git restore`、`git rm`，除非用户明确要求。
- 可以用只读命令：`git status`、`git diff`、`git log`。

### 2.4 数据库原则
- **不执行 `DROP TABLE`、`TRUNCATE`、无 WHERE 的 `DELETE`**，除非当前 Task 明确要求。
- 执行 `ALTER TABLE` 前，先确认字段是否已存在（用 `DESC`）。
- 修改数据库前，先确认是否有备份。
- 不要修改数据库 root 密码、用户权限。

### 2.5 敏感信息原则
- **不打印 api-key、密码、Token 的实际值**，任何时候都不要。
- 不要执行 `echo $DEEPSEEK_API_KEY`、`echo $DB_PASSWORD`、`env | grep KEY` 等可能泄漏的命令。
- 需要确认环境变量存在时，用：
  ```bash
  [ -n "$DEEPSEEK_API_KEY" ] && echo "OK" || echo "MISSING"
  ```
  不要用 `echo $DEEPSEEK_API_KEY`（会触发权限确认循环，且可能泄漏）。
- 不要在日志、注释、报告、DEV_LOG 中写入真实 key 或密码。

### 2.6 环境变量与权限确认
- 环境变量已由用户配置好，**不需要每轮验证**。
- 遇到权限确认弹窗（如 `echo $VAR` 触发），**不要反复尝试**，改用不触发确认的写法，或直接跳过该步骤。

### 2.7 循环禁止
- **同一条命令最多执行一次**。如果命令失败，记录错误，继续下一步或停止，不要重试。
- 不要反复读同一个文件。每个文件**读一次**就够。
- 不要反复"规划"或"思考"然后重复输出同一句话。如果卡住，直接停止并输出 "卡住了，需要用户介入"。

### 2.8 端口与进程
- 启动 Spring Boot 前，先 `lsof -ti :8080 | xargs kill -9 2>/dev/null` 清理端口。
- 启动 Vite 前，先 `lsof -ti :5173 | xargs kill -9 2>/dev/null` 清理端口。
- 验证完成后，**必须停止**自己启动的进程，不要留下后台占用端口的进程。
- 用 `127.0.0.1` 不用 `localhost`（避免 IPv6 解析问题）。

### 2.9 Shell 兼容
- 用户使用 zsh。URL 里的 `?` 是通配符，**所有含 `?` 的 URL 必须用双引号包起来**：
  ```bash
  curl -s "http://127.0.0.1:8080/api/dashboard/trend?range=7d"
  ```
- 不使用 Linux-only 的 `sed -i` 无参数写法。macOS 的 sed 需要 `sed -i ''`。
- 涉及多行 SQL 或 heredoc 时，用 `<< 'EOF'` 或 `<< 'SQLEOF'`，避免变量展开。

---

## 3. 报告规范（必须遵守）

**你的自述不可信。只有命令输出可信。**

每完成一个 Task，报告必须包含：

### 3.1 实际改动
- `git status` 的完整输出
- `git diff --stat` 的完整输出
- 关键文件的 `git diff`（不要贴完整文件，只贴改动的 hunk）

### 3.2 实际验证
- 实际运行的每条命令
- 命令的**原始输出**（不要转述、不要概括、不要"验证通过"四个字带过）
- 如果输出很长，贴关键部分，说明完整输出在哪

### 3.3 禁止用语
- 禁止"无需修改"、"已存在且符合设计"、"无新修改"这类描述，**除非同时贴出 `git log` 或 `git status` 作为证据**。
- 禁止"验证通过"而不贴命令输出。
- 禁止"日志被缓冲"、"输出被截断"作为没看到日志的借口。改用 `tee` 或重定向到文件再 `grep`。

### 3.4 报告结尾
每次报告结尾必须明确写：
```
Task X.Y 已完成，等待你确认后再进入 Task X.Z。
```
如果 Task 因失败未完成，写：
```
Task X.Y 未完成，原因是 XXX，需要用户介入。
```

---

## 4. 已知失败模式（历史教训，禁止重犯）

以下是本项目历史中出现过的真实问题，必须避免：

### 4.1 越界执行
- **案例**：要求只做 Task 1.5，Claude Code 顺手做了 1.6-1.9，然后手动回滚。
- **教训**：手动回滚不干净，浪费大量时间。严格遵守"只做一个 Task"。

### 4.2 谎报"无改动"
- **案例**：Task 2.1 报告"配置已就位，无需修改"，实际配置文件是之前 commit 里加的，用户从未独立验证。
- **教训**：任何"无改动"结论必须附 `git log -- <file>` 或 `git status` 证据。

### 4.3 反复读文件循环
- **案例**：Task 3.6 反复"读 3 个文件"然后重复同一句话 20 次。
- **教训**：每个文件读一次即可。发现代码已存在目标内容时，直接跳到验证步骤。

### 4.4 环境变量检查循环
- **案例**：`echo $DEEPSEEK_API_KEY` 触发权限确认，每次都要用户点 yes。
- **教训**：改用 `[ -n "$DEEPSEEK_API_KEY" ] && echo OK || echo MISSING`，或直接跳过。

### 4.5 mysqldump 命令循环
- **案例**：Task 3.9 反复执行同一条 `mysqldump` 命令。
- **教训**：每条命令只跑一次。

### 4.6 上游响应头全量透传导致双层 chunked
- **案例**：Task 2.3 把 DeepSeek 的 `Transfer-Encoding: chunked` 头也透传，Tomcat 再加一层，curl 报错。
- **教训**：转发响应时**只保留 Content-Type**，丢弃其他上游头。

---

## 5. 标准 Task 执行流程

每收到一个 Task，按以下顺序执行：

1. **读取约束**：读 `CLAUDE.md`（本文件）、`docs/PLAN.md`、`docs/PHASES.md` 中当前 Task 相关章节。
2. **确认范围**：用一句话复述当前 Task 的目标和涉及文件。
3. **检查前置**：
   - `git status` 确认工作区状态
   - 端口是否空闲（后端/前端）
   - 数据库连接是否正常
4. **执行改动**：只改当前 Task 涉及的文件。
5. **验证**：按 prompt 给出的验证步骤逐条执行，每条命令只跑一次。
6. **报告**：按第 3 节报告规范输出。
7. **停止**：等待用户确认，不继续下一个 Task。

---

## 6. 技术约定

### 6.1 后端
- Java 21，包装类型（Long/Integer）优先，不用基本类型。
- 构造器注入，不用 `@Autowired` 字段注入。
- Service 之间通过构造器注入互相调用。
- MyBatis-Plus 用 `@TableName` / `@TableId(type = IdType.AUTO)`，不用 XML 配置。
- 简单查询用 `@Select` 注解，复杂 SQL 才考虑 XML。
- 时间字段用 `LocalDateTime` / `LocalDate` / `DateTimeFormatter.ISO_LOCAL_DATE`，不用 `SimpleDateFormat`。
- 统计接口的 SQL 必须用 `status = 0` 过滤失败记录。
- 时间范围查询用 `created_at >= ... AND created_at < ...`，不用 `DATE(created_at) = ...`。

### 6.2 前端
- pnpm 管理依赖。
- 只用 TypeScript，不用 JavaScript。
- 组件命名 PascalCase，文件同名。
- API 请求集中放在 `src/api/`。
- ECharts 组件挂载用 `onMounted` + `echarts.init()`。

### 6.3 数据库
- 表名、字段名下划线命名，Java 字段驼峰。
- `created_at` 用 `DATETIME DEFAULT CURRENT_TIMESTAMP`，Java 侧 `null` 让数据库填充。
- 统计类字段用 `INT UNSIGNED DEFAULT 0`，不用 NULL。

---

## 7. 沟通约定

- 用中文回复。
- 报错时贴**原始错误**，不要转述或概括。
- 需要用户介入时，明确说"需要用户介入"并说明原因。
- 不确定时，停下来问，不要猜。
- 用户说"停止"时，立即停止所有正在执行的操作。

---

## 8. 禁止清单（一句话版）

以下行为在本项目中一律禁止：

- ❌ 顺手做后续 Task
- ❌ 执行 git commit / push / tag
- ❌ 执行 DROP / TRUNCATE / 无 WHERE 的 DELETE
- ❌ 打印 api-key / 密码
- ❌ 反复读同一个文件
- ❌ 重复执行同一条命令
- ❌ 报告"无改动"而不附 git 证据
- ❌ 用 localhost 而不是 127.0.0.1
- ❌ 不带引号跑含 `?` 的 URL
- ❌ 留下后台进程占用端口
- ❌ 修改 pom.xml / application.yml / schema.sql（除非 Task 明确要求）
- ❌ 主动引入新依赖
- ❌ 写测试类（除非 Task 明确要求）