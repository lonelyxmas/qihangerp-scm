/*
 Navicat Premium Dump SQL

 Source Server         : dev
 Source Server Type    : MySQL
 Source Server Version : 80036 (8.0.36)
 Source Host           : rm-wz95h4f7996784subvo.mysql.rds.aliyuncs.com:3306
 Source Schema         : qihang-work-ai

 Target Server Type    : MySQL
 Target Server Version : 80036 (8.0.36)
 File Encoding         : 65001

 Date: 30/07/2026 18:53:28
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for activity_log
-- ----------------------------
DROP TABLE IF EXISTS `activity_log`;
CREATE TABLE `activity_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `kb_id` bigint NULL DEFAULT NULL COMMENT '关联知识库 ID',
  `dataset_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联数据集 ID',
  `record_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联记录 ID',
  `action_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作类型: create_note / update_note / create_record / update_record / assign_task / approval_request / approval_result / notification / report / analyze',
  `action_desc` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作描述',
  `source` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ai' COMMENT '来源: user / ai / system',
  `triggered_by` bigint NULL DEFAULT NULL COMMENT '触发人用户 ID',
  `triggered_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '触发人姓名',
  `target_user_id` bigint NULL DEFAULT NULL COMMENT '目标用户 ID',
  `target_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '目标用户姓名',
  `metadata_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '扩展数据 JSON',
  `created_at` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_kb_id`(`kb_id` ASC) USING BTREE,
  INDEX `idx_action_type`(`action_type` ASC) USING BTREE,
  INDEX `idx_triggered_by`(`triggered_by` ASC) USING BTREE,
  INDEX `idx_target_user_id`(`target_user_id` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '活动日志（动态流）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of activity_log
-- ----------------------------
INSERT INTO `activity_log` VALUES (1, NULL, NULL, NULL, 'record_create', '记录 #0001 在「客户信息」已创建', 'system', NULL, '系统', NULL, NULL, NULL, '2026-07-29 23:18:00');
INSERT INTO `activity_log` VALUES (2, NULL, NULL, NULL, 'record_update', '记录 #0001 在「客户信息」已更新', 'system', NULL, '系统', NULL, NULL, NULL, '2026-07-29 23:22:51');
INSERT INTO `activity_log` VALUES (3, NULL, NULL, NULL, 'record_create', '记录 #0002 在「客户信息」已创建', 'system', NULL, '系统', NULL, NULL, NULL, '2026-07-29 23:59:02');
INSERT INTO `activity_log` VALUES (4, NULL, NULL, NULL, 'record_create', '记录 #0002 在「客户信息」已创建', 'system', NULL, '系统', NULL, NULL, NULL, '2026-07-30 00:00:04');

-- ----------------------------
-- Table structure for agent_traces
-- ----------------------------
DROP TABLE IF EXISTS `agent_traces`;
CREATE TABLE `agent_traces`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `step_index` int NOT NULL,
  `step_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `details` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `duration_ms` bigint NULL DEFAULT 0,
  `created_at` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of agent_traces
-- ----------------------------
INSERT INTO `agent_traces` VALUES (1, 'be34c13c-661', 0, 'thought', '理解用户意图: 你好，你是谁', '用户消息: 你好，你是谁', 0, '2026-07-30 17:46:39');
INSERT INTO `agent_traces` VALUES (2, 'be34c13c-661', 1, 'answer', 'AI 回复', '你好！😊 我是你的**智能笔记助手**，一个拥有记忆、规划和工具调用能力的 AI 智能体。\n\n我的核心使命是帮你高效管理知识和日常事务，具体可以帮你做这些事：\n\n📝 **笔记管理** — 创建、搜索、整理笔记文件，帮你把想法变成结构化的知识库\n\n📊 **数据记录** — 管理数据集，比如客户跟进、项目进度、Bug 追踪等，支持增删改查\n\n✅ **任务管理** — 创建待办事项、设置截止日期和...', 0, '2026-07-30 17:46:39');
INSERT INTO `agent_traces` VALUES (3, 'be34c13c-661', 2, 'thought', '理解用户意图: @工作笔记 帮我查一下有没有合同', '用户消息: @工作笔记 帮我查一下有没有合同', 0, '2026-07-30 17:53:11');
INSERT INTO `agent_traces` VALUES (4, 'be34c13c-661', 3, 'answer', 'AI 回复', '', 0, '2026-07-30 17:53:11');

-- ----------------------------
-- Table structure for ai_analysis
-- ----------------------------
DROP TABLE IF EXISTS `ai_analysis`;
CREATE TABLE `ai_analysis`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `kb_id` bigint NULL DEFAULT NULL COMMENT '知识库 ID',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型: daily_report / dir_analysis / daily_report_prompt / dir_analysis_prompt',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容（日报/分析报告/提示词）',
  `prompt` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '生成时使用的提示词',
  `dir_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '目录分析的子目录路径',
  `report_date` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '报告日期 yyyy-MM-dd',
  `created_at` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建时间',
  `updated_at` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_kb_id`(`kb_id` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_report_date`(`report_date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI 分析（日报/目录分析）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_analysis
-- ----------------------------

-- ----------------------------
-- Table structure for ai_llm_profiles
-- ----------------------------
DROP TABLE IF EXISTS `ai_llm_profiles`;
CREATE TABLE `ai_llm_profiles`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置名称',
  `api_key` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'API Key',
  `base_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'API Base URL',
  `model` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '模型名称',
  `timeout` int NULL DEFAULT 60 COMMENT '超时时间（秒）',
  `is_default` tinyint NULL DEFAULT 0 COMMENT '是否默认 0/1',
  `vision_support` tinyint NULL DEFAULT 0 COMMENT '（已废弃）是否支持多模态',
  `model_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'text' COMMENT '模型类型: text / multimodal / embedding',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_is_default`(`is_default` ASC) USING BTREE,
  INDEX `idx_model_type`(`model_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'LLM 模型配置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_llm_profiles
-- ----------------------------
INSERT INTO `ai_llm_profiles` VALUES (1, 'SenseNova-6.7-flash', 'sk-Zz8T4LfBMyfy3XFSd6vPZfdh2eCzQbOi', 'https://token.sensenova.cn/v1', 'sensenova-6.7-flash-lite', 600, 1, 1, 'multimodal');
INSERT INTO `ai_llm_profiles` VALUES (2, 'SenseNova-U1', 'sk-Zz8T4LfBMyfy3XFSd6vPZfdh2eCzQbOi', 'https://token.sensenova.cn/v1', 'sensenova-u1-fast', 600, 0, 0, 'image');
INSERT INTO `ai_llm_profiles` VALUES (3, 'SenseNova-DS', 'sk-Zz8T4LfBMyfy3XFSd6vPZfdh2eCzQbOi', 'https://token.sensenova.cn/v1', '	deepseek-v4-flash', 600, 0, 0, 'text');
INSERT INTO `ai_llm_profiles` VALUES (4, '语义向量模型', 'sk-jkahyxrsmcxjskdbjogfbszamkdhdfntoxchkkqicomwdowq', 'https://api.siliconflow.cn', 'BAAI/bge-m3', 600, 0, 0, 'embedding');

-- ----------------------------
-- Table structure for ai_messages
-- ----------------------------
DROP TABLE IF EXISTS `ai_messages`;
CREATE TABLE `ai_messages`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话 ID',
  `source` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '来源',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色: user / assistant / system',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `mode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '模式',
  `kb_id` bigint NULL DEFAULT NULL COMMENT '知识库 ID',
  `created_at` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_session_id`(`session_id` ASC) USING BTREE,
  INDEX `idx_kb_id`(`kb_id` ASC) USING BTREE,
  INDEX `idx_role`(`role` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '消息记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_messages
-- ----------------------------
INSERT INTO `ai_messages` VALUES (2, 'be34c13c-661', 'web', 'user', '你好，你是谁', 'knowledge', NULL, '2026-07-30 17:46:29');
INSERT INTO `ai_messages` VALUES (3, 'be34c13c-661', 'web', 'assistant', '你好！😊 我是你的**智能笔记助手**，一个拥有记忆、规划和工具调用能力的 AI 智能体。\n\n我的核心使命是帮你高效管理知识和日常事务，具体可以帮你做这些事：\n\n📝 **笔记管理** — 创建、搜索、整理笔记文件，帮你把想法变成结构化的知识库\n\n📊 **数据记录** — 管理数据集，比如客户跟进、项目进度、Bug 追踪等，支持增删改查\n\n✅ **任务管理** — 创建待办事项、设置截止日期和优先级，帮你追踪工作进展\n\n⏰ **定时提醒** — 设置每日/每周/每月提醒，重要事情不遗漏\n\n🧠 **长期记忆** — 记住你的偏好、项目信息、重要约定，让每次对话都更懂你\n\n🌐 **联网搜索** — 帮你获取最新信息、新闻、网页内容\n\n📚 **多知识库** — 支持多个知识库切换，不同项目井井有条\n\n---\n\n你可以直接告诉我你需要什么，比如：\n- \"帮我记个任务：周五前完成报告\"\n- \"搜索一下我之前的客户记录\"\n- \"提醒我明天下午3点开周会\"\n\n有什么我可以帮你的吗？', 'knowledge', NULL, '2026-07-30 17:46:39');
INSERT INTO `ai_messages` VALUES (4, 'be34c13c-661', 'web', 'user', '@工作笔记 帮我查一下有没有合同', 'knowledge', 1, '2026-07-30 17:53:01');
INSERT INTO `ai_messages` VALUES (5, 'be34c13c-661', 'web', 'assistant', '\n\n\n\n\n\n\n\n\n', 'knowledge', NULL, '2026-07-30 17:53:11');

-- ----------------------------
-- Table structure for ai_sessions
-- ----------------------------
DROP TABLE IF EXISTS `ai_sessions`;
CREATE TABLE `ai_sessions`  (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话 ID（手动生成）',
  `source` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '来源: web / feishu',
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '会话标题',
  `chat_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '飞书 chat_id',
  `chat_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '飞书 chat_type',
  `mode` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '模式',
  `created_at` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建时间',
  `updated_at` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_source`(`source` ASC) USING BTREE,
  INDEX `idx_chat_id`(`chat_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI 对话会话' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_sessions
-- ----------------------------
INSERT INTO `ai_sessions` VALUES ('be34c13c-661', 'web', '你好，你是谁', NULL, NULL, 'knowledge', '2026-07-30 17:46:29', '2026-07-30 17:53:11');

-- ----------------------------
-- Table structure for app_config
-- ----------------------------
DROP TABLE IF EXISTS `app_config`;
CREATE TABLE `app_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置键',
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '配置值（JSON格式）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '应用配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of app_config
-- ----------------------------

-- ----------------------------
-- Table structure for approval_requests
-- ----------------------------
DROP TABLE IF EXISTS `approval_requests`;
CREATE TABLE `approval_requests`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '审批请求唯一标识',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '审批标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '审批说明',
  `source_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '来源类型: dataset_record / knowledge_article / report',
  `source_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '来源 ID',
  `submitter_id` bigint NOT NULL COMMENT '提交人用户 ID',
  `submitter_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '提交人姓名',
  `approver_id` bigint NOT NULL COMMENT '审批人用户 ID',
  `approver_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审批人姓名',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT '状态: pending / approved / rejected',
  `comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '审批意见',
  `processed_at` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '处理时间',
  `created_at` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `request_id`(`request_id` ASC) USING BTREE,
  INDEX `idx_submitter_id`(`submitter_id` ASC) USING BTREE,
  INDEX `idx_approver_id`(`approver_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_request_id`(`request_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '审批请求' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of approval_requests
-- ----------------------------

-- ----------------------------
-- Table structure for collector_tasks
-- ----------------------------
DROP TABLE IF EXISTS `collector_tasks`;
CREATE TABLE `collector_tasks`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务唯一标识',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务名称',
  `task_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '任务类型',
  `prompt_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '提示词 Key',
  `url` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '目标 URL',
  `cron_expression` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'Cron 表达式',
  `enabled` int NULL DEFAULT 0 COMMENT '是否启用 0/1',
  `dataset_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联数据集 ID',
  `params_json` json NULL COMMENT '额外参数 JSON',
  `created_at` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建时间',
  `updated_at` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_task_id`(`task_id` ASC) USING BTREE,
  INDEX `idx_dataset_id`(`dataset_id` ASC) USING BTREE,
  INDEX `idx_enabled`(`enabled` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '采集任务' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of collector_tasks
-- ----------------------------

-- ----------------------------
-- Table structure for data_center_datasets
-- ----------------------------
DROP TABLE IF EXISTS `data_center_datasets`;
CREATE TABLE `data_center_datasets`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dataset_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '数据集唯一标识',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '数据集名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '数据集描述',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '类型',
  `status` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '状态',
  `schema_json` json NULL COMMENT 'Schema 定义 JSON',
  `import_configs_json` json NULL COMMENT '导入配置 JSON',
  `collab_config_json` json NULL COMMENT '协作配置JSON',
  `module_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '所属模块 ID',
  `created_at` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建时间',
  `updated_at` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_dataset_id`(`dataset_id` ASC) USING BTREE,
  INDEX `idx_module_id`(`module_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '数据中心 - 数据集定义' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of data_center_datasets
-- ----------------------------
INSERT INTO `data_center_datasets` VALUES (1, '8e013798-754', '客户信息', '', '[\"电商企业\",\"零售企业\",\"软件公司\"]', '[\"交付中\",\"已交付\",\"运维中\",\"已结束\"]', '[{\"name\": \"公司名称\", \"type\": \"text\", \"displayName\": \"公司名称\"}, {\"name\": \"地区\", \"type\": \"text\", \"displayName\": \"地区\"}, {\"name\": \"联系人\", \"type\": \"text\", \"displayName\": \"联系人\"}, {\"name\": \"联系电话\", \"type\": \"text\", \"displayName\": \"联系电话\"}, {\"name\": \"详细地址\", \"type\": \"text\", \"displayName\": \"详细地址\"}, {\"name\": \"微信\", \"type\": \"text\", \"displayName\": \"微信\"}, {\"name\": \"备注\", \"type\": \"text\", \"displayName\": \"备注\"}]', '{}', '{\"feishuNotify\": true}', 'MOD1785318320738', '2026-07-29 22:09:54', '2026-07-29 22:53:52');

-- ----------------------------
-- Table structure for data_center_modules
-- ----------------------------
DROP TABLE IF EXISTS `data_center_modules`;
CREATE TABLE `data_center_modules`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `module_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模块唯一标识',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模块名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '模块描述',
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图标',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `created_at` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建时间',
  `updated_at` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_module_id`(`module_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '数据中心 - 模块' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of data_center_modules
-- ----------------------------
INSERT INTO `data_center_modules` VALUES (1, 'MOD1785318320738', '客户管理', '', '📁', 0, '2026-07-29 17:45:20', '2026-07-29 17:45:20');

-- ----------------------------
-- Table structure for data_center_records
-- ----------------------------
DROP TABLE IF EXISTS `data_center_records`;
CREATE TABLE `data_center_records`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `record_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '记录唯一标识',
  `dataset_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '所属数据集 ID',
  `data_json` json NOT NULL COMMENT '动态字段数据 JSON',
  `source` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '数据来源',
  `content_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '内容 MD5',
  `record_num` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '记录编号',
  `record_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '记录类型',
  `record_status` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '记录状态',
  `assigned_to` bigint NULL DEFAULT NULL COMMENT '负责人用户ID',
  `assigned_at` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '指派时间',
  `approval_status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'none' COMMENT '审批状态: none/pending/approved/rejected',
  `approved_by` bigint NULL DEFAULT NULL COMMENT '审批人用户ID',
  `approved_at` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审批时间',
  `created_at` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建时间',
  `updated_at` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '更新时间',
  `created_by` bigint NULL DEFAULT NULL COMMENT '创建人用户ID',
  `created_by_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人用户名',
  `updated_by` bigint NULL DEFAULT NULL COMMENT '最后修改人用户ID',
  `updated_by_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后修改人用户名',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_record_id`(`record_id` ASC) USING BTREE,
  INDEX `idx_dataset_id`(`dataset_id` ASC) USING BTREE,
  INDEX `idx_record_status`(`record_status` ASC) USING BTREE,
  INDEX `idx_record_type`(`record_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '数据中心 - 数据集记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of data_center_records
-- ----------------------------
INSERT INTO `data_center_records` VALUES (1, 'e33e837e-b54', '8e013798-754', '{\"地区\": \"云南昆明\", \"备注\": \"连锁珠宝门店金箔雲ERP\", \"微信\": \"王宇航\", \"联系人\": \"王总、赵总\", \"公司名称\": \"云南海上网络\", \"联系电话\": \"\", \"详细地址\": \"\"}', 'manual', '40bb18cfca526e764cbe8f66169a9df2', '0001', '零售企业', '交付中', NULL, NULL, 'none', NULL, NULL, '2026-07-29 23:18:00', '2026-07-29 23:22:51', NULL, NULL, NULL, NULL);
INSERT INTO `data_center_records` VALUES (3, '41811d44-276', '8e013798-754', '{\"地区\": \"贵州贵阳\", \"备注\": \"\", \"微信\": \"\", \"联系人\": \"王浩\", \"公司名称\": \"有方大健康\", \"联系电话\": \"\", \"详细地址\": \"\"}', 'manual', 'f271be8175d1d290e754ace83948002b', '0002', '零售企业', '已交付', NULL, NULL, 'none', NULL, NULL, '2026-07-30 00:00:04', '2026-07-30 00:00:04', 1, 'admin', 1, 'admin');

-- ----------------------------
-- Table structure for exam_papers
-- ----------------------------
DROP TABLE IF EXISTS `exam_papers`;
CREATE TABLE `exam_papers`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '试卷名称',
  `image_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '图片路径',
  `image_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'image/jpeg' COMMENT '图片类型',
  `kb_id` bigint NULL DEFAULT NULL COMMENT '知识库 ID',
  `model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '模型',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT '状态',
  `created_at` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_kb_id`(`kb_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '试卷' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exam_papers
-- ----------------------------

-- ----------------------------
-- Table structure for exam_practices
-- ----------------------------
DROP TABLE IF EXISTS `exam_practices`;
CREATE TABLE `exam_practices`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_id` bigint NOT NULL COMMENT '试题 ID',
  `user_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户答案',
  `is_correct` int NOT NULL DEFAULT 0 COMMENT '是否正确 0/1',
  `used_time` int NOT NULL DEFAULT 0 COMMENT '用时（秒）',
  `created_at` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_question_id`(`question_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '练习记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exam_practices
-- ----------------------------

-- ----------------------------
-- Table structure for exam_questions
-- ----------------------------
DROP TABLE IF EXISTS `exam_questions`;
CREATE TABLE `exam_questions`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `paper_id` bigint NOT NULL COMMENT '试卷 ID',
  `seq_num` int NOT NULL DEFAULT 0 COMMENT '序号',
  `question_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '未知' COMMENT '题型',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '题目内容',
  `options` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '选项',
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '答案',
  `explanation` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '解析',
  `knowledge_tags` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '知识点标签',
  `difficulty` int NOT NULL DEFAULT 0 COMMENT '难度',
  `created_at` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_paper_id`(`paper_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '试题' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exam_questions
-- ----------------------------

-- ----------------------------
-- Table structure for image_analyses
-- ----------------------------
DROP TABLE IF EXISTS `image_analyses`;
CREATE TABLE `image_analyses`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `image_name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片名称',
  `image_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图片路径',
  `image_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片类型',
  `prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提示词',
  `result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '分析结果',
  `model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '模型',
  `source` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'upload' COMMENT '来源',
  `kb_id` bigint NULL DEFAULT NULL COMMENT '知识库 ID',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT '状态',
  `created_at` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '创建时间',
  `completed_at` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_kb_id`(`kb_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '识图分析记录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of image_analyses
-- ----------------------------

-- ----------------------------
-- Table structure for kb_bases
-- ----------------------------
DROP TABLE IF EXISTS `kb_bases`;
CREATE TABLE `kb_bases`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '知识库名称',
  `labels` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签（逗号分隔）',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序',
  `created_at` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建时间',
  `auto_report` int NULL DEFAULT 0 COMMENT '是否自动生成日报 0/1',
  `feishu_push` int NULL DEFAULT 0 COMMENT '是否飞书推送 0/1',
  `visibility` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'private' COMMENT '可见性: public=公开, private=私有',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识库' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of kb_bases
-- ----------------------------
INSERT INTO `kb_bases` VALUES (1, '工作笔记', '{}', 0, '2026-07-30 12:24:57', 0, 0, 'private');

-- ----------------------------
-- Table structure for kb_categories
-- ----------------------------
DROP TABLE IF EXISTS `kb_categories`;
CREATE TABLE `kb_categories`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `kb_id` bigint NOT NULL DEFAULT 0,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `sort_order` int NOT NULL DEFAULT 0,
  `created_at` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '',
  `updated_at` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of kb_categories
-- ----------------------------

-- ----------------------------
-- Table structure for kb_embeddings
-- ----------------------------
DROP TABLE IF EXISTS `kb_embeddings`;
CREATE TABLE `kb_embeddings`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `kb_id` bigint NOT NULL COMMENT '知识库 ID',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件相对路径',
  `chunk_index` int NULL DEFAULT NULL COMMENT '块序号',
  `path_context` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '路径上下文',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文本内容',
  `embedding` json NULL COMMENT '向量数据（JSON 数组）',
  `content_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '内容 MD5',
  `created_at` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建时间',
  `updated_at` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_kb_id`(`kb_id` ASC) USING BTREE,
  INDEX `idx_file_path`(`file_path`(255) ASC) USING BTREE,
  INDEX `idx_content_hash`(`content_hash` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '笔记文件向量' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of kb_embeddings
-- ----------------------------

-- ----------------------------
-- Table structure for kb_notes
-- ----------------------------
DROP TABLE IF EXISTS `kb_notes`;
CREATE TABLE `kb_notes`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `kb_id` bigint NOT NULL COMMENT '所属知识库 ID',
  `path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件路径，如 folder/file.md',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件名',
  `is_dir` int NULL DEFAULT 0 COMMENT '是否目录 0-文件 1-目录',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '文件内容（仅文件类型有效）',
  `created_at` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建时间',
  `updated_at` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '更新时间',
  `file_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `file_size` bigint NULL DEFAULT 0,
  `tags` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '[]',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'ready',
  `created_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `original_file` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '',
  `category_id` bigint NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_kb_path`(`kb_id` ASC, `path` ASC) USING BTREE,
  INDEX `idx_kb_id`(`kb_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '知识库笔记表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of kb_notes
-- ----------------------------
INSERT INTO `kb_notes` VALUES (3, 1, '启航ERP系统软件销售合同.pdf.md', '启航ERP系统软件销售合同.pdf', 0, '# 启航ERP系统软件销售合同.pdf\n\n\n第 1 页 共 8 页\n\n启航电商ERP系统软件\n\n销售合同\n\n甲方 北京本数科技有限公司 网址\n\n地址 北京市平谷区平谷镇迎宾花园 3号楼 1至 3 层 111 号-241345(集群注册)\n\n联系人 阎寒 电话 13809480580 邮箱 1251878116@qq.com\n\n乙方 深圳市启航数链软件技术服务有限公司 网址\n\n地址 广东省深圳市宝安区新安街道海富社区新城花园四栋 407\n\n联系人 齐李平 电话 15818590119 邮箱 280645618@qq.com\n\n根据《中华人民共和国民法典》及有关法规、条例的规定， 深圳市启航数链软件技术\n\n服务有限公司 （以下简称乙方）与 北京本数科技有限公司（以下简称甲方），就乙方向甲\n\n方提供 启航电商 ERP 系统软件 事宜，经友好协商，达成如下合同：\n\n一、产品明细\n\n产品名称 产品明细\n\n启航 OMS 系统软件\n\n项目 明细\n\n源码\n基于 B/S 架构的启航 ERP 系统 3.0 版本（包含：\n\noms、mms、vms 三套子系统）；\n\n软件架构要求：SpringCloud 微服务架构；\n\n软件功能\n多平台多店铺综合电商订单业务处理系统；\n\n支持淘宝天猫、京东、抖店、拼多多、微信小\n\n店。\n\n内部系统对接改造 按需求实现内部小程序商城、内部 WMS 系统\n\n的对接。\n\n文档 包含平台架构文档、系统设计文档、数据库说\n\n明文档、部署文档\n\n总价格 RMB： 40000 元（大写人民币：肆万元整）\n\nNo.A3-382\n\n\n\n第 2 页 共 8 页\n\n二、合同总价：\n\n本合同价（含税）款为： 40000 元整（大写：人民币肆万元整），包含软件产品永久\n\n使用的费用和源代码以及本合同其他相关文档交付及相关服务的全部费用。\n\n三、付款方式\n\n1、付款方式：\n\n1 合同生效后，甲方在 5个工作日内向乙方支付合同总价 30%的首付款 12000 元整\n\n（大写：人民币 壹万贰仟元整）；\n\n2 乙方按规定时间内完成需求部署及需求改造，甲方按照附件一《系统功能列表》在\n\n10 个工作日内进行功能验收后，甲方确认验收合格后向乙方支付合同总价 50%的批次款\n\n20000 元整（大写：人民币 贰万元整）；\n\n3 乙方在 30 个工作日内进行上线验收后，甲方确认验收合格后向乙方支付合同总价\n\n20%的批次款 8000 元整（大写：人民币捌仟元整）；\n\n本合同中约定的价款或交易金额为含税金额，乙方应在甲方每次付款后三个工作日内向甲方\n\n提供正规足额增值税普通发票。发票信息如下：\n\n名称：北京本数科技有限公司\n\n纳税人识别号：91110108MACMPHY08T\n\n地址：北京市平谷区平谷镇迎宾花园 3号楼 1 至 3 层 111 号-241345(集群注册)\n\n电话：010-64038203\n\n开户行及账号信息：\n\n收款账户名称: 北京本数科技有限公司\n\n银行账号: 110955953110001\n\n开户银行: 招商银行股份有限公司北京望京支行\n\n联行号: 308100005310\n\n2、乙方户名：深圳市启航数链软件技术服务有限公司\n\n账号： 770580039521\n\n开户行：中国银行深圳新安支行\n\n四、甲方权利及义务\n\n1、合同有效期内，甲方有权按照合同规定要求乙方提供产品及附件二服务内容。\n\n2、甲方及甲方的分公司、全资子公司拥有对合同产品的永久使用权和二次开发权，甲\n\n方及甲方的分公司、全资子公司有权基于自身业务开展的商业需要使用合同产品（包括\n\n但不限于与自有软件系统进行组合以及其他业务经营的使用方式），并享有因使用本合\n\n同项下的系统产品进行正常经营运营而产生的社会和经济效益。\n\n3、乙方可以根据甲方要求免费提供微信小店（视频号）、抖音小店、快手、小红书 4\n\n个主流电商平台的数据接口的软件功能和相关系统设计文档，以确认适配甲方系统或满\n\n足甲方功能需求。\n\n4、乙方应当根据甲方要求付费对合同产品进行二次开发，以确认适配甲方系统或满足\n\n\n\n第 3 页 共 8 页\n\n甲方功能需求。乙方根据甲方提供的需求说明书、技术规格文档等材料，为满足甲方的\n\n要求而专为甲方量身定制的产品为甲方的定制化产品。乙方不得向其他客户销售甲方的\n\n定制化产品，不得向任何第三方透露甲方提供的文档等材料或定制化产品，以及将甲方\n\n的定制化产品在任何地方申请任何专利或其他知识产权。\n\n5、甲方应当按照合同约定按时向乙方支付合同款项的全额，乙方应按照甲方要求提供\n\n合法等额的增值税普通发票，甲方自收到发票后 5个工作日内支付对应的款项。。\n\n五、乙方权利及义务\n\n1、乙方有权要求甲方遵守本合同及其附件规定。\n\n2、乙方所提供软件产品须符合附件一《系统功能与需求改造清单》。\n\n3、乙方为甲方提供付费二次开发服务，甲方提出新增数据接口需求后，经双方协商后，\n\n乙方需要在约定的时间内完成接口开发和上线验收。\n\n4、乙方必须保证所提供的软件产品，遵循中华人民共和国法律法规，禁止在交付产品\n\n中设置后门等恶意程序，同时适用于中华人民共和国范围内的合法销售，且不侵犯任何\n\n第三方的知识产权及其他合法权益。违反本条款的将视为乙方严重违约，甲方将追究乙\n\n方违约责任，并要求乙方与第三方对甲方因此遭受的全部损失承担连带责任。\n\n5、乙方应遵守，且应促使其关联方及合作方遵守网络安全与数据保护相关法律及其他\n\n适用法律，确保在合作中涉及任何数据的收集、使用与共享均已取得法律所要求的必要\n\n同意与授权。\n\n六、售后服务\n\n1、于本合同验收合格日起，乙方为甲方提供为期 12 个月的产品免费维护期及升级服务。\n\n甲方有权利要求乙方提供后续的售后服务支持，次年起由甲方单方面决定是否续签售后\n\n服务支持，每年收费 RMB：10,000 元（大写人民币： 壹万元整）。\n\n2、售后服务内容及方式详见附件二《系统售后维护服务细则》。\n\n七、保密义务\n\n1、本合同所称保密信息（文字、口述及其他任何形式）都应有其不被公开的必要性。\n\n包含：合作计划、技术文件、商品理念、合约以及财务讯息，并且经揭露方于交付时指明为\n\n「保密」之营业或技术信息，包括但不限于方法、技术、还原工程、程序、市场情报、产品\n\n价格、报价及付款条件、商业决策、工作计划、特定程序、系统、设计、技术、观念等，以\n\n及其他可用于生产、销售或经营具有商业或财产价值之信息。收受方如对于是否属于保密资\n\n料有疑问时，应即向揭露方征询意见。\n\n2、甲、乙双方应共同采取一切必要措施，对双方应通过本次合作而从对方取得的保密\n\n信息采取保密措施，以防止商业秘密被泄露、使用、公开或落入未经授权人手中。\n\n3、收受方同意应严格限于双方约定之目的范围内使用保密信息，除非本协议另有约定\n\n或基于双方约定之目的范围内使用保密信息外，不得为自己或第三人之利益而使用保密信息。\n\n4、双方同意对本次合作涉及的所有商业信息，应严格保守秘密，任何一方违反本条规\n\n定导致上述保密信息泄露给第三方或者被使用于非约定目的的违约方需对此承担全部赔偿\n\n责任。\n\n5、本合同之保密义务效力及于本合同有效期间及本合同终止或提前解除后一年，如资\n\n\n\n第 4 页 共 8 页\n\n料内含有原始代码(source code)时，保密义务期间为永久。\n\n八、知识产权\n\n1、本合同规定，乙方初始交付给甲方的系统及其对应的计算机程序源代码的所有权及\n\n知识产权归乙方所有。乙方就其对初始交付的系统及其对应的计算机程序源代码所拥有的相\n\n关知识产权，在此授予甲方在合同产品二次开发以及后续业务经营使用时所必需的永久和免\n\n费的知识产权许可。\n\n2、如果甲方（或乙方按照甲方要求）利用本合同项下的系统源代码进行二次开发、修\n\n改和升级的部分，其权利归甲方所有。甲方有权进行包括但不限于软件著作权申请及其他用\n\n途或目的。\n\n3、本协议不授予任何一方使用他方商标、专利、著作权等知识产权的权利。\n\n九、不可抗力\n\n1、任一方对因不可抗力造成的损失、合同不能履行或延迟履行均不应承担任何责任。\n\n不可抗力是指不能预见、对其发生和后果不能避免且不能克服的事件。包括但不限于火灾、\n\n水灾、雷击、地震、洪水、台风、龙卷风、火山爆发、瘟疫和传染病流行、罢工、战争或暴\n\n力行为或类似事件等。鉴于网络所具有的特殊性质，不可抗力亦包括下列情形：\n\nA.黑客攻击、计算机病毒侵入或发作；\n\nB.计算机系统遭到破坏、瘫痪或无法正常使用而导致信息或记录的丢失、乙方不能提供\n\n本合同项下之服务或甲方无法使用系统；\n\nC.电信部门技术调整导致之重大影响；\n\nD.因政府管制而造成的暂时性关闭等；\n\nE．法律法规规定的其他属于不可抗力的情形。\n\n2、上述不可抗力事件如果是由第三方责任人造成的，遭受损失的一方应根据《中华人\n\n民共和国民法典》等有关规定及时追究该第三方的侵权和赔偿责任。\n\n3、发生不可抗力事件的，受影响的一方应立即以可能的最为快捷的方式通知对方，并\n\n在不可抗力事件发生十五天内向对方出具能有效证明该不可抗力事件发生的文件。遭受不可\n\n抗力影响的一方应采取积极有效的措施以尽量减少因本合同不能或延迟履行而给对方造成\n\n的损失。一方因不可抗力而延迟履行其相关义务的时间应与不可抗力持续的时间相同。\n\n十、违约责任\n\n1、本合同签订即具有法律效力，因故需变更或解除时须经双方协商同意。若甲方在乙\n\n方未违约的情况下单方变更、撤销、解除本合同，则已支付合同款项不予退还，甲方应向乙\n\n方支付剩余合同款项，并按本合同总金额的 30%向乙方支付违约金。若乙方在甲方未违约的\n\n情况下单方变更、撤销、解除本合同，则需退还已支付合同款项，乙方应按本合同总金额的\n\n30%向甲方支付违约金。\n\n2、双方中任何一方违反本合同或有关附件、补充合同中的任何约定、陈述、承诺和保\n\n证的，则守约方有权选择是否单方解除本合同，并要求违约方向守约方支付合同总金额的\n\n30%作为违约金。如乙方已开始本合同项下产品的实施，在乙方未违约之情形下，已支付合\n\n同款项不予退还。\n\n3、甲方未按约定时间付款的，每逾期一天，甲方应按合同总金额的 0.1%向乙方支付延\n\n\n\n第 5 页 共 8 页\n\n迟履行违约金。逾期超过 15 日的，乙方有权单方解除本合同，在要求甲方全额支付本合同\n\n总金额的同时，甲方应按本合同总金额的 30%向乙方支付违约金；如上述违约金不足以弥补\n\n乙方直接损失的，甲方仍应就其违约导致乙方的直接损失承担赔偿责任。\n\n4、乙方未按约定时间交付产品或提供附件一和附件二的相关服务的，每逾期一天，乙\n\n方应按合同总金额的 0.1%向甲方支付延迟履行违约金。逾期超过 15 日的，甲方有权单方解\n\n除本合同，乙方应按本合同总金额的 30%向甲方支付违约金；如上述违约金不足以弥补甲方\n\n直接损失的，乙方仍应就其违约导致甲方的直接损失承担赔偿责任。\n\n5、如双方中任何一方违反本合同第七条保密义务、第八条知识产权的，除按照本条约\n\n定支付合同总金额的 50%作为违约金外，违约方还应承担因其违反上述合约义务而给守约方\n\n造成的，包括但不限于财产损失、商誉损失、潜在客户流失的损失、客户索赔、因违约方导\n\n致守约方可以获得的平均收益、因制止违约而支付的相应的诉讼和律师费用等所有损失。\n\n6、乙方就其产品或服务给甲方造成的直接损失承担赔偿责任，赔偿限额以乙方实际收\n\n取的合同总价为限，甲方充分理解本条款并无异议。\n\n十一、争议解决方式\n\n1、本协议以及因本协议或其成立而引起的或者与之相关的任何性质的争议、纠纷、程\n\n序或索赔（包括任何非合同争议或索赔）均将适用中国法律，并将根据中国法律进行解释。\n\n因执行本合同发生的或与本合同有关的一切争议，合同双方应通过友好协商解决，如协商仍\n\n不能达成合同时，则任何一方可以将争议提交至上海国际经济贸易仲裁委员会/上海国际仲\n\n裁中心在上海按照申请仲裁时该会现行有效的仲裁规则和条款进行终局仲裁。\n\n2、在诉讼期间，除了双方提交诉讼的争议部分外，合同的其他部分应继续执行。\n\n十二、合同附件\n\n本合同之附件、补充合同与本合同具有同等效力，附件约定与本合同不一致的，以本合\n\n同为准。\n\n十三、其他\n\n1、未经乙方书面同意，甲方不得将本合同全部或其部分以任何方式进行转让。\n\n2、任何本合同要求或允许的通知应以书面方式并应通过邮寄或电子邮件按合同载明的\n\n通讯地址送达对方。通知自送达之日起生效。\n\n3、合同在履行过程中如有未尽事宜，或因业务发展需要对本合同现有内容进行补充、\n\n变更、修改时，由双方或任何一方提出补充、变更、修改的建议和方案，经双方协商并达成\n\n统一意见后，以书面形式表达并经双方签字盖章后，成为本合同的补充文件，与本合同具有\n\n同等法律效力。其他在履行合同中可能涉及的文件，由双方各自授权的代表签署，视为其公\n\n司的行为。\n\n4、本合同任何部分被认定无效或不可执行，将不会因此而影响本合同其他条款或部分\n\n的有效性与可执行性。\n\n5、本合同的签署将取代双方之前已经达成合意的包括但不限于口头的、书面的任何合\n\n同，本合同内容与注册时所确认的电子版合同在内容上有冲突时以本合同为准。\n\n6、本合同自双方授权代表签字或加盖公章/合同专用章之日起生效，一式叁份，甲方两\n\n份，乙方壹份，具有同等法律效力。\n\n\n\n第 6 页 共 8 页\n\n甲方：北京本数科技有限公司 乙方：深圳市启航数链软件技术服务有限公司\n\n授权委托人： 授权委托人：\n\n签署日期：2025 年 07 月 07 日 签署日期：2025 年 07 月 07 日\n\n\n\n第 7 页 共 8 页\n\n附件一： 系统功能与需求改造清单\n\n模块 功能介绍\n\n核心电商\n\nERP模块\n\n商品库管理\n\n店铺管理\n\n店铺商品管理：管理各平台店铺商品，实现 API 拉取店铺商品\n\n店铺订单管理\n\n店铺售后管理\n\n其他相关流程设置\n\n发货管理\n\n分配订单路由（可以由自己仓库发货，也可以分配给供应商发货）\n\n电子面单打印发货\n\n补发等其他需求发货\n\n库存管理\n\n发货备货管理\n\n发货出库管理\n\n入库管理\n\n库存查询\n\n供应商子系\n\n统\n\n供应商发货订单管理\n\n供应商发货管理（打单发货）\n\n供应商库存管理\n\n供应商库存出入库管理\n\n商户子系统\n\n商户独立商品管理\n\n商品独立店铺管理\n\n商户独立店铺商品管理\n\n商户发货管理（可以自己发货、也可以分配给供应商发货）\n\n商户独立店铺售后管理\n\n商户独立商品库存管理\n\n商品库管理（总部商品库，商品库库存等）\n\n需求改造清\n\n单\n\n云仓对接：推送订单、售后数据到云仓，接收云仓商品数据、订单发货数据、\n\n售后处理数据等。\n\n小程序对接：接口接收小程序订单推送、私域店铺推送，推送小程序发货订\n\n单到小程序接口。\n\n快递 100 物流对接：订单物流轨迹查询功能\n\n支持直播间礼品订单处理：直播间中奖订单 excel 导入，推送到云仓发货，\n\n接收云仓发货回传物流信息。\n\n支持内部私有直播平台（螳螂系统）订单处理：接口接收直播平台订单推送，\n\n并推送到云仓发货，接收云仓发货回传物流信息。\n\n\n\n第 8 页 共 8 页\n\n附件二：\n\n系统售后维护服务细则\n\n一、售后维护服务具体内容\n\n1、甲方准备好部署前必要资料的前提下，乙方有义务在甲方首次购买产品时免费提供\n\n一次产品单机部署服务（乙方协助甲方解决淘宝天猫平台、京东平台、抖店平台、拼多多平\n\n台 appkey 申请相关事宜）。\n\n2、乙方提供工作日 8小时实时技术支持和实时故障响应支持，协助甲方查找问题的根\n\n源，解决软件产品本身的问题，保证系统能正常运行。\n\n3、使用过程中，出现原系统功能故障或不正常现象（主要指产品在运行中出现的影响\n\n服务的问题，不含二开后出现的问题），乙方提供多种在线售后服务方式（微信聊天、电话\n\n服务、远程服务），协助甲方解决启航 OMS 系统软件在运行过程中遇到的故障问题。\n\n4、系统更新升级：乙方提供在线的代码下载地址，甲方自行进行代码更新合并。\n\n二、故障定义及响应要求\n\n严重等级 描 述 响应时间\n\n1-致命缺陷\n\n项目系统瘫痪，所有业务功能均不可用，或由于本系统瘫痪导致关键\n\n业务系统不能正常使用。如前端无法访问、无法登录、无法处理订单\n\n等关键业务流程。\n\n12 小时内解决问\n\n题\n\n2-严重缺陷\n自身出现严重影响系统可用性的问题，导致部分业务（非关键业务）\n\n不能使用。如修改登录密码等。\n\n24 小时内解决问\n\n题\n\n3-一般缺陷\n\n实现不正确，但不会影响系统稳定性的，如：\n\n1）过程调用或其它脚本错误；\n\n2）功能已实现，但结果错误。比如计算结果错误，数据不一致等；\n\n3）操作界面一般性错误，比如界面上的控件在点击后无作用、对数\n\n据库的操作不能正确实现。\n5-7 个工作日内\n\n解决问题\n\n4-轻微缺陷\n\n操作不便但不影响工作或使用重要功能，主要指系统的UI 问题，如：\n\n1）输入、输出没有进行必要的类型校验；\n\n2）滚动条无效，上下翻页，首尾页定位错误；\n\n3）日期或时间初始值错误（起止日期、时间没有限定）；\n\n4）出现错别字，标点符号错误，拼写错误，以及不正确的大小写等；\n\n5）编码时数据类型、长度定义错误。\n\n5-业务需求优\n\n化\n\n根据甲方业务实际运行过程中产生的需求优化项，经过双方友好协\n\n商，共同协商确定代码改动范围和时间。\n\n2-3 个工作日内\n\n协商确定需求范\n\n围及开发计划\n\n\n		PFNpZ25hdHVyZT48aXNDbG91ZD4xPC9pc0Nsb3VkPjxzaWQ+aHR0cHM6Ly93d3cudHNpZ24uY24/c2VydmljZUlkPTVkZTE5ZDNkMmVmODQ1ZDM5M2ZhMDljYWIxNGY3ZDNmJmZpbGVJZD0zYzgzY2FkYjc5NmI0M2Y4YjA4NzkzMDY3MTU0Y2IzZTwvc2lkPjxzZWFsPlBGTmxZV3crUEVobFlXUmxjajQ4U1VRK1JWTThMMGxFUGp4MlpYSnphVzl1UGpNd01ERThMM1psY25OcGIyNCtQRlpwWkQ1VWFXMWxkbUZzWlR3dlZtbGtQanhzWlc1bmRHZytNRHd2YkdWdVozUm9Qand2U0dWaFpHVnlQanhUWldGc1NXNW1iejQ4WlhOSlJENW9kSFJ3Y3pvdkwzZDNkeTUwYzJsbmJpNWpiajl6WlhKMmFXTmxTV1E5TldSbE1UbGtNMlF5WldZNE5EVmtNemt6Wm1Fd09XTmhZakUwWmpka00yWW1abWxzWlVsa1BUTmpPRE5qWVdSaU56azJZalF6WmpoaU1EZzNPVE13TmpjeE5UUmpZak5sUEM5bGMwbEVQanhRY205d1pYSjBlVWx1Wm04K1BIUjVjR1UrT1R3dmRIbHdaVDQ4Ym1GdFpUNDhMMjVoYldVK1BHTmxjblErUEM5alpYSjBQanhqY21WaGRHVkVZWFJsUGpJd01qVXRNRGN0TURRZ01UZzZNakE2TkRnOEwyTnlaV0YwWlVSaGRHVStQSFpoYkdsa1UzUmhjblErUEM5MllXeHBaRk4wWVhKMFBqeDJZV3hwWkVWdVpENDhMM1poYkdsa1JXNWtQand2VUhKdmNHVnlkSGxKYm1adlBqeFFhV04wY25WbFNXNW1iejQ4ZEhsd1pUNVFUa2M4TDNSNWNHVStQR1JoZEdFK2FWWkNUMUozTUV0SFoyOUJRVUZCVGxOVmFFVlZaMEZCUVd4alFVRkJTbGhEUVZsQlFVRkRLMDh6UjBWQlFVRkVRVVpDVFZaRlZVRkJRVUZCUVVSTlFVRkhXVUZCU210QlFVMTNRVUZRT0VGTmQwRkJUWHBOUVUweVdVRk5OV3RCVFRoM1FVMHZPRUZhWjBGQldtcE5RVnB0V1VGYWNHdEJXbk4zUVZwMk9FRnRVVUZCYlZSTlFXMVhXVUZ0V210QmJXTjNRVzFtT0VGNlFVRkJla1JOUVhwSFdVRjZTbXRCZWsxM1FYcFFPRUV2ZDBGQkwzcE5RUzh5V1VFdk5XdEJMemgzUVM4dk9IcEJRVUY2UVVSTmVrRkhXWHBCU210NlFVMTNla0ZRT0hwTmQwRjZUWHBOZWsweVdYcE5OV3Q2VFRoM2VrMHZPSHBhWjBGNldtcE5lbHB0V1hwYWNHdDZXbk4zZWxwMk9IcHRVVUY2YlZSTmVtMVhXWHB0V210NmJXTjNlbTFtT0hwNlFVRjZla1JOZW5wSFdYcDZTbXQ2ZWsxM2VucFFPSG92ZDBGNkwzcE5laTh5V1hvdk5XdDZMemgzZWk4dk9XMUJRVUp0UVVST2JVRkhXbTFCU214dFFVMTRiVUZRT1cxTmQwSnRUWHBPYlUweVdtMU5OV3h0VFRoNGJVMHZPVzFhWjBKdFdtcE9iVnB0V20xYWNHeHRXbk40YlZwMk9XMXRVVUp0YlZST2JXMVhXbTF0V214dGJXTjRiVzFtT1cxNlFVSnRla1JPYlhwSFdtMTZTbXh0ZWsxNGJYcFFPVzB2ZDBKdEwzcE9iUzh5V20wdk5XeHRMemg0YlM4dksxcEJRVU5hUVVSUFdrRkhZVnBCU20xYVFVMTVXa0ZRSzFwTmQwTmFUWHBQV2sweVlWcE5OVzFhVFRoNVdrMHZLMXBhWjBOYVdtcFBXbHB0WVZwYWNHMWFXbk41V2xwMksxcHRVVU5hYlZSUFdtMVhZVnB0V20xYWJXTjVXbTFtSzFwNlFVTmFla1JQV25wSFlWcDZTbTFhZWsxNVducFFLMW92ZDBOYUwzcFBXaTh5WVZvdk5XMWFMemg1V2k4dkwwMUJRVVJOUVVSUVRVRkhZazFCU201TlFVMTZUVUZRTDAxTmQwUk5UWHBRVFUweVlrMU5OVzVOVFRoNlRVMHZMMDFhWjBSTldtcFFUVnB0WWsxYWNHNU5Xbk42VFZwMkwwMXRVVVJOYlZSUVRXMVhZazF0V201TmJXTjZUVzFtTDAxNlFVUk5la1JRVFhwSFlrMTZTbTVOZWsxNlRYcFFMMDB2ZDBSTkwzcFFUUzh5WWswdk5XNU5Memg2VFM4dkx5OUJRVVF2UVVSUUwwRkhZaTlCU200dlFVMTZMMEZRTHk5TmQwUXZUWHBRTDAweVlpOU5OVzR2VFRoNkwwMHZMeTlhWjBRdldtcFFMMXB0WWk5YWNHNHZXbk42TDFwMkx5OXRVVVF2YlZSUUwyMVhZaTl0V200dmJXTjZMMjFtTHk5NlFVUXZla1JRTDNwSFlpOTZTbTR2ZWsxNkwzcFFMeTh2ZDBRdkwzcFFMeTh5WWk4dk5XNHZMemg2THk4dk9GTkZhRWxaUjBKblpVaG9OR3RLUTFGeFMybHZkMDFFUVRKT2FsazRVRVI0UTFGclNrbFRSV2hQVkdzMVZWWkdVbUZYYkhCbldVZENiVnB0V25OaVIzaDVZMjVLTkdWSWFDdG1ialpGYUVsVFMybHZjVkZyU2tOWGJIQmhZMjVLZVdsdmNVdHZjVXRwZFhKeE5qQjBURk0yZFhKeVFYZE5SRWQ0YzJKTmVrMTZVekIwVEZreVRtcGxNM1EzYXpWUFZIRTJkWEozT0ZCRU1qbDJZamd2VUhkblMxZE1SRUZCUVVGRFdFSkpWMWhOUVVGQk4wVkJRVUZQZUVGSFZrdDNOR0pCUVVGck5GVnNSVkZXVWpReWRUTmtWelZKWWs5aVNrWXdXbkl2UkVoeE1Ea3pObFpYV25SaFUydFZSVWhKUVM4eGFtSkVWRFZYVTBkUlVXTjRlbVJDV25aRFptWjNSR2RGVUM4emVub3ZMM1ZVU0UxUVFVRkJTVVZpU21obmIwRkJRVUZyYVc1blFrRkJRVU5TWW05QlFVRkRTazF2WjFoQlFVRkZlV2xDWkVGQlFWRkxXVTUzUVZGQlFVMXRWMUZNVVVGQlEwcFdRblJuUVVGSlJrMUhNbEZKUVdkRmQxcENkR3REUVVsQ1RVZFhVVXhCUVVGNVdsSm9hME4zUWtGd1ozbEVZVUZGUVZOS1ZtaEZRekJCUVVGcFZsbFNRWFJCUVVOb1RXZDVVMEpSUVdkV1NWcENkRUZCUVhCTmIzZEVTMGxHUVVOQ1ZXTjRkWGQxVTJSYVFVRkVUbFoyVGxWTU1tOUdRVXRDU21GdmNuRjVWa0pVUVV0QlFtRnVXbEZrTW05UVFVdERlR0ZYVkhGVk1qQkRRVVJSZEdwUmNuRldLekJEWjB0aGEwbFZSbVJ4TW10Qk1FaDNNRWh4YURNNVVUUkJNRWRSTUVZNVowZzVaMFZCWVVOWmJHMXZhRWRrWVRsbE4xRXJVMEpSUTJ0eGJVZDZNRXRVZFRGeE1EbFJOMGxCWjBaUk1XRjNjV0V4VDNjMVNqRnJRVzlEYTBvdk1FNTZjWFJ3YlVOcE0wcEJaMEpUU21WNVpGaHlWMjgxT1ZoSWN5czVTVVpuUVVrNWMwSTFTV3hsZW5obGNrVjFkRzFNUVVWRGNWSnVNR2R1Vm5wV2NtVXZTWGczVFM5VFVsbEJRMDh3YTJwV1ZVUTJhVVpZY0RsbVQyWm5WVUZWZFZWMkwyTm9WbVUzYmt0SlVISXlUVUZCVVhFMVNtaFlVRmQ2VHpKdksyUndObm94VTJwS1FXZEJRak5QY2pKRFZUWjJia1p5V2pWM1VVeG5RVmxxWWtSbVRVZzNiV0ZMVm1ZeVVHTnJRMUZMYjBWaE5FbFVTek5LUm5KdFVVSjVVVXBCY1d0WlJ6WlpiSEl4TVdwUGNsZEdSMWRhVFU1Q1FYVkJPRUozVkc1eFpYVllWazl3WXpKeVZtRmlNMnRDUVVFd1EwMXpUMk12YmpJMWVXVmphRVY1VTNFME5YSlNZa2xCWjBaU1pHNWpjMlJVVm1kcVNWWmtlV2hIUVVKRlNXZDBkeTlDVURFek9XRnpSbFF4UjJKdU5rMTBZMVJIYWpkS1FYRkNjRU5NTDNSd2VITnllaXN2Y0M5SFoyVmxNRFpTWm1oTGNtbFFWMVZOVkVsSFowMUJWR1ZDSzJ4S2JFdDFiMmRZVDFodWFGaHVNVWQ1WWtvMWJFUm5RV2hLSzBGUFUwNXFXREI1ZEU0ME9UVXJZMGR3Um5OblFtOUpaMGwwZDFKd1JVTTFXbTFqVnk5bWEwdHpaVzFoVkdsQldrRnhZM1pYWm5sT1RYZ3ZSSHB1UldKTU1YUm5XVzF5UzA0NFFXbERNR2hPV1RGMVdISjVSMDlpTmk5SWNUbHhaakl6V1hWV2VsZG1TVXRuUzBGVFZXazVVRkZ1WWt0c1dHc3ZTemhQVW1VNFRHSTFOMGxNWjBkQlUxUkpabXMyYlN0UVdXWTNkbmxXV0VWMmRucFVkalZXYW1ObmVVRlJRa3BIUjNoMU1pc3hNMVpyTm5aV0wxZHJUalZDYjBGWmIxZEdhSEl6Tm1WU2VISkZZamxYU2psaVpYWndTblpCU1ZOUE1FNXVVWFJMVUdaT2NsRjFaV1ZpZHpkbFRscFNNV3RJV1VoRVdWZEphemxqY2xoeVVTczRORkEwWmxkb1IxRkNSVVJFUXpWV1RHcEtiR1pyUTJwSlVXZEZSRUpTY201aE9XRkdNMjVLTUM4ck1HTmxRV2hCYTJkMVUzZFlSVmhRZFdaWVMwc3haRzFXZWxsRFJVSTJRelE1U21GUGFXNUtTVGN2VjFGck4wdFRWVUpaUTBsMGFXRTFhR1JGVFdsV2RGcERXazFvVFZGRmEwdHBNVXB2T1dabGVYWXhNa3BzV1cxWVNVeFNXR3R3TDNkRlFrbE9VWFZNYUhWVlVURXpPVGN2U0RNclpIUmtaREp6YUZOM1JrbEJaMUV6U25oWVJ5czNTMFozYkdKdFZ6aHBkRXh3V0hKSlJtTjBVVXRCUVUxRGJFcG9OemxqT1ZvemVuZzNPRGN6T0dKMFZqZFhVamhaUTJGTVluQjZXRFpQT1ZrMFYweFBjMk4yZHl0bUwwWjBlVXBYZG14TVYwTnFielZvWTJaU1ZYTnpOelVyVjNKWE5tUXpPVFpJUTNab1FsTXdRVmx2VmtWamRsWkZRVTE0Tm1wR1ZEbEtSV1IyT1RVM01VbFdaMEZpUjNCeldIWXZiekEyZFM5MlYxWnNlblJtTTNsa2RUTmFLekZPVjFkNU9VRmFjMXBHTW05bk5uWlVjWGxsWlVKNlVHUmxkVmh3ZWt4WVlrZDJSbmN5T0hkRGVIZDFSVFpwUkRZNVpYWk1aamRpZFRsamRsWkhhbWxIWmtGWWFHeG9RVEl4WTBWTGRVcDBWRXR5Y3pndlVtTnRWbGhWUjNkNlJEVkJja3hEY0RCaE9Fa3hkRTF0VUhKR2JVbHRObHBGUTFaWU9YRnVUV3gwYlVGVVdYRk1jSGxwY25ReGRVbEZUR2gxWTNnMGNFOHJVa3RrYzNSMVdVMUViVTVQZFRFMk1reHNPVTl3Y2tSVk5tRTRNbXBhTWxodFdHWlNRWE5CUVZVeWNFa3hZbEUyTml0dWJEVTVVRk50WWsxMVpHWlVjbGhKUm1KNFVVSnZhbFkxSzNWNlkyNVliRFp6ZG5aemQzSnVOV052WVhsSVlVTkNWRlJrWmtSYWVIWnRZU3MwTkZCWGVFOXlUVk5TU3pWUGNtUlBSMUkyWmxsQlNFVlRjMDEwVEd4amNuQjVja05QY0dSalYxbGxOQzlpVEhJNFZGQnVkbE54UVdkRE5ua3lWM3BuYmtoSlZtTmlVRkIyTXpGSk0yZHlZMng1YzNwbU1qWk9aRzlyWW5kUlNXRnBaRmhQTmpkaFlXUjRja1l3Tml0NFYxWnNWRFkzZW01V1QxUk1lbFkyZEZGVmVrZGFjRUV5WW5oTlEzaE5jWEkyWTFOMmQyeG1hMkYyVmpkQ2NUTkdWMkpyZVRndlkzcGhMMlpJVEVGblYwMUdVM052WjB4SFduUTRkbFl2THlzdk5WWnRTVFY2TTI0eGVWazVlblJ5UmpWdVpEQXpjME5SUzNsVGRrdHhNalJYVDJGM2RYRllUbXB6T1VsV1pqSXdibkF5WlRsRlRrVkxkVkV3ZVcwelpEWm5ibFo0U0ROUFRFMVRUV1pRYUhKa1p6Wm5ibmhMVm1kblYydElha1JRUVcxTFEweHVZV05ZY0d3d0t5dFNjVGxWTlhScVltNDFUWEFyZVV4UFYxZFVOWGxSWWtOQlNYQjJiRFpqT1N0aFpIbFNjREpPTkROM1VqSnVTVXBaYnk5cE5USklhMFJYUzNsMmVGVXdhRWxXYUVGdmF6SjVORGhVYjNCR2VYQnNhakY1UmxSSVVIZDJhVTFZUjJ3NEsySk9Oamh2ZEhsblJtZEdibWh5ZEN0SmMzbEhlblJQY2s0MVNUaE5OWEpKUm1SeU9EWnVhRFZqTDNONmFqRkZkbGxHV1dKbWQxUTBTak12TlhVeWNFZFhUR3hoZG1NeFYwbDBXWFZXVERGbFdFbzNVMms1VW1SNVFsZEhMemxETlZkMmFpOTJja0poYUhVM2JteDRUbGhoZFZRNWVscFRPV0pyZVcweVFVSjRSM0Z5V1VWVksyaHJNR1JrTVhCRGNuWnlTV3haY2xCdFpHdEZRM2xDVjNrNVppdHdXRWN2TDJZME4yMHZjbVFyZEhsVlNDdHpWVkIzWm5GUU1tUmxiak0zYUd0eVNGQnhRa0ZSY1RoUFEwWm1NWEV6VFdGMVNWWm1XbTF6ZW5SbVZUWnpXbTluVm5kUlMwa3hWa3A2Wm01UVN6bE9VR3BtY214MWJUTnhPVzltZG1OV1VqSTFNSE42Y1ZOR1dGY3JhV1JaU1VaWlNpOTVjbmQ1TkdWcU16TjRiWEZ1Y0hkV1ZIVXhUM1pZY1hWMmMyRnVabWREWVRReWMzQnNhR3hRVFZWcFYwTkNWME5tT0hFNFRYWnViMDQzWTNoVWFtbHlka2R4YWtaNGJFOWFibUZrVnpnMU9WWTFTSEZzWWpNeWNXNVFlWGhKYzJ0TGRHc3pNVE14VGpsR05VdHNaRTl5S3pkTWJGRXJNVGM1TTFoWFVqUllOVGhZY1hwWU4yODVTREpGUzJkeVJXRnZUbWN2WVc1U2RuaFZNRVJtZW1OMWNEUXJNMXB0ZVhSNVRXVlVOek5ZYkdGdmJtVjVURVJJZVZGU1RFSkRja04zV0RnMVV6TkRkRGd3T0RSelVIUk9ibEpsZFZwdmNWZEVkak50U0hKbWJqZ3dWbkJLZUdkM1pWbDBXSEpvWmtKbGRuSmlVbUk0T1dWRE5UbFRVbGc1TURRdlZuWTJTVkpOTTNaclpVaGlaM1JYYkZJMmJGQkxUbkZCYUhKc05uWlJVM0l4SzI1V1pFeHVZVGhZVDJFeE4yMVVjV2h6TTFwRU5qbHdaMUZNZUVOeWNHWldUMmxIY3pOMlNHeG1SSEZUTVZoS01EUkJTM055Vm1wU1kyZGhkbmhpY0hRMU5tMHhSRkJCYjFsWE5tRTBMMUpaT0ZOTVRsZFpWRFkwYlVOa1lrOXJkM0pPUzNvMlRHSTNPVWw1VUdsU1JETkpSbGx1V0RWUFZXTkhMekZsTlVsc2FtdExkSFJsYVVwaGNWaFpPRE5MV1dSWVlYWk1hekkwVW1SbE5XcFZRbUpGTmxCQll5OU9VM0Z1VmpOVlJtRTVTbVpFU2pRNFdXUk1RVE5uYmxaVUx6bFhVRkpOYzBWTGRqQmpOMEUyU2pNclUwdFpTbFpZTmpSdGVpc3lUak5LYldWMU1Ea3JXblJtWVZad01TOHZVWGhOVURORlJESlRSbGhVTlhWV2FscDZkbHBOYTJFMVFrZE5lbk12TlRkakwzUjZhalE1YWxkRFFsZEpWVE0xWW1WbVpUTm9lV2d2WlhabmNWWlROemh2VmpkdFZsRTVLMlEyZFRnMk4zcHFNMEpCY2taTE1uQkNXRkIzWm5oV1RuaEpVU3Q0Tm0xdk1HVlBWMUp0T1hBMVlUTm1aMlJsYUhsSk1WaERlR1ZxY0VobU1uWjVWQ3RYVGxoS1JYSTJNbU5PWWpVMGMyUlNWVWhuYjFaWFFsWmtjRVIwTkVWbWRsUllaWGhEYzAweWMwcGhPV3B3ZUU5eVZWZ3lOM0ZrZDFGTVEzVXpXV2xqYWxodEwyUTVSVk4zWWl0UWF6WnRVVmh5TWtReWNqTTNkM2RwSzJkSUsyZzNRbWRuU1RkS2JHUjJjRmRtTVRsbmN6Sk5RME55WkRkNk5HTjJjMHgyVVM5RlMzUlZjbmg1T1hZdlZWZzVUSEJWVFVWTGRVbG1TbWxaVEZodlowWkdWbWwxWkc5V1ZFRlJUR3RPVFZKWWVIRXZOaTltYjJoUlVVeHBiVzF5V0Vvd1ZVeEtjMWh0U201V1dETXJWMWRQYlVwVlJWSnNOWFVyYmk4M09EZFpSekZsWjBab1JuWjVRMk50YVZnMlNXaFNVVVUzYkdGMlUwOTZlbEYyVFhsMWIyUjFVMVpJT1VWamEwdzFOa3BqTDJodGFtbFFRekI0YjBGNFEyOXhkU3RSU0hkWlMybExVMDVZWWpoSmVGZHlRbFZQVlVOek5VbGtaVU5qVmtOeWFHSXZSWE5xUjBKbWNHNWtVMWRDTUhwUU1WTXdWMmxWVEdKSk1XVXZiVkJVY0VWeVVXdDNURFowTXk5Q05IcHlNaXREWjFwbFZuRTNMMlJrUkZScGNHNHZWMElyYVZRd01UbDVhREZuVWt4RGFVOHhia3d4Y0hsRFRIWnBiVzlrVVVvMmFURlhWVmxOYTBkUVVsTkxiM0ZXWXpkbWFIbzJUV2t4Y3psdFFpc3hiVGswSzFKS2FIVm1jM0JYV2xoTlUybEpXQ3RaTWpBMmRGTmhkM1l3VUhKWFMyWndSbTQxZGxaVVRVOHhWMk5vV0RscGRsSlFVREowT1dkYU5XazVWbUYzTjBoSFEwSlZWbEZhYnpacWR6TlFTSFE1YkdKWE5rTnVWMFF6VGtSbWRHTmlORmhHU0RkTlR6QlpUREU1V2pWaE1XaG1TV3RTUlRkSWMzTmxjamxXYW5wVE5uaDNjMDR4WlVKMWEwOHJXSEZUV2pGWlVHRkRWMWRGVnpoeFNVMHJRekJhWkZSeGVXVXZURGgyTDNrM2FUVTJkekZqUkN0dVZDOVJRazB3SzNkWlRFWmlRblZpU2pBMmRWWTFNa0V4WjFwd2FUVlZWMVIzZDNoWk5rNWFja1ZRVm5ZemNYbEtUbEZQU1d4UmVsaGtNa2RDZURZdlZqWnVjVk5MTmtSUU0ybGtWMlZ4T1RGek4yZEpRM1JwYjJSaVNqSlJTakpqY21sd2R6QkpUWFJ5U1ZWa2N6SmlhME5wUWxkTmEwRm1hR2RYTVdKMVVVdEpSbUZHWm1jdlR6a3lUWHBoZWtoNFdXVXhWMWh2Vm1GUk5rSkhVRzVqVVU5RWFuTnpTa0ZYYzJRek5scG5jSHBCVEZkNlFYWnZlVWRNU1RGS2JHWkJNa0YzWjFaMmNYcDBZbFkwVTBOQ1dGcG5MMjlzVVVaUUt6UkNXakEyVG1nd1lrRlJjazlSUzIxS2MwSk5iSGxtYUd0VlJIVlJTVkZzUVZCNVZ6WXJSM2hSU3pWQmNrRjRPRGd6VTNKSU5YUjZVekJWUkhOeFZuUlJWRWxHVW5sSmQwTkxRbGhCUmxsNVNEQjZjRWhtUkRSdlFtTkJVV3BMWm5KUGFtWTRVR2swVDA5aGRpOXVNMXBuTUdkV2RFUkVXVlpJZDJ4NlZUTkhORUZqYTBGbU5uVkdOWFZSVlVOMVFVRkJUMU5wZDBjNGNUSTNiVkZCUVZCV01WQjBkMmRuVm5kQlFXWllNMDlCYkdsRlYyMTBkazVuUWtGbU9XWmlWRlEyUXpGME5VMUJSVU4wTjB3M1dqTXZWVTVaWjFaNVFsRkNhazB4ZFdaS01XTm5WbWREUVhZdlVuRjJXalZaUVZGRFFYZEVOblF6TlUxeVFVRkJVVEZLTHpGbVIwbEdRVUZEUTJVM1R5dFFNR2wxZWtOdlFVRlFSVGtyWkdRdmNDOWxWRXQzUVVFNFMwVm1MeTlTZGpsWU9XbGtaVk5oY2xOM1FXOUxUbFV2WlRkdWVVSlhOVEp1TjA1V2FGVkJNRVp0Y1daMlpucENTWFJaV0ZNNVNVRkJRM0ZUZEZkVWVIbEtXRFZEY25NcmJURmFRVVZFVm5ad2RFNHdRV3BYZDBsdE1HMUJRVUZWYTFkMWVVNVlSelV5U0RkQlowRTJVM1JYU2xocmVYZHBRbGMxUVdkRE1HeHhjVWxZZFhsNlZpdFNjVFppYmxsNVowTkJObXhKVmpKWVpETjJaVlpKYzBKS1RqTkpOMHBLYkZsQlowOXdVMFpUTUhCMWVpOVFVbUUwVTFST2VrOTVVMXBZUVVsRFMxVnVXSEZSSzIxV1JHMUhTVEZoV0VOSlZsbEJaMHMxVTNSbFRtRjVSbFY2ZFdSdmRGZGxVVXRCU2tOMGJEYzNOWFZhZGxoUmNrRkxhVGxWZFhsVFNsZEJTVUZ4VlhaWWNubzFOalJHY21Sc1IwTktXRlZhVEd3eFozTkJaME53VTBaa01qTnVkbHBRY0RGbFJIaERjRU56YjJkV1FVdERRMVpGVjBzeFdURXJVMkUwUzFRNVEySXpLMWhWUTJkQ1VWRmhjV2xsYkdKRlQzb3haQzlaUmxsaVlucFBlbEJOZFc5blEyZFVOemxqWldGNFlqRXJUREJoY21oalVsWXhkblIyYTFkV2QwRjNValp3VnlzNFJFOWhlVVpaVVRoWWNWTTBSbXgxUjJKbWNYZFJRWEJIY0VaeVJFeG1SalZLWTA1YWVWRk9PV1ZVTmxNNFpYbFNWVUY2U2xkeE5rTTVibXB1Y1N0Q1EzVm9XRWRXSzBoeFpVdDNXREE1UVV0Q00xUjZkMmhXY1dZdmEwbDBZMDlpVm1GbGFEWnVOVFJTWjBGVlExQnJObkZ2ZEM5T1YzSnRaVWhIUjFVNWRsTktXR2hsVVhFcmJqRjFaMmRWUVhaaFdIRnBORUpGZGpoRVpreFZXWEoyT1M5d1ZsUlFSQzlPVUhRdlJUbGpUalpyUTJkQ2JGTTVZblZtYVZKVFZVeEdMemRzZGtWM2QzRnVWbmhsWkhnNGFWRk1RVVJDU0hGdU56WXlaRmhJZWxoS1RHaGtVR1pGTUhsMVEzQXhZVkJrTUVsSFZ6WkJTM1ZKUVc5SldsVnlabUpOTTFSTGVtTnVjREUyYzFBM1NYZFRjamcyYkZac1IwUmtablE0WVVGRVFsQnhhelU1UzB3elMzZ3lXa2Q1TVZoSFZUWnpibWR5Vnpjd1RXdFdRVTlVZEdNM2RqWTFWbVpLVHpOR1RHaGlaQzlHVlcxMWFITnlWbmx1VUdVdlYzRkRWMGRHY1ZGNlRWUnhTSGhoUmxOc2NsZ3ZPR0ZqWTJab2R5dHNaVkJyY1hkMWRESlNVRlp3TW05d09IWjFVVXMxUVc1TU1IVktUakZJZGtWQ09UWm9jblpPV0VSNVZsaDZWVFp6ZG5Bd2F6ZENSWFp6V1ZWd1ZFMTRkVmxYYnl0U1pucFhOSFU1WXpkNlEwWlliMkZKVXpsWFpYbGllamQyU21WdmR6ZGpWRUZxUjBKNUwyRXZZelJ0U0VoSlZVcFhNbGQzYUZZNVRsQnlWbHBsU21GNUswZG9SVGxKUm1aQlVFeElOblZ0T1RJelJpdExaRE5wVTJZM015dFlaazFTVjJKNVlVUkRXVE5HTjAxRFZYSllNbGx0VWtoWU5uSjFTR1Z0ZVhCa1RVZ3lWRTB3YUZSa0wxWnBkVU5OVEZoS2JVSnRVWEZ0T1drNVpWSkJiMWx4Uld0TGRFSndNVnBTY2pkd09YQlJOVUZ5YTBOeFpIWlhZVVJNWkZWSlJtWnJObTl3WjJ0VGVHOVBUMjlsWVdwNmFYUkhjV3hrTVdWa2R6ZExRMDVtTldKeFp6aEtSbk50UTNoeFVHMVJZWEZwZUV0eVZFaHBUbGhST0ZoeE1VTmlVV05GUTNWblJHc3hkbVpOWkd0VmNqZHBNWGxTY1RkQlltaHdTWE5VUnpsRFlXaDRWR0V6Ym01YU0yMXlOM0YwVjB4MVNYUjNXRTlEWkdaS04wTTBSa3RaVjIxdFRVOVhhMHQxU2tabGRsUnFhME4wVUhCeVVtZHlaRGQwV0ZKTlEzVlJTMGt4WmxNNFMwOUZhelZEY0hVM2NEY3JNalJuTVhOUk0xSkxVMnBPUm1wdk1DdDRkbmt3UnpNdmRFcG5Za016ZUU5elMxb3ZWMEpJTW1oc2NrZHdRMXBRY2sxcFZsSlJObVYzTlZaVVN6VkpSbU5uV0RCaFprcFFLelJST1hNd09VTlFXVWR0Y0RGak4zWXdlR0ZaTUV0WVowUlNjalpHWWk5VU0zWkZNVE13YTFCM1lrbHNZek13T0hwbFdGSTRObElyVldaWVkwUlNlbTFNUVZoa2NubEpiSGd2YTJGclVrSTNTbE55WVZKelRqVkJjVmxNUm1FdlVIRlpUVk5hZDNRMVMzSkhVbTV5Tms4NE1DdFBiMU5wTWxsT0syZE1TMDlSY1hwUGVtRXdXbWRXUTFWS04wRklNVVJoTlVWdllVSTRNbk15U1VKVWJFTmxkMEp6TTB4WVZsWjRkM2MwU1hkSmVFTkhUV2RZUlVOMlpHTTFZMmMyYTB0MWVsSlhSVEpKUVhwc1EzTm5Wa2R6ZFZaMGQxUlFSalJoV21kRVExVkxOV2hpTHpKWmJXZzNUMUZ4TkZwR1dWUlpaMEpQVlV0cFFsaEpiRkZCVFc1SE9IcEJVMFZ2V0RCRGRUQkVVbGhvVGlzNWQycEJZa1ZKVEhsQ1lrNXhNeXQzVFhsQ1dHaGtOMk0wZWtGUlJXOUlkMEoxWTBzNVprTkdXRFZCYjFsTVZtWXlSRTFuVm5oemJWWTFVMXBaZDBVMmVITnNMMUZ2WmpkT1ZVdzJUVWxXWm10RGFVSllRVXhyZVhneGJIcFNaV2RDY1VOYVdITm5XbVZWUTAwMldqaG5WbWRPUm1sS1YzTkJha3BGY25sM2VVRllRVWR2YldwWWEwTnpRbTkxV2trMVFVWktiV2x4UVVSVlJsZHpXa0UyUVRaTmQyaFdkMFJKUmxGRFdtdDVNVmhvUW5sQmVXNUpiR1YzUTJ0NWFGRkNRalpETmxkTmEyVkJUMUZMWjBwRFZGQlJRVk5hZHlzMVFXcENaWEoxVVZCblJGTmFTWFJuUVdSQ1FYSkhVVkZuVW1GWlNVNW5SR3REYjBRNFNWWmpRWGxLVldOQmNrRTVaemhuVm1kUVJtbEtXV05CV0UwNFZXZFJZVUZZUVVkUlVTdFJTMEZNYlZOU2QwUjVlVnBWZDBFNVFsSnlUMUZTUVVoSlJsRktha3BKZDBSclUzQkJRbmxEZEZoaloyMVJVaXRSUzJkRFFXcFdkME5UV21oTE5VRnJRM1UxUWtsbmF6aG5Wa0ZGUmtkelFVUnJla05XZVVKWlFsbDVVMXBCVG5BelQwVm5SVWRuUm5kQ2EwVXhTalZqY25sQlVVdE5XVUZGWjFZMFNVeDNRVU40YTJ4SFFXWkRTbGhCVFdkV2RWRkxVVTFLTDBsR1VVSnBTbUZqUVU5VlYzVkJTa0Z5WTJkWFoyOUdkMHBNVVVSclEyOURZMU5wY0Zoc1p6QlJWMEZSVEVGTWExTldaMFJKUmxGQ1dsSmhORUZyUTNRMVFtVkNiVlp3UlhKQlRWSkxXbWRJZVdsc2QwSkpSbVpyUTJ0RVQzcERTbGhCVFdsV2VrRkthekYxTkUxRlZsRkJjRzl4VmpOQlRHdFdibkUxYzJ4NVFXdERTbGhCVFdsV1owRktRWEpuUkVsTVdFbEdaMFpxU2t4M1FWcHpiM1JqUVZOQ1dEaG5kVkZZTVZCc1UzVkJRalZKY0dOQlUyZHNWakZ1UkZOa2FVSktRbWRIUzFWWGRXcEtWRGRDWlZoTGNHZFlRazFuZUdsb1pucGFTMUJOVUhsMFYwOUtObVZyVVdKQlRXY3haMmhhZW1KTEt6WlNlVnBtVDBOYVFtdEhjVkZNU2tsc1pVZ3phRXN3YVZWSGQwUkpUbGx2VlRReWEzRjBhMk4wV0RGNVFrVkZlWHBEU1VaVFltMUpOMnhMSzNCbFExaFVaWHBCUTA1YWFHdEhjVVZLTVZCS1NYUmpVRmczUTNVek9HMDRlVXRMUXpSS2JFZE5VVXRzWkRkMGNWWm9VRmN2V2t4R04yNUxka2xDUTJwWFVWcENja2hEY1dWNGVHNWxVbkZYWVRSNVFqUjRRVVV6UzBkUllXOTNLMlF5VUVOMFpFeHlhVFF5ZERFeVRFcFRORWxzYlVWUlN6VkRjMlU1WkU1eWFUUXpkVWMxU0c5VFFscG9hVWRpSzNWVVRtdDVlWEZ1U21ack5uSkNZMXBTUlhOdlUxaDNSRWxPVlRSV1ltVldTSGxvYmk5Vk1sVlBVbkU0VjJWbFVFNWlabTV4WmxGQmMwVjVhVUpXZFRVd016RmtNVkY1ZGl0T1JYSnFZa2t4WTNGeVFuZG5PWGQ1UWxkMVEyeFlZak5QY0ZNME5EWjFVMjl4VmpSSlMwSk5jMmRXWVdkcFZqRXJlWEZYY1cwemMzQnhZM0pZTkZkSlNVdENUWE5uVm5GbmNWZE9Wa1pMTDB0a2J5OUVPV3hZYmtSV2JteGlSVU5DV2tKeVJrTldZbXhoZVdGc2MyTTFRV2h1Tmk5TU1XUk5VR2xhVFhKRlEzcEVTVVpZV1V3eFkxSlhWRlU1YlRZdlNqRmtVRWhKYkdOblYwbGFRbkpJUW1WeWJscHJSbkpzU3psMVpVOVllV0ZJV0VsR2EwZFpZamhKYkdVMVkyOTBZMkphYVRCcmVXUnBhV2RuUlhsNVFsWnRTbWRzVjJaUVRIbFdWME5QTldWMkwybHNiRGxqV1c1TFoybFhXV05uVUZwS1lYSk9lbTFYTDFWVGRuQkdlWFJ1U0VvNWJXTm9UMVJWT1d0clEzcEVhMEp1U1V4c1kxWlVkRVpoZVdSWWNVUmlNbTE1TlZkM1NrWnBSMGxUWkZGVFlUY3JkbG8xVFU5WWEzbGxOQ3RrV0VzeFRWaG5ZVFY1ZEVFd1VsRm1RazFuZWpWblEzQjViRlZIYlhsT1ZVRjFXVzl2U1ZCR1FuTm5lRVJLYlVKdWVqaHdVMWcxUjJZNWVqWmtkQ3RUUzFoSlJtZEhZMUZMTlVOeVJqa3JWR1Z5ZGk5UkwxVnRkVmwxTURrcmRtSTNOR2RLUlhkd1FVd3lUakF6WWpsYVdHeHliMjVXZDI1c1lYTmxXRkp2YjB0clEzaEVTbWxETmtJeVlXOXlOSGN4VkRZMlUzbFdXREE1ZVhsTFEwSkJjMmN4WjJoMWJ6bHJjVXN2VFhSVk5uVkVkR2cwZUVnd2VHSnZhMmhEU2xwQ2NrOUVNRXR3VUUxV1MyaDZZMjVYY0RKbGR6WXhhRk5wU1VaclIzRmpUSEJZUTBKV05VTndUbWMyYkdWUVEwSlpRbkpGUTNGVFNsWTFRM0JrV1RGdE5VcG9SVUZuYlZoSlEyUjZZVGsyVTB0WVJ6RmFhRVpRVUZVelEwTmFRbTFyUTJ4dU1uVk9UWEZqY2xaV2NuRkxkV0pZVjNoNFFWRkpiR2xGVkhOSWRDOXJlWEI1VmxWaGRVbG9aRTFZU1VKblIyRnZWekJtY3pjd0wxZE5SVFp6ZEVRd2FYVnVSMHRDV2tKdGEwTjJTMnhWU1RKVWNUWmFSbUZoZFVOWlFrVnliMFpQTWxSTk0wdEZia3BHYzBGRFExSmhkMEV2V1hoamEyRjJVRU15Y1dKbmJWTlNTM0ZDZVhacmVsQjRMMUo1VG1KWWNESmhXV2RYVFZGTGNVcG5kSE5xUnpWWVJFWXZaMGRCVWtzMlFsZDBjMmhHWTJ0WGQyOU9XVTFWWjFkQldFVXhZbUZFVFVKbmExZHpRVXBCY2tGQlUweFhRVWRaU3pGalJVTjRRVzlDYzBWRFdrTkhOVUZyUTNWNVFsVkJZMmRYUVZoS1JYSkJUMUpMYjBGRVEzaEtRMGhuUkhkclZuZEVTVVppYTBOUlN6UkJhME4wZVVKWlFtTkRVazlCVjBKcmVVVmFRMG8xUVc5UlNrRmhOVUZyUTNWQlNrRnlZMmRYUVZoQlJXZFdaMUZNUVV4clUwbHZRVkZOWlZGcFNVSk5MemR1VldoQloyZFRaekYzUWsxd1JtTkJVMEpYUWtGelFYVlJTa0Z5YzJkV1FVaEpiRkZCUVdoWmMyaEhVVU0yVTB0M1JHdHBiVUZDUzBNNVdFRm5VV2RXYjFwelFrMWlUR3hrVFhKUlNVRlpOVUZ4VVdwbFVVdEJUR3RwVjBGRVNVWlJRbWxTWVRSQlpFMTRSMk5uVlVsRlNVNWpRV0pLZURVNU5GZEliMEZCVFZGblYwbENkVlI1Y0ZoM1FVbHBWa2xTT0VKamFWVTRRVTlHYUd0RGRFRlFjMkoxWVN0RlFrTkJLMFJaUVVoNWExWjNRa2xHWW10RFowTTFRV3REZFhsQ1YwRk1kbXhKY21kRWFGVldTa1ZEUW1GQlZYRmtWelZCYjFGSVJsaHJaekYzUWtkRGJGaG5aMDFuVm5wMmVtY3hkMEpMUTBaWVZIRTRRWGRXUmtwUFoyZFhRVWhKUm1kR2FIUjVRWGg1UWxsQ1kwRlRRbGgzV0d4Q2NtZEVZM2xuZEhsQ1VXbFBNVzlLUW5OQlExRkxkMEpxZUdOdlkzbEZjV2RsYXpkdEwwbFZRVkp2Y1VabFdrTldRVXhyVTBkblF6VkxjR2RLUWtGMFFXbHZUV3RuVVVWSmFrVTBRMUZoTkVGclEzTkJOMU5VUXk5TmFFdG5SbmRLUkVsQ1kwNWpkMEZuWjFoSlUxaEpSbWRHWjA1RlUzaFdSRVJST1hSVFNsaG5UVU5aYzNabVRrZFRRWEpUT0hGV2QwbG5ka0Z5VDBONlMwcG5NM1ZSYkZGTE5tTlljRlYwUVVSTlJWbHRWVTlCV2tGeVFrMDJMMWRWU1ZkTlZFTlFOV2hMV1hWTlprcFdaRTlHVGpKUE5FeFJXRzB3Y0hkRE5VdHlTMEpXYURBNGQzQnBXbFkwUWtwT00xaE9aVE5QV1hZdlVETktiR0phU2tWQll6SjBLMEZZU2xZclUwbEhUSFpFWW01NlQySmhhMjVxU2pGblFXbG9kMkZEV1hCNmFUZDJlVGd5V2xaUVYyNDBOV2h5UVZGTWExTjZTRVo2V1cwMWVIVjFSMkkyWms5RFdtSmllSFJDTjA1U1RFVkVTVEpIZUZvd1JqTkdTV2gwYnpkWk1HVldTVXhrVjJoWFEzTm9WblZ2YzNCMlMwRXlSbUZ2TVdWRVRrMWlSa2N6UkhNeFRUaHpUV2xSWlVWV1F5dFpNV1EzVGt4alRrZHFRbk16VVVGTWJYa3dTSFZNZUZsM1NVTXpkVlUyUlV3NWNXRlZRMEl3VjBOM2RXdFdka0pKUkRKUldFcHphaXRFTlRBNVVVVkRjMUZMTVdkaU9VdHdSR013UjNVNFIwRjBla2swTm5ORFpVWlBZVVJIUW5OeFZtOU9hWHBDYlZwSlVHUnBTRUpDYWpRM09YQjZiWEJXT0V3MVMzSjJRVlpvYkhSVFJsQlhhVGxuVGk5V01qSndUakp1T0VnMWIxSmphMU55VlVSM2RYcFNOMEYzT0RsU2NYRnRVMDVyY1haaWFUVlhlRmxCVVhOa2RUbEdUVEJsVFZGaE5VODVTME5OTDFvMVkwaGFOVzlWWjFacFFsZDFTWGxYU1ZZeldrMXhPWFJvY21VM1EwUllSMWMyU0cxSFMwVXZkbEZxUWtWemEwdDJiMDl1TTNwak1EWjBUMmhvWjJOeFJ6Vk5WR05EWlZjMVZFNXNkR3RIVkU1eU5tWmlhbXR4ZEVKd01XTXpjbmt6VERkNFRTOTRRWEpYUmxReFVIQnlTMGt4WVRWeVNWWmpTbWNyVEVka1ZUWlJUMDlSVEVOeVRrVnpUa1F6ZEU5dGJsZDBhbHBWTURjNVdFZGFibGxaUjFoVU4wOTVVM0JXVVRkMEswRjZVa05DVW5ReU1YcHdXVUpVY1RCUE56ZHpTamszZERabE9USkZRMnBrY2pCMWVWRmhUbEpsTXpSNk1UUmtVM0Z6Um5oV1EwbHdjM28xUmxWRFYxUnlWRXhNVFZSbk9WSjJiakF2Y1cxb1NqWnViMHhDYmt0V05reHNXREpMVTFrNE1tOVljRUp2T1RWUGNVVmhSMVUwZEZad05XNUtTMlZOYkZkMWJrWkphRk1yTWxwRlVUTmhjbEJSVVhKTGVUazBkR0Z3Vm1SVU9HeFFWMVZUV1V4c2JFRnhXbUYwUW5OeFFVOHhVVXMxTW1sMFluUkVOMHRVY1RoYWVUVmFVVXRIWlhaU1ZFVkJWMFZMZW1SUVpWaHRWelJMYW5oaGNYcFlTa1Z4V2tGNGJITjNRakZSWWtJeU9UVnJWbE5rY25oSFlrdDRablJLU25OTVN5czVWV1Y1UVVKRGMwZFpTMVkyVTNaWlluSTRiRTlPY0U1eGFpdENVM0ZrVlVwQmMwRlRSbUpIWVRkb09TczBaREpvZWpkU1ZDdE1WVVUyYmpaQldGVnVWMUZCZDFRM1NuVnVNbUZrVUhKVllVd3haVlp1YTJsWWVqRktOWFY0UVVGVFJsbHRkMlpMVnpSRlF6VTJkbWRDWkZvdlRFRnZSRnB2Y2xnM09UVTBWVWx4WkZkQ05UVk5OVW94WkhVNUsySjViMlpuUVdOQ2NGWnZXbkprUjNKV09GQlNjVGgyWmpoRlUzTkJTVVp4TXk5NWFuTjBiSGhXTDFGNE5GZHlibmxRV0RncmFIZFZRVXBQZG1WSU5HTTFkRmR2YjFkSE56SXJaV1J5UlRCTlFWRk1Takl2TXpWNU1WVjVkVk5PVjJWV2VFVkJRVXRNTVRWdVptVnNURk5YWW5kdVpXWklTMFZEWjBOQk9ETXdjalp1UjJSWGFWWTRaM0ZSUzBGSlFuWjJVM28yYzJOc1ZrRTNiSGxYZDA1blVtOU5kME01V1ZaMVVUUk5UVzR3WmxsWWRYZ3lka1ZyY3pONVFqVmxMeXQyVXpKaVVEQnFUV2gyVjBWa1kxYzNiblptTVUxV1lYVm9NWGN4WldGSmNuWXhZMmN4UVc5UWNqbHdjbGd4ZGtaVkswNU5NVFZzT1VnMVEzTXpTREkyWjIxRFZIRTRkRkJrVFdWSU5rY3pVa2QyVjFnNVVuRmtTMGg2Wm5SNFZuRXdTSEpYTXpaemEyRXZPR2hDTjBWeE9HOVNMeTlpYldSMk9GQlhja1pPTTBKTFdtMXpOblEyWTNCWWFHMXhkblJvWVRsYVRGQnVUemxYTW01V2RWUnhlVUpqYmpJME5XVkRWMlpaU0RkelNGVmxiVEYyYkRNeWNrSlBkblZ4WkZwSFkxZExXRU5YZDJJMWRtRnhaRlZWZFZOTVRHUTBVbmQ0TWs1Vk1rRjJaSGhrZEhCV2JEWTFZVzFsTDNBck4xbDJkWFpYUkdKWlpuVmxjSGxIY0ZBNVRGVkdlVEZsTDFWTUwzQnllbmxTYUM5MFQzTklXV05qTlU5eFExaEtNVFJtY2xwbGVtbGFZelZoTUZFeE1GZDFjWE54Vm1zeFdsb3JLMVZsYkdRMFUweFFZbXRpTXpCcWRVVXlaSFIzYlZSdFJEVjVSbVpHV1RWSmNHYzBaSGsyTnl0eFdqUXlkbTFvYkhwa1pXNDJNbGR6TjFSRVVrcEVjbXMwTDB3eU9FeDRkVEF2TkhSVWNrNU5kWEJXWlVoVWNYaDJVREJtWWt0TE1XTmFRbGwwWTI1aWJXVk1UaTg0VFVadGRWWjJaV1pyTm0wcmIzVllWV2xzZVZKeE9FcHBNV1pzUlhCelNqRlVabmR5VTBoSlZtUjRNMnR4Y1RsclkxRnhRMmhqU21sc1ZrWnZZbXd6VERBMk9USjFkbGgwUTBZd1JXa3hla1pZU1haak4zazVZV1pKU21OSldHeGtWRmhyYkhZdlRqTjFjMk5XZFdOeE1HNXVTMlpaU0VWS2F6UkphV3BVWm1Kb00zaDJaazFFTm5sWFpsUjVRbGMxZVdsU1dGZ3pObVp3VFhrMVNqTm5SWFZWU1hwelkzSlRkRW92SzJSbFVIQjBPR0U0WkZWeGRYTjFlbUpwVEdWUGNFOVhUV3MyTDNoRWJVWjVhMDlWTURReldYUmFaak55VW01S1JuSnlTMk5ZYTFJNVNtdGtjVGhuWkhsU1lUVjNWVWxvZFRGd1RtSk5Xa055ZVZoSlZrdFhSbEJsYjNvd05VRTNkR0U0U1d0SlZrOTZkbFpXVkRWSmNHTldXbE55VTJ3NE5ISnJMM2RDYmtwc2EyNURlREJXWVRsRU1VeHVOamR4ZUVoMGJIcE1hM1JYUldsNGQyaHBSVlJhWkUxUmNUVXhNVk5oTlRaNU9WWktXVk5qV0VGTWEzbFhWV2hZU25wMFVFeFZOMWRIVEc1TFkxSXlibUp3Y0dFclZFZDVXalJUYTBwR1dqaDNXVkZvYjFacU9UZHJkRVprT1daaE1ubzVTbEF6VlRaTk1HMTFWSEkxTVU1clYzVnpiak5xUVV4cVEwOUZPSGRoWW1kMFZtbzRNVzQ1TVRFNUsxWXJVRFprUlV4MGRGaFZNVkk0S3pKNU5saEZNeXR2ZUVFMGFFTkdXRk5HT0ZoT2VWWjFXaXN5ZEVOTkwwOTJWVXQxT1hSNmJWazJTbU1yVlhkYVQwbE1TbEU1VEZSeGRITkNibUZJTlZad1MzSm1lQ3RxZVhOc2JEVm9ZMG8xUlhJck5uZHVjWGNyVW1oaGVVODJTR1JMTTFBelRqRTJOVk56S3l0cmJIVmxjSG80YVhoS2RWRkZNMDFKYTI5SFNUWXpVSFpDVFhKeloxWjFVMHBZZGtsQldHMUZVemhYYm1ab1VXRTNTVVppYTJsV0swRkZTbWhOUW1GNU5VRjVRbGMxU1d4bWEwTnVla0ZhUVV4ck5tNUJWMnREZEhsU1lUWTBRVUpqZDNGUlF6VlRkR2QzWWpsNll6QnNPRXdyYTNSQ09FbENNbXN5Y0drd1lVZzFhM0YxTkhnM01sWmFaVk5MV0VsR1dXMVdlVTFzUWpWNVpGWmxkVTF1T0ZaNldUTm1OVEl6UWxoTk9FNUViR1JOYzBGc2RWZDRaVlptZDJVMWVXNVFhVEJtV0RBMWJsbFhhMmx6TkZkRVNFcEhRemRrVmxweVVUVktUM0pwYlhaek5VbHdZM2RoUjB0cFVXRTFVMjVETmxGaE56SnVRMFJsVjBkT2VWSmhOMmRSU1ZabmIxWjNPVzVMWnpOS01XUXpOek5QTVU4d2MzRXhRM1UxU1d4a05uWldOWFozZEVaSGNuSkpPWEExVURkdlRrOWxkVnBWUWpWTmNHWkRNRXRtVGk5RloxWXJVM0Z1Vm5wa00xQjJhMmxzZUVKcWVtWjRTVVptUmpWRGNubzNOWEJUVFN0UmNUZDJaRGw2VjI1YWNuSXJja0ZSYzBGamExZDFhVUpYTlRKcFFsaEZiRXBtVm5kelYwRmxVMHRZUkZWV1N6Tk1NVGRaT0ZJMVRFOHJjbkZrWWtOS1EzSnhNMHN4T0hKemNYbEZibXQyUm1GMk1WbHdlVVphV0hoME1GRlFLM0pyUm5OVFJHdGhjRUpqVkZScU5XbFVlV2cyZVVJek5VdHhkVnBGVFhaMGVVRm5WalJZTDFoTUwycGtWbGc1WTFCT1QxRlRRbGhrTWxWT0szSnBSbk5VUkd0cGJIbFdkalkyTXl0NldHcE9XR1ZVY1RoNlptMVRhbkphTDFwMk5qSndlRkZMTjBreFUySktlVzVMT1hVM0t6WktObEIzYTFOek5FaE1Sa0ZHYjJoamEyRnpiVEUxVm5oSU1DdFJjUzg1SzNwS01YSkxMMWd3WWxnellsRnZSbU5yWVhORlpUY3ZZblpRYmsxR1psSnpVR1IwUTJkV2VWSnhOMGRUZEdWUU0ydDVkbTh4TDNFeFFsVlBOVGxpTlZwQ2VHd3JOVFlyTDNVNVMyVjVURk5ZWkM4emRVbzBMMkpKWlU1cmMyUjZWM0Y1TUdGNVJsaHdSVFpLVHl0NVNFeDBaUzgyYmxZdksxVndTbU4zVTBWSlRFSjRVMmxOYVU1UGMyb3lUbE15Vmprd1NWZDFZbkJrU1N0UlN5dHFUWE5KVFdoV05IWjBTbFprYzBoME5UZEVjV0p4YzBwS2RtdERibkY2UW1KVFNUVkhjamhDT1hFM01WQTVUblZqY2pJeVQxRkxlRUZ2VjBWMFpFOUNNMWt6ZVVGd2VXUlhTMDlQZWpaUVUyMHJNV3RYU1RRNFFVUkNkMjlwWVdSVU1qRndZV1pEYVZaRVZpOUtXVkVyY2tJMmMwdG5RV2xCVjJGVGFXOWpZMnhvV1dsM2MwRm5VRFJNYVhkelFXZE9Obkk1TVhCclFVRkVNRmhJTTFoWmJIUnZRVUZFTUZkNlFuQkJRVU5KUmxOM05rRkJRalp5UkRWeU5GTXdPRUZCUkRaTGVGRkJRVUZETVdVMmRscFdVVkZMUVZGRFozQXJjWEJwYTBWb1FVRkNRWEpMUVdkQlFVUlJValpGZDBGQlJGRlJMMVpRZUdGR1FVRkJSRkZQT0VjclFWRkJaMVpzUVc5QlFVUnZiREZCZDBObldVRnZSbVp4YTNsQ1dVRkJSRzlyVmtFNFFVRkViMnBXUWtGRFoyZEJiME12Y1dsM2NFcEpVVVZCT1VWUU9VVkJiMHRCU1VRd1psWkJkbFpHYVV0RFowTm5MeXR0UWxWR2QwRkJUMmc1VlVkUlFVRlBhRFZWUjNsTFJGRkRaek1yYkROVlNFRkJRVTlvZWpaR2REQkRaemhCVVV0NVp5dENVV2RCUlVKMk1EbGtRWE5CUVVFd1RsQkJPR2RGUVVsR1dsRnNRVzlUUVVWRGMwRkpTVVpCVGtNM09VTTRVVXhCUVVGcFFsVlZjV3RKUmtGUGFGaG5TVWxHUVU5b1ZDdG9VVlZNWjBKQmFqbExabTlJWjFaTlFVTkJWMFZGVWtzeVVVRkJURVZEUTBKWlFWRkNMMU5wTUVOM1FVRkJaMVpzUkdkcGFITkJVVXQzUVdkblZVRkpSbGxCZDFGSlFUWkVUalpFVWxNcmQyZGpRVFpET1VGb2RVc3pRVkZDUVdJNVJsaFpRbEJaUTBGQlFWbG5WVkZNUVVGQmMxRkpTVVpuUWtGRU9VVXZXVWhRV1VsQlEyZGlLMmRpWjBrd1EwRkRhbGhNTDFGTk1rUkJNa04zUkc5Rk0yOUdVVXhCUVVGUWIwUlpRVTFDUVZCUlJuZEZZWGx0VVVKQlVEbEJURmxGVUZwV1FVTm5SQ3RuUWtGTlJVTkJRa0Z5ZDBOaGVubFJSRUZwTW5KQmFISlFhRUZGUkU5NU0yNUJlR2ROUVhsSVprRkNaMUZCTVUwMU1IVlJOMWxxUVVGbmVUSlZOVmxHVFVOQlNFcHNkVVpyU0VOQ1dVRjVSekkxUkdONlduRkVXWEpCUWtGeWQwbGhNV0ZSUjBGWFFVVm5WMEZCWjIweVZYcFpRMDFFWjBSNVYzZzBRVTVpVlUxRVowSjNSMWxIVFVSblQzbFdkalJDVGtSblFYbFdLMWxEVG5KMlRrUm5SRXB6ZEdKelFYcFpLMEZOYUZjclVXOUpRVk5GUVVGSWJubFdTMWxEUVd0RlowRktRMnBOYUZGUlJITkpRa0ZNZHpSQ1UwRnZRVXRDWkZaemNFeFJSMmRKUkZGQmVWVnFORU5CYTFOQlFVbEJXRzVuUTBWRFVVTXdlVEJHV2tOQloxZDNVVXBCTDNOckswRkZTVWRCVEhsdlFrTkNkMEZGUkU5QlVrRTRkMmRsUVdKS1RuSkJRVkZTUVVacmJYcDNRVWxLVVVOUldWRkRSV3MwUVVOUlMyOUJRME4xUWtKVlFrOTVVMnRCY2xsT1RHVkJSMUZUZDBGRmJWUkJSREJFVTBoYVFrVkJhMmRXUVRkeloyVkJTVXBQTUVGSGVWSjBXVUZGU0hsRFJEQkRjbUpLUlhaQlJXZFhRVVpyYVZSM1FVbFNZVVZKZVVFdk5VRlJRa05GYTBOeWRrcEJXa0ZCVTIwd1FWSnJhRWgzUVVsRlEwWkxRMEZVTlVGRlFXdERkMEZ5Wm1FdlJFRkJaMWxKVlhOWlRTOWlOM2RDVVZRM1NVVk1LMEpHUmtGQlNWbFZSVTB5VFhZeVRYZERhRXhLWjBKbE9XWmxRbGxFZUZGVE1uTlpZUzloY0hkQlozVkJWVFEwVFZWUVFVRm9NR2RSTjFsbUwxbG5RVUZvTlVsUk9UZDZXRFJFUVUxRmRpdEhSbVl5Vm5OQmIwSnNiME5OUTNjdlYwMVFRVmxCYlc5V1NFRlFja1pZUVVWRWVqQkZRbWRVT1dkVVFVdERhR0ZEZUZFck1tOW1RVXRFVW1GRWFGRTBLMjlpUVVSUlozcFJhSEZYVWpCRVowMWhhMVZWUnpseGJGVkJaMHRoYkd0aGJFWjBVV2RCTUU1M01FOHpWbTF4UkVWQk1FRkJUa1JXU0RseFEwVkJaMFZoY0dWaGIwaE9VVVZCTUVaVFRtZE5Xbk55WjJkV1FVbENiMGRSWVdoQlowRlJUR05OZDFOQ1ZVRm5SMmRhUW5GRlEwRktRWE4zZVVKVlFVRkRhVnBTYVVWRFowRkJiMjFWV1doQmIwRlJURmxOWnpGQlFrRkZRekpFUkVsR1FVRkVXazF6WjFWQlFVSnJlWGxDVkVGQlEwRmhRbXhyUTJkQlFYTnRWMUZMVVVGQmVVcGFRbkJuUVVGQlQydHBWVkZCUVdkSVoxcEtRVzlCUVU1S2JFVkRaMEZCUlVNNFEwSlJRVUZEUW1od1FXdEJRVU5EU25KS2EzQkJRbTQxWmpjMVN6WnFVWE1yTVRJMFFVRkJRVUZGYkVaVWExTjFVVzFEUXdvOEwyUmhkR0UrUEhkcFpIUm9QakUwTkM0d1BDOTNhV1IwYUQ0OGFHVnBaMmgwUGpFME5DNHdQQzlvWldsbmFIUStQQzlRYVdOMGNuVmxTVzVtYno0OEwxTmxZV3hKYm1adlBqeFRhV2R1U1c1bWJ6NDhZMlZ5ZEQ0OEwyTmxjblErUEhOcFoyNWhkSFZ5WlVGc1oyOXlhWFJvYlQ0OEwzTnBaMjVoZEhWeVpVRnNaMjl5YVhSb2JUNDhjMmxuYmtSaGRHRStNakF5TlMwd055MHdOQ0F4T0RveU1EbzBPRHd2YzJsbmJrUmhkR0UrUEhOcFoyNUVZWFJsUGpJd01qVXRNRGN0TURRZ01UZzZNakE2TkRnOEwzTnBaMjVFWVhSbFBqd3ZVMmxuYmtsdVptOCtQQzlUWldGc1BnPT0KPC9zZWFsPjxzaWduRGF0ZT4yMDI1LTA3LTA0IDE4OjIwOjQ4PC9zaWduRGF0ZT48aGFzaD5udWxsPC9oYXNoPjxjZXJ0U04+PC9jZXJ0U04+PHNpZ25hdHVyZUFsZ29yaXRobT5TR0RfU0hBMjU2X1JTQTwvc2lnbmF0dXJlQWxnb3JpdGhtPjxzaWduUmVzdWx0Pm51bGw8L3NpZ25SZXN1bHQ+PC9TaWduYXR1cmU+\n\n	2025-07-04T18:20:48+0800\n	https://www.tsign.cn?serviceId=5de19d3d2ef845d393fa09cab14f7d3f&fileId=3c83cadb796b43f8b08793067154cb3e\n\n\n		PFNpZ25hdHVyZT48aXNDbG91ZD4xPC9pc0Nsb3VkPjxzaWQ+aHR0cHM6Ly93d3cudHNpZ24uY24/c2VydmljZUlkPTVkZTE5ZDNkMmVmODQ1ZDM5M2ZhMDljYWIxNGY3ZDNmJmZpbGVJZD0zYzgzY2FkYjc5NmI0M2Y4YjA4NzkzMDY3MTU0Y2IzZTwvc2lkPjxzZWFsPlBGTmxZV3crUEVobFlXUmxjajQ4U1VRK1JWTThMMGxFUGp4MlpYSnphVzl1UGpNd01ERThMM1psY25OcGIyNCtQRlpwWkQ1VWFXMWxkbUZzWlR3dlZtbGtQanhzWlc1bmRHZytNRHd2YkdWdVozUm9Qand2U0dWaFpHVnlQanhUWldGc1NXNW1iejQ4WlhOSlJENW9kSFJ3Y3pvdkwzZDNkeTUwYzJsbmJpNWpiajl6WlhKMmFXTmxTV1E5TldSbE1UbGtNMlF5WldZNE5EVmtNemt6Wm1Fd09XTmhZakUwWmpka00yWW1abWxzWlVsa1BUTmpPRE5qWVdSaU56azJZalF6WmpoaU1EZzNPVE13TmpjeE5UUmpZak5sUEM5bGMwbEVQanhRY205d1pYSjBlVWx1Wm04K1BIUjVjR1UrT1R3dmRIbHdaVDQ4Ym1GdFpUNDhMMjVoYldVK1BHTmxjblErUEM5alpYSjBQanhqY21WaGRHVkVZWFJsUGpJd01qVXRNRGN0TURRZ01UZzZNalk2TWpROEwyTnlaV0YwWlVSaGRHVStQSFpoYkdsa1UzUmhjblErUEM5MllXeHBaRk4wWVhKMFBqeDJZV3hwWkVWdVpENDhMM1poYkdsa1JXNWtQand2VUhKdmNHVnlkSGxKYm1adlBqeFFhV04wY25WbFNXNW1iejQ4ZEhsd1pUNVFUa2M4TDNSNWNHVStQR1JoZEdFK2FWWkNUMUozTUV0SFoyOUJRVUZCVGxOVmFFVlZaMEZCUVd4alFVRkJTbGhEUVZsQlFVRkRLMDh6UjBWQlFVRkVRVVpDVFZaRlZVRkJRVUZCUVVSTlFVRkhXVUZCU210QlFVMTNRVUZRT0VGTmQwRkJUWHBOUVUweVdVRk5OV3RCVFRoM1FVMHZPRUZhWjBGQldtcE5RVnB0V1VGYWNHdEJXbk4zUVZwMk9FRnRVVUZCYlZSTlFXMVhXVUZ0V210QmJXTjNRVzFtT0VGNlFVRkJla1JOUVhwSFdVRjZTbXRCZWsxM1FYcFFPRUV2ZDBGQkwzcE5RUzh5V1VFdk5XdEJMemgzUVM4dk9IcEJRVUY2UVVSTmVrRkhXWHBCU210NlFVMTNla0ZRT0hwTmQwRjZUWHBOZWsweVdYcE5OV3Q2VFRoM2VrMHZPSHBhWjBGNldtcE5lbHB0V1hwYWNHdDZXbk4zZWxwMk9IcHRVVUY2YlZSTmVtMVhXWHB0V210NmJXTjNlbTFtT0hwNlFVRjZla1JOZW5wSFdYcDZTbXQ2ZWsxM2VucFFPSG92ZDBGNkwzcE5laTh5V1hvdk5XdDZMemgzZWk4dk9XMUJRVUp0UVVST2JVRkhXbTFCU214dFFVMTRiVUZRT1cxTmQwSnRUWHBPYlUweVdtMU5OV3h0VFRoNGJVMHZPVzFhWjBKdFdtcE9iVnB0V20xYWNHeHRXbk40YlZwMk9XMXRVVUp0YlZST2JXMVhXbTF0V214dGJXTjRiVzFtT1cxNlFVSnRla1JPYlhwSFdtMTZTbXh0ZWsxNGJYcFFPVzB2ZDBKdEwzcE9iUzh5V20wdk5XeHRMemg0YlM4dksxcEJRVU5hUVVSUFdrRkhZVnBCU20xYVFVMTVXa0ZRSzFwTmQwTmFUWHBQV2sweVlWcE5OVzFhVFRoNVdrMHZLMXBhWjBOYVdtcFBXbHB0WVZwYWNHMWFXbk41V2xwMksxcHRVVU5hYlZSUFdtMVhZVnB0V20xYWJXTjVXbTFtSzFwNlFVTmFla1JQV25wSFlWcDZTbTFhZWsxNVducFFLMW92ZDBOYUwzcFBXaTh5WVZvdk5XMWFMemg1V2k4dkwwMUJRVVJOUVVSUVRVRkhZazFCU201TlFVMTZUVUZRTDAxTmQwUk5UWHBRVFUweVlrMU5OVzVOVFRoNlRVMHZMMDFhWjBSTldtcFFUVnB0WWsxYWNHNU5Xbk42VFZwMkwwMXRVVVJOYlZSUVRXMVhZazF0V201TmJXTjZUVzFtTDAxNlFVUk5la1JRVFhwSFlrMTZTbTVOZWsxNlRYcFFMMDB2ZDBSTkwzcFFUUzh5WWswdk5XNU5Memg2VFM4dkx5OUJRVVF2UVVSUUwwRkhZaTlCU200dlFVMTZMMEZRTHk5TmQwUXZUWHBRTDAweVlpOU5OVzR2VFRoNkwwMHZMeTlhWjBRdldtcFFMMXB0WWk5YWNHNHZXbk42TDFwMkx5OXRVVVF2YlZSUUwyMVhZaTl0V200dmJXTjZMMjFtTHk5NlFVUXZla1JRTDNwSFlpOTZTbTR2ZWsxNkwzcFFMeTh2ZDBRdkwzcFFMeTh5WWk4dk5XNHZMemg2THk4dk9GTkZhRWxaUjBKblpVaG9OR3RLUTFGeFMybHZkMDFFUVRKT2FsazRVRVI0UTFGclNrbFRSV2hQVkdzMVZWWkdVbUZYYkhCbldVZENiVnB0V25OaVIzaDVZMjVLTkdWSWFDdG1ialpGYUVsVFMybHZjVkZyU2tOWGJIQmhZMjVLZVdsdmNVdHZjVXRwZFhKeE5qQjBURk0yZFhKeVFYZE5SRWQ0YzJKTmVrMTZVekIwVEZreVRtcGxNM1EzYXpWUFZIRTJkWEozT0ZCRU1qbDJZamd2VUhkblMxZE1SRUZCUVVGRFdFSkpWMWhOUVVGQk4wVkJRVUZQZUVGSFZrdDNOR0pCUVVGMFkydHNSVkZXVWpReWRUTmtNMXBMYlU5S1NtOHdXSG92VGl0cGJtNVlUVEZaTWxZeFQybE5RWFZUVkM5WFpIVk5hU3MzU3pSQlRrbzNjalJTU1ZBM09FRlpRa1F2VFN0bVVDOHJOWE5YYkRWQlFVSkJha3BLZEZKblFVRkJRMEpTZUVGelFVRkNRVzh3WjFWQlFVVnBWV3BZWjBKQlJVTm5Za3RSVEVGQlFXbGFVMDVqUVVGRFFWUk9ia2xHWjBGQmFFMXdSM1JuUVVGSlJrMHljMmRWUVVGS2JYa3lZMmRYUVVGQ2EzbHJZVEpCUVVGblZYcFpZakpSU1VGclEyMWlhbGRuUWtGRmFWWjZWV0V3UVVGQloxWkVXV0l3VVVsQlJVTnhZbXBYVVVKQlFXbFdlbFZoTUVGQlEydDViV0Y2UlZNd1FVRkxSMkZYTkVNeFVHTnJRMEZEYVRKcGNXWjRXWEYzUVVGQ1VrcFNaRVkwYzJoc1ZFRkxRVUZMYmxsM04yOTNPVUZKUkVOd2NFRmFiamhaYlVGRlJGSlZjV29yWVRVdlpDOUlkbW94T1dkR1FVTm9TeThyYTI5R1RHWXJNM0puTW5CblJVRlJOSFpRYUZBMVlqTlpabmhpY25kRVFVSlRXakJqVld3MGNuZHVSakphZUVGQlFsRlVRbEZTWTJsVkszaEJZMEZMUW5GTGVHVnNLMWhrYkVnMVlsbFJUU3RKUjBGRmFWWnZjRUp4TlhGd05sYzBaMnc0VVZGQlEyOUlhMGc1Y2xCeEwzWnZNRUUxYVZSS2QwSm5TVkYyTWxjclluUlljbE51ZEROck5uTmFOV2xFYzBGSlJsRnFSVE4yV0RsaE1TdHJOblptZW5Cc1kybFZaVk5DVVVOVFpVVnBZbVJsZGk5eUsyWmlWV0UxVjBZeFZWWnVlVkZNUVV0dFUzUkJkazlqU25sWGNYaFZRbTE1U2xjdk9YbG1iVU5XV2tGRmFGWTJkMU01WTNwYWFHdHNlamxrVDNWM2FURjRSamRyT0dOcmVYZEJlRXR3ZEUxME5WSlBRMk5KUW1KdVMwb3hZMnREZDBKSlZsbHlSVWN2YXpoNllWRklkVXRtWkVaeGQzRXpLMGxqUVVscVZqaFhVV0k1WW5acmNYQmtZemRhZVRGMWRGVjFOR2cwUVVKcFpsaEVSekZEY25SaVprSkRVbGc1TW1WMU5VRkpRV3RGYWt4R2RFcEtlakYxVW5GNk4ycFJNVFJCUVUxdFZGaERWMkowV205blZqRXpSbE53TkVGblIySktjM1ZOYzBKaWJuRXdValZVV25FeFNVWm5RMUZ4Y2xwNWJHRldaSFowZDFOcVNrTnlOMHhtUzBwemJWWlFRVWxCYUZKS2FXdzFiVXRJVkUweFIxY3JVR3R4ZGxwamExZDVRVXBCY1hsbEsyOVlSVlYyVUZaRWRHOW1OV1JFTnpGWWJUaElja3hzV25sRVFVSlRTbVZGWkc0M1ZXaFdOMWhyUzIxS1YySnhWblpQTXpkVlZ6ZzBRbEZMZDJ0MVJrSm9LMUJsTDJwYVEzSlhNak0wY0drNGNtbzJTR1ptZFM5S1pqVnpjMVo1VVV4QlMyMVRNRXhpVERGVUx5OHZXbVl5ZVV3MWVUazJNSGh0UTFZclQzTjRTMVJ6ZUVwTFoxVkJWWEJYZDJaaU9VbFRVRk56VkRCaE5USnVTRGR6Um5CalZGb3JNVWxzYTBGS1F6RktOak5OYUdaT2MwOTJPREUwVUVwSGVUZEtPRUZsYms5TmJscGpaa2xHWm5sR1VVTktVM0JKUzJGMVkyUXJOMnBrZUhWVWNYVXlRa2g1YkZjekswcExOMEZGYUUxQmVFNVVPVWhzTHpOVlpsWTFOakJ0ZVU1WVlrMWlVR0ZXZGt0WlVFRmFaMUZGU1hsUmVGWmliazFzVm1waVJWSklZVTlrTW05c2EwRmFRMFZLUzBKSVNXaE5kRlo1WmlzM2NWcGpXbFFyZGxVeVN6aE5jazR4VnpaNmJFNTNRMnRMYlc1VGNXSjRaMkZNVVUxa1NtVnliRkZsTm5BNGFGWlNSSHBNWkZGUlRGRkxUbXRyTmtWdldtVnRTRU5ZVERFeU9GQmlWU3RXY1RFdk5GVm1Ta2xHWjBabmRHNVZUVWgxV1hGUmNFMTZVRXB3UjNKTk4wNVhZakpPWmtScFVsbEJRMU5WZEc1TU1Yb3ZPWFpTVTJKbFJrNXJTMk53VmpsU2NUWkxXRXN5TW1JdldUSktWbWRCYVU1WWFHZHdTWGhyWVRBck5sQTNiR1V6VnpOcGRYRllZM2xXV0dRdldsaFBZMlpLYWxGRVIybEdWMGRCY0ZWNGFXRXlkVzUyTlZWeWJGcHRlREkzU2pGbGIxTkZNVlpwVFhWUUswdDFZelJsVWtsQmNWRnZLM1l5YjBaaFMwcGpVRkpZYTNKb09IUk9iWFZXVmpGWlNrWm5Ra28wY3k4elZ6Sm9Xa1YwYm5GSGJHaFdNV3BRTm1OMmRWTllTa2R5U0ZKa1dqaHBaVUZyYjJ0b1l6WklTMnh6VTJrMVEzQTNNRk5XV0RWTGNWTllSVE5OY0ZGQmEyZHFZWHBXZVdac1MydE5ablpVTXk5TFNXNU1SM0ZqTHk5YWRFODFNM2MyTlRodGNrRkRVMEZSTTB0V1RWZHRkRkIwUW1SUllUWXJNMHh2T0VwV1pXNHlLMVp3WmpNNVdtbHZUbU51V0RKQ2FHMVJRbmhMY0RsM1REaHdORXB1WmtkUGQzTldNVGxyV1RSS1kzSm1WRlExUVVvNFdUTjNWRXhKUWxWd1VXNHdSekJ0ZGxWMlJscHRZMjQwYzJsS00wWmliVXRzZFhkTldYWkpNSEpxUzJaUk9XSTRaREZMWTBOU1drRnlSa2xGSzA4d2NDdDVjRGtyWWxZMFdrb3lPVTl1TW5KT2NYUmpabGx0Y0hGQk9XSmtlWHBKUlZKalpVSkJkRUVyWVVNck9XVkNjSFJtTmswcmJTOXJObll5VEVSVWRtRmFhVmR0U1cxTGVsbDZSaXRsS3pSRlF6QkVjbGxRTlZOWFRIRk1NVkoxUWxkeWJIUmxTM1IwTTJ0eVR6ZFlSaXROYTFvemVGQjRjSFZqWnpaaE0xZzJMMEZyVjFGTGVFdG1SM2cxWkZGYWFXOXNlRlpYU2tsb2RURjRPV0phZG1SNmVYY3JkbGN5V1V0U05YVjZNRFZRWlhSb1pHUlJVVWRZZUdzNVpsbGlhMkZZVERnM1l6SnlSR3htYVdJclZXbzBLM3BXTm5Kc1NHbFhjVzByVG5kb1MxcHJXRGs1TVRGblVtSXhZbmRyVjFGTGVHRm1Nbm8xTm5kNlIzcFpValp4YXpKcGJqa3ljR05QTmxKek5FTXpXVzVVYkZCR1pVWk1SM1EzYmtKRE1uSlBaRkJ6UVVKcFpHRlhXVkJvUjNOMk1UTlNMeXN6WmxabGNtWTJTbTFKY2pZdk5XczJkakZQUmpBNWVqWndlWFJoVDJaeE16TnhhVzFCUW5oRGNsWjROVnBZUmpGd1kzWlpNbGQyV1RoNmVrTkxVWEV2VlhoMlpYUTRTMm8xYW5RclVIWk5hakV6VW5KQlFYTXhXSEJEZEVaaWQxaHlOblF4Ykc1aU16WlVkM2d3ZWtzclVYRTNiSGd5ZWxWRVpHRjBUVTB2VmpWV2NuSndaa3RCVUVWTGRraEVjbkYwZERoSVZDOVdWQzltUlhreFdIUjVWMnR6TVhoR2JqSlFia3BWYkZkTWNGbHhkR2RZUWtGblZHWXhZVXcyT1dwdGEzQXdWekpwTVhoR2NrcENVSEp0Y1dNME5GSjJSRlZpUldWNFdFcEtSbWxCYjB4MFUyTkxTM1ZhUTFBeVpqWjBUbFJ6YWxaNlprMXJWak4yYTNGdWNHZ3pabGRYV21aaFVIUlNUWE5uUm1ka1R5dGxTV2R5V0RaNllqVnhjekYwWmpWVGNuSkVSMXBJZFZsdlZUZG5ObnBXYlRsNlVUWmpXbEJKU1VaRFRFTnFOWGcyVmxCTFVHdExkVTEwYUdRdkt6TmtkRnBCV0V0V09DOTVjVXhFZFZGVFlYZHhhV0ZpWW1oQlEzaFRiSFEwZG5SNFMyVXZkazFXTlZaaVlVTjJVRzlhUjNKUWJreFdUMU5rVjBoQlkwVkRlVUpYUzFka2FuWm9ZV1p1VVM5T1dubDJWWEYzTHpWck5uUTJORE4xWVZkTU1sWlVWRmRDV1VsR1dXcFJlV2xyTlM5aVpWWkxWVXQwTVV0bVpIVlBTek01VEhKMU5tTXllRk0xWlhCeVNETnBkMnN2VGxBdlZtaHpTVVp2YUZZMkswUmFabUo0Wm1Jdk9WWmxRV295WW1aMVVuRXpOWGxPVlZkek0ybzBNMWRZYTFkVE5EQkJSV2RrVFRGWFVWTnVVMU55VUhKNksxcFNlRVo2U0hCc1Z6RkRWMWhFTVM5d2NrSnBiMWw0WTB4RWFsUjFRMXBaWjBkQmNFb3haRkp1ZUVOd1ptaHpNVGhwUm1aamJsWjJXRVZtTURKVVUzaFhjRE0xVldwTlNVWnZhRllyZDBRMWJXdFNNM1JZTDBZNFZsTXhWVWhsVm5FMmFub3Jka3BEVW5CbFl5dE9kazB6WkhRNWNXZ3dSVU00VW5GWVIwTTRabE5DTjFwalNGSnllWFV6Vm5oaGNrb3lNVGh2ZHpOSk1XWjJlbTFUV2xkeE5EaERWRWh4VEd0dFEwSlhRVzFKUlV4dFMwVndURXRMZW5GVWNUbHNlVlpVTUdZdmRUSmpiMm8xVlROMU0ybFZlakJDYzFKSlNWbFZXSGg2V0d4SVprOTVOVEJ5Wmtsa1oyOXpkV0p3TjBoT1YxZERiMnRUY1RGWE5YbHFXV1V2YmpGTlFrRnpaMVp6Wm1KaVlsVkJVRGR1TVVaNU1YZE9PR1JVYUhSc1FtTnlXRzV1UkhKRkwzaDFlREoyVm5Cd2JYbE1lWEJ5UWtGdmFsWjBZWFk1ZVVFck0xQnlNMVk0Um1GMVpIWmFSRGxRVFc5MU1UaGpTVVprYmpWaGNUWlhTekZqUTBoWVNXSjRXSEZxU1c5UFdXcFhieTlXWW1zMmMyeFdOMHBrWW1sT1JqbEZjbFpUWkZsU1kyNVNVa2xqZEZaSWNuUTJTMVptVVhSM1kzaDBWV0Z1WlhGUGQxbE1NVmxrV0hwMEsyMDFhV2xpTlNzNWEyRnpWRk5ZVFd4WFlTc3JXVlpzVG5KcVNqaE1NMGN6YjBaUlZuRXdiWHBXYW5ZM2FqSkRRbGRDTUdFMVJqRlhZUzhyVTI1RFNWUjRTbVpXYjFoa1VEazJPRmM0VWpGMlYxcExjamczVGxkdVkxZHhiVEY1WkRaRlQwTkNWMG94V1VoQ00yeExjek41V0dveE1YUnVTemMxTWlzd2J6TTJMMFV5Ylhkc1FrSnlhelk0WW1Sc1duSkxZa2t4Vld4YVNWWm5aMVozWTBka1NXTklXVVprYldzMlMwdFlZbE4zVmxwUGNrVXlUM0J4YkhwMFdFTXJkRmx1Umpkak4zaFVlRmR5TTA5U1NYTkZTM05FUVROdGFWaFFNWFJXYlVoSVdqTk9UMnB4TUc5cldXaGxPRlJ4YWtST01sVXpPVEZwYkZSWFQxWTFORTQzVTJoWWNUZG1NR1EwT0dabloxWnBaRmRuUjFwRlpFSllhak4xY2poWWVuaFBlbFkyWmtWV01WUTROelZEY2paSmNVTkRXRTh4Y1dzMGNVWk1Wa3R6VDNOcVZsTnNkV05YYUhWT1dVbEdXVXBhUTNKSE4wNW5SV0pqUjFaM1ZYSlJNVXRLV0VwT2NUazBla3RPVEcxTFlrcFBNM2t5Tld0WGVFRjZZMjl0VUdKdVNWWk1ZV05GUXpoUmNYbGhSR1JtVlZZNVpYVmljWFIzVXpKbGVtMUlNSGRyYkdreGVuUkxaMUprTlZOeGVWUmlUVlowWVdjemJWY3ZNbGhSWVhobGFYVmtRa0YyUlV0MGJHZHFWalpYU1U1emFXcGlPSFJVVUdoVmMwdzJLMDlxTlZweWFVdzNPRzFoUW5WNU1HRjBlR1YwZGxoSEsyNVhZWFJVYzJreFIxTjNVWEUwVW5aQ2EySXZWSFJhYTNSMmNYQnFXWEJZTVZOaldHMU9NR3hSZVdaaU5IWmhSbmsxWmxsNmVrTkVaVzVLYUZodFNWWkdlQzlyUTNOU2NWazBUR1l2WTBobWFsQXplWFJUSzNKRFQySlVORGh2YzFKRWRHVlNPRGxoVjB0MWRWZ3paRU55UzNKTE1WazNVR01yTVc5RU5FbEdXWFpYZUhOUE1WbEdlVzVFUm1aUFQzQlFMekZYTWxwV1NHbDNLMVZpVVdzd01tUjBWek51T1hWc00wZ3pWVmQxVkc5c1ZuQlhPRko1WjJkdlNURmlVblF2TVU5bU5tVjJVVzVNWWs1V1VGSlhZVGQzTjIxTGJqSnhjblIxT1RRd2NtaFNXRmRqVkV0b05UVkNjbWMxS3pCdFdqTkpjM05qVVV0akszcHNlREU1ZFhGd1FVTkNXRFJqTUdkWFRHTlhUWEUxVlc5RksweEdZMFZEYzJSdk5FRk1PRWxXZFZKTlVqUlpRV2xzZDFwbE9ISnpiRmxSTTB0NU9XeEZjMHRXVjFoS2FUbGtMM3BIT0VWRGMyUnZkemhEU1dabmRuaFRha1JOYzNGQ2EzQm1jREJGTmpkbE1rVmtWVVUySzFGek1pdHlha1E1V0dGUVpuSkRaVkJrYm1kaloxWnBUbFpDZDJaeU5ubFpXV0l2VWs5U2JrMXJWblYxWVc5WE1Vd3hUbFEwY1dacUwzaFRZVFpPWmt0VFJGbEpSbU5JWWpWMlNHa3hXSFI0VUhweFdWUmxWMkpEU1RGamR6aDBXSEZTVldwRlQySTBibFppTDJzeUsyeHNXbWRuVm1sa1pVZDZUbXNyVTFvclVYSjJlRzFFTmt4YVkzWlhNaTh6WWs0MlJYUjFjMlpHV0RGMVMyOTBPUzlNYlhkdFlraDVkblJRT1d0dGQxRkxkekpLWWtkV1JtTnZTakZ3Ym1ZeU0waDFTVzVzYlROeGIzTm1TakF2VkV3emNtc3dZbEpNTml0UlMzaERja1EwVERFMVZIbHlXR294YmxnMWNHaDBNbmxNTlc1dE5YRTViM05VT0ZkTWRFNHpURFZ0VWxsV1NscG5iMXA0WXpkUk5tRk9kemg2VURFelQyOUdUbWw2UkVJM1NtUnNaMWRvTjJSTVZFdFVlRmR3V0RKNFJYTjBRbGR5Y0RobU1VNU5SR1V6UVV4elNXeG9kbXM1ZW5WWFV5dEtRbWhRZW1GRlZ6VnhhVFZYUm1JMVJFdEdjVWxXWTI4elFUY3JjemxpVTNscWJGaFdhREZLVUhsMFdHWmFaMnhZTW5oVWIxQnRkVlpMV0dRd1JVTjBlVUpYU3pGSldta3JkbXBNT0RBM0wzQk1SbWhRV25VdlpTOTBObUk0VTFWTFRWZ3pWMkYxWTAxNlJWTjRjWEpLUldjMloyaFdObXRtV0hZdk5sUk9WMkl6TVd0V2NrdDRlVGxoWWpsV2FFdDBRa0ZQZWxaeWJteDVjVEZCWjJkWFJEVjJWVTB4VjNKVFpVUlFjbFpWYlhkMmREUmxMMlp4YUdFMGEwWjRRM0ZtV0VVeEsyVktNV2QzVjBGS1JYRjVkaXN6T0hKWFNtdG1WVWt5VVVzNGEwVnBTWFo2Vnk5R1EzSlBOMWhVUmtaRmNrNUpUMnhKWjFCTVdEbGthVXhUY1ZsTU1sSXdjV2w0U1M5d1p6RnBjVkJZVHpFclkyVm1UazR5UVhwcE5WaGFTelIzTWpoREt6TkNjQ3RqZWpGelFqSmFWamhpT0c1V05taG5VMk5VUW5Kc1ZTdDFkbk5hZERsSmVGWndXWFJWWjJkWFJFa3ZRbU53ZDJwWFJ6ZHVTMDlxYzBoV1Nua3hWM0JIWkdGRGJVMHJUVVI1TVhrNE1sWk5aM1pDUVhaRk5uTlZUVEZqY0RWV2JIRmFaa2xrWTBGbVNuQklZbXcyYlRsT1R6Vk1jR1I2T0hoWGNrdFZhV2t4YVZaRllYTXphV1YyY25WWVdWRk1TRWxHTTBKSGNqWkdiWGRwVDFBck4xRkpNWE5zTW1WWVozbHljSGhuYkZZNWJrWTJkWFZXTkZVck0wSjVUM1ZEVEZCTGJGZEJTRGR6YkZad2JVNWxXRmxaYkhOcU5scHpTVUphY1RSU2FVWlVhM0k1Wm1Kb2VYbHhRMUpoTmtGUE1rdFdLMXBwU214a2IwdFpkbGcyTVhRelN6RmtlbGcwT0c1UmFuVlJTekpJVFZKU05uaHBOVmR3ZVdwU1ZuUjRRM0pXSzJJMVRtcEtSblowYm5kV2JVbDRRMHR6YVVKMVlrNXpSVlk1YTFkTGJGSTJjWHBqZVRablRIUTRiblV5V25KelozWldSVVpuVlRSTlJuVnpNMmszVDFCRk1GRkRRbUY0TUhSc0x6ZHllakpYTWtWQ1ZteEZSRGxOTXpsME9GTkxXVXBGY2toVU1tdHZNamc1WWtVMmQwRkhVMlJ5V1c5UmNUTXZLemw1WmpWV2REQnNWMFJ4TkZWUlpDOHZZMXBuYUVwQlNVbG5SR1I0VjNKeGRXNXNjWEk0TkdRellteFNNemQwWVZOWGFVTkRUVVJQYmt3NVJIcExTeXRTTjNBMmEyRnZSMUU0WXpKdFRETTJjMnBDY0ZKR09GcDZVVUl5ZW14YVJtbGtXRlJET1V0dU16SjRWbWc1VlVaSVZIQmpjbWxKVTI1SlFVTXdSVWR6YjJ4a09WWTBMMVpCY0ROYVZFczFWMFZyT1ZWcmJIUmFVMVkwTUVGTFVYRnRNV2hHU0hJdlNrUm9RM0p2Ymt3eFNrSkdSWFJ3TVN0Qk0wSkRVMjVhT0d0SU5HeDRObTVNT0dwMFJHSjBhSFZZTlZCUmJEWXZSMnQ1YzBGRldHdG9hekZvVGxkWk1tUlpRV3NyYmxKamExWXdMMkU1TWpGRFpYWnZOMHN6TUhKTFowRjZWbFpHYVhSbGN6VnhlVzAxVTI4elYyRlRUVWhtVDFSV1dTdFVjekZWY0dacGQzbG5WREUxTHpoMU9WQnBkRlpVZFZwTFpqRkhiV1JTY1RkRGNtbExhbG8yT1ZkYU9VMUJNVTEwVG10aWJVMVhTMjVXTUVadWEyRjBVRE4zUVVSTmVUSlZOek51Y1U5WVRYQkhkbFpoZW1Sa1VsRjFabkpUVm14SFUxcExVVVJsU2swelltOXhWalJ0T1VOU1EyWndjRkE1Y25oNlpFbzJMMVJ6YkZwRlR6UkZkazh5UXpGWFZEVmxiREJVZEhGME9EZENjRFoyUW1GTlNGTktkME5wWVRoTGNWZE1NbEpQVEd4TUwyUlpOVTl0WmpWRGFUSnBiamhuVm1kR01ERkpWbkZ6Wm14emNWSnZLMjgwVkhKc1VVdGtWVWQ0UWxKREszZzVSbE4zUWtGNVExUlhTek5OYW1zemRXczVUakZZUWpoclJUWnpWRWhXU25CSlJWSXZZaXQwZEZCM1oxZEJRa1l4WjFacWJIRjRNVzF5T0hoaGFscFhja1JRTUdoWFFVSkZOVGxYZGs5V1ozZ3pPVTB5UWtsMFdWQlVjVWRUUzNaMk1tazVaaTkwWW05Q04wSnFjRzlTVlRkamRqY3pVMlJOYVU1WFJrRlZRM1V4YURsUlJubG5RV1IxWVhBelpVbDNkbEZhV0cxcVoxSkhTbU5pVURGTGQwOTJXbEJ3UmxGTFJXZERibmhGY2tKcWNTczFWbE5hVUVKRllrTlNiaTgzUnpZMU5ubENWMEVyTWt0c1YwMW1WakpQTlRGdWJIaGtZbTVTY2s0ck1sSkxlRVZCTkVoVWRGZE5iR1J1WlhadmVtNXdSM05KYWxad01XdFlXREJSYmxaM1Fua3hielp2ZGs1WGRHSjJOMVZDZFZOTFdFWXhkbUpDTVU1eVowUmpjWGh6VWlzMWEyMXNjaTlzWW1KT1dIaFBjRFpKTXprMU4xcGtZME5SYjBGbFYzRlJReXRNT1U1WmRHTkdXbGR5ZWtkTU1USXpiVlJMZDBGblZqbHVUM1pqVEhOc1pFaGpja2xGYWxnd2MyeFdkMEozY2k4MVRWQm1PR1JrV1RWblJXRjFVVGcwWnpZdk1HeDVRbEZFYTNGcllYVktiR1pyY1c5U1RXUkNkMnRDYW10QlkzWllOemN5V1RZM05YWTFibGRCTVVWaGRHUkJNbEF4WWpkelRVVnZUV0pCVEcxTFJqVjFUV3QzVEd0aGNtaGpjbUo1VjNWdGMydzVhV05NTWxaV0sxQldhbUZDYTBOMU9IWXhNbmhQSzFwMlUwcFhjakpVY0hwaUsxQlhiSFpzWW1aMVVVdDNRMjlNTVdSbVlXeFFNRGNyTW04MFpWSnhhMFo0UmtOV1lWVllUekEyTld0Nk9VdHRNRVJ4Um5GSVQyZzFURGxJTm1wcU5XUm5SbEpUY2t3eFkzQjFOVGN4YVhacE4zSkJUMGhYUVVkdlYwbDFlVFZNUTJSM2NrTTJiamt3ZVZFMk5FdDVPVmRpVVdKYVZEWk1OR05rTkZWQ1VrczBRVlpMY0VadFdUbHFOVFV5WWxoWVZ6Wm5lRGhSY1Rnd1Z5OHJVazUyYUhWNlZEVnJTRVZNYTBOblJtcEtNaTlYY0cxSmQxTlJOalpoVGs1NlpqbDJXR3RQWVhWaWEyaFZNVzg1WWpGVGFFRkJjWFJZUlZkNlZUQnRPRkZSY2tWWlRqbDFPVGxTY25oT2JVWXllV1J5VjJwa1FXdEJNeTlLTTJoeWNEWlhNVXhKVm1KTlIyazNkMjUyWkc5dFZWY3dZbVpqTkVGblRIVjZWamM1WkRGSVlWRkZXVXhXY2t0R2RYcEZUR1J1YzFkNVVHZG5RVEZDVDNOMGVUbHNZMUZhZVdSbU0yaDFVblkzU1d4clFWRk1ReXMxVUdoMVkydFhkMGR2Y2xZM1ZsWnljemt3Y1VwR2QwRmpSaXQzTTNWVWJGUjBLM0JLVm1SR1J5dHFjRGMzZHpWd2J6WXpRM05yVmtGS2VYSnNlWE5ZZGtZemVVNXpSbkV3UkVNdkwyVmhXRms0ZHl0cGVGZzVWRlZaUVhkT2JUWlRZVGRKVmxsdFIybGFha1psYm5KV2ExVlhlVE4yTms0NVFXTkJPU3R5YlhCS2VFNXpRbTh4ZVU5eE9UZGFUek5EYm1aUVdYQkZja0ZOYUZKTU55OTFhVEIxVVhFeFVVNXpkVTFDZDIxeE0wTnphMVpuU1cxcFZUSkhWMkZ6YjBaTlkwaGhNa0puV25vcldFcDJOQ3RaVUdSdmJGZEZLMDlvVm5kQ2JVUTJSR1JNYzNWeWRuaEhOVE0wV2tveFkyUkhlVXcyTWtSS1RpOHZjVTFCUVVSRmVubHBkREZ5Y1VsUGVIVlVLMjlTWTBaWE5rbDVRV1pWU1RZMlYzTm5hMlpCUlhkVWNsWXpNV0ptY25wV2JXRjJRbWh3YlRsQlpWUk5lak5GVTBzMFFVbEZaR1JKTVdacllXeDNSSEpHZVVvM1NtZGFhVEkwTjFsblZVRk5ZbFYxTkhKamJYazVWMVZwVW5OdUx6QkhTVWxxTlVSTmVqSTBRVWREZVhsS1IzSm5XVWt4Wm1SeWRUWlpTMmt3WkV3eU5VY3JiRXAzUTI5TWVGazNUSE53VGpSRWFuQTRiVEJSZGxVclEwSlJRbm8xUjNKdVNqbGtOR2h3VG5WSVVXbFNRelZCVTB4RmVUVmhaMk50UTNSYWNXcFRRbGw1WkhaSVlUVk1kakoxVmt4MU16QlNUVzlKUm1ObldEQnllVmhyYVd4NVRsZHhkM2swWjFCUVJWcEtSbk5PUVdob2NsRkhVMEpYTlVkcVYxcFJOalVyZGl0TFR5dENhakJUYWtGU1RFaFNTWEJHYjBWV1dFd3JOM0J5TmpWdE9FbFdhMGh1U1VaaWRuSnlhV3B1TjFBMlIyNTZVMGxOWjFaclJYVjFWbTF2YkhWVFNsZzBOVFl4SzJsS1RtSjNkazFxZGxjeVowZDVOVkpoZFdkdk1YcDBMM0JaYUhWVGNtMUlhRFZyWmpsYVIySTVkblkyTHpZcmVraGFTbWcyYVZkUlRGVk5jMjh2V0hsQ2NqWldaWGRKUm5KcllXNTZVak5tY205dFlXeEdWR2RHZDBKTldFdHNTSFpKVUdOc1Z6UnJSSGc1TkRGRFdXOXNUR2t4UkhGdlRVZGFTa1pSWkVwalZrbEhNV1F2WW1SaVpHZGhXRlpWVXpWd1lVTkdXSGwwZG5CSWNuQjNWWGRSVEVsR1dFTjNSbTF3UWtoclMzVkRhM0pYTUhkV1NuUlVXWEZLVlhsMGFFOXZSbGh6ZEhoclpUQnVXVFpFZEVWaGVUTkxPRkp5WWxaU1MyeHNiMHN4VVhJM09VeFphVlkxVGs5d2NXeG5ZVmgxVVV0NVJFaGxUbFpIS3pseGFERlhVVkIxWTNKU04ydzVhMVJPZFdwNU1IbEJNV2RNTmpGTE0zaFVhMHR6SzJjNVZXOU5jVVp0ZUZsRVlYTlZNVXg1YkRSRmJFMVRjbWRKUkdOdFdITkJLMUp4YWpWalkzcDVNRnBVTWtKeE1HeFdhMDFHTW5OcVNIVm5XRzU0V0RoYVVGTmpibGhwWlVGblZ6QkdaWFZxU0hWQldVcEhja1JSWTNaRFpuVlZSR05uVm1kT2JIbFdWbkYzYzJnMk5IQkZlWGROUms5emFraGxRVmhLVjFkeEszZElMMkpsVUVWck9Vc3dRVzlQU25OeFZqaFJObTlWWlZSeFowWjRSa2hNYzNKQk5FSmpRV0ZvVm04d1lrd3hZek5QYlhCRE1FWlNlRTFGYVhScVNHRm9WRUU1ZDJGaVEzQllXRGcyY0dOeVIxTmFha0pDY205NE1UUlBaRFJ0YkV0bVUzTnNWalZXZFVOdVVWbHNkMUZMTlVsc1pFRlNSbmhzVDB4aFNtc3dSbXcxUTNCeGRpOHZaWG8xWmpsYWNqRkRhMGRpVVZoaGVVMWpNa0U1ZW5Ob1ZsbGliR0VyWXpCVWN6RmFMeXN5T1hZeU5GWm5RV0pWZGpOQlJIaHNkbk0xTm1aS05UVmxVa0oyZEROSVEySnRTMDlzV1VFMVFYSnZSMGgxYm5RdlNqVTFaRkpDWm5SdVVISnNkVU5QT1hSSVUwRlFOMnR5ZEZkQ1puSk1WbkphU205YVVubHNaV3RpYUdSWGJsZG5TRXBZUm5kQ00xbFhURmhJTWxsMFpuSjVNalZ1YTJGeFdHUm9SRWxyWWxoSlJtdExhemw2TWpKdWVXbDFWR3h0UkZrNVptTkxRa1ZEZFdkRE5uaGxTM0F5Y25SVWNERkliV3gzYUVsTlIyWmhlblU1VDBaUWVWSXdUVkZRWTJseU9XUk9XRlJ6YTJkNmF6WnNNR0p1WWpWeFFVbHBWa2RCUm5VeGFHeDVNV1Z1UVM5MFV6VktWV2t3UVVoSlJtUkNTM0Z5UkZjd2VrRlVVblJHV0ZweGR6Uk5ZVkZVYTFOc2QwRnhPRGxQY2xNMVNGSkxOSEZIT1N0bU1uTTVZamRsWjBoTFVWaEZVMng0UVZCRlZ6aDBXQ3MyUkc4Mk4wNVZhWFUzWnl0TGRpOHlSMGxuU25sU1lUUm5hRzVoYzBNelZreHljMkpOV0c1V0x6TnBjamRNWTBVemRqWkhVV2RHZDFKTVNXbGFWemRtTXpOQ2J6QmhOMVpHTUVjMlMwWlZRM041UWxoRlUzRlJhek5hY1RsSmJHWnJObk1yU2tKTE5VRm5SbmxTU3pScFRsVXlkRkIxVkZaSmNuTlpLMkozVjFGTE5FbEdVWFJXU25KellrMVliVlpNWWs1SU4zSjJRemhzWlVsQldXdFhkVWxDTlhWNlUwcFdLM1J6VTJOd1ZYQnpaVE0yU0dKalJVRllTVVphU1hsTU1ucFhSVmhCTWxGeE5pOUlZekJ4ZFZSblUyUTVRVTU1VW1KRFFUZHVTbFpUWWtKaGVVWllSWE5WV0U5UGNESmpkRlpKVlZGTGVrbEdXRUpFY21sNlNrVktlRlJMYVZjd2F6aGtWemhpZFVwQlRHdFRVM2hDTTA0NFVuTmFVMGhVVEVobVFuaHpjbFkyYmtkbWJVTnJhbFkxUkdzM09IVldaVWxNV25GNk1TOTBLMDEwZUhneGVHWkVWMlprUlhCdFYxZFRURmhGUm5sS01XUkJTamR0Y1dWd1pVbFlRVEJYVEdOVlFUVkpjR05CVkdac05uUTVMMjByUlZKdVIzbzFjVTFaVUVvd0x6UktkMVpQU1ZGRE5VbHNhRUZzZEcxeWRDOUZWaXRrVEZseWRWVndlVTVXUVRCV1NVVlJTM3BKUmxoQ1JISnlURVZWTjFsV1FqVmlVR1l6YjFNeU56RnZjVk5KUVdOclYzVm5UR1I0ZEVWMWRYTnpZbEUzYW1aNGVWSllVbFZvUWtGeVoyZFhla1J4Um5wUlNsWnBTbXhVY3pKTWEzRnJhVkl5VGpOS1ZXY3lTV3hrZDBWamRWZERkbkJDWTFOWFFuaElXazU0TTNkRE5VVnJUMjlIVlU5RmFXeDVUalpoVkdSaVlVVjNVVUZKYWxkQ1FrUXhValZIU2pGbWF6WnNhRzVTWlRsbU1tZEhlR3R4VFhkTE5HSk5WSEJGY2xKeWVEVkZWRlJvUVZoSmJGSXdTRGhIVDNOR01teGtibTUxZEVGeVVVOUthVEpCUWs4eVNraFRlV0ZpVkVwTE1IcHVZV2xzYjBkclVtRTJRV2xPYWxKbGEyNXNVM1JKYVZkUlF6VkJkWEpGYTBwWllVcHNaVFkzV0RKdVlXaFdTVmRCVVV3clJuUmpZVkZXZVZwU1FWRk1RbXBZTlVGdlFYVmFTM05UUWxwQmNtZEVTVlpaUmpWU1RFeExNV1JHWVVKTlUwdFpRVVZuVm5oSlZuZFpUSGhUTmpSQmFrMTBXalZMY0Zsb01uTk9hME4wZVVKaFEyZFlSV3hWUWtGMVdVdHNZbWxFU2tNek1ITjFWamRwU2xsclMxUkpSbEZDZVVwVlIxWkhRVUpoUVhWVFMxaEJSV2RXZDBGclMwbEpSbTlJTTNWSmJHTkJlVXBZT0VKamFHWTFRVzlCZFZOS1dFRkJja2xzWlZGRk9VcE5SVzAwTWxWQmRWUnhWVkZOV1VKcFFsbE9hSFY0UVd0aVNqRmxObFJPWjNoQmMwZDNNbGxuVjFGeE9FTlVUbEY0UVhOdGR6SlZaMWRSY1RoRFZFNUJlRUZ6UjNjeVdXZFZOVTlIY1hOMGNHTnlVVkV5UTFwaVRWSkxPR2xJU2pobWNtMU9kVU5yWm5WWVJFVkRlV0pFV2xOS1pqaFpjeXRRYkRaeFptb3Jkbkl6ZDJoSlUyNU5NVWR5VDFGaldTOWhXVmxIVlZCM2JGRnRRMnRvTkU1clZVczRhekkyWTJKVWVWYzJVR3h4YzNOQmJFTlphMUJLZEU1dWMwdDBaa3hPUkZSeVN6RkVOMnRoUzBaalUyMUpVbTV6T0d4TWVVcHFTSFYxVW1samNsZDRNSGxWZVd0RGVXSlVWRGREVUdsdFNucEtVR3R4YjJoak0ySkNlRU5WTWxOemRHeEpSbUZ5YlhGM2NUVk5ablJyUkV4c1MxWnVhVVpQYzBkNU1sbG5WblYwVTJod0wzWXpkR2xETlRKc05UQm9WRGRLYzNSdFNVWlRja3RXWlZNclYzbDNhMU54TnpaVVkwZERXVTVzYzNCQmJ6ZE1kbEoyYW1wT2VWSmhOU3MzUkdwb1RGcElXbUpOVVV0WVprNVRhRzVQTlU5c1RsZHZWRWMzZVZwWGQyeHplSE5PYlV0R1ltcHRjR053TUd4V01GZEdVbTFKUkhkaVMxSkxibFJNVXpseFFWaEdNM1pQUTBWTloyMVZhbFpuUXpVeWFEZEJWQzlpVmxGaE5FRnJiVlZxVm5OQmJESmFhMmxXTWk4eVMyTnNRVUZ5U0ZvMVJIWkpWamQyYVc5SlZtTjJaREp1UWtGalNua3lZVlJrZVVJek0wbzNkMU5UYkZoWUwyUndhMFZwVjBWeFdFNUtiR1JCY21scGVsRjJkbkEzTHpOeldISkVUVmxKUjJ0aFdrMW1TVVF2Vld4d2VsWk9hRzlvVmpGSVZHUlRZemRUVVV0R1FrZHZhbFoxYVZWSU1EUm1OQ3R0TDBoVFpGZ3ZMM00zZEhoeGNsVTNRa2xNV2t0dmVsTlpaalJNWTJOclpVYzBlVVpZZW1SaE5rVkpRV2xYYWxab2FGZHdOalJtVTNsV05VTnlZMmc0WjFaUlRFSnplRUZ2T1dNNFdFNXVSVk4xZVVKV1FYTnRlV3REYVRGNmVHRXlPRkUyTjBsR1ZVTjNZazFSUzJKbVVFWnFhSGw2VlRORVNVWmlhME5EU21GT1YwdEhjMWxQTVdGTFNuaGphMU4xUVZwT2JFbEdZMWxKVm5SYVltVXJVMHRZUVVWRmVUQmhjekJETldZelMzSmFWREptWVhsT1YyaExZekl6UkZOWVJWRk1Rbk40UVhCNWVFMHJabTFwVGxnMVEzQXhjMUpJWVVsR2F6SjFVVWxhT0RoTFZHWXdLM1Y1VGxWbVExSlZVM0ZWTUdWblJuZFJLM296Wmt0TlNFcElSbk5xTldGd04xbGFSVXRSVEVOSlJsaENWSEZ3TjNOd01uSmxUMU53VjFjeldqUmxaV0Z4WTNGSFUwWnJRM2xUUWxaM1Z6WjVaVGR5VGt0NmFtc3JZVEJYZFdOblYwUXhRVU5EVW1GNVFVaFlSaXRSZEhCMWVEQXZSWEpWZUhsQ1dVSm5SVk4xU1RkNVVHcHlaRWxaYWpGNFpGbEpVbU5UVlc5QmVWTktWbmRHYlhobGRrbzNPSEZQV2tzd1FYbDBhRVZ5UlVOelZFaHlVR3hUYUdkQ1FrbDBXVUZZWmtZMmNDc3ZURk00VjJ4NWMwRktTWFJWUVdVdmFWZE5kVkZMZDBGRmFURm5RbEZpUjNOT1kyZFdRVWxLUm5KSlJFRlhUbGxUUTJWVFMxbEJSVk5wYnpGblFWaEphSFZSU2tGeWMyZFdRVWhKUm1kR2VWSkxkMFJyVTJ0SlFrcENUMkptUVdwSmFDdFJTMEZNYTJsV2QwUkpSbEZDZVZKaE5FRnJRM1pLUWtOQ1YwNXFhMUpyUWxCS1JsTkRVakpOWjFaQlNFbEdaMFo1VWt0M1JHdERaME40U1d4blFYbEtWV3RyY1dWVWRGRnFTVVppYTBOVlJXbHplVVpYWkdwMFZsTkpSbVpyUTJkRE5WRnVRVzVoYTBWUlN6UkpSbWRHZW5CT1J6QkxZMnRYZFVGS1FYSkRZMVJOUmtsNFVHMDVlVWxQYmtkMFNtTnBWa1JvUlUxSlJtTkZRM2RwVHpONFVFaEpRemxsYTB0MlQwTlZVemRSUVV0NGVWRklTVWhOWlc1bWJIUjFaazVxSzBWbmFWcEJjbTE1YTFOelVYSkhiVU5TWVRaVFRqWTJVVUkzYlhsNVVsQkpSVTF6UldreGVWcHNVVXRKUm1KclEwWjFURFZ3THl0bVdVb3pUR3BsVUd0VGEwdEZXa2QzYWxZclozTldlV05HY1RoMU5ETm9OMkl6V2twSWFGaFBVV1pGUjNWRFFsbFJTMVpqTTBKaGRta3lRelJzVmpaalluVk5jWGhUY2pSblZtcGllR3BTVGxOak1uSnpWbEl2WmpWTGNrcHlTbEZyUkVoS2JFMTViMUpIWkUxYU5VdG1jVWRFT0ZNeE1XMVVlSGhUV210cVNrRnlaMmRYUlVNd2VWcHlSVWQ1ZEZkVVpUaFlWbUl2UmtweVEwSlhUbXByUVU0eVpVcERUbGxuZFdGeFZXOURVbE5yUTNOaWRWVktWblZVYjNCWFZrNVhSVmRuYkZZd1VVdHRRMlJZV0ZkWVVrTk5WbkIxWW05d1YwNXVSeTlNUmxsUWNHc3dla1Z2UW5oUGNISmlTa1Z5YVU4eU1VMVlWRGR0VjJ4NWRHVnVTSGxDVWtGeWJscG1iMHBGY210RGRYcFhRbXR0YkZBMlkzWlBiMnRWWjBNMVYyNHdhR2h0UTBKWVR6Tk1SbE5tZVVWaWEzRnJSRkZyVDNGRGRXUkNRa28wVEhoalZGcFZjMk5yVjIxblVHRjVVVk5wUW5VelNURlRZa05QZUROSE1uQkRSMHBCWmpGR1p6Rm5RemM0WmpSeVVucFRVV0pNU1ZaaFIwVktZMjFEVjA5VldUY3JVVXMxUjNBdlNFWllOV3BTVTNWVk0yMWhkakpOUW1zd1lsRldVM2t3WjNwb1NHSnlibkZNUm0xc05WZHhNVkZoWVVwc1FWTk5RMVpMYUZCalVUTmhjMjVXTUhwcWNUbHFkbWh6V25aNE4xb3ZTMmxpYW5sRVFqSkpWbEZsUWtsR1ptOUhUMDlhV1RadVRHSTNVMVZ4ZVc5S1pEbG1RV3cwZWxKVFUwc3dhak5xUjNaV2JYSjVTRWRZT0dSRloyTnNWVGh6WkRZNGMzQkRWVlZXUldWMFFraENVWFI2UlhkMEwwZFRjR0pJWkcwM1NEZEpNR05OUlhGMWMyb3dkblIyZGpod1ExTlJhR1p4UTFwU1Uwd3pNSEpxVEU1UE5teDFiRzV5WVhKTFZtTmlhbEJRYkV0eFpGRkZiM0ZFVGtsSllUZHFOakpVZEhwV1ZIa3hWMGRoWTBwTk1HbGtPVzlNYzI5aFJHUkdSVkJtU0Zrdll6SjVhRXg2Y0ZnM09IUjNaa1pXTkRSb2R6WjJaekJuWlVsc1ZGbHJWalJuWldveGNVNVlSakphYkdOeU1pdHhiMmhxYVRWcGIwSXlNVXBqTms1VWVUSlNTamhpVEhsR1Z6Rk9MMmhYZWpoR1VWSXpWWEF3U21KaFJrTkNXSGxSTDJkc1MzaHNObmw0UkVoelVrdDFPRzlJVVVveU5FeHRUak4xTVdRMWVqZHBlWEUyVTB0cGFFdG5ZbUpWZG1ORGRVZFRjM1pXYWxsTk5DdFpTRWR0TjA1VGEybHpjVU0wUkRJeFl6VkJhR2hvTlVkNlpIQlpjbEpxYjI1cU5qQlFkWFJYVkhCS1JuTlNTMWMwZERkcFNWQTNNeTlGZEVsV1pscDJhVEV5VjNGaEszUnhPVkZTYjFwRmNEWllVRVo0WVhScVl6ZHFVSFp4TkZOck1GbGtWWE5YUjJNNVFtTnJXREpaY1N0V1UxTXlUVGhoZW1vNUwxSnFVRzFJYmxacWJGSldRbTlqUldseGVVWllaM1JVUjNob1ZFaGpXbklyYkdsek1VdHBjVXB5VFVwR1NtdExLelZoYkN0RVEzVlBOREJTYkhaTU1XRTJkbU5WZEdOclFVRldaRXRKVEc1Q2RsUnJNazh3ZGs4emRDdHRNa3BEZDNFMlNXczFNbWRtWkdwWGJuZEhkR3RJVjJ4a2Iyb3hjbk5SVUd4RVNYaFpSeXRCV0RSbGVUSktlWEZHZURsUVUyWm9RVEZrYUVsR2FFRXplbTgxVVhFMmFVWjJjR2sxU2tRd1JrZDNRMFJMUW5OTVIyRlJjVGt5VG1KbWFVSlhSVVl2UVdWTWRtRkRlV1ZzY1hWVWFsWTFjR2RCWjFaTFRrbEZRM2RETlZOMk1IQnVTMjlFVWsxb1NVWnZiM3BOVVZwQmNtbFRTMVJsTW5aa1dYZEVPR0ZaVUVGbGVERnNOVzR5VWpZMFoxVlNaMkpDUVhSQlRsYzRhRlk0VVV0a1drdEdNV2xNVEdkUVoyRktiR05UZUZoeE4yRjRibXAzVG1wUmNuZEVTV3hWVWtKeVRFRjRMM0pSVTNkUlNWRTNlWGR6YTBaNGFHRkxURkZSWjFGaFVVWkdaa2xXWmpGNFJYRmlPVEEwVlZkdlpFVlpPR0ZaTUVKTWEybHRRblJoYlhOR01sWnFVV28wY1VReGNHczVSbkpSU1hWVFNsaERZMVpMZFRnNFlrUjVSRlpOUVRaSlZsZEtVRWxXWlRWRE5tNHlaSFJWU3l0UlEzcG9SVWh5VGxCRlZXZDJWbm95T1hoSmNFdFNjVkp4VEZGSGMyRkNjMkZFTHpWWWFIbE9Wa3QxVm5NM2NIa3daWFZDWkhsTlVrdEZiRWxNV1dnMWVtWXlSVE5MTVVvdlJ6bGhVbU5DVG1sa1VtRkJWVmxHTVVGRWFIWm5TbmRtY1RsUVdHRkpiRmxCUTFGTVJIZDBRVFZ5UlROTVZuSnFUV3A0Vlc5QlFWRkVLMWN3TVZGSmVIQTFVMlpTU2xaUE56aHNXRTVoUmxOemNFSm5SRzE1Y0ZWTU9GZGFlVEZsY0VWT2NtWkNWamRGYVZaUlEwRnVNbkpFTVRsd1JYSnphRloxZVhWTU16aFRTMVpCUlVGMmRGTlhjVnBNVm5sclptTkhkbmd5WkdaSE0yTjVXbFpCU1VGMlpGZFhObHBNVm5wclZXeDVkR0pMTkRVd0x6ZEpSbFZCWjBNNE1VcGljRzFGZVhSNVpHWXdZMVl2SzFkV1FVaEJSRUpzTmxWNlQyazVjbFpVYzJwTVZuSk1XV1V3YkcxM1NXODNVblI0V1VKWlN6VlZabUl3Wnk4NWRDOVBNVzB2VkRSblpVSjRiREpaYkVoSU9TdGhPRkJqWTBkQlNGVkdTMjV5WkhjekwxaG9kRE51T0U5WWRrOTJiRWhoTjI1TFlrMUtXakp2WkhOQlZVTk9LMjVrYVVwdGJqRXJSUzg1Ym1OdFpIZFRaRFJsYzJGTFlVRkdRVlEzYlRaMllqaGtjekE1ZFVOVWNrSnpTRkJ3VFhCT1NISnNRM0JoUjJ0R0wxUndUbkpyTlV0c2JHMXlRWFpWZDNFeFFqaGxWWE5xT0d3MU5URldkV2d4V2s5alJYUmhjbUZIYTA0dlZuQlNjMHR5U1cweGJYSTFUMDE0TWpSc0t5dGlNbFo2T1dNNFQxazRjV0l3VWpKVFNFTjFNbTF6WmxjNFZYaHRaa2RHU0V4R1lWZzNRakpUUm1JeFYxTjBlVlpXZVhWV2RscGtNbVp4TjBwTVpYTjRNbWhITkZnMmVEWjBOaTlHWlV0QldVNHlVa3hNVGxkQ1kyUm1iR2hRWlV4VU9GSnBOamxzTjJGT1QxTlRNM3BpU1hocE1UTlFiVXR6VFhoV01URkVjMDl2Y3pSWk1XTjBTa3hJZWxaeFVuRXZLMk5VYVhkeVkzQlhPWGRJV2l0bE4wUTJZWFl2WlVwMU1IQldMM0Y2Vm5jM1RtVkJTRGM1Y21OcE0yMU1jMGRIWm1zMmNFSmpOMUkxYjBwNmRUQXJOMGxOYkdNMlNsZFFWMU0xVWpGRGRVTnpTR1J0Y1hKTGJVMXFiekpVWkc1TVZUWmxWMlZ4TlZwS1JWWmtPR0V2UkVweVJtUkdSM0JIY1hWWVJHdDFZM0JXTVVneGJtSnhka3Q2WlVKcmF6WXJZWE5HWW10aFRHeGtVRGx3VnpWdlJURmhVRkJVU21sM2JtdHBiSGxTY1M5NE5XWnJTMEo2U0hrM1kxQmxjekZaTmtwRFdFcFdZMDB5Y2tjeU1VWnhiVXRNVFVsdWNFa3haRlkwYmpkaGVrSllRbkZwTVZwT01uVlZWMkYxUjNNeFpXUXhjVXRhTDBsdFluQXlkVXRyV21rMVkyNVlOblpPZDFkcVNYVXZRMWh1Y3pGd1l6Vmljak5rWW5SaVN6ZE9XQzg1ZW1SV01pOWFVRms1TmpoSFdsQXhPVmRHZVhSbFpEUmljMVZGZFZseFRIWXJhMWhwY1daMVlYQnBNVWxzWkZoYWNUaDVaRnBwVUUxeU9WQjJUbHAxZFc1a1kwVTVLME5LUm1SNGVIcEllRkV2VUZwamNuQmFTelJMTVhKYVRuWklMeXMwVVdKTmFEWldjVGMyWm1ONFppOVFiR2xyTmtOU1lUVnBhbTFZTmtJcldYbzFVR0oxY3pGaWF6WjFUSE14WVRGcUx6ZHhMM0ZZU1RFMVZYQXJOVEk1WVRRMGNHTmFaWEpRTmxoTFZsRmlURTFYYUVkelNURm1lWFI2TDB0WFpVZDZUM1JyVEdJM1lVaG1Semc0V1ZBeFJYcE5hMVoxWTI5clZqRTVLMkkwY0dkdVZtaHRkelozVm5WUmNuUTRRM3BRVlU0eU5qQjFaMjFXYkcxTE9YUlBNMGN3TDJaSGJsQnliRVo0Ykdsa2RVbDRlV2x0ZWxkTWRGZHlOMEp5UWxoQ2RXbzJRWE5uVkVsd1RWSlRZMVpaYXpZMk1XUmphMWQxVkhKWVRISnllbk42WTBrNFRUQXhiWEpqYWxac1ZHVkpVRVU1ZDFZMGFYbFFRMUp4UzFGYWVVNVZNblZrZFZaclRDOWlSVzVZZFcxQ1dIbEtWbVZRV25FcmFHZEtNVkUxUTNOWWRHZ3dWRXBHWW0xeFNrWmxWbEJxYUhVcldXSlpVbmxrVjNneVFraGllVUpZUXpkT1dGZ3pObGhVVGxWeGRFWllXRkZsY0RoWVJHWTJTVE4xWlhrMVozSkRWbHBRZVdSeGVHcENRM2hMYVdoWVR6SmhkbFpvT0VWWWJHNXRaMVZ4WkV3dlp6TXpPRWxvVmpObWFqUnpZa2hqYzJ0R2MydHdWM1J3WlhKNFJVZDZNbUYxYm5ZM1pUWllTVWxGWlV3dlFWQm9WbkZqZEZoMlQwVTBkSEZXU2pWSU9XeDZhRVY1TldaNVluSXhiMWRNYVRSM1IwUlNXWGhUTjFnMlZ6bE1RVWhpUmpaUE1rODFORGx0VmpOUlYyc3ljVXRPVGpKamNubFdWM1ZYWTBKTmRqQmxlWHBHY1ZwMlpIQlZlVWhqYzBGVFFUVXpRa2R5YmpSeVVEZHVTRE0xWkdKNFMxcEhOemhhYlcxaWNtWlFjSE4yVmpWS1pFTlRTbHBhU3pOTU1VbzRaRXN5WTB3NVZHNHJjMUJGWTFoTVUwWmFXVzlXWXpkYWEwcHVlV2hZYm1sdGNsYzBVRTVYYUVkelZXOHdia2RpZUhadE9YTktUMnRRZUhKVFVsZ3ZOM1ZRTmxoR1EzSnphbFpXVFdzMk9XRnBUemhrUjNNNFlYQXZNRFkxY1VsaWNXUndSemd2WXpOV2NteHhlamMzUTFjMU5tcFFla3hLVUhWeE9GWnRjbEZvVnpKdlpHWktXVXcyWWxoTWNtZFhaSGxTWVRkSmJGcDRZVkZpVEUxWGNFZHliekpKVms5bFp6WTVjbk5yVW5FM1NVWmlhMmxXTDFWc2FURm5VbkpMVG1sR1ZETnlNR0ZZVUVwVE9YbFNZVGRKUm1KdWNVbFdiRzF5WTJwV1RtSkhVMEZGUTNVemMyTmpkVk5LV0RWRGNTOWFRa1Z5WjI1Vk9EQmxkR3RyUzNZM1FtWlFSelJ3WW1WR2RsTnRORXhSTmxSaE5FZDVUbGR3YUZWWVVscDNlVk54TlRWNVpHWktPRTF4TTBKU05qVjNZWGxoVEZkS2JUbFRiR3hyWTBZMU5ubE9XR1JqV2k4MVZYcFJNMlp6T1hSUlZHMDJZM015ZFZCTWJFTnlaelowTTBzMWJqaHZlVXg1Y2psQ2NuWk1UV1ZJVTJSMlkyMHlSR2xETlcxc2RUTjZWbkZhZGxOS1YxRTJWemRUYUVkaFVFaE9Wbk5aTDA1WVNrVnlSamM1YlhKamVHVXZXRUZOWlcxUGRWaEhWWEZFUms5bWRWUnZlR1J6WjFaMVdVbEtSbGt4T1hGTlJqRlVkVGQ0WTBkTE9HMWliVFoxT0RkV04yeDRVMWxpZVZOTE0wdHNNWEJOY2tSWk5ESmpjRmgwYmtVM1IxRmhaVmwxV2xWRWVVcFhNMEpoU0U5aE0ybFJTek5NVmxSeE5YVjRhalkxU1d4a1VUUjZWVGg1UmxWNGRXTnlPRmN4VUVkRVRHMUxLemN5ZG1WV2NIVldPU3RPUVZJd1FXTnJWM1ZwUWxjMU1tbENXRTF4VXpaaWFYcHZRa3BCY21OMFZsVnlUV3BXZERWalVqVkhaREZZVlROWVJWTkNXRllyVm5FMVltTnhlVVZzYTNVeFdXWnhlRmhzUzJseVNETjRTVGx4VDJNMlVrbGxVWEV3Um5sT1YwaHRjRGhRYVhaNmN5OUlSU3QxWVc4d1JuRlBWVFpDVDFOeE5rOTJOa2haSzNJMmMxQk9UM2RYUWxoT01sWk9ZV3BxVDJ0aVNHdERkSGxXWmpZME0zTmFjbmh0VUhWS2JHVmFkalZyYnpFNEszTXpMM0JXTlRSQ1kydGhkRTVyY0Zoc1pVaGtMM1ZwWldvNFNrVnliVUo2VWxGVWNVbFlTa2R5U25ObFZrMVpORzU1VGxVdk9UZHRlbUl5VlRsa1ZuWmtNV3hGWjFZclVYRlJaWGd6WVhwbVVGaEZTRTVXY2s0eFJrMW5WblZTYjNKWFZIUXJiakY0UW5aV1lYWmtVbXBMT1dabVRtTmFSR2hPTHk4NU1qVldhVWxzVFhNM0wzSjBTaTkyZEd0UFVHdFpibXhpY21SYWNFbEdaV3hhTkdjMmVGVlBWMWs1THpGdE1TOWxiRU5TV0UxQmEwTklXV05WU1c1S2FraEhWRGRVUlhac1YwMW5hVlkzWmtoRFRHMURLMmQzWkVOSVMxWmxSREp3WVc1R2R6WjRlRTlxWTNSTGMydHRkVzlFWW5KUlNqRkpjbk5aTHpCT05Xd3ZUaXRWY1RKNk4wcHNZMmRXZEVOU2RVUlpOM05NZEVGV2NFTnlSVEl6WXpoVWQzRXpWMjlxZUZSRWFFRlpTMFpGVjFCWE9HUlRWMjVuYjNwUmJGaDZkSGQ1YUVSb2MxQlBhRlZCYzFWQ1ZGTlpWa3BFYURKeVkzZEZRVlZJSzJoamQwVkJWVWgyVmxod01FMUJTVU5oY1NzM2NXSkNNRTVCU1VJMlEzbFpUa0ZCUTNobmF6UklRVVZEVGxaWFpERjJTVFJJUVVWQ09XaFJSVUZCUlVSME1uRndNVVJSU1VSQlVVTm5jSEZ4Y0VKdlQwSkJRVUZCYzFsSlFrRlJRMEZQWjI5RVFYZEJRVTVXVkRsT1JHZE5SVUZCUVRGRk5uZGlkMEZCYVVKVlRVWkJRVUV4UlhOWlRVRlpUVUZGUTNSV1EyUkNjMEZCUVZWRFRtZzRRVUZCYjBSaVEwRkVTMEZCUVVSeGIzSndiMGxDYkVsQlFVUXhWVVF5UlVGUlZVRlJVRzgyY1VKWllWZEJXVlpCUlVRNVZYZE9hR05CUlVGdlVHSkNTVUZOUVZGTk1rUjNWMkYzUVZGRVZVOHZWVTlDYUhkQlFVOXZZeXRuTkRaQmR6aEJVVXQ0WnpoQ2JVRkJRVU14VkZZd1JIZFJTVUZSUlRCRWVYZGpRV2RHYWtKdlJGRm5RVkZFUlVOcFFsbEJRVU14VXk4d1EzZFJTVUZuUm1wQ1VVUldVVUZSUkhGR1YwUkJRV2RFVlV0WVZVdENhVFJCVVVreFUyNHlSSGRIYzBGQlFVZEpSbWM1YUVGQ1owRlJTelJDWjBGUlJGVkpZbFZKUWtGelFVRkhTVVpCT1hwblFtZEJVVXMwUW1kQlVVTkpSbFZEZDBGQlJIRnFSbTlFUVRrdlFVSjNRMjlNTUVOSGQxTTRRVUZGUW5SVlZtTm5RMEZSUTBGSlFsbEJVVkZNUVVWRGMwRkpTVVpCUmtKRU1VRTRTVVJuUlVOQlQzRkhkV2RGU1VaQlFrRjFXSEZvV210RVFVTkNXVUZWUTJaVlEyOUNaMEZSUkZWQ01FRkJRVkZFVlVKVlFXZERVMWxCVlVFdlZVRm5aMjlSVVZWQk5tOUJZVUZDUVhOQlFVTjRRV2RUV2tsQlRVRkdPVmRCWjBKT2QwRkRSRkI1TDA5QmQwRk5RWGxQSzBGQlFWRkJNVTAzY0RocWIyZEhRVVpCVEhCbVRFRlZSVXBCVFdsV2R6ZFZObEZNUVVGUlRqWlhkRFJGTldkVGNGbEJXVUpaUVZGS1Z6QkJTVUZ6VVVwQmMwRkNRV0p3WVdKQldVVk5RVkJMZUdaQmQwbGhRVVZPUVZCSmQwRkpSVTVCU0V0MkwwRnpTV05uUTFGaksxWmpVVXhCVEdSblFrbHNiWFV4VUdsRWQwRlZRblZzVmpoQ1UxVkJVMEZKUVRnclZsSlBRbE5SUlVOUlIwRlFRM0ZJUVhCTFJEVkJRVUZNYXpSQ1UwSlJRVEJETlZoNWNHVkJjRU5HY0VGS1FXbzFWV1JCUVhCR1FVRk5RMFpLZDBSS1FrRkVZVFZWUnpWRlNrSlpTa0paUVRod0wyTkNNRU5UUVZGQldHeFJRV3RJUVVOUk5YZENTVkJLU1ZCQlRHeE9XR2ROWjBWUlIxRjVLMUY2UVVwSlUwRk5hR2hCUTFGdVExRnZRWEZSU1dkWFZXeFpRVTlSY0dWUmNFRTJPRkZzWlZGSFVXeDNRa2xhU2tsYVowdGFOVk5ETkVOUlRFbEJlVVIwZVVSM1EwcFVjVWxFTlVKeE5VSnZSRVZLTDBWQ1lVcFdZalZDWTBGS1FYVkJXRU5MWmtGS1FWVktWVlpCTDNCQkwwRkZRMU5DVGtGeFdEaG5Xa0ZEVWsxVFVrOVJTU3RSU0VGQ1MyOUtRWEpKUTJaSlFrRktRWE5CU3pOcFdIYzBRVWxOUmt0emIwTlpSaXM0UVZWRkszbEtSamRCVWxKUlFWTk5TVk5OVTBOWGVGUk5RVk5XYkdsQ2MxTjFNa0ZYUVRoWmJHRnpiMXBaUm1GalFVbElSa3cwU1VOTVNIZERVVEJEVmpCUlVIbEtVVkZEVVRWRFZqVnBSRmg0UW1kQlUzWTRVVkJqVTFjeVFVVkJlRlZDUTBGWlprVnFhR2RDUVd0V1FXOUpSVGRGUTJkQmIwaG5iMGw0U1ZOWlFVRkJSbEpYUjBKelZ5OXpRWGRCVlVkblZVaDRjbXA0UkZGQlMydEhTVVZaT1dzMFFtZERSbE5oUjBNNFYzRnpRV2RCVlV4WldFMVhSRkZYUVZGRFMyMHlTbTV1VG0xTlRWRkNVVUZITUV0dmRrWnFSRUZGUVVaRmNrWXdNMmQzU21kQlFXbHhiM1J2UkVKeVN6QkpSa0ZEUW1GT2FIVm9RV2RCVVV4YWRrNVNjVzlCUVVWVVRGcHBUbFZCUVVOVFdtSk5Va3RuUVVFd1lreGFRMEpWUVVGRlZFeGFhVTVWUVVGRGVWcGlUVkpMWjBGQmVVcGhUbFJCUlVGUlRGcHpXa0Z2UVVGTVNteEpNVTFCUVVsQ2J6SmpaMVZCUVVKcmVUQmhiVUZCUVdkWGVsbDVRbEZCUVZOQ1pVcEJaMEZCZUUxMFIyOW5RVUZKUmpBeVFXZFZRVUZKWjFoblVVbEJRVU5UVFU1QlJVRkJRMU5TVGxNd1JrbERVQzlFZDAxamFsUlJaa3N4VTBsQlFVRkJRVVZzUmxSclUzVlJiVU5EQ2p3dlpHRjBZVDQ4ZDJsa2RHZytNVFEwTGpBOEwzZHBaSFJvUGp4b1pXbG5hSFErTVRRMExqQThMMmhsYVdkb2RENDhMMUJwWTNSeWRXVkpibVp2UGp3dlUyVmhiRWx1Wm04K1BGTnBaMjVKYm1adlBqeGpaWEowUGp3dlkyVnlkRDQ4YzJsbmJtRjBkWEpsUVd4bmIzSnBkR2h0UGp3dmMybG5ibUYwZFhKbFFXeG5iM0pwZEdodFBqeHphV2R1UkdGMFlUNHlNREkxTFRBM0xUQTBJREU0T2pJMk9qSTBQQzl6YVdkdVJHRjBZVDQ4YzJsbmJrUmhkR1UrTWpBeU5TMHdOeTB3TkNBeE9Eb3lOam95TkR3dmMybG5ia1JoZEdVK1BDOVRhV2R1U1c1bWJ6NDhMMU5sWVd3Kwo8L3NlYWw+PHNpZ25EYXRlPjIwMjUtMDctMDQgMTg6MjY6MjQ8L3NpZ25EYXRlPjxoYXNoPm51bGw8L2hhc2g+PGNlcnRTTj48L2NlcnRTTj48c2lnbmF0dXJlQWxnb3JpdGhtPlNHRF9TSEEyNTZfUlNBPC9zaWduYXR1cmVBbGdvcml0aG0+PHNpZ25SZXN1bHQ+bnVsbDwvc2lnblJlc3VsdD48L1NpZ25hdHVyZT4=\n\n	2025-07-04T18:26:24+0800\n	https://www.tsign.cn?serviceId=5de19d3d2ef845d393fa09cab14f7d3f&fileId=3c83cadb796b43f8b08793067154cb3e\n\n\n		PFNpZ25hdHVyZT48aXNDbG91ZD4xPC9pc0Nsb3VkPjxzaWQ+aHR0cHM6Ly93d3cudHNpZ24uY24/c2VydmljZUlkPTVkZTE5ZDNkMmVmODQ1ZDM5M2ZhMDljYWIxNGY3ZDNmJmZpbGVJZD0zYzgzY2FkYjc5NmI0M2Y4YjA4NzkzMDY3MTU0Y2IzZTwvc2lkPjxzZWFsPlBGTmxZV3crUEVobFlXUmxjajQ4U1VRK1JWTThMMGxFUGp4MlpYSnphVzl1UGpNd01ERThMM1psY25OcGIyNCtQRlpwWkQ1VWFXMWxkbUZzWlR3dlZtbGtQanhzWlc1bmRHZytNRHd2YkdWdVozUm9Qand2U0dWaFpHVnlQanhUWldGc1NXNW1iejQ4WlhOSlJENW9kSFJ3Y3pvdkwzZDNkeTUwYzJsbmJpNWpiajl6WlhKMmFXTmxTV1E5TldSbE1UbGtNMlF5WldZNE5EVmtNemt6Wm1Fd09XTmhZakUwWmpka00yWW1abWxzWlVsa1BUTmpPRE5qWVdSaU56azJZalF6WmpoaU1EZzNPVE13TmpjeE5UUmpZak5sUEM5bGMwbEVQanhRY205d1pYSjBlVWx1Wm04K1BIUjVjR1UrT1R3dmRIbHdaVDQ4Ym1GdFpUNDhMMjVoYldVK1BHTmxjblErUEM5alpYSjBQanhqY21WaGRHVkVZWFJsUGpJd01qVXRNRGN0TURRZ01UZzZNalk2TWpZOEwyTnlaV0YwWlVSaGRHVStQSFpoYkdsa1UzUmhjblErUEM5MllXeHBaRk4wWVhKMFBqeDJZV3hwWkVWdVpENDhMM1poYkdsa1JXNWtQand2VUhKdmNHVnlkSGxKYm1adlBqeFFhV04wY25WbFNXNW1iejQ4ZEhsd1pUNVFUa2M4TDNSNWNHVStQR1JoZEdFK2FWWkNUMUozTUV0SFoyOUJRVUZCVGxOVmFFVlZaMEZCUVVsblFVRkJRMGxEUVZsQlFVRkJPSFZ4VGxOQlFVRkZSRlZzUlZGV1VqUXlkVE5rTUZrM1lrMUJlRVV3Wm5vdlZEWjFkakpWVnpsd2MzZGFhWEJNZGtGQmMxVmhaVzgwT1d4c1RHOW5hakE0ZVVkRlJVVkpTVWxaVVZGUloyZG9hRUpDUTBORFIwVkZSVWxKU1ZsUlVWRjBVVm8yY0M5MlVrWTFMMk4wZUUxSmMyVmpMMWMzUTBnMFFVRkNRMEZCVDFKSFNUUTJTbTVxS3k5SGIyZEphUzlEZVVGQlFWRm5RVVJyWkdsSGNYVkZZbXQzTUZwemVtVTJOWFY2U1RWeVFqVkRURFIzVFVWSlFVRkNhVWhpYjNsak5EZElSWFIyWjBGQlJVbEJRVUpUUlhObk1sbHhjR0Z1Tm1oUlp6UlJaMEZCUlVsQlFuQlZNRzA1ZFd0cVN6VlhiRzFKTmpWcFptdFhjQzlsU0U1QmQyaEJRVXhKTjBWSVl5OVRTRkpaYlZWWVZUUllWV0ZvYzNkT1VUZDBRVUZCYUVGQlVFcEVlRFpLYXpWM2RWWnlXVXhHTVhkVlowRkJSVWxSVGpSRlNraHhlRTExT1hnelFub3pMMGR5TWpnMFRIWlFlRTVKT1dveU4wRXhSVEZQVVVWRlNVaHpRbFZrTVRneFVWcGtSbTl4YWxCa1NsSlFWREp4TVVZMFNrcEpjMFZKUVVGQ1UwOWpOVk5IVVdVNFYxRTFjWGhxV0ZoamRHazFabXRrZGpoNWRETjJSakJRTXpCQlFXaERRVGRFVEVWeVFVdHBLMGMwUVZObVdtVjFUWEprU0ZOaFR6ZHBXV2huUVVSclVFTkRXa2xsVFhSUlJsSkpWMmRPZUROSVJHeG9XR0ZtYmpKd1QwcEtkMjlCZDFGblFVcEZWakJLTm1WdU1uTlplV2hVYzJkelptUkJNR2hzZVZSeVlrRXJjbVV5V0ZoUVpEZGhZMmQzUVVWSlIydG5NRk0wZVdkTWVXOVZSbUpTVFV0Uk5uSnRjRU0yVm1sRFNERnpiMEYzYUVGSWFqbExXakZqWWswNFQwNXZjV3BXUm1OcGRtRTNTR1phY0RKeFRHeEdVbEJXVlRsalpIbDJTRGxYTUVSQ1EwRjBRVVpUYzFwc1YwUlhSbkpKVFZjdlNGTllhaXRrVUhweE9XZzRSRUozU0VsQlFVSkRSVUpQUVdGTVlXaGplazlTZUhsMGFVdHlkalJITkhFeWJXRlRRMmhEUVhSQlUya3pTRmxJVTBKRlVYaDNNVlJNY1ZWa2VsUXlWbE16TmtGQlFWRm5SR2xEVDNCMVJIVlJUR0p3UzB0MVpXMUxjRFpOVmxGUk0wMTJielJFYTBGQ1EwRkJRV05wWW1kR1V6QkJSbUpRWWxwM09VbEZZekZFUVVWRlNVRkJRbWxNWTJaNFJsVk9XRlJYTTNGalZDODBUSE5CUWtOQlFVRmpaM0JSUW5oTWVFTmtUSGRXV0ZaWGRGVTRVbGxuVEVsQlFVSkRSVUpQUVdWTVpUTkpjU3N6TVVoR2NsWjZiVU5zY3pOQlVVbFJaMEZFYTNoRmNYRmxPV3hhYzJKU1ZucFdhMWRNY1d0Q2IyazJNVUYzVVdkQlQyeGpVMWhYTXk5WU1pOTZOMmRLVm5SaU5TOXljRk5sZVZWUldsTXdSRWxCUVVKNVMzQkxjVzF5WTNwbVlVUmFSa0UwYkhOblJreFpiMEZCVVdoQlFWQkxiUzk0V2xaWFZFZE9RVWhJVUVZeFZFeFZNR014Um5sQlFVRlJhRUZZWjBSRlpGSkZaRWN6WlZaT01YY3hTSGRPU1RSak5IVlJRVUZEYTBwUFFucEROa1phZVhWMk4ycGFSMVozZG14TlluVTFNV1l5V2pkd01XRk9OVVJyWkdkU1FVRkJTVkV5Vm1jcll6Qk9ZMHhaVkZwdWNHRTNWbEo0UVdsdlFXOXVNbEZNTm5sTlFVRmpaMEpSUmxwV1ZYSlFhbVZWWTNOaVRsbGFVeXN5TjBFeVJUTkdlVVIyUVZaTVVrUXhTalZtVDBWSGJYWXpiall6ZW1WRE5sUTJUVE5aUWpoMWREaEJRVXRSZGxsQlVWRnJhblpxVFUwdmFuaFVaSE5PYlZkMlMzVXZkVEZ2ZVVSdVJERkNWRXQ2T1dnclFsQXhPV2hIUkdNelpXZFVUMmxUZVVORlptMU9TRlZGYzFaNGFrZG5ObU5DUzBwdlRVMVRUVWxhUVZKMUwzWXJUeXM1WTFSWlVWTmxUbXRFV2xsSmFVb3pVRk5hU2pGU2F5dEhTMGxoVVVGcVpYTlFSM2hJUkhsMVptMHpTWHBvYTJkaFRIaExiVmxKU25CSWFrUjVRV1puU25kSVNrUkxjRWhVWkZCcVpHdHVSRmRyZVZOYU1tUm5NRk4zYWxsdFNqaFNNRkZ6UkZObFp6QlRUMlJtWkVWdFF6SjFWVkZ6YUdoQ1FrTkRRMGRyU1hZNFFWRkhNazR4UnpSVmRGVkZRVUZCUVVGVFZWWlBVa3MxUTFsSlNUMEtQQzlrWVhSaFBqeDNhV1IwYUQ0M01DNHdQQzkzYVdSMGFENDhhR1ZwWjJoMFBqY3dMakE4TDJobGFXZG9kRDQ4TDFCcFkzUnlkV1ZKYm1adlBqd3ZVMlZoYkVsdVptOCtQRk5wWjI1SmJtWnZQanhqWlhKMFBqd3ZZMlZ5ZEQ0OGMybG5ibUYwZFhKbFFXeG5iM0pwZEdodFBqd3ZjMmxuYm1GMGRYSmxRV3huYjNKcGRHaHRQanh6YVdkdVJHRjBZVDR5TURJMUxUQTNMVEEwSURFNE9qSTJPakkyUEM5emFXZHVSR0YwWVQ0OGMybG5ia1JoZEdVK01qQXlOUzB3Tnkwd05DQXhPRG95TmpveU5qd3ZjMmxuYmtSaGRHVStQQzlUYVdkdVNXNW1iejQ4TDFObFlXdysKPC9zZWFsPjxzaWduRGF0ZT4yMDI1LTA3LTA0IDE4OjI2OjI2PC9zaWduRGF0ZT48aGFzaD5udWxsPC9oYXNoPjxjZXJ0U04+PC9jZXJ0U04+PHNpZ25hdHVyZUFsZ29yaXRobT5TR0RfU0hBMjU2X1JTQTwvc2lnbmF0dXJlQWxnb3JpdGhtPjxzaWduUmVzdWx0Pm51bGw8L3NpZ25SZXN1bHQ+PC9TaWduYXR1cmU+\n\n	2025-07-04T18:26:26+0800\n	https://www.tsign.cn?serviceId=5de19d3d2ef845d393fa09cab14f7d3f&fileId=3c83cadb796b43f8b08793067154cb3e\n\n\n\n\n', '2026-07-30 16:01:37', '2026-07-30 16:01:37', 'pdf', 466580, '[]', 'ready', 'admin', 'kb-files/1/3_启航ERP系统软件销售合同.pdf', NULL);

-- ----------------------------
-- Table structure for notifications
-- ----------------------------
DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '接收人用户 ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知内容',
  `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知类型: task_assignment / approval_request / approval_result / reminder / system',
  `source_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '来源类型: dataset / knowledge_base / system',
  `source_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '来源 ID',
  `is_read` int NOT NULL DEFAULT 0 COMMENT '是否已读 0/1',
  `read_at` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '已读时间',
  `created_at` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_is_read`(`user_id` ASC, `is_read` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '通知消息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notifications
-- ----------------------------
INSERT INTO `notifications` VALUES (1, 1, '新增记录', '「客户信息」新增记录 #0001', 'record_create', 'dataset', 'e33e837e-b54', 1, '2026-07-29 23:25:00', '2026-07-29 23:18:00');
INSERT INTO `notifications` VALUES (2, 1, '记录更新', '「客户信息」记录 #0001 已更新', 'record_update', 'dataset', 'e33e837e-b54', 1, '2026-07-29 23:24:57', '2026-07-29 23:22:51');
INSERT INTO `notifications` VALUES (3, 1, '新增记录', '「客户信息」新增记录 #0002', 'record_create', 'dataset', 'cf78fa76-974', 0, NULL, '2026-07-29 23:59:02');
INSERT INTO `notifications` VALUES (4, 1, '新增记录', '「客户信息」新增记录 #0002', 'record_create', 'dataset', '41811d44-276', 0, NULL, '2026-07-30 00:00:04');

-- ----------------------------
-- Table structure for reminders
-- ----------------------------
DROP TABLE IF EXISTS `reminders`;
CREATE TABLE `reminders`  (
  `id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提醒 ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提醒名称',
  `message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '提醒内容',
  `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提醒类型',
  `time` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '09:00' COMMENT '提醒时间',
  `date` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '提醒日期',
  `day_of_week` int NULL DEFAULT 0 COMMENT '星期几',
  `day_of_month` int NULL DEFAULT 1 COMMENT '每月几号',
  `month_day` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '月日',
  `enabled` int NULL DEFAULT 1 COMMENT '是否启用 0/1',
  `created_at` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '创建时间',
  `last_triggered` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最后触发时间',
  `kb_id` bigint NULL DEFAULT NULL COMMENT '知识库 ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_kb_id`(`kb_id` ASC) USING BTREE,
  INDEX `idx_enabled`(`enabled` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '提醒' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of reminders
-- ----------------------------
INSERT INTO `reminders` VALUES ('daily-report-reminder', '下班日报提醒', '到6点了老齐，写一下今天的工作记录！\n写完后我来帮你更新记忆文件，明天综合日报就会包含这些内容。\n内容包括：\n- 今天做了什么事\n- 客户沟通情况\n- 开发/文章进展\n- 明天计划', 'daily', '18:00', NULL, NULL, NULL, NULL, 1, '2026-07-28 23:18', NULL, NULL);

-- ----------------------------
-- Table structure for solve_follow_ups
-- ----------------------------
DROP TABLE IF EXISTS `solve_follow_ups`;
CREATE TABLE `solve_follow_ups`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL COMMENT '会话 ID',
  `question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '追问问题',
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '追问回答',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_session_id`(`session_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '识题追问' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of solve_follow_ups
-- ----------------------------

-- ----------------------------
-- Table structure for solve_sessions
-- ----------------------------
DROP TABLE IF EXISTS `solve_sessions`;
CREATE TABLE `solve_sessions`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '新识题' COMMENT '标题',
  `image_name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '图片名称',
  `image_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '图片路径',
  `image_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'image/jpeg' COMMENT '图片类型',
  `image_data` longblob NULL COMMENT '图片二进制数据',
  `model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '模型',
  `prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '提示词',
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '答案',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT '状态',
  `created_at` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '识题会话' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of solve_sessions
-- ----------------------------

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `role_id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色权限字符串',
  `role_sort` int NOT NULL COMMENT '显示顺序',
  `data_scope` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '数据范围（1全部 2自定义 3本部门 4本部门及以下）',
  `menu_check_strictly` tinyint(1) NULL DEFAULT 1 COMMENT '菜单树选择项是否关联显示',
  `dept_check_strictly` tinyint(1) NULL DEFAULT 1 COMMENT '部门树选择项是否关联显示',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '超级管理员', 'admin', 1, '1', 1, 1, '0', '0', 'admin', '2024-01-01 00:00:00', 'admin', '2024-01-01 00:00:00', '超级管理员');
INSERT INTO `sys_role` VALUES (2, '普通角色', 'common', 2, '2', 1, 1, '0', '0', 'admin', '2024-01-01 00:00:00', 'admin', '2024-01-01 00:00:00', '普通角色');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '部门ID',
  `user_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户昵称',
  `user_type` varchar(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '00' COMMENT '用户类型（00系统用户）',
  `email` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '手机号码',
  `sex` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '头像地址',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '密码',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `del_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `login_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 103, 'admin', '管理员', '00', 'admin@qihang.com', '13888888888', '0', '', '$2a$10$a.pN.GYB/iFPxBSwsAwQIuRt8Wpk5hhX010x31zAostyzsjx7ZANS', '0', '0', '127.0.0.1', '2026-07-30 18:26:22', 'admin', '2024-01-01 00:00:00', 'admin', '2026-07-30 18:26:23', '管理员');

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`user_id`, `role_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户和角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 1);

-- ----------------------------
-- Table structure for tasks
-- ----------------------------
DROP TABLE IF EXISTS `tasks`;
CREATE TABLE `tasks`  (
  `id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务 ID',
  `title` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '任务描述',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT '状态',
  `priority` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'mid' COMMENT '优先级',
  `due_date` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '截止日期',
  `created_at` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '创建时间',
  `updated_at` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '更新时间',
  `kb_id` bigint NULL DEFAULT NULL COMMENT '知识库 ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_kb_id`(`kb_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '任务' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tasks
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
