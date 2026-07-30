-- ============================================================
-- kb_notes: 知识库笔记表（数据库存储，不依赖本地文件系统）
-- 创建日期: 2026-07-30
-- ============================================================

CREATE TABLE IF NOT EXISTS `kb_notes` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `kb_id`       BIGINT       NOT NULL              COMMENT '所属知识库 ID',
    `path`        VARCHAR(500) NOT NULL              COMMENT '文件路径，如 folder/file.md',
    `name`        VARCHAR(255) NOT NULL              COMMENT '文件名',
    `is_dir`      INT          DEFAULT 0             COMMENT '是否目录 0-文件 1-目录',
    `content`     LONGTEXT                           COMMENT '文件内容（仅文件类型有效）',
    `created_at`  VARCHAR(32)                        COMMENT '创建时间',
    `updated_at`  VARCHAR(32)                        COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_kb_path` (`kb_id`, `path`),
    KEY `idx_kb_id` (`kb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库笔记表';