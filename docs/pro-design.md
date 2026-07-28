# 启航 AI 工作台 — 专业版系统设计

## 1. 产品定位

启航 AI 工作台专业版是一个**企业级 AI 协作智能体系统**，定位为多笔记库与多代码库融合版本，支持多用户协作、AI 自动化工作流、业务数据集管理，面向团队级知识管理与自动化协作场景。

### 核心理念

- **知识库是记忆** — 非结构化知识（文档、笔记、规范）存储在数据库，支持版本管理
- **数据集是血液** — 结构化业务数据（客户、Bug、任务、订单）是协作的核心载体
- **AI 是协作者** — 自动分拣信息、路由任务、推送通知，人只做决策和审批

---

## 2. 系统架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                        接入层                                        │
│    Web UI (Thymeleaf)    飞书/钉钉/企微    API 接口   定时任务       │
└────────────────────────┬────────────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────────────┐
│                        AI 编排层                                    │
│          ChatClient + ToolCallingAdvisor (Spring AI 2.0)           │
│          用户输入 → AI 决策 → 调用工具 → 返回结果 → 推送           │
└──────┬────────────────────┬──────────────────────────┬──────────────┘
       │                    │                          │
┌──────▼──────────┐  ┌──────▼──────────┐  ┌───────────▼──────────────┐
│   NoteTools     │  │   DataTools     │  │   CollaborationTools    │
│  知识库工具      │  │  数据集工具      │  │  协作工具（新增）        │
│ • readNote      │  │ • listDatasets  │  │ • assignTask            │
│ • writeNote     │  │ • queryRecords  │  │ • sendNotification      │
│ • searchNotes   │  │ • addRecord     │  │ • submitForApproval     │
│ • listNotes     │  │ • updateRecord  │  │ • getMyTasks            │
│ • getVersion    │  │ • deleteRecord  │  │ • getActivityLog        │
└──────┬──────────┘  └──────┬──────────┘  └───────────┬──────────────┘
       │                    │                          │
┌──────▼────────────────────▼──────────────────────────▼──────────────┐
│                          存储层                                      │
│  ┌────────────────┐  ┌──────────────────┐  ┌────────────────────┐   │
│  │  知识库 (MySQL)  │  │  数据集 (MySQL)   │  │  协作 (MySQL)      │   │
│  │  note_articles  │  │  data_center_*   │  │  activity_log     │   │
│  │  note_versions  │  │                  │  │  notifications    │   │
│  └────────────────┘  └──────────────────┘  └────────────────────┘   │
│  ┌────────────────┐  ┌──────────────────┐                           │
│  │  用户权限 (MySQL)│  │  聊天/向量 (MySQL)│                           │
│  │  sys_user       │  │  sessions       │                           │
│  │  sys_role       │  │  messages       │                           │
│  │  sys_user_role  │  │  turn_embeddings│                           │
│  └────────────────┘  └──────────────────┘                           │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3. 核心功能模块

### 3.1 用户与权限系统

| 功能 | 说明 |
|------|------|
| 用户管理 | 账号/密码/姓名/部门/角色 |
| 角色管理 | 管理员、主管、成员、只读 |
| 权限控制 | 知识库级、数据集级、操作级（CRUD） |
| 登录认证 | 账号密码 / 飞书扫码 / SSO |

### 3.2 企业知识库

| 功能 | 说明 |
|------|------|
| 笔记管理 | 数据库存储，支持富文本/Markdown |
| 版本管理 | 每次修改保留历史版本，可回滚 |
| 权限控制 | 按知识库设置可见范围（公开/部门/指定成员） |
| 全文检索 | MySQL 全文索引 + 向量语义检索 |
| 导入导出 | 支持导入本地 MD 文件，导出为 MD/PDF |
| 多人协作 | 多人同时编辑，行级锁 + 版本对比 |

### 3.3 业务数据集

| 功能 | 说明 |
|------|------|
| 数据集定义 | Schema 自定义，字段名/类型/下拉选项 |
| 记录管理 | 增删改查，导入导出 JSON/Excel |
| 负责人字段 | 每条记录可指定负责人，AI 自动指派 |
| 状态流转 | 自定义状态（如：待审批→进行中→已完成） |
| 审批流程 | 数据集记录提交审批，主管审核后生效 |
| AI 查询 | 自然语言查询："本周有哪些报价阶段的客户？" |

### 3.4 AI 协作流

| 功能 | 说明 |
|------|------|
| 动态流 | 展示 AI 所有操作日志：谁说了什么→AI 做了什么→推送给谁 |
| 任务指派 | AI 自动识别任务并 `@负责人` 推送 |
| 审批通知 | AI 生成内容→提交审批→审批通过/驳回→通知相关人 |
| 定时巡检 | AI 每天定时检查数据集异常→自动推送 |
| 事件驱动 | 数据集变更/飞书消息触发→AI 自动执行工作流 |

### 3.5 自动化工作流

| 功能 | 说明 |
|------|------|
| 条件触发 | 数据集新增/更新/删除 → AI 自动执行动作 |
| 动作列表 | 推送通知、指派任务、更新记录、发送飞书消息 |
| 定时任务 | Cron 表达式定时执行 AI 分析/巡检 |
| 外部 Webhook | 外部系统调用 API → 触发 AI 处理 |

---

## 4. 数据库设计

### 4.1 用户与权限

```sql
-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `username`     VARCHAR(64)  NOT NULL UNIQUE COMMENT '登录账号',
    `password`     VARCHAR(256) NOT NULL COMMENT '密码（BCrypt 加密）',
    `real_name`    VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '真实姓名',
    `email`        VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `phone`        VARCHAR(32)  DEFAULT NULL COMMENT '手机号',
    `avatar`       VARCHAR(512) DEFAULT NULL COMMENT '头像 URL',
    `department`   VARCHAR(128) DEFAULT NULL COMMENT '部门',
    `status`       INT          NOT NULL DEFAULT 1 COMMENT '状态 0禁用 1启用',
    `feishu_id`    VARCHAR(128) DEFAULT NULL COMMENT '飞书 user_id',
    `created_at`   VARCHAR(32)  NOT NULL COMMENT '创建时间',
    `updated_at`   VARCHAR(32)  NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_department` (`department`),
    KEY `idx_feishu_id` (`feishu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户';

-- 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `role_code`   VARCHAR(64)  NOT NULL UNIQUE COMMENT '角色编码: admin / manager / member / reader',
    `role_name`   VARCHAR(64)  NOT NULL COMMENT '角色名称',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
    `created_at`  VARCHAR(32)  NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色';

-- 用户角色关联
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id`      BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `role_id` BIGINT NOT NULL COMMENT '角色 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联';
```

### 4.2 企业知识库

```sql
-- 知识库（改造）
CREATE TABLE IF NOT EXISTS `knowledge_bases` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `name`         VARCHAR(255) NOT NULL COMMENT '知识库名称',
    `description`  TEXT         DEFAULT NULL COMMENT '知识库描述',
    `owner_id`     BIGINT       NOT NULL COMMENT '所有者用户 ID',
    `visibility`   VARCHAR(32)  NOT NULL DEFAULT 'private' COMMENT '可见范围: private / department / public',
    `department`   VARCHAR(128) DEFAULT NULL COMMENT '可见部门（visibility=department 时有效）',
    `labels`       VARCHAR(500) DEFAULT NULL COMMENT '标签（逗号分隔）',
    `sort_order`   INT          DEFAULT 0 COMMENT '排序',
    `status`       INT          NOT NULL DEFAULT 1 COMMENT '状态 0禁用 1启用',
    `created_at`   VARCHAR(32)  NOT NULL COMMENT '创建时间',
    `updated_at`   VARCHAR(32)  NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_owner_id` (`owner_id`),
    KEY `idx_visibility` (`visibility`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库';

-- 知识库成员
CREATE TABLE IF NOT EXISTS `knowledge_base_members` (
    `id`      BIGINT NOT NULL AUTO_INCREMENT,
    `kb_id`   BIGINT NOT NULL COMMENT '知识库 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `role`    VARCHAR(32) NOT NULL DEFAULT 'member' COMMENT '角色: owner / editor / reader',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_kb_user` (`kb_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库成员';

-- 笔记文章（替代本地 MD 文件）
CREATE TABLE IF NOT EXISTS `note_articles` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `kb_id`        BIGINT       NOT NULL COMMENT '所属知识库 ID',
    `title`        VARCHAR(512) NOT NULL COMMENT '标题',
    `content`      LONGTEXT     NOT NULL COMMENT '内容（Markdown）',
    `content_html` LONGTEXT     DEFAULT NULL COMMENT '内容（HTML 渲染）',
    `author_id`    BIGINT       NOT NULL COMMENT '作者用户 ID',
    `editor_id`    BIGINT       DEFAULT NULL COMMENT '最后编辑者用户 ID',
    `parent_id`    BIGINT       DEFAULT NULL COMMENT '父级 ID（目录层级）',
    `sort_order`   INT          DEFAULT 0 COMMENT '排序',
    `status`       VARCHAR(32)  NOT NULL DEFAULT 'draft' COMMENT '状态: draft / published / archived',
    `version`      INT          NOT NULL DEFAULT 1 COMMENT '当前版本号',
    `tags`         VARCHAR(500) DEFAULT NULL COMMENT '标签（逗号分隔）',
    `created_at`   VARCHAR(32)  NOT NULL COMMENT '创建时间',
    `updated_at`   VARCHAR(32)  NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_kb_id` (`kb_id`),
    KEY `idx_author_id` (`author_id`),
    KEY `idx_editor_id` (`editor_id`),
    KEY `idx_status` (`status`),
    FULLTEXT KEY `ft_content` (`title`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记文章';

-- 笔记版本历史
CREATE TABLE IF NOT EXISTS `note_versions` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `note_id`     BIGINT       NOT NULL COMMENT '笔记 ID',
    `version`     INT          NOT NULL COMMENT '版本号',
    `title`       VARCHAR(512) NOT NULL COMMENT '标题',
    `content`     LONGTEXT     NOT NULL COMMENT '内容',
    `editor_id`   BIGINT       NOT NULL COMMENT '编辑者用户 ID',
    `change_log`  VARCHAR(500) DEFAULT NULL COMMENT '变更说明',
    `created_at`  VARCHAR(32)  NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_note_id` (`note_id`),
    KEY `idx_version` (`note_id`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记版本历史';
```

### 4.3 业务数据集（改造）

```sql
-- 数据集定义（改造）
CREATE TABLE IF NOT EXISTS `data_center_datasets` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT,
    `dataset_id`         VARCHAR(64)  NOT NULL UNIQUE COMMENT '数据集唯一标识',
    `name`               VARCHAR(255) NOT NULL COMMENT '数据集名称',
    `description`        TEXT         DEFAULT NULL COMMENT '数据集描述',
    `type`               VARCHAR(50)  DEFAULT NULL COMMENT '类型',
    `status`             VARCHAR(50)  DEFAULT NULL COMMENT '状态',
    `schema_json`        JSON         DEFAULT NULL COMMENT 'Schema 定义 JSON',
    `import_configs_json` JSON        DEFAULT NULL COMMENT '导入配置 JSON',
    `module_id`          VARCHAR(64)  DEFAULT NULL COMMENT '所属模块 ID',
    `owner_id`           BIGINT       DEFAULT NULL COMMENT '所有者用户 ID',
    `visibility`         VARCHAR(32)  NOT NULL DEFAULT 'private' COMMENT '可见范围: private / department / public',
    `department`         VARCHAR(128) DEFAULT NULL COMMENT '可见部门',
    `created_at`         VARCHAR(30)  DEFAULT NULL COMMENT '创建时间',
    `updated_at`         VARCHAR(30)  DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dataset_id` (`dataset_id`),
    KEY `idx_module_id` (`module_id`),
    KEY `idx_owner_id` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据中心 - 数据集定义';

-- 数据集记录（改造）
CREATE TABLE IF NOT EXISTS `data_center_records` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `record_id`     VARCHAR(64)  NOT NULL COMMENT '记录唯一标识',
    `dataset_id`    VARCHAR(64)  NOT NULL COMMENT '所属数据集 ID',
    `data_json`     JSON         NOT NULL COMMENT '动态字段数据 JSON',
    `source`        VARCHAR(50)  DEFAULT NULL COMMENT '数据来源: manual / ai / import',
    `content_hash`  VARCHAR(64)  DEFAULT NULL COMMENT '内容 MD5',
    `record_num`    VARCHAR(100) DEFAULT NULL COMMENT '记录编号',
    `record_type`   VARCHAR(100) DEFAULT NULL COMMENT '记录类型',
    `record_status` VARCHAR(100) DEFAULT NULL COMMENT '记录状态',
    `created_by`    BIGINT       DEFAULT NULL COMMENT '创建人用户 ID',
    `assigned_to`   BIGINT       DEFAULT NULL COMMENT '负责人用户 ID',
    `assigned_at`   VARCHAR(32)  DEFAULT NULL COMMENT '指派时间',
    `approved_by`   BIGINT       DEFAULT NULL COMMENT '审批人用户 ID',
    `approved_at`   VARCHAR(32)  DEFAULT NULL COMMENT '审批时间',
    `approval_status` VARCHAR(32) DEFAULT 'pending' COMMENT '审批状态: pending / approved / rejected',
    `created_at`    VARCHAR(30)  DEFAULT NULL COMMENT '创建时间',
    `updated_at`    VARCHAR(30)  DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_id` (`record_id`),
    KEY `idx_dataset_id` (`dataset_id`),
    KEY `idx_record_status` (`record_status`),
    KEY `idx_created_by` (`created_by`),
    KEY `idx_assigned_to` (`assigned_to`),
    KEY `idx_approval_status` (`approval_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据中心 - 数据集记录';
```

### 4.4 协作与通知

```sql
-- AI 活动日志（动态流核心）
CREATE TABLE IF NOT EXISTS `activity_log` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `kb_id`           BIGINT       DEFAULT NULL COMMENT '关联知识库 ID',
    `dataset_id`      VARCHAR(64)  DEFAULT NULL COMMENT '关联数据集 ID',
    `record_id`       VARCHAR(64)  DEFAULT NULL COMMENT '关联记录 ID',
    `action_type`     VARCHAR(64)  NOT NULL COMMENT '操作类型: create_note / update_note / create_record / update_record / assign_task / approval_request / approval_result / notification / report / analyze',
    `action_desc`     TEXT         NOT NULL COMMENT '操作描述（如：AI 新增客户 ABC 公司，金额 80 万）',
    `source`          VARCHAR(32)  NOT NULL DEFAULT 'ai' COMMENT '来源: user / ai / system',
    `triggered_by`    BIGINT       DEFAULT NULL COMMENT '触发人用户 ID',
    `target_user_id`  BIGINT       DEFAULT NULL COMMENT '目标用户 ID（推送给谁）',
    `metadata_json`   JSON         DEFAULT NULL COMMENT '扩展数据 JSON',
    `created_at`      VARCHAR(32)  NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_kb_id` (`kb_id`),
    KEY `idx_dataset_id` (`dataset_id`),
    KEY `idx_action_type` (`action_type`),
    KEY `idx_target_user_id` (`target_user_id`),
    KEY `idx_triggered_by` (`triggered_by`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 活动日志（动态流）';

-- 通知消息
CREATE TABLE IF NOT EXISTS `notifications` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`         BIGINT       NOT NULL COMMENT '接收人用户 ID',
    `title`           VARCHAR(255) NOT NULL COMMENT '通知标题',
    `content`         TEXT         NOT NULL COMMENT '通知内容',
    `type`            VARCHAR(64)  NOT NULL COMMENT '通知类型: task_assignment / approval_request / approval_result / reminder / system',
    `source_type`     VARCHAR(64)  DEFAULT NULL COMMENT '来源类型: dataset / knowledge_base / system',
    `source_id`       VARCHAR(128) DEFAULT NULL COMMENT '来源 ID',
    `is_read`         INT          NOT NULL DEFAULT 0 COMMENT '是否已读 0/1',
    `read_at`         VARCHAR(32)  DEFAULT NULL COMMENT '已读时间',
    `created_at`      VARCHAR(32)  NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_read` (`user_id`, `is_read`),
    KEY `idx_type` (`type`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知消息';

-- 审批流
CREATE TABLE IF NOT EXISTS `approval_requests` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `request_id`      VARCHAR(64)  NOT NULL UNIQUE COMMENT '审批请求唯一标识',
    `title`           VARCHAR(255) NOT NULL COMMENT '审批标题',
    `description`     TEXT         DEFAULT NULL COMMENT '审批说明',
    `source_type`     VARCHAR(64)  NOT NULL COMMENT '来源类型: dataset_record / knowledge_article / report',
    `source_id`       VARCHAR(128) NOT NULL COMMENT '来源 ID',
    `submitter_id`    BIGINT       NOT NULL COMMENT '提交人用户 ID',
    `approver_id`     BIGINT       NOT NULL COMMENT '审批人用户 ID',
    `status`          VARCHAR(32)  NOT NULL DEFAULT 'pending' COMMENT '状态: pending / approved / rejected',
    `comment`         TEXT         DEFAULT NULL COMMENT '审批意见',
    `processed_at`    VARCHAR(32)  DEFAULT NULL COMMENT '处理时间',
    `created_at`      VARCHAR(32)  NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_submitter_id` (`submitter_id`),
    KEY `idx_approver_id` (`approver_id`),
    KEY `idx_status` (`status`),
    KEY `idx_source` (`source_type`, `source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批请求';
```

### 4.5 自动化工作流

```sql
-- 自动化规则
CREATE TABLE IF NOT EXISTS `automation_rules` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `name`            VARCHAR(255) NOT NULL COMMENT '规则名称',
    `description`     TEXT         DEFAULT NULL COMMENT '规则描述',
    `trigger_type`    VARCHAR(64)  NOT NULL COMMENT '触发类型: record_created / record_updated / record_deleted / cron / webhook',
    `trigger_config`  JSON         NOT NULL COMMENT '触发配置 JSON（如数据集 ID、Cron 表达式）',
    `condition_expr`  TEXT         DEFAULT NULL COMMENT '条件表达式（SpEL）',
    `action_type`     VARCHAR(64)  NOT NULL COMMENT '动作类型: assign_task / send_notification / update_record / call_webhook / send_feishu',
    `action_config`   JSON         NOT NULL COMMENT '动作配置 JSON',
    `enabled`         INT          NOT NULL DEFAULT 1 COMMENT '是否启用 0/1',
    `created_by`      BIGINT       NOT NULL COMMENT '创建人用户 ID',
    `created_at`      VARCHAR(32)  NOT NULL COMMENT '创建时间',
    `updated_at`      VARCHAR(32)  NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_trigger_type` (`trigger_type`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自动化规则';

-- 自动化执行日志
CREATE TABLE IF NOT EXISTS `automation_logs` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `rule_id`         BIGINT       NOT NULL COMMENT '规则 ID',
    `trigger_type`    VARCHAR(64)  NOT NULL COMMENT '触发类型',
    `trigger_data`    JSON         DEFAULT NULL COMMENT '触发数据 JSON',
    `action_type`     VARCHAR(64)  NOT NULL COMMENT '动作类型',
    `action_result`   JSON         DEFAULT NULL COMMENT '动作执行结果 JSON',
    `status`          VARCHAR(32)  NOT NULL DEFAULT 'success' COMMENT '状态: success / failed',
    `error_message`   TEXT         DEFAULT NULL COMMENT '错误信息',
    `created_at`      VARCHAR(32)  NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_rule_id` (`rule_id`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自动化执行日志';
```

### 4.6 现有表改造

```sql
-- 会话表（加 user_id）
ALTER TABLE `sessions` ADD COLUMN `user_id` BIGINT DEFAULT NULL COMMENT '用户 ID' AFTER `id`;
ALTER TABLE `sessions` ADD INDEX `idx_user_id` (`user_id`);

-- 消息表（加 user_id）
ALTER TABLE `messages` ADD COLUMN `user_id` BIGINT DEFAULT NULL COMMENT '用户 ID' AFTER `id`;
ALTER TABLE `messages` ADD INDEX `idx_user_id` (`user_id`);

-- AI 分析表（加 user_id）
ALTER TABLE `ai_analysis` ADD COLUMN `user_id` BIGINT DEFAULT NULL COMMENT '用户 ID' AFTER `id`;
ALTER TABLE `ai_analysis` ADD INDEX `idx_user_id` (`user_id`);
```

---

## 5. 页面结构

| 页面 | 说明 | 变动 |
|------|------|------|
| **首页看板** | 我的待办、AI 推送、知识库摘要 | 改造 |
| **AI 对话** | 对话 + 工具编排，按用户隔离 | 改造 |
| **知识库** | 笔记浏览/编辑，多人协作，版本管理 | 重构 |
| **数据中心** | 数据集看板，按负责人/状态筛选 | 改造 |
| **动态流** | AI 操作日志，协作动态 | 新增 |
| **通知中心** | 我的通知、待审批、已处理 | 新增 |
| **自动化规则** | 可视化配置触发条件+执行动作 | 新增 |
| **用户管理** | 用户/角色/权限管理 | 新增 |
| **配置管理** | 知识库/数据集/LLM 配置 | 改造 |

---

## 6. 对比总结

| 维度 | master（个人版） | pro（专业版） |
|------|-----------------|--------------|
| 用户规模 | 单人 | 多人 |
| 知识库存储 | 本地 MD 文件 | 数据库 `note_articles` |
| 协作方式 | 无 | 指派、审批、通知、动态流 |
| 自动化 | 定时日报 | 条件触发 + 事件驱动 + 工作流 |
| 权限控制 | 无 | RBAC + 知识库/数据集级权限 |
| 数据库 | H2 | MySQL |
| 部署方式 | 本地运行 | 服务器部署，多用户访问 |