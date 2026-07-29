-- ============================================================
-- 启航 AI 协作平台 — MySQL 初始化脚本
-- 数据库: qihang-work-ai
-- ============================================================

-- 1. 知识库
CREATE TABLE IF NOT EXISTS `knowledge_bases` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `name`         VARCHAR(255) NOT NULL COMMENT '知识库名称',
    `notes_dir`    VARCHAR(500) NOT NULL COMMENT '笔记库根目录路径',
    `labels`       VARCHAR(500) DEFAULT NULL COMMENT '标签（逗号分隔）',
    `sort_order`   INT          DEFAULT 0 COMMENT '排序',
    `created_at`   VARCHAR(30)  DEFAULT NULL COMMENT '创建时间',
    `dir_settings` TEXT         DEFAULT NULL COMMENT '目录设置 JSON',
    `ignore_dirs`  TEXT         DEFAULT NULL COMMENT '忽略的目录（换行分隔）',
    `ignore_files` TEXT         DEFAULT NULL COMMENT '忽略的文件（换行分隔）',
    `auto_report`  INT          DEFAULT 0 COMMENT '是否自动生成日报 0/1',
    `feishu_push`  INT          DEFAULT 0 COMMENT '是否飞书推送 0/1',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库';

-- 2. AI 对话会话
CREATE TABLE IF NOT EXISTS `sessions` (
    `id`         VARCHAR(64)  NOT NULL COMMENT '会话 ID（手动生成）',
    `source`     VARCHAR(50)  DEFAULT NULL COMMENT '来源: web / feishu',
    `title`      VARCHAR(500) DEFAULT NULL COMMENT '会话标题',
    `chat_id`    VARCHAR(100) DEFAULT NULL COMMENT '飞书 chat_id',
    `chat_type`  VARCHAR(50)  DEFAULT NULL COMMENT '飞书 chat_type',
    `mode`       VARCHAR(50)  DEFAULT NULL COMMENT '模式',
    `created_at` VARCHAR(30)  DEFAULT NULL COMMENT '创建时间',
    `updated_at` VARCHAR(30)  DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_source` (`source`),
    KEY `idx_chat_id` (`chat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 对话会话';

-- 3. 消息记录
CREATE TABLE IF NOT EXISTS `messages` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `session_id` VARCHAR(64)  NOT NULL COMMENT '会话 ID',
    `source`     VARCHAR(50)  DEFAULT NULL COMMENT '来源',
    `role`       VARCHAR(20)  NOT NULL COMMENT '角色: user / assistant / system',
    `content`    LONGTEXT     NOT NULL COMMENT '消息内容',
    `mode`       VARCHAR(50)  DEFAULT NULL COMMENT '模式',
    `kb_id`      BIGINT       DEFAULT NULL COMMENT '知识库 ID',
    `created_at` VARCHAR(30)  DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_kb_id` (`kb_id`),
    KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息记录';

-- 4. 对话轮次向量（用于语义检索）
CREATE TABLE IF NOT EXISTS `turn_embeddings` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT,
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话 ID',
    `turn_order` INT         DEFAULT NULL COMMENT '轮次序号',
    `embedding`  JSON        DEFAULT NULL COMMENT '向量数据（JSON 数组）',
    `created_at` VARCHAR(30) DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_turn_order` (`turn_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话轮次向量';

-- 5. 笔记文件向量（用于语义检索）
CREATE TABLE IF NOT EXISTS `note_embeddings` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `kb_id`        BIGINT       NOT NULL COMMENT '知识库 ID',
    `file_path`    VARCHAR(500) NOT NULL COMMENT '文件相对路径',
    `chunk_index`  INT          DEFAULT NULL COMMENT '块序号',
    `path_context` VARCHAR(500) DEFAULT NULL COMMENT '路径上下文',
    `content`      LONGTEXT     NOT NULL COMMENT '文本内容',
    `embedding`    JSON         DEFAULT NULL COMMENT '向量数据（JSON 数组）',
    `content_hash` VARCHAR(64)  DEFAULT NULL COMMENT '内容 MD5',
    `created_at`   VARCHAR(30)  DEFAULT NULL COMMENT '创建时间',
    `updated_at`   VARCHAR(30)  DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_kb_id` (`kb_id`),
    KEY `idx_file_path` (`file_path`(255)),
    KEY `idx_content_hash` (`content_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记文件向量';

-- 6. 文件索引元数据（增量索引追踪）
CREATE TABLE IF NOT EXISTS `file_index_meta` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `kb_id`           BIGINT       NOT NULL COMMENT '知识库 ID',
    `file_path`       VARCHAR(500) NOT NULL COMMENT '文件相对路径（/ 分隔）',
    `last_modified`   BIGINT       DEFAULT NULL COMMENT '最后修改时间戳（毫秒）',
    `file_size`       BIGINT       DEFAULT NULL COMMENT '文件大小（字节）',
    `content_hash`    VARCHAR(64)  DEFAULT NULL COMMENT '内容 MD5',
    `last_indexed_at` VARCHAR(30)  DEFAULT NULL COMMENT '最后索引时间',
    `created_at`      VARCHAR(30)  DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_kb_file` (`kb_id`, `file_path`(255)),
    KEY `idx_content_hash` (`content_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件索引元数据';

-- 7. AI 分析（日报/目录分析/提示词模板）
CREATE TABLE IF NOT EXISTS `ai_analysis` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `kb_id`       BIGINT      DEFAULT NULL COMMENT '知识库 ID',
    `type`        VARCHAR(50) NOT NULL COMMENT '类型: daily_report / dir_analysis / daily_report_prompt / dir_analysis_prompt',
    `content`     LONGTEXT    NOT NULL COMMENT '内容（日报/分析报告/提示词）',
    `prompt`      LONGTEXT    DEFAULT NULL COMMENT '生成时使用的提示词',
    `dir_path`    VARCHAR(500) DEFAULT NULL COMMENT '目录分析的子目录路径',
    `report_date` VARCHAR(20) DEFAULT NULL COMMENT '报告日期 yyyy-MM-dd',
    `created_at`  VARCHAR(30) DEFAULT NULL COMMENT '创建时间',
    `updated_at`  VARCHAR(30) DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_kb_id` (`kb_id`),
    KEY `idx_type` (`type`),
    KEY `idx_report_date` (`report_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 分析（日报/目录分析）';

-- 8. 数据中心 - 模块
CREATE TABLE IF NOT EXISTS `data_center_modules` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `module_id`   VARCHAR(64)  NOT NULL COMMENT '模块唯一标识',
    `name`        VARCHAR(255) NOT NULL COMMENT '模块名称',
    `description` TEXT         DEFAULT NULL COMMENT '模块描述',
    `icon`        VARCHAR(255) DEFAULT NULL COMMENT '图标',
    `sort_order`  INT          DEFAULT 0 COMMENT '排序',
    `created_at`  VARCHAR(30)  DEFAULT NULL COMMENT '创建时间',
    `updated_at`  VARCHAR(30)  DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_module_id` (`module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据中心 - 模块';

-- 9. 数据中心 - 数据集定义
CREATE TABLE IF NOT EXISTS `data_center_datasets` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT,
    `dataset_id`         VARCHAR(64)  NOT NULL COMMENT '数据集唯一标识',
    `name`               VARCHAR(255) NOT NULL COMMENT '数据集名称',
    `description`        TEXT         DEFAULT NULL COMMENT '数据集描述',
    `type`               VARCHAR(50)  DEFAULT NULL COMMENT '类型',
    `status`             VARCHAR(50)  DEFAULT NULL COMMENT '状态',
    `schema_json`        JSON         DEFAULT NULL COMMENT 'Schema 定义 JSON',
    `import_configs_json` JSON        DEFAULT NULL COMMENT '导入配置 JSON',
    `collab_config_json` JSON        DEFAULT NULL COMMENT '协作配置 JSON（审批条件/审批人/通知规则）',
    `module_id`          VARCHAR(64)  DEFAULT NULL COMMENT '所属模块 ID',
    `created_at`         VARCHAR(30)  DEFAULT NULL COMMENT '创建时间',
    `updated_at`         VARCHAR(30)  DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dataset_id` (`dataset_id`),
    KEY `idx_module_id` (`module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据中心 - 数据集定义';

-- 10. 数据中心 - 数据集记录
CREATE TABLE IF NOT EXISTS `data_center_records` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `record_id`     VARCHAR(64)  NOT NULL COMMENT '记录唯一标识',
    `dataset_id`    VARCHAR(64)  NOT NULL COMMENT '所属数据集 ID',
    `data_json`     JSON         NOT NULL COMMENT '动态字段数据 JSON',
    `source`        VARCHAR(50)  DEFAULT NULL COMMENT '数据来源',
    `content_hash`  VARCHAR(64)  DEFAULT NULL COMMENT '内容 MD5',
    `record_num`    VARCHAR(100) DEFAULT NULL COMMENT '记录编号',
    `record_type`   VARCHAR(100) DEFAULT NULL COMMENT '记录类型',
    `record_status` VARCHAR(100) DEFAULT NULL COMMENT '记录状态',
    `assigned_to`   BIGINT       DEFAULT NULL COMMENT '负责人用户 ID',
    `assigned_at`   VARCHAR(32)  DEFAULT NULL COMMENT '指派时间',
    `approval_status` VARCHAR(32) DEFAULT 'none' COMMENT '审批状态: none / pending / approved / rejected',
    `approved_by`   BIGINT       DEFAULT NULL COMMENT '审批人用户 ID',
    `approved_at`   VARCHAR(32)  DEFAULT NULL COMMENT '审批时间',
    `created_at`    VARCHAR(30)  DEFAULT NULL COMMENT '创建时间',
    `updated_at`    VARCHAR(30)  DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_record_id` (`record_id`),
    KEY `idx_dataset_id` (`dataset_id`),
    KEY `idx_record_status` (`record_status`),
    KEY `idx_record_type` (`record_type`),
    KEY `idx_assigned_to` (`assigned_to`),
    KEY `idx_approval_status` (`approval_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据中心 - 数据集记录';

-- 11. 编码记录
CREATE TABLE IF NOT EXISTS `coding_records` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `time`        VARCHAR(30)  DEFAULT NULL COMMENT '时间',
    `start_time`  VARCHAR(30)  DEFAULT NULL COMMENT '开始时间',
    `end_time`    VARCHAR(30)  DEFAULT NULL COMMENT '结束时间',
    `duration`    INT          DEFAULT NULL COMMENT '耗时（秒）',
    `ai_engine`   VARCHAR(100) DEFAULT NULL COMMENT 'AI 引擎',
    `message`     LONGTEXT     DEFAULT NULL COMMENT '用户消息',
    `response`    LONGTEXT     DEFAULT NULL COMMENT 'AI 响应',
    `elapsed`     VARCHAR(50)  DEFAULT NULL COMMENT '耗时描述',
    `success`     TINYINT      DEFAULT 1 COMMENT '是否成功 0/1',
    `source`      VARCHAR(50)  DEFAULT NULL COMMENT '来源: debug / feishu',
    `project_dir` VARCHAR(500) DEFAULT NULL COMMENT '项目目录',
    PRIMARY KEY (`id`),
    KEY `idx_source` (`source`),
    KEY `idx_ai_engine` (`ai_engine`),
    KEY `idx_time` (`time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='编码记录';

-- 12. 采集任务
CREATE TABLE IF NOT EXISTS `collector_tasks` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `task_id`         VARCHAR(64)  NOT NULL COMMENT '任务唯一标识',
    `name`            VARCHAR(255) NOT NULL COMMENT '任务名称',
    `task_type`       VARCHAR(50)  DEFAULT NULL COMMENT '任务类型',
    `prompt_key`      VARCHAR(100) DEFAULT NULL COMMENT '提示词 Key',
    `url`             TEXT         DEFAULT NULL COMMENT '目标 URL',
    `cron_expression` VARCHAR(100) DEFAULT NULL COMMENT 'Cron 表达式',
    `enabled`         INT          DEFAULT 0 COMMENT '是否启用 0/1',
    `dataset_id`      VARCHAR(64)  DEFAULT NULL COMMENT '关联数据集 ID',
    `params_json`     JSON         DEFAULT NULL COMMENT '额外参数 JSON',
    `created_at`      VARCHAR(30)  DEFAULT NULL COMMENT '创建时间',
    `updated_at`      VARCHAR(30)  DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_id` (`task_id`),
    KEY `idx_dataset_id` (`dataset_id`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采集任务';

-- 13. LLM 模型配置
CREATE TABLE IF NOT EXISTS `llm_profiles` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `name`           VARCHAR(255) NOT NULL COMMENT '配置名称',
    `api_key`        TEXT         NOT NULL COMMENT 'API Key',
    `base_url`       VARCHAR(500) DEFAULT NULL COMMENT 'API Base URL',
    `model`          VARCHAR(255) DEFAULT NULL COMMENT '模型名称',
    `timeout`        INT          DEFAULT 60 COMMENT '超时时间（秒）',
    `is_default`     TINYINT      DEFAULT 0 COMMENT '是否默认 0/1',
    `vision_support` TINYINT      DEFAULT 0 COMMENT '（已废弃）是否支持多模态',
    `model_type`     VARCHAR(50)  DEFAULT 'text' COMMENT '模型类型: text / multimodal / embedding',
    PRIMARY KEY (`id`),
    KEY `idx_is_default` (`is_default`),
    KEY `idx_model_type` (`model_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LLM 模型配置';

-- 14. 提醒
CREATE TABLE IF NOT EXISTS `reminders` (
    `id`             VARCHAR(128) PRIMARY KEY COMMENT '提醒 ID',
    `name`           VARCHAR(255) NOT NULL COMMENT '提醒名称',
    `message`        TEXT         DEFAULT NULL COMMENT '提醒内容',
    `type`           VARCHAR(64)  NOT NULL COMMENT '提醒类型',
    `time`           VARCHAR(32)  DEFAULT '09:00' COMMENT '提醒时间',
    `date`           VARCHAR(32)  DEFAULT NULL COMMENT '提醒日期',
    `day_of_week`    INT          DEFAULT 0 COMMENT '星期几',
    `day_of_month`   INT          DEFAULT 1 COMMENT '每月几号',
    `month_day`      VARCHAR(64)  DEFAULT NULL COMMENT '月日',
    `enabled`        INT          DEFAULT 1 COMMENT '是否启用 0/1',
    `created_at`     VARCHAR(32)  NOT NULL COMMENT '创建时间',
    `last_triggered` VARCHAR(32)  DEFAULT NULL COMMENT '最后触发时间',
    `kb_id`          BIGINT       DEFAULT NULL COMMENT '知识库 ID',
    KEY `idx_kb_id` (`kb_id`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提醒';

-- 15. 任务
CREATE TABLE IF NOT EXISTS `tasks` (
    `id`          VARCHAR(128) PRIMARY KEY COMMENT '任务 ID',
    `title`       VARCHAR(512) NOT NULL COMMENT '任务标题',
    `description` TEXT         DEFAULT NULL COMMENT '任务描述',
    `status`      VARCHAR(32)  NOT NULL DEFAULT 'pending' COMMENT '状态',
    `priority`    VARCHAR(32)  NOT NULL DEFAULT 'mid' COMMENT '优先级',
    `due_date`    VARCHAR(32)  DEFAULT NULL COMMENT '截止日期',
    `created_at`  VARCHAR(32)  NOT NULL COMMENT '创建时间',
    `updated_at`  VARCHAR(32)  NOT NULL COMMENT '更新时间',
    `kb_id`       BIGINT       DEFAULT NULL COMMENT '知识库 ID',
    KEY `idx_kb_id` (`kb_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务';

-- 16. 识图分析记录
CREATE TABLE IF NOT EXISTS `image_analyses` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `image_name`   VARCHAR(512) NOT NULL COMMENT '图片名称',
    `image_path`   VARCHAR(512) DEFAULT NULL COMMENT '图片路径',
    `image_type`   VARCHAR(64)  NOT NULL COMMENT '图片类型',
    `prompt`       TEXT         NOT NULL COMMENT '提示词',
    `result`       TEXT         DEFAULT NULL COMMENT '分析结果',
    `model`        VARCHAR(128) DEFAULT NULL COMMENT '模型',
    `source`       VARCHAR(64)  NOT NULL DEFAULT 'upload' COMMENT '来源',
    `kb_id`        BIGINT       DEFAULT NULL COMMENT '知识库 ID',
    `status`       VARCHAR(32)  NOT NULL DEFAULT 'pending' COMMENT '状态',
    `created_at`   VARCHAR(32)  NOT NULL COMMENT '创建时间',
    `completed_at` VARCHAR(32)  DEFAULT NULL COMMENT '完成时间',
    PRIMARY KEY (`id`),
    KEY `idx_kb_id` (`kb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='识图分析记录';

-- 17. 试卷
CREATE TABLE IF NOT EXISTS `exam_papers` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `name`       VARCHAR(512) NOT NULL DEFAULT '' COMMENT '试卷名称',
    `image_path` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '图片路径',
    `image_type` VARCHAR(64)  NOT NULL DEFAULT 'image/jpeg' COMMENT '图片类型',
    `kb_id`      BIGINT       DEFAULT NULL COMMENT '知识库 ID',
    `model`      VARCHAR(128) NOT NULL DEFAULT '' COMMENT '模型',
    `status`     VARCHAR(32)  NOT NULL DEFAULT 'pending' COMMENT '状态',
    `created_at` VARCHAR(32)  NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_kb_id` (`kb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试卷';

-- 18. 试题
CREATE TABLE IF NOT EXISTS `exam_questions` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `paper_id`        BIGINT       NOT NULL COMMENT '试卷 ID',
    `seq_num`         INT          NOT NULL DEFAULT 0 COMMENT '序号',
    `question_type`   VARCHAR(64)  NOT NULL DEFAULT '未知' COMMENT '题型',
    `content`         TEXT         NOT NULL COMMENT '题目内容',
    `options`         TEXT         NOT NULL COMMENT '选项',
    `answer`          TEXT         NOT NULL COMMENT '答案',
    `explanation`     TEXT         NOT NULL COMMENT '解析',
    `knowledge_tags`  TEXT         NOT NULL COMMENT '知识点标签',
    `difficulty`      INT          NOT NULL DEFAULT 0 COMMENT '难度',
    `created_at`      VARCHAR(32)  NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_paper_id` (`paper_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试题';

-- 19. 练习记录
CREATE TABLE IF NOT EXISTS `exam_practices` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `question_id` BIGINT      NOT NULL COMMENT '试题 ID',
    `user_answer` TEXT        NOT NULL COMMENT '用户答案',
    `is_correct`  INT         NOT NULL DEFAULT 0 COMMENT '是否正确 0/1',
    `used_time`   INT         NOT NULL DEFAULT 0 COMMENT '用时（秒）',
    `created_at`  VARCHAR(32) NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='练习记录';

-- 20. 识题会话
CREATE TABLE IF NOT EXISTS `solve_sessions` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `title`      VARCHAR(512) NOT NULL DEFAULT '新识题' COMMENT '标题',
    `image_name` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '图片名称',
    `image_path` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '图片路径',
    `image_type` VARCHAR(64)  NOT NULL DEFAULT 'image/jpeg' COMMENT '图片类型',
    `image_data` LONGBLOB     DEFAULT NULL COMMENT '图片二进制数据',
    `model`      VARCHAR(128) NOT NULL DEFAULT '' COMMENT '模型',
    `prompt`     TEXT         NOT NULL COMMENT '提示词',
    `answer`     TEXT         NOT NULL COMMENT '答案',
    `status`     VARCHAR(32)  NOT NULL DEFAULT 'pending' COMMENT '状态',
    `created_at` VARCHAR(32)  NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='识题会话';

-- 21. 识题追问
CREATE TABLE IF NOT EXISTS `solve_follow_ups` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT,
    `session_id` BIGINT      NOT NULL COMMENT '会话 ID',
    `question`   TEXT        NOT NULL COMMENT '追问问题',
    `answer`     TEXT        NOT NULL COMMENT '追问回答',
    `sort_order` INT         NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at` VARCHAR(32) NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='识题追问';

-- 22. 活动日志（动态流）
CREATE TABLE IF NOT EXISTS `activity_log` (
    `id`              BIGINT      NOT NULL AUTO_INCREMENT,
    `kb_id`           BIGINT      DEFAULT NULL COMMENT '关联知识库 ID',
    `dataset_id`      VARCHAR(64) DEFAULT NULL COMMENT '关联数据集 ID',
    `record_id`       VARCHAR(64) DEFAULT NULL COMMENT '关联记录 ID',
    `action_type`     VARCHAR(64) NOT NULL COMMENT '操作类型: create_note / update_note / create_record / update_record / assign_task / approval_request / approval_result / notification / report / analyze',
    `action_desc`     TEXT        NOT NULL COMMENT '操作描述',
    `source`          VARCHAR(32) NOT NULL DEFAULT 'ai' COMMENT '来源: user / ai / system',
    `triggered_by`    BIGINT      DEFAULT NULL COMMENT '触发人用户 ID',
    `triggered_name`  VARCHAR(64) DEFAULT NULL COMMENT '触发人姓名',
    `target_user_id`  BIGINT      DEFAULT NULL COMMENT '目标用户 ID',
    `target_name`     VARCHAR(64) DEFAULT NULL COMMENT '目标用户姓名',
    `metadata_json`   TEXT        DEFAULT NULL COMMENT '扩展数据 JSON',
    `created_at`      VARCHAR(32) NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_kb_id` (`kb_id`),
    KEY `idx_action_type` (`action_type`),
    KEY `idx_triggered_by` (`triggered_by`),
    KEY `idx_target_user_id` (`target_user_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活动日志（动态流）';

-- 23. 通知消息
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
    KEY `idx_type` (`type`),
    KEY `idx_is_read` (`user_id`, `is_read`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知消息';

-- 24. 审批请求
CREATE TABLE IF NOT EXISTS `approval_requests` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `request_id`      VARCHAR(64)  NOT NULL UNIQUE COMMENT '审批请求唯一标识',
    `title`           VARCHAR(255) NOT NULL COMMENT '审批标题',
    `description`     TEXT         DEFAULT NULL COMMENT '审批说明',
    `source_type`     VARCHAR(64)  NOT NULL COMMENT '来源类型: dataset_record / knowledge_article / report',
    `source_id`       VARCHAR(128) NOT NULL COMMENT '来源 ID',
    `submitter_id`    BIGINT       NOT NULL COMMENT '提交人用户 ID',
    `submitter_name`  VARCHAR(64)  DEFAULT NULL COMMENT '提交人姓名',
    `approver_id`     BIGINT       NOT NULL COMMENT '审批人用户 ID',
    `approver_name`   VARCHAR(64)  DEFAULT NULL COMMENT '审批人姓名',
    `status`          VARCHAR(32)  NOT NULL DEFAULT 'pending' COMMENT '状态: pending / approved / rejected',
    `comment`         TEXT         DEFAULT NULL COMMENT '审批意见',
    `processed_at`    VARCHAR(32)  DEFAULT NULL COMMENT '处理时间',
    `created_at`      VARCHAR(32)  NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_submitter_id` (`submitter_id`),
    KEY `idx_approver_id` (`approver_id`),
    KEY `idx_status` (`status`),
    KEY `idx_request_id` (`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批请求';