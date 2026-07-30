-- Phase 1: 统一对话相关表前缀为 ai_
-- 执行时间: 2026-07-30
-- 注意：kb_embeddings、kb_bases、kb_notes 等知识库表保持原名不变

RENAME TABLE sessions TO ai_sessions;
RENAME TABLE messages TO ai_messages;
RENAME TABLE turn_embeddings TO ai_turn_embeddings;
RENAME TABLE llm_profiles TO ai_llm_profiles;
