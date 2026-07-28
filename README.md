# 启航AI协作系统-团队业务协作平台

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0-brightgreen.svg)](https://spring.io/projects/spring-ai)

`AI Agent` `Spring AI` `Spring Boot` `Tool Calling` `Function Calling` `多知识库` `RAG` `Ollama` `DeepSeek` `飞书机器人` `MySQL` `Thymeleaf` `团队协作` `数据集管理` `日报生成` `多模态`

> **启航AI协作系统是一款面向小团队的轻量级业务协作平台，融合AI能力帮助团队高效管理任务、项目、客户关系与日常办公。系统支持多成员协同、任务分配、进度追踪、文档共享、AI智能助手等功能，覆盖团队协作全场景。可灵活搭建CRM客户管理系统、进销存管理系统、项目管理看板、工单系统、人事审批等业务模块，满足不同团队的个性化需求。无需复杂部署，开箱即用，让团队专注于业务本身，提升协作效率。**

---

## 目录

- [核心定位](#-核心定位)
- [功能全景](#-功能全景)
- [快速开始](#-快速开始)
- [功能详解](#-功能详解)
- [技术架构](#-技术架构)
- [数据存储](#-数据存储)
- [项目结构](#-项目结构)
- [配置参考](#-配置参考)

---

## 🌟 核心定位

启航AI协作系统-团队业务协作平台是一个**企业级 AI 协作智能体系统**，不是简单的 AI 聊天工具。

它的架构围绕三个核心理念：

| 组件 | 角色 | 说明 |
|------|------|------|
| 📚 **知识库** | 长期记忆 | 数据库存储的笔记文章，支持版本管理、多人协作 |
| 📦 **数据集** | 协作血液 | 结构化业务数据（客户、Bug、任务、订单），多人读写，AI 路由 |
| 🧠 **AI 编排** | 决策中枢 | Spring AI ChatClient + ToolCallingAdvisor，自动分拣信息、指派任务 |

AI 能自主判断：你说"下午和 ABC 公司聊了，报价 80 万" → AI 自动记笔记 + 更新数据集 + 推送主管审批。

---

## 🚀 功能全景

```
┌─────────────────────────────────────────────────────────────────────┐
│                     启航AI协作系统-团队业务协作平台                              │
│                                                                     │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                     AI 编排层                                  │  │
│  │    ChatClient + ToolCallingAdvisor (Spring AI 2.0)            │  │
│  │    用户输入 → AI 决策 → 调用工具 → 返回结果 → 推送通知         │  │
│  └──────┬────────────────────┬──────────────────────┬────────────┘  │
│         │                    │                      │               │
│  ┌──────▼──────────┐  ┌─────▼──────────────┐  ┌─────▼───────────┐  │
│  │   NoteTools     │  │    DataTools       │  │  CollabTools    │  │
│  │  知识库工具      │  │    数据集工具       │  │  协作工具        │  │
│  │ • readNote      │  │ • listDatasets    │  │ • assignTask    │  │
│  │ • writeNote     │  │ • queryRecords    │  │ • sendNotify    │  │
│  │ • searchNotes   │  │ • addRecord       │  │ • submitApproval│  │
│  │ • listNotes     │  │ • updateRecord    │  │ • getMyTasks    │  │
│  │ • getVersion    │  │ • deleteRecord    │  │ • getActivity   │  │
│  └──────┬──────────┘  └─────┬──────────────┘  └──────┬──────────┘  │
│         │                   │                        │             │
│  ┌──────▼───────────────────▼────────────────────────▼──────────┐  │
│  │                           存储层                               │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐  │  │
│  │  │ 知识库 (MySQL) │  │ 数据集 (MySQL) │  │ 协作 (MySQL)      │  │  │
│  │  │ note_articles │  │ data_center* │  │ activity_log      │  │  │
│  │  │ note_versions │  │              │  │ notifications     │  │  │
│  │  └──────────────┘  └──────────────┘  │ approval_requests  │  │  │
│  │  ┌──────────────┐  ┌──────────────┐  │ automation_rules   │  │  │
│  │  │ 用户权限      │  │ 聊天/向量     │  └────────────────────┘  │  │
│  │  │ sys_user     │  │ sessions     │                          │  │
│  │  │ sys_role     │  │ messages     │                          │  │
│  │  └──────────────┘  └──────────────┘                          │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                     │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                    业务功能                                    │  │
│  │  💬 AI 对话  📦 数据中心  📊 综合日报  🖼️ 识图                │  │
│  │  📋 任务管理  ⏰ 提醒  🧩 模块  🔔 动态流                      │  │
│  │  📄 知识库  🏠 首页看板  ⚙️ 配置  👥 用户管理                  │  │
│  │  🤖 自动化规则  📋 审批中心  🔔 通知中心                       │  │
│  └───────────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                    集成通道                                    │  │
│  │  🌐 Web UI (Thymeleaf)  💬 飞书 WebSocket  ⏰ 定时任务         │  │
│  └───────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

| 功能 | 说明 | 入口 |
|------|------|------|
| 💬 **AI 对话** | 工具编排 + 语义召回 + 连续对话，按用户隔离 | `/chat` |
| 📦 **数据中心** | 多数据集管理，Schema 定义，负责人/审批/状态流转 | `/data` |
| 📊 **综合日报** | AI 读取笔记 + 查询数据集，结合生成日报，推送团队 | 首页/定时 |
| 🖼️ **图片识别** | 多模态分析，结果写入知识库 | `/image` |
| 📋 **任务管理** | 看板视图，AI 自动指派负责人 | `/tasks` |
| ⏰ **提醒系统** | 定时提醒，飞书推送 | `/reminders` |
| 📄 **知识库** | 笔记文章管理，版本历史，多人协作编辑 | `/kb` |
| 🔔 **动态流** | AI 操作日志：谁说了什么 → AI 做了什么 → 推送给谁 | `/activity` |
| 🔔 **通知中心** | 我的通知、待审批、已处理 | `/notifications` |
| 🤖 **自动化规则** | 可视化配置触发条件 + 执行动作 | `/automation` |
| 👥 **用户管理** | 用户/角色/权限管理 | `/admin/users` |
| 🏠 **首页看板** | 我的待办、AI 推送、知识库摘要 | `/` |
| ⚙️ **配置管理** | LLM / 知识库 / 系统设置 | `/config` |

---

## 🚀 快速开始

### 环境要求

| 组件 | 要求 | 说明 |
|------|------|------|
| JDK | 17+ | |
| Maven | 3.8+ | 仅编译需要 |
| MySQL | 8.0+ | 业务数据库 |
| LLM API Key | 必填 | DeepSeek / 商汤 / 智谱等兼容 OpenAI 格式 |
| Ollama | 可选 | 本地语义检索加速（bge-m3） |

### 启动

```bash
# 1. 初始化数据库
mysql -h <host> -u <user> -p < docs/init.sql

# 2. 编译
mvn package -q

# 3. 启动（使用 dev 配置）
java -jar target/ai-assistant-1.0.0.jar --spring.profiles.active=dev

# 4. 浏览器打开 http://localhost:6790
```

### 支持的 LLM

兼容所有 OpenAI 格式的 API 提供商，Web 页面配置 Base URL 和 API Key 即可：

| 类型 | 说明 |
|------|------|
| 文本模型 | DeepSeek、智谱 GLM、商汤、Ollama 等任意 OpenAI 兼容接口 |
| 多模态模型 | 支持图片识别的模型（如商汤日日新），需在 Web 页面标记为 multimodal |
| Embedding 模型 | Ollama 本地 或 在线 API（如硅基流动） |

---

## ✨ 功能详解

### 1. AI 工具编排（核心能力）

AI 通过 Spring AI 的 `ToolCallingAdvisor` 自主判断何时调用工具，无需手写 ReAct 循环。

**NoteTools — 知识库操作（数据库存储）**

| 工具 | 功能 | 触发场景 |
|------|------|---------|
| `readNote` | 读取笔记文章 | AI 需要了解已有知识 |
| `writeNote` | 创建/更新笔记（自动版本管理） | 记录会议纪要、工作记录 |
| `searchNotes` | 全文搜索 + 语义搜索 | 查找相关笔记 |
| `listNotes` | 按目录/标签列出笔记 | 探索知识库结构 |
| `getVersion` | 查看笔记历史版本 | 回溯变更内容 |

**DataTools — 数据集结构化操作**

| 工具 | 功能 | 触发场景 |
|------|------|---------|
| `listDatasets` | 列出所有数据集 | 查看有哪些可用数据 |
| `queryRecords` | 按条件精确查询 | "待修复的 Bug"、"报价阶段的客户" |
| `searchRecords` | 关键词模糊搜索 | 不确定具体字段时全文搜索 |
| `addRecord` | 添加记录（自动设创建人） | 记录新客户、新 Bug |
| `updateRecord` | 更新记录（自动触发动作者） | 修改状态、跟进信息 |
| `deleteRecord` | 删除记录 | 移除错误数据 |
| `getRecord` | 查看单条详情 | 获取完整记录信息 |

**CollabTools — 协作工具（新增）**

| 工具 | 功能 | 触发场景 |
|------|------|---------|
| `assignTask` | 指派任务给负责人 | "这个 Bug 分配给张三" |
| `sendNotification` | 推送通知给指定用户 | 通知相关人员 |
| `submitForApproval` | 提交审批 | "报价 80 万需要主管审批" |
| `getMyTasks` | 查看我的待办 | "我今天有什么任务？" |
| `getActivityLog` | 查看动态流 | "最近发生了什么？" |

**典型场景**：

| 你说 | AI 执行 | 结果 |
|------|---------|------|
| "下午和 ABC 公司聊了，报价 80 万" | `addRecord("客户", {...})` + 检测金额 > 50 万 → `submitForApproval` | 记入数据集 + 推送主管审批 |
| "这个 Bug #1024 分配给小王" | `updateRecord("Bug", {status:"进行中"})` + `assignTask("小王", ...)` | 更新状态 + 通知小王 |
| "今天有什么任务？" | `getMyTasks(当前用户)` | 列出待办事项 |
| "生成本周周报" | 查数据集 + 读笔记 → 综合输出 | 保存+推送团队 |

### 2. 企业知识库

笔记从本地 MD 文件迁移到数据库存储，支持多人协作。

**核心特性**：
- **数据库存储**：`note_articles` 表，支持全文检索
- **版本管理**：每次修改保留历史版本，可回溯对比
- **权限控制**：按知识库设置可见范围（公开/部门/指定成员）
- **多人协作**：多人同时编辑，记录编辑人
- **导入导出**：支持导入本地 MD 文件，导出为 Markdown

### 3. 数据中心（数据集系统）

结构化数据是协作的核心载体，所有业务数据围绕数据集流转。

**核心设计**：
- 记录分两部分：固定字段（编号、类型、状态、创建人、负责人）+ 动态字段（Schema 定义）
- Schema 自由定义：字段名、类型、下拉选项
- 每条记录支持**负责人指派**、**审批流转**、**状态管理**

**新增协作字段**：

| 字段 | 说明 |
|------|------|
| `created_by` | 创建人（AI 自动填写） |
| `assigned_to` | 负责人（AI 自动指派或手动指定） |
| `assigned_at` | 指派时间 |
| `approval_status` | 审批状态：待审批 / 已通过 / 已驳回 |
| `approved_by` | 审批人 |
| `approved_at` | 审批时间 |

**使用示例**：
- 建一个"客户跟进"数据集，字段：公司名、联系人、金额、阶段、负责人
- 建一个"Bug追踪"数据集，字段：标题、项目、优先级、状态、负责人
- 直接问 AI："本周有哪些客户在报价阶段？"、"优先级高的 Bug 有哪些？"

### 4. AI 协作流

AI 自动分拣信息、路由任务、推送通知，人只做决策和审批。

**核心流程**：
1. 用户对话输入 → AI 理解意图
2. AI 判断：记笔记？更新数据集？指派任务？推送通知？
3. AI 执行操作 + 记录到 `activity_log`（动态流）
4. 如需审批 → 创建 `approval_request` → 通知审批人
5. 如需通知 → 创建 `notification` → 推送给目标用户

**动态流页面**：展示所有 AI 操作日志，团队可追溯「谁说了什么 → AI 做了什么 → 结果如何」。

### 5. 自动化工作流

从被动回答进化为主动执行，支持条件触发和定时任务。

| 规则类型 | 触发条件 | 执行动作 |
|---------|---------|---------|
| 数据集新增 | 客户记录金额 > 50 万 | 自动提交审批 |
| 数据集更新 | Bug 状态改为"已修复" | 通知测试人员验证 |
| 定时任务 | 每天 09:00 | AI 巡检异常数据并推送 |
| 外部 Webhook | 外部系统调用 API | AI 分析处理并写入数据集 |

### 6. 多知识库系统

一个实例同时管理多个知识库，数据完全隔离，支持按部门/角色控制可见范围。

| 维度 | 隔离方式 |
|------|---------|
| 用户权限 | sys_user_role 控制知识库可见性 |
| 笔记文章 | note_articles.kb_id 过滤 |
| 数据集 | data_center_datasets 按知识库/部门隔离 |
| 聊天历史 | sessions.kb_id + 向量召回限定 |
| 文件操作 | NoteTools 路径基于当前 KB |

### 7. 飞书集成

| 方式 | 说明 |
|------|------|
| Webhook 推送 | 日报推送、提醒推送、审批通知 |
| WebSocket 长连接 | 双向实时通信，AI 自动回复群消息 |
| 消息路由 | AI 识别群消息中的 @提及，自动响应 |

---

## 🏗️ 技术架构

### 技术栈

| 类别 | 技术 |
|------|------|
| 后端框架 | Spring Boot 4.1 |
| AI 框架 | Spring AI 2.0 (ChatClient + @Tool + ToolCallingAdvisor) |
| LLM | DeepSeek（OpenAI 兼容协议，可切换） |
| 嵌入模型 | Ollama (bge-m3) |
| ORM | MyBatis-Plus 3.5.16 |
| 数据库 | MySQL 8.0+ |
| 前端 | Thymeleaf + 原生 JS |
| IM 集成 | 飞书 OpenAPI SDK + WebSocket |
| Excel | Apache POI 5.4.0 |

### AI 调用链路

```
                     ┌────────────────────────────┐
                     │   LlmConfigResolver        │
                     │   优先级: Web > 环境变量     │
                     └──────────┬─────────────────┘
                                │
          ┌─────────────────────┼─────────────────────┐
          ▼                     ▼                     ▼
   ┌──────────┐       ┌──────────────────┐   ┌──────────────┐
   │LlmService│       │ NoteAssistantSvc │   │AgentAnalysis │
   │ 直连 LLM  │       │  对话 + 工具编排   │   │ 日报/分析生成  │
   └──────────┘       └────────┬─────────┘   └──────┬───────┘
                              │                     │
                     ┌────────▼─────────┐           │
                     │  ToolRegistry    │           │
                     │  @Tool 自动发现   │           │
                     └──┬───────┬──────┘           │
                        │       │                  │
                 ┌──────▼──┐ ┌──▼────────┐         │
                 │NoteTools│ │DataTools  │         │
                 │ 知识库操作 │ │ 数据集操作 │         │
                 └─────────┘ └───────────┘         │
                              │                     │
                              └─────────┬───────────┘
                                        ▼
                              ┌────────────────────┐
                              │ DeepSeekChatModel  │
                              │ (OpenAI 兼容协议)   │
                              └────────┬───────────┘
                                       ▼
                              LLM API (DeepSeek/商汤/智谱/Ollama)
```

### 工具自注册机制

`ToolRegistry` 通过反射扫描所有 `@Component` 中的 `@Tool` 方法，自动注册到 `ChatClient`。新增工具只需：

1. 写一个 `@Component` 类
2. 加 `@Tool` 方法
3. 无需修改任何 Service 或 Controller

---

## 📂 数据存储

| 数据类型 | 存储位置 | 说明 |
|---------|---------|------|
| 知识库笔记 | MySQL `note_articles` | 数据库存储，支持版本管理 |
| 笔记版本 | MySQL `note_versions` | 历史版本回溯 |
| 会话/消息 | MySQL `sessions` + `messages` | 按用户隔离 |
| 对话向量 | MySQL `turn_embeddings` | 语义检索 |
| 数据集 | MySQL `data_center_datasets` + `data_center_records` | 结构化业务数据 |
| 用户权限 | MySQL `sys_user` + `sys_role` + `sys_user_role` | RBAC 权限控制 |
| 协作动态 | MySQL `activity_log` | AI 操作日志 |
| 通知 | MySQL `notifications` | 消息通知 |
| 审批 | MySQL `approval_requests` | 审批流程 |
| 自动化 | MySQL `automation_rules` + `automation_logs` | 自动化规则和执行记录 |
| AI 分析 | MySQL `ai_analysis` | 日报/分析报告 |
| 配置 | `config.json` | LLM 凭据、飞书配置、全局设置 |

---

## 🏗️ 项目结构

```
src/main/java/com/laoqi/assistant/
├── AssistantApplication.java            # 启动入口
│
├── controller/                          # Web 控制器
│   ├── ChatController.java              # AI 对话（SSE 流式）
│   ├── IndexController.java             # 首页
│   ├── ConfigController.java            # 配置页
│   ├── KnowledgeBaseController.java     # 知识库 CRUD
│   ├── DataSetController.java           # 数据集 REST API
│   ├── DataPageV2Controller.java        # 数据中心页面
│   ├── ImageRecognitionController.java  # 图片识别
│   ├── TaskController.java              # 任务管理
│   ├── ReminderController.java          # 提醒管理
│   ├── ModuleController.java            # 模块系统
│   ├── AiGuideController.java           # AI 引导
│   ├── LogController.java               # 操作日志
│   ├── ActivityController.java          # 动态流（新增）
│   ├── NotificationController.java      # 通知中心（新增）
│   ├── AutomationController.java        # 自动化规则（新增）
│   ├── AdminController.java             # 用户管理（新增）
│   ├── CollectorController.java         # 采集器
│   ├── CollectorPageController.java     # 采集器页面
│   ├── HealthController.java            # 健康检查
│   └── GlobalModelAdvice.java           # 全局模板变量
│
├── service/                             # 业务服务
│   ├── NoteAssistantService.java        # AI 对话编排
│   ├── AgentAnalysisService.java        # 自动分析
│   ├── ReportService.java               # 日报生成
│   ├── LlmService.java                  # LLM 直连
│   ├── LlmConfigResolver.java           # LLM 配置解析
│   ├── ToolRegistry.java                # 工具自注册中心
│   ├── NoteTools.java                   # 知识库操作工具
│   ├── DataTools.java                   # 数据集操作工具
│   ├── CollabTools.java                 # 协作工具（新增）
│   ├── KnowledgeBaseService.java        # 知识库管理
│   ├── SessionService.java              # 会话管理 + 语义检索
│   ├── SchedulerService.java            # 定时任务
│   ├── ReminderService.java             # 提醒管理
│   ├── TaskService.java                 # 任务管理
│   ├── ModuleService.java               # 模块系统
│   ├── ModuleDataService.java           # 模块数据操作
│   ├── ConfigService.java               # 配置读写
│   ├── TodoService.java                 # 待办解析
│   ├── LogService.java                  # 操作日志
│   ├── ActivityService.java             # 动态流（新增）
│   ├── NotificationService.java         # 通知服务（新增）
│   ├── ApprovalService.java             # 审批服务（新增）
│   ├── AutomationService.java           # 自动化规则（新增）
│   ├── UserService.java                 # 用户管理（新增）
│   ├── FeishuService.java               # 飞书推送
│   ├── FeishuLongConnectionService.java # 飞书长连接
│   └── db/                              # MyBatis-Plus DB 服务
│
├── datacenter/                          # 数据中心模块
│   ├── DataSetService.java              # 数据集核心业务
│   ├── DataSetImportService.java        # 数据导入
│   ├── DataModuleService.java           # 数据模块
│   └── model/                           # 数据模型
│
├── entity/                              # MyBatis-Plus 实体
├── mapper/                              # MyBatis-Plus Mapper
├── model/                               # POJO 模型
└── util/                                # 工具类

src/main/resources/
└── templates/
    ├── 2.0/                             # v2 UI
    │   ├── layout.html
    │   ├── data.html
    │   ├── activity.html                # 动态流（新增）
    │   ├── notifications.html           # 通知中心（新增）
    │   ├── automation.html              # 自动化规则（新增）
    │   ├── admin-users.html             # 用户管理（新增）
    │   └── ...
    └── 1.0/                             # v1 界面
```

---

## ⚙️ 配置参考

```yaml
server:
  port: 6790
  address: 0.0.0.0

spring:
  profiles:
    active: dev
  datasource:
    # 由 application-dev.yml 覆盖
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/qihang-work-ai
    username: root
    password: root
  ai:
    deepseek:
      api-key: ${DEEPSEEK_API_KEY:dummy}
    retry:
      max-attempts: 3
      backoff:
        initial-interval: 2s

app:
  config-dir: ${ASSISTANT_CONFIG_DIR:.}
  timezone: Asia/Shanghai
  max-history-chars: 6000
```

> **说明**：`application.yml` 中配置默认占位值，真实凭据由 `application-dev.yml`（已 gitignore）覆盖。

---

## 📖 完整设计文档

见 [docs/pro-design.md](docs/pro-design.md) 包含完整的 Pro 版系统设计、数据库设计（22 张表）和功能规划。

---

> **知识库是记忆，数据集是血液，AI 是协作者。**




## 📱 关注我

|                   公众号：启航电商ERP                   |                   个人号：码农老齐                   |
|:-----------------------------------------------:|:--------------------------------------------:|
|                 产品动态·行业方案·客户案例                  |                技术实战·开源故事·创业心得                |
| <img src="docs/wxmp_qherp.jpg" width="200px" /> | <img src="docs/wxmp_qi.jpg" width="200px" /> |


**感谢关注！我希望将从事电商 10 余年的行业经验沉淀在代码中，帮助大家真正提升经营效率。**

💖 如果项目对您有帮助，请点个 **Star ⭐** 给予鼓励！


---