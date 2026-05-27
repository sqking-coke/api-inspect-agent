# 接口自动化巡检Agent — 学习指南

> **项目地址**：https://github.com/sqking-coke/api-inspect-agent <br>
> **适用对象**：Java 后端开发者，具备 Spring Boot 基础，想深入学习 AI Agent 架构设计

---

## 目录

1. [项目全景](#1-项目全景)
2. [开发环境与快速启动](#2-开发环境与快速启动)
3. [分层架构详解](#3-分层架构详解)
4. [核心流程：一次巡检的完整生命周期](#4-核心流程一次巡检的完整生命周期)
5. [模块源码精讲](#5-模块源码精讲)
   - 5.1 [AgentCore — 核心调度引擎](#51-agentcore--核心调度引擎)
   - 5.2 [HttpInvokeTool — HTTP 调用工具](#52-httpinvoketool--http-调用工具)
   - 5.3 [AssertTool — 断言校验工具](#53-asserttool--断言校验工具)
   - 5.4 [RetryTool — 通用重试工具](#54-retrytool--通用重试工具)
   - 5.5 [LLMClient — 大模型客户端](#55-llmclient--大模型客户端)
   - 5.6 [LLMPromptTemplates — Prompt 工程](#56-llmprompttemplates--prompt-工程)
   - 5.7 [ReportService — 报告服务](#57-reportservice--报告服务)
   - 5.8 [ExceptionAnalysisService — 异常分析](#58-exceptionanalysisservice--异常分析)
   - 5.9 [AlertService — 告警通知](#59-alertservice--告警通知)
   - 5.10 [InspectScheduler — 定时调度](#510-inspectscheduler--定时调度)
6. [数据模型详解](#6-数据模型详解)
7. [API 接口文档](#7-api-接口文档)
8. [配置说明](#8-配置说明)
9. [设计模式与技术亮点](#9-设计模式与技术亮点)
10. [扩展指南](#10-扩展指南)
11. [常见问题排查](#11-常见问题排查)

---

## 1. 项目全景

### 1.1 一句话定位

**用 Java 原生代码实现一个 AI 驱动的接口自动化巡检系统** — 不依赖 LangChain、Spring AI 等重型 AI 框架，自己手写 Agent 调度闭环。

### 1.2 解决什么问题

| 痛点 | 本项目的解决方式 |
|---|---|
| 人工逐个调接口验证 | 批量自动执行，串行/并行可选 |
| 断言规则僵化，只能判断 200/非 200 | 支持 JSONPath 断言、关键字断言、状态码断言 |
| 报错后不知道原因 | AI 自动分析异常根因，给出修复建议 |
| 每天凌晨需人工巡检 | Cron 定时任务自动触发 |
| 巡检结果无沉淀 | 每次生成结构化报告，日志持久化可回溯 |
| 线上出问题才发现 | 异常时通过钉钉 Webhook 实时告警 |
| 对历史情况不了解 | AI 对话复盘，基于历史数据回答问题 |

### 1.3 技术栈

| 层级 | 技术 | 版本 |
|---|---|---|
| 框架 | Spring Boot | 3.5.x |
| 持久层 | MyBatis-Plus | 3.5.x |
| 数据库 | MySQL | 8.0 |
| HTTP 客户端 | OkHttp | 4.x |
| JSON | FastJSON2 | 2.x |
| 定时任务 | Spring `@Scheduled` | — |
| 线程池 | `ThreadPoolExecutor` | — |
| AI | DeepSeek / 通义千问 / 任意 OpenAI 兼容 API | — |
| 简化代码 | Lombok | — |

### 1.4 运行时架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        接入层 (Controller)                        │
│  InspectController  │  CaseController  │  ConfigController      │
└──────────────┬──────────────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────────────┐
│                     Agent 调度核心层                              │
│                      AgentCore.java                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │ 用例加载  │→│ 策略选择  │→│ 并发/串行 │→│ 结果聚合+AI分析│  │
│  └──────────┘  └──────────┘  └──────────┘  └───────────────┘  │
└──────┬──────────────┬────────────────┬──────────────────────────┘
       │              │                │
┌──────▼──────┐ ┌─────▼─────┐ ┌───────▼────────┐
│  工具能力层  │ │  LLM 层   │ │   基础服务层    │
│ HttpInvoke  │ │ LLMClient │ │  定时调度       │
│ AssertTool  │ │ Prompt模板│ │  线程池管理     │
│ RetryTool   │ │           │ │  告警通知       │
└──────┬──────┘ └─────┬─────┘ └───────┬────────┘
       │              │               │
┌──────▼──────────────▼───────────────▼──────────────────────────┐
│                      数据持久层                                   │
│  api_inspect_case  │  api_inspect_task  │  api_inspect_log      │
│  (用例定义)         │  (任务汇总)          │  (执行日志)            │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 开发环境与快速启动

### 2.1 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- 一个 LLM API Key（推荐 DeepSeek，成本低效果不差）

### 2.2 数据库初始化

```sql
CREATE DATABASE IF NOT EXISTS api_inspect_agent DEFAULT CHARSET utf8mb4;

USE api_inspect_agent;

-- 巡检用例表
CREATE TABLE api_inspect_case (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_name VARCHAR(200) NOT NULL COMMENT '用例名称',
    api_url VARCHAR(500) NOT NULL COMMENT '接口地址',
    method VARCHAR(10) DEFAULT 'GET' COMMENT 'HTTP方法',
    request_header TEXT COMMENT '请求头JSON',
    request_body TEXT COMMENT '请求体JSON',
    query_params VARCHAR(500) COMMENT 'URL查询参数',
    timeout INT COMMENT '超时ms',
    retry_count INT COMMENT '重试次数',
    retry_interval INT COMMENT '重试间隔ms',
    assert_rule TEXT COMMENT '断言规则JSON',
    group_name VARCHAR(100) COMMENT '分组',
    description VARCHAR(500) COMMENT '说明',
    priority INT DEFAULT 0 COMMENT '优先级',
    status TINYINT DEFAULT 1 COMMENT '0禁用 1启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 巡检任务表
CREATE TABLE api_inspect_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_no VARCHAR(50) NOT NULL COMMENT '任务编号',
    total_count INT DEFAULT 0 COMMENT '总用例数',
    success_count INT DEFAULT 0 COMMENT '成功数',
    fail_count INT DEFAULT 0 COMMENT '失败数',
    skip_count INT DEFAULT 0 COMMENT '跳过数',
    task_duration BIGINT DEFAULT 0 COMMENT '耗时ms',
    execute_mode VARCHAR(20) COMMENT 'SERIAL/PARALLEL',
    ai_summary LONGTEXT COMMENT 'AI巡检总结',
    ai_risk_items TEXT COMMENT 'AI风险项',
    error_rate DECIMAL(5,2) COMMENT '错误率',
    avg_response_time INT COMMENT '平均响应时间ms',
    task_status TINYINT DEFAULT 0 COMMENT '0执行中 1已完成 2已停止',
    trigger_type TINYINT DEFAULT 1 COMMENT '1手动 2定时',
    error_message TEXT COMMENT '异常信息',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 巡检日志表
CREATE TABLE api_inspect_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '任务ID',
    case_id BIGINT NOT NULL COMMENT '用例ID',
    case_name VARCHAR(200) COMMENT '用例名称',
    api_url VARCHAR(500) COMMENT '接口地址',
    method VARCHAR(10) COMMENT 'HTTP方法',
    request_header TEXT COMMENT '请求头',
    request_body TEXT COMMENT '请求体',
    response_header TEXT COMMENT '响应头',
    response_body MEDIUMTEXT COMMENT '响应体',
    status_code INT COMMENT 'HTTP状态码',
    response_time INT COMMENT '响应耗时ms',
    success TINYINT DEFAULT 0 COMMENT '0失败 1成功',
    error_message TEXT COMMENT '错误信息',
    error_type VARCHAR(50) COMMENT '错误分类',
    assert_detail TEXT COMMENT '断言详情JSON',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    ai_analysis TEXT COMMENT 'AI分析结论',
    ai_suggestion TEXT COMMENT 'AI修复建议',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_case_id (case_id),
    INDEX idx_success (success)
);

-- 系统配置表
CREATE TABLE api_inspect_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    config_type VARCHAR(20) DEFAULT 'STRING' COMMENT '值类型',
    description VARCHAR(200) COMMENT '说明',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_config_key (config_key)
);
```

### 2.3 配置文件

`application.yml` 核心配置：

```yaml
server:
  port: 9090

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/api_inspect_agent?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver

# LLM 配置 — 对接 DeepSeek（也兼容 OpenAI / 通义千问等）
llm:
  enabled: true
  api-url: https://api.deepseek.com/v1/chat/completions
  api-key: <你的API-KEY>
  model: deepseek-v4-flash
  max-tokens: 4096
  temperature: 0.3
  connect-timeout: 30
  read-timeout: 120

# Agent 巡检参数
agent:
  inspect:
    default-timeout: 5000
    default-retry-count: 2
    retry-interval: 1000
    parallel-threads: 10
    cron-expression: "0 0 3 * * ?"
    cron-enabled: false
    log-retention-days: 90

# 告警配置
alert:
  enabled: false
  webhook: ""
```

### 2.4 启动命令

```bash
# 编译
mvn clean compile

# 启动
mvn spring-boot:run

# 或打包后启动
mvn clean package -DskipTests
java -jar target/api-inspect-agent-1.0.0.jar
```

### 2.5 录入用例

```bash
curl -X POST http://localhost:9090/agent/inspect/case/save \
  -H "Content-Type: application/json" \
  -d '{
    "caseName": "用户登录接口",
    "apiUrl": "http://your-service/api/user/login",
    "method": "POST",
    "requestHeader": "{\"Content-Type\":\"application/json\"}",
    "requestBody": "{\"username\":\"admin\",\"password\":\"123456\"}",
    "timeout": 5000, "retryCount": 2,
    "assertRule": "{\"statusCode\":200,\"bodyContains\":\"token\"}",
    "groupName": "用户模块", "status": 1
  }'
```

### 2.6 手动触发巡检

```bash
# 全量并行巡检
curl -X POST http://localhost:9090/agent/inspect/start \
  -H "Content-Type: application/json" \
  -d '{"executeMode": "PARALLEL"}'

# 指定用例串行巡检
curl -X POST http://localhost:9090/agent/inspect/start \
  -H "Content-Type: application/json" \
  -d '{"executeMode": "SERIAL", "caseIds": [1,3,5]}'
```

### 2.7 开启定时巡检

```bash
curl -X PUT http://localhost:9090/agent/config \
  -H "Content-Type: application/json" \
  -d '{"cronExpression": "0 0 2,6,10,14,18,22 * * ?", "cronEnabled": true}'
```

### 2.8 分页查询巡检任务列表

```bash
curl http://localhost:9090/agent/inspect/task/list?page=1&size=5
# 预期: {"code":200,"message":"success","data":{"total":0,...}}
```

### 2.9 查看报告

```bash
curl http://localhost:9090/agent/inspect/report/1001
```

---

## 3. 分层架构详解

### 3.1 包结构（实际代码）

```
com.inspect.agent
├── AgentApplication.java              # Spring Boot 启动类
│
├── agent/                             # Agent 核心引擎
│   ├── AgentCore.java                 # 调度中枢，串联全流程
│   └── AgentContext.java              # 任务运行上下文（状态持有者）
│
├── tool/                              # 工具层（可被 Agent 调用的能力单元）
│   ├── HttpInvokeTool.java            # HTTP 请求执行器
│   ├── AssertTool.java                # 断言校验器
│   └── RetryTool.java                 # 通用重试器
│
├── llm/                               # LLM 层（AI 能力的封装）
│   ├── LLMClient.java                 # 大模型 HTTP 客户端
│   └── LLMPromptTemplates.java        # Prompt 模板常量与方法
│
├── analysis/                          # 异常分析（AI 消费层）
│   └── ExceptionAnalysisService.java  # 批量 AI 分析 + 对话复盘
│
├── report/                            # 报告生成
│   └── ReportService.java             # 结构化报告 + AI 摘要
│
├── alert/                             # 告警通知
│   └── AlertService.java              # 钉钉 Webhook 告警
│
├── scheduler/                         # 定时任务
│   └── InspectScheduler.java          # Cron 定时巡检入口
│
├── controller/                        # REST 接口层
│   ├── InspectController.java         # 巡检控制、报告查看、AI 对话
│   ├── CaseController.java            # 用例 CRUD
│   └── ConfigController.java          # 系统配置读写
│
├── service/                           # 通用业务服务
│   ├── CaseService.java               # 用例服务（分页、批量操作）
│   ├── ConfigService.java             # 配置服务（key-value 读写）
│   └── InspectTaskService.java        # 任务服务（分页查询）
│
├── entity/                            # 数据库实体
│   ├── ApiInspectCase.java
│   ├── ApiInspectTask.java
│   ├── ApiInspectLog.java
│   └── ApiInspectConfig.java
│
├── mapper/                            # MyBatis-Plus Mapper
│   ├── ApiInspectCaseMapper.java
│   ├── ApiInspectTaskMapper.java
│   ├── ApiInspectLogMapper.java
│   └── ApiInspectConfigMapper.java
│
├── dto/                               # 数据传输对象
│   ├── InspectResult.java             # 单用例执行结果
│   ├── ReportDTO.java                 # 巡检报告
│   ├── TaskStartRequest.java          # 启动任务请求
│   ├── ChatRequest.java               # AI 对话请求
│   ├── BatchStatusRequest.java        # 批量状态请求
│   └── ConfigUpdateRequest.java       # 配置更新请求
│
├── config/                            # Spring 配置类
│   ├── AgentConfig.java               # Agent 参数配置
│   ├── LLMConfig.java                 # LLM 连接配置
│   ├── ThreadPoolConfig.java          # 线程池配置
│   └── MyBatisPlusConfig.java         # 分页插件配置
│
└── common/                            # 通用组件
    ├── Result.java                    # 统一响应体
    ├── BusinessException.java         # 业务异常
    └── GlobalExceptionHandler.java    # 全局异常处理
```

### 3.2 依赖关系图（谁依赖谁）

```
Controller  ──→  AgentCore (启动/停止)
             ──→  ReportService (查看报告)
             ──→  ExceptionAnalysisService (AI对话)
             ──→  CaseService (用例管理)
             ──→  ConfigService (配置管理)

AgentCore   ──→  HttpInvokeTool (发请求)
            ──→  AssertTool (断言校验)
            ──→  RetryTool (重试控制)
            ──→  LLMClient (AI分析)
            ──→  AgentContext (状态管理)
            ──→  Mapper (数据库读写)

ReportService ──→ LLMClient (AI摘要)
              ──→ Mapper (查询数据)

ExceptionAnalysisService ──→ LLMClient (AI分析)
                          ──→ Mapper (查询日志)
```

**设计原则**：单向依赖，上层依赖下层，没有循环依赖。Controller 不直接依赖 Tool 层，而是通过 AgentCore 间接调用。

---

## 4. 核心流程：一次巡检的完整生命周期

```
用户/定时器触发
      │
      ▼
┌──────────────────────────────────────┐
│  1. AgentCore.startInspect()         │
│     - 检查是否有运行中的任务           │
│     - 参数: executeMode, caseIds,     │
│       triggerType(1手动/2定时)        │
└──────────────┬───────────────────────┘
               │
               ▼
┌──────────────────────────────────────┐
│  2. loadCases(caseIds)               │
│     - caseIds 非空 → 按ID批量查询     │
│     - caseIds 为空 → 查全部启用用例    │
│     - 过滤：只取 status=1 的用例       │
└──────────────┬───────────────────────┘
               │
               ▼
┌──────────────────────────────────────┐
│  3. 创建 AgentContext + 数据库任务记录 │
│     - 生成任务编号 INSPECT-yyyyMMdd-NNN│
│     - 写入 api_inspect_task 表        │
│     - 上下文: taskId, totalCount,     │
│       startTime, executeMode          │
└──────────────┬───────────────────────┘
               │
          ┌────┴────┐
          │ 执行模式？│
          └────┬────┘
     ┌─────────┴─────────┐
     ▼                   ▼
┌─────────┐        ┌──────────┐
│ SERIAL  │        │ PARALLEL │
│ 串行    │        │ 并行     │
└────┬────┘        └────┬─────┘
     │                  │
     │    for case in cases:        CompletableFuture
     │      executeOneCase(case)    .runAsync(executeOneCase)
     │      ctx.addResult()         线程池: inspectExecutor
     │      saveLog()               整体超时: 5分钟
     │                              .allOf().get(5, MINUTES)
     │                  │
     └─────────┬────────┘
               ▼
┌──────────────────────────────────────┐
│  4. executeOneCase(case)  — 单个用例 │
│                                      │
│  4a. RetryTool.executeWithRetry()    │
│       └─ HttpInvokeTool.execute()    │
│          (url, method, headers, body,│
│           timeout)                    │
│                                      │
│  4b. AssertTool.assertResponse()     │
│       └─ 根据 assertRule JSON 校验   │
│          - statusCode                │
│          - maxResponseTime           │
│          - bodyContains              │
│          - JSONPath(responseCode)    │
│          - JSONPath(notNull)         │
│                                      │
│  4c. 判断: httpOk && assertPassed    │
│       ├─ 成功 → 跳过 AI 分析         │
│       └─ 失败 → classifyError()      │
│               → analyzeWithAI()      │
│                  (调用 LLM 根因分析)  │
└──────────────┬───────────────────────┘
               ▼
┌──────────────────────────────────────┐
│  5. 每个用例结果 saveLog()            │
│     - 写入 api_inspect_log 表         │
│     - 长响应体截断到 8192 字符         │
│     - 含断言详情、AI分析、重试次数     │
└──────────────┬───────────────────────┘
               ▼
┌──────────────────────────────────────┐
│  6. 全部完成后，更新 api_inspect_task │
│     - endTime, successCount,         │
│       failCount, taskDuration,       │
│       taskStatus(1已完成/2已停止)     │
└──────────────┬───────────────────────┘
               ▼
┌──────────────────────────────────────┐
│  7. 异步后处理（单独线程）            │
│     - ReportService.generateAiSummary│
│       → LLM 生成巡检总结             │
│     - ExceptionAnalysisService       │
│       .batchAnalyze()                │
│       → 补充失败日志的 AI 分析        │
│     - AlertService.sendAnomalyAlert  │
│       (如有) → 钉钉群告警            │
└──────────────────────────────────────┘
```

**关键设计决策**：
- 步骤 7 是异步的（`new Thread().start()`），因为 AI 调用耗时长，不应阻塞接口响应
- 步骤 4 中只对失败用例调用 AI（节省 Token），成功用例跳过
- 步骤 5 的截断是为了防止超长响应体撑爆数据库字段

---

## 5. 模块源码精讲

### 5.1 AgentCore — 核心调度引擎

**文件**：`agent/AgentCore.java`

**职责**：整个系统的调度中枢，负责串联「加载用例 → 执行巡检 → 结果收集 → AI 分析 → 持久化」的完整流程。

#### 关键字段

```java
// 当前正在执行的巡检上下文，同一时间只允许一个巡检任务
private volatile AgentContext currentContext;

// 任务编号自增序号（每天从 1 开始）
private final AtomicInteger taskSeq = new AtomicInteger(1);
```

**教学要点**：
- `volatile` 保证 `currentContext` 在多线程间的可见性
- `AtomicInteger` 保证 `taskSeq` 的原子递增，避免并发下生成重复编号

#### startInspect() — 巡检入口

```java
public AgentContext startInspect(String executeMode, List<Long> caseIds, int triggerType) {
    // 1. 防重入检查 — 同一时间只能有一个任务运行
    if (currentContext != null && !currentContext.isFinished() && !currentContext.isStopped()) {
        throw new BusinessException(409, "已有巡检任务正在运行中");
    }
    // 2. 加载用例（只取 status=1 的启用用例）
    // 3. 生成任务编号、创建 AgentContext
    // 4. 写入 api_inspect_task 表
    // 5. 根据模式选择串行/并行执行
    // 6. 更新任务状态
    // 7. 返回上下文
}
```

**教学要点**：
- **防重入设计**：通过检查 `currentContext` 状态，防止同时启动多个巡检任务
- **参数设计**：`triggerType` 区分手动/定时触发，方便统计和分析

#### 串行 vs 并行执行

```java
// 串行 — 一个接一个，可随时中断
private void executeSerial(AgentContext ctx, List<ApiInspectCase> cases) {
    for (ApiInspectCase c : cases) {
        if (ctx.isStopped()) break;  // 响应停止指令
        InspectResult result = executeOneCase(ctx.getTaskId(), c);
        ctx.addResult(result);
        saveLog(ctx.getTaskId(), c, result);
    }
}

// 并行 — 线程池并发，5 分钟整体超时
private void executeParallel(AgentContext ctx, List<ApiInspectCase> cases) {
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    for (ApiInspectCase c : cases) {
        futures.add(CompletableFuture.runAsync(() -> {
            if (ctx.isStopped()) return;
            InspectResult result = executeOneCase(ctx.getTaskId(), c);
            ctx.addResult(result);        // CopyOnWriteArrayList 线程安全
            saveLog(ctx.getTaskId(), c, result);
        }, inspectExecutor));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .get(5, TimeUnit.MINUTES);   // 最长等 5 分钟
}
```

**教学要点**：
- `CopyOnWriteArrayList` 保证并发 `addResult` 时的线程安全 — 写时复制，读无锁
- `CompletableFuture.allOf().get(timeout)` 是实现「等待所有完成 + 整体超时」的标准写法
- 5 分钟超时后会抛出 `TimeoutException`，但不影响已完成的那些任务（结果已写入日志表）

#### executeOneCase() — 单用例执行全流程

```java
public InspectResult executeOneCase(Long taskId, ApiInspectCase c) {
    // 1. 获取超时/重试参数（用例级 > 全局默认）
    int timeout = c.getTimeout() != null && c.getTimeout() > 0
            ? c.getTimeout() : agentConfig.getDefaultTimeout();
    int maxRetries = c.getRetryCount() != null
            ? c.getRetryCount() : agentConfig.getDefaultRetryCount();

    // 2. 带重试的 HTTP 调用
    RetryResult<HttpResponse> retryResult = retryTool.executeWithRetry(() -> {
        HttpResponse resp = httpInvokeTool.execute(url, method, headers, body, timeout);
        // 502/503 强制重试 — 典型的临时故障
        if (resp.success() && (resp.statusCode() == 502 || resp.statusCode() == 503)) {
            throw new RuntimeException("Retryable status");
        }
        return resp;
    }, maxRetries, retryInterval);

    // 3. HTTP 层面成功 → 执行断言校验
    // 4. 断言 + HTTP 状态码综合判断是否成功
    // 5. 失败时 → AI 分析
    // 6. 返回 InspectResult
}
```

**教学要点**：
- **参数优先级**：用例级配置 > 全局默认配置，实现了个性化覆盖
- **可重试状态码判断**：502/503 是典型的临时故障，强制重试；其他 4xx/5xx 不重试（参数错误重试也没用）
- **失败时 AI 分析**：成功用例不调 AI，节省 Token 成本

#### classifyError() — 错误分类策略

```java
private String classifyError(HttpResponse resp) {
    if (resp.error() != null) {
        if (resp.error().startsWith("TIMEOUT")) return "TIMEOUT";
        if (resp.error().startsWith("NETWORK_ERROR")) return "NETWORK_ERROR";
    }
    int sc = resp.statusCode();
    if (sc >= 400 && sc < 500) return "HTTP_4XX";
    if (sc >= 500) return "HTTP_5XX";
    return "ASSERT_FAIL";  // HTTP 200 但断言不通过
}
```

**教学要点**：这个分类逻辑直接决定了后续 AI 分析的上下文质量。`ASSERT_FAIL` 这个类型特别重要 — 表示 HTTP 通了，但业务逻辑不对（比如返回了错误码）。

---

### 5.2 HttpInvokeTool — HTTP 调用工具

**文件**：`tool/HttpInvokeTool.java`

**设计亮点**：

```java
public HttpResponse execute(String url, String method, String headersJson,
                             String bodyJson, int timeoutMs) {
    long start = System.currentTimeMillis();
    try {
        // 每次调用创建带自定义超时的 client（复用连接池）
        OkHttpClient timeoutClient = client.newBuilder()
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build();

        // 构建请求
        Request.Builder builder = new Request.Builder().url(url);
        // 解析 JSON 请求头
        if (headersJson != null && !headersJson.isBlank()) {
            Map<String, String> headers = JSON.parseObject(headersJson, ...);
            headers.forEach(builder::addHeader);
        }
        // 根据 method 设置 HTTP 动词
        // ...
    } catch (SocketTimeoutException e) {
        return new HttpResponse(false, 0, null, elapsed, "TIMEOUT: " + e.getMessage());
    } catch (IOException e) {
        return new HttpResponse(false, 0, null, elapsed, "NETWORK_ERROR: " + e.getMessage());
    }
}
```

**教学要点**：
- **不抛异常，返回结果对象** — 这是一种「铁路导向编程」风格，让调用方通过 `result.success()` 判断成功/失败，而不是 try-catch
- **异常分类**：`SocketTimeoutException` 和 `IOException` 分开处理，分别标记为 TIMEOUT 和 NETWORK_ERROR
- **耗时计算**：每次执行都记录 `elapsedMs`，用于性能监控
- **record 类型**：`HttpResponse` 使用 Java `record`（不可变数据类），简洁且线程安全

---

### 5.3 AssertTool — 断言校验工具

**文件**：`tool/AssertTool.java`

**支持的断言类型**：

| 断言规则 | 含义 | 示例 |
|---|---|---|
| `statusCode` | HTTP 状态码 | `{"statusCode": 200}` |
| `maxResponseTime` | 最大响应时间 | `{"maxResponseTime": 3000}` |
| `bodyContains` | 响应体包含关键字 | `{"bodyContains": "success"}` |
| `bodyNotContains` | 响应体不包含关键字 | `{"bodyNotContains": "error"}` |
| `responseCode` + `responseCodePath` | JSONPath 业务状态码 | `{"responseCode": 0, "responseCodePath": "$.code"}` |
| `notNullPath` | JSONPath 字段不为 null | `{"notNullPath": "$.data.id"}` |
| `notEmptyPath` | JSONPath 字段不为空 | `{"notEmptyPath": "$.data.list"}` |

**JSONPath 实现**：

```java
// 简易 JSONPath 取值，支持 $.a.b.c 格式
private Object getByPath(JSONObject json, String path) {
    String[] parts = path.replace("$.", "").split("\\.");
    Object current = json;
    for (String part : parts) {
        if (current instanceof JSONObject jo) {
            current = jo.get(part);
        } else {
            return null;
        }
    }
    return current;
}
```

**教学要点**：
- 这是一个极简的 JSONPath 实现，不需要引入 Jayway JsonPath 等重型库
- 用 `try-catch` 包裹 JSON 解析，非 JSON 响应体自动跳过 JSONPath 断言
- `assertItem()` 工厂方法统一构建断言结果对象

#### 断言规则 JSON 示例

```json
{
  "statusCode": 200,
  "maxResponseTime": 3000,
  "bodyContains": "success",
  "responseCode": 0,
  "responseCodePath": "$.code",
  "notNullPath": "$.data",
  "notEmptyPath": "$.data.list"
}
```

---

### 5.4 RetryTool — 通用重试工具

**文件**：`tool/RetryTool.java`

**核心逻辑**：

```java
public <T> RetryResult<T> executeWithRetry(Callable<T> task, int maxRetries, int retryIntervalMs) {
    int attempts = 0;
    Exception lastException = null;

    while (attempts <= maxRetries) {
        try {
            T result = task.call();
            return new RetryResult<>(true, result, attempts, null);  // 成功直接返回
        } catch (Exception e) {
            lastException = e;
            attempts++;
            if (attempts <= maxRetries) {
                Thread.sleep(retryIntervalMs);  // 重试前等待
            }
        }
    }
    return new RetryResult<>(false, null, maxRetries, lastException);  // 全部失败
}
```

**教学要点**：
- 使用 `Callable<T>` 泛型，可以重试任意类型的任务，不局限于 HTTP 调用
- `attempts` 是**实际尝试次数**（含首次），`maxRetries=2` 意味着最多执行 3 次
- 正确处理 `InterruptedException`：恢复中断标记后立即返回
- `RetryResult` 使用 `record` 类型，不可变，线程安全

**重试决策树**：
```
任务执行
  ├─ 成功 → 立即返回结果
  └─ 抛异常
       ├─ 还有重试次数 → sleep(interval) → 重试
       └─ 用尽重试次数 → 返回失败结果（含最后一次异常）
```

---

### 5.5 LLMClient — 大模型客户端

**文件**：`llm/LLMClient.java`

**设计思路**：直接使用 OkHttp 调用 OpenAI 兼容 API，不依赖任何 AI SDK。

```java
public String chat(String systemPrompt, String userMessage) {
    // 1. 检查开关 — LLM 禁用或 API Key 未配置时返回 null
    if (!config.isEnabled()) return null;
    if (config.getApiKey() == null || config.getApiKey().isBlank()) return null;

    // 2. 构建 OpenAI 格式的请求体
    JSONObject body = new JSONObject();
    body.put("model", config.getModel());
    body.put("messages", [
        {"role": "system", "content": systemPrompt},
        {"role": "user", "content": userMessage}
    ]);

    // 3. 发送 HTTP POST
    // 4. 解析响应: choices[0].message.content
}
```

**教学要点**：
- **兜底策略**：LLM 不可用时返回 `null` 而不是抛异常，保证巡检任务不中断
- **兼容性**：任何兼容 OpenAI Chat API 的服务都可以用（DeepSeek、通义千问、Moonshot、本地 Ollama 等）
- **两参数重载**：`chat(systemPrompt, userMessage)` 和 `chat(userMessage)` — 后者使用默认系统提示词

**LLM 在整个系统中的作用**：

| 调用场景 | 系统提示词 | 触发时机 |
|---|---|---|
| 异常分析 | `ANOMALY_ANALYSIS_SYSTEM` | 单用例执行失败时（AgentCore） |
| 批量补充分析 | `ANOMALY_ANALYSIS_SYSTEM` | 巡检完成后（ExceptionAnalysisService） |
| 巡检总结 | `REPORT_SUMMARY_SYSTEM` | 巡检完成后（ReportService） |
| AI 对话复盘 | `CHAT_SYSTEM` | 用户主动提问（InspectController→chat） |

---

### 5.6 LLMPromptTemplates — Prompt 工程

**文件**：`llm/LLMPromptTemplates.java`

这是整个系统 AI 效果的核心——Prompt 的质量直接决定 AI 分析的准确度。

#### 异常分析 Prompt 的结构

```
System Prompt（角色设定 + 规则约束）:
  "你是后端排障专家，分析 API 巡检失败..."
  规则: 1.错误分类 2.根因分析 3.修复建议 4.中文回答 5.简洁具体

User Prompt（具体数据）:
  **Case Name**: 用户登录接口
  **API**: POST https://api.example.com/login
  **Request Body**: {"username": "test"}
  **Response Status**: 400
  **Response Body**: {"code": 1001, "msg": "密码不能为空"}
  **Error Message**: null
  **Assertion Results**: [{"rule":"statusCode","expected":"200","actual":"400","passed":false}]

  Please provide:
  1. Error classification
  2. Root cause analysis
  3. Suggested fix
```

**教学要点**：
- **System Prompt 给角色和规则**：让 LLM "扮演"后端排障专家，约束输出格式
- **User Prompt 给具体数据**：把巡检失败的所有上下文（请求、响应、断言）都传进去
- **模板方法模式**：`buildAnomalyAnalysisPrompt()` 是静态工厂方法，用 `String.format()` 填充模板

---

### 5.7 ReportService — 报告服务

**文件**：`report/ReportService.java`

**两个核心方法**：

```java
// 1. 获取结构化报告（不调 LLM，只读数据库）
public ReportDTO getReport(Long taskId) {
    // 查任务 → 查全部日志 → 拆分为 successes 和 anomalies
    // → 组装 ReportDTO{taskInfo, successList, anomalyList, aiSummary, aiRiskItems}
}

// 2. 生成 AI 摘要（调 LLM，结果回写到任务表）
public String generateAiSummary(Long taskId) {
    // 查任务 + 失败日志 → 拼 statistical summary → 调 LLM → 回写 aiSummary
}
```

**教学要点**：
- **读写分离思想**：`getReport` 是纯读操作，`generateAiSummary` 是写操作（更新 AI 摘要字段）
- **ReportDTO 的嵌套结构**：`TaskInfo`、`AnomalyItem`、`SuccessItem` 都是静态内部类，避免了文件膨胀

---

### 5.8 ExceptionAnalysisService — 异常分析

**文件**：`analysis/ExceptionAnalysisService.java`

```java
// 批量补充分析：只处理"尚未 AI 分析"的失败日志
public void batchAnalyze(Long taskId) {
    List<ApiInspectLog> failLogs = logMapper.selectList(
        wrapper.eq(taskId).eq(success, 0).isNull(aiAnalysis)  // ← 关键过滤
    );
    for (ApiInspectLog log : failLogs) {
        String analysis = llmClient.chat(SYSTEM, buildPrompt(log));
        if (analysis != null) {
            log.setAiAnalysis(analysis);
            logMapper.updateById(log);  // 逐条回写
        }
    }
}
```

**教学要点**：
- `isNull(aiAnalysis)` 过滤避免重复分析（幂等性保证）
- 逐条处理而非批量提交 LLM，因为每次分析的上下文不同
- 单个失败不影响后续处理（try-catch 包裹）

---

### 5.9 AlertService — 告警通知

**文件**：`alert/AlertService.java`

钉钉 Markdown 格式的 Webhook 告警，内容包含：
- 任务编号
- 异常接口数
- 每个失败接口的错误类型、错误信息、AI 分析摘要（截断到 100 字）

**条件检查**：`alert.enabled=true` 且 `webhook` 已配置且确实有失败用例时才会发送。

---

### 5.10 InspectScheduler — 定时调度

**文件**：`scheduler/InspectScheduler.java`

```java
@Scheduled(cron = "${agent.inspect.cron-expression:0 0 3 * * ?}")
public void scheduledInspect() {
    // 双重开关检查: 数据库配置 OR yml 配置
    String enabled = configService.getValue("cron_enabled");
    if (!"true".equals(enabled) && !agentConfig.isCronEnabled()) {
        return;
    }
    agentCore.startInspect("PARALLEL", null, 2);  // triggerType=2 表示定时
}
```

**教学要点**：
- `@Scheduled` 的 cron 表达式支持占位符 `${...:default}`，可在运行时动态修改
- 双重开关：数据库配置（运行时热修改） + yml 配置（部署时设置），任意一个启用即可

---

## 6. 数据模型详解

### 6.1 ER 关系图

```
api_inspect_case (用例定义)
       │
       │ 1:N (一个用例可在多个任务中被执行)
       ▼
api_inspect_log (执行日志) ──────────── api_inspect_task (任务汇总)
       │                                        │
       │ N:1                                   │
       └────────────────────────────────────────┘
       (一条日志属于一个任务)              (一个任务有多条日志)

api_inspect_config (系统配置) — 独立表，与业务表无外键关系
```

### 6.2 状态机

**任务状态 (task_status)**：
```
0 (执行中) ──→ 1 (已完成)
    │
    └──────→ 2 (已停止)
```

**用例状态 (status)**：
```
0 (禁用) ←→ 1 (启用)
```

**触发类型 (trigger_type)**：
```
1 (手动触发) / 2 (定时触发)
```

---

## 7. API 接口文档

### 7.1 巡检控制

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/agent/inspect/start` | 启动巡检任务 |
| POST | `/agent/inspect/stop` | 停止当前任务 |
| POST | `/agent/inspect/case/test` | 单用例调试 |

**启动巡检请求体**：
```json
{
  "executeMode": "PARALLEL",
  "caseIds": [1, 2, 3]
}
```
> `caseIds` 为空时执行全部启用用例；`executeMode` 可选 SERIAL/PARALLEL

### 7.2 报告与日志

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/agent/inspect/task/list` | 任务列表（分页） |
| GET | `/agent/inspect/report/{taskId}` | 巡检报告详情 |
| GET | `/agent/inspect/log/{taskId}` | 任务下的日志列表 |

### 7.3 AI 对话

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/agent/inspect/chat` | AI 复盘问答 |

**请求体**：
```json
{
  "taskId": 1,
  "question": "最近有哪些接口不稳定？"
}
```

### 7.4 用例管理

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/agent/inspect/case/save` | 新增/更新用例 |
| DELETE | `/agent/inspect/case/{id}` | 删除用例 |
| GET | `/agent/inspect/case/list` | 用例列表（分页+筛选） |
| GET | `/agent/inspect/case/{id}` | 用例详情 |
| PUT | `/agent/inspect/case/batch-status` | 批量更新状态 |

### 7.5 配置管理

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/agent/config` | 获取配置 |
| PUT | `/agent/config` | 更新配置 |

---

## 8. 配置说明

### 8.1 配置优先级

对于每个配置项，优先级为：
```
用例表字段 > 数据库 api_inspect_config 表 > application.yml > 代码默认值
```

例如超时时间：
1. 先看 `api_inspect_case.timeout`（每条用例可单独设置）
2. 为空时用 `agent.inspect.default-timeout`（yml 配置）

### 8.2 关键参数调优建议

| 参数 | 建议值 | 说明 |
|---|---|---|
| `parallel-threads` | CPU 核心数 × 2 | 并行巡检的线程池核心大小 |
| `default-timeout` | 5000~10000ms | 根据接口实际响应时间调整 |
| `default-retry-count` | 2 | 重试次数过多会拖慢整体巡检 |
| `retry-interval` | 1000~3000ms | 给下游服务恢复的缓冲时间 |
| `llm.temperature` | 0.3 | 巡检分析需要稳定输出，不宜过高 |
| `log-retention-days` | 90 | 配合定时清理任务使用 |

---

## 9. 设计模式与技术亮点

### 9.1 使用的设计模式

| 模式 | 应用位置 | 说明 |
|---|---|---|
| **模板方法** | `LLMPromptTemplates` | 定义 Prompt 骨架，参数填充 |
| **策略模式** | `AgentCore` 串行/并行 | `executeMode` 决定执行策略 |
| **工厂方法** | `Result.ok()` / `Result.fail()` | 统一响应对象构造 |
| **铁路导向编程** | `HttpInvokeTool` / `RetryTool` | 不抛异常，返回结果对象 |
| **建造者模式** | OkHttp `Request.Builder` | 逐步构建 HTTP 请求 |

### 9.2 关键代码亮点

**1. 并发安全的上下文状态管理**
```java
// AgentContext — 用 volatile + CopyOnWriteArrayList 保证线程安全
private volatile int completedCount;       // volatile 保证可见性
private final List<InspectResult> results  // COW 保证并发写安全
        = new CopyOnWriteArrayList<>();
```

**2. 防重入设计**
```java
// 同一时间只允许一个巡检任务
if (currentContext != null && !currentContext.isFinished() && !currentContext.isStopped()) {
    throw new BusinessException(409, "已有巡检任务正在运行中");
}
```

**3. LLM 不可用的优雅降级**
```java
// LLM 禁用、API Key 未配、网络异常 — 全部返回 null 而非抛异常
// 巡检核心流程不依赖 LLM 也能正常运行
if (!config.isEnabled()) return null;
if (config.getApiKey() == null || config.getApiKey().isBlank()) return null;
```

**4. 参数的三级覆盖**
```java
// 用例级 > 全局配置级 > 代码默认值
int timeout = c.getTimeout() != null && c.getTimeout() > 0
        ? c.getTimeout() : agentConfig.getDefaultTimeout();
```

**5. 智能重试判断**
```java
// 只有临时性故障（502/503）才重试，其他错误不浪费重试次数
if (resp.success() && (resp.statusCode() == 502 || resp.statusCode() == 503)) {
    throw new RuntimeException("Retryable status");
}
```

**6. `record` 类型用于不可变数据传输**
```java
// HttpInvokeTool.HttpResponse 和 RetryTool.RetryResult 都用 record
// 线程安全 + 简洁 + 自带 equals/hashCode/toString
public record HttpResponse(boolean success, int statusCode, String body,
                           int elapsedMs, String error) {}
```

---

## 10. 扩展指南

### 10.1 添加新的断言规则

在 `AssertTool.assertResponse()` 方法中添加新的 `if (rule.containsKey("新规则名"))` 分支即可：

```java
// 示例：添加"响应体 JSON 数组长度"断言
if (rule.containsKey("arrayMinLength") && responseBody != null) {
    int minLen = rule.getIntValue("arrayMinLength");
    JSONArray arr = JSON.parseArray(responseBody);
    boolean pass = arr != null && arr.size() >= minLen;
    results.add(assertItem("arrayMinLength", ">= " + minLen,
            "size=" + (arr != null ? arr.size() : "null"), pass));
}
```

### 10.2 添加新的告警渠道

参考 `AlertService`，实现新的告警发送类：

```java
@Service
public class WechatAlertService {
    public void sendAlert(String taskNo, List<InspectResult> failures) {
        // 企业微信机器人 Webhook 格式
    }
}
```

然后在 `InspectController.start()` 的异步线程中添加调用即可。

### 10.3 添加新的 LLM Provider

`LLMClient` 已经兼容 OpenAI API 格式，只需修改 `application.yml`：
```yaml
llm:
  api-url: https://api.openai.com/v1/chat/completions    # OpenAI
  # 或
  api-url: https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions  # 通义千问
  # 或
  api-url: http://localhost:11434/v1/chat/completions     # 本地 Ollama
```

### 10.4 启用定时巡检

1. 修改 `application.yml`：`agent.inspect.cron-enabled: true`
2. 或在数据库 `api_inspect_config` 表中插入：`INSERT INTO api_inspect_config (config_key, config_value) VALUES ('cron_enabled', 'true')`
3. Cron 表达式默认为每天凌晨 3 点

### 10.5 添加 Redis 分布式锁（防止多实例重复执行）

```java
// 在 InspectScheduler.scheduledInspect() 开头添加
String lockKey = "inspect:schedule:lock";
Boolean locked = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, "1", Duration.ofMinutes(10));
if (Boolean.FALSE.equals(locked)) {
    log.info("另一实例正在执行定时巡检，跳过");
    return;
}
```

### 10.6 添加接口性能分析

在 `AgentCore.executeOneCase()` 中，`InspectResult` 已包含 `responseTime`，可以在任务完成后统计：
```java
double avgTime = logs.stream()
        .filter(l -> l.getSuccess() == 1)
        .mapToInt(ApiInspectLog::getResponseTime)
        .average().orElse(0);
task.setAvgResponseTime((int) avgTime);
```

---

## 11. 常见问题排查

| 问题 | 原因 | 解决方案 |
|---|---|---|
| 启动巡检报 409 | 已有任务在运行 | 等待完成或调用 stop 接口 |
| AI 分析返回 null | LLM 未启用/API Key 未配置/网络不通 | 检查 yml 配置，确认 API 地址可达 |
| 并行巡检部分用例未完成 | 5 分钟整体超时 | 增大 `CompletableFuture.get()` 的超时时间 |
| 数据库连接池耗尽 | 并行线程数过大 | 调小 `parallel-threads` 或增大 Hikari `maximum-pool-size` |
| 定时巡检不触发 | cron_enabled 未开启 | 检查数据库 `api_inspect_config` 中 `cron_enabled` 值和 yml 中 `cron-enabled` |
| 告警不发 | alert.enabled=false 或 webhook 为空 | 配置 `application.yml` 的 `alert` 部分 |
| 任务编号总是 001 | 旧版本 bug | 已修复 — `AtomicInteger` 改为类级别字段 |
| Docker 部署时区不对 | 容器默认 UTC | 添加 `-e TZ=Asia/Shanghai` 或 Dockerfile 中设置 |

---

## 附录 A：思维导图 — 系统能力全景

```
                        API 巡检 Agent
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
   巡检执行能力           AI 分析能力           运维能力
        │                    │                    │
   ┌────┴────┐          ┌────┴────┐          ┌────┴────┐
   │ HTTP调用 │          │ 异常分类 │          │ 定时调度 │
   │ 断言校验 │          │ 根因分析 │          │ 钉钉告警 │
   │ 重试机制 │          │ 修复建议 │          │ 报告生成 │
   │ 超时控制 │          │ 巡检摘要 │          │ 日志追溯 │
   │ 串行/并行│          │ 对话复盘 │          │ 配置管理 │
   └─────────┘          └─────────┘          └─────────┘
```

## 附录 B：关键文件速查表

| 想要了解... | 看这个文件 |
|---|---|
| 整体流程怎么跑通的 | `agent/AgentCore.java` |
| HTTP 请求怎么发的 | `tool/HttpInvokeTool.java` |
| 断言规则怎么配置 | `tool/AssertTool.java` |
| LLM 怎么调用的 | `llm/LLMClient.java` |
| Prompt 怎么写的 | `llm/LLMPromptTemplates.java` |
| 报告长什么样 | `dto/ReportDTO.java` + `report/ReportService.java` |
| 告警怎么发的 | `alert/AlertService.java` |
| 定时任务怎么配 | `scheduler/InspectScheduler.java` |
| 数据库表结构 | 本文第 2.2 节 SQL |
| API 怎么调用 | `controller/InspectController.java` |
| 代码里有什么设计模式 | 本文第 9.1 节 |

遇到任何问题，欢迎提 Issue 或 PR。
