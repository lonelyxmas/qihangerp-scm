-- ============================================================
-- 表重命名 & 清理脚本
-- 日期: 2026-07-30
-- 说明:
--   1. knowledge_bases → kb_bases
--   2. knowledge_bases_seq → kb_bases_seq (若存在)
--   3. note_embeddings → kb_embeddings
--   4. note_embeddings_seq → kb_embeddings_seq (若存在)
--   5. 删除废弃的 file_index_meta 表
-- ============================================================

-- 1. knowledge_bases → kb_bases
RENAME TABLE `knowledge_bases` TO `kb_bases`;

-- 2. note_embeddings → kb_embeddings
RENAME TABLE `note_embeddings` TO `kb_embeddings`;

-- 3. 删除废弃的 file_index_meta 表
DROP TABLE IF EXISTS `file_index_meta`;

-- ============================================================
-- 验证
-- ============================================================
-- SELECT TABLE_NAME FROM information_schema.TABLES
-- WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME IN ('kb_bases', 'kb_embeddings', 'kb_notes');
-- 应返回 3 条记录，不包含 file_index_meta
-- ============================================================
