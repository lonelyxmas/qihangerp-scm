# 后端重构方案（2026-07-30 更新）

## 一、当前进度总览

| Phase | 内容 | 进度 | 估算 |
|-------|------|------|------|
| **1** | 表前缀 + 实体注解变更 | ✅ 已完成 | — |
| **2** | Embedding 服务统一 | ✅ 已完成 | — |
| **3** | KB 自动向量化索引 | ❌ 未开始 | 1.5d |
| **4** | Chat RAG 路径 | ❌ 未开始 | 1.5d |
| **5** | Agent/Chat 分离 | ⚠️ 部分完成 | 1d |
| **6** | 清理收尾 | ❌ 未开始 | 0.5d |

---

## 二、已完成工作（Phase 1 & 2）

### ✅ Phase 1：表重构

| 原名 | 当前 | 状态 |
|------|------|------|
| `sessions` → `ai_sessions` | ✅ `SessionEntity` `@TableName("ai_sessions")` | 完成 |
| `messages` → `ai_messages` | ✅ `MessageEntity` `@TableName("ai_messages")` | 完成 |
| `llm_profiles` → `ai_llm_profiles` | ✅ `LlmProfileEntity` `@TableName("ai_llm_profiles")` | 完成 |
| `turn_embeddings` | ✅ 已从项目中移除（跨会话语义检索功能不再使用） | 确认 |
| 全量 SQL 脚本 | ✅ `docs/sql/qihang-work-ai.sql` 已使用 `ai_` 前缀 | 完成 |

### ✅ Phase 2：Embedding 服务统一

| 事项 | 当前 | 状态 |
|------|------|------|
| `OllamaEmbeddingService` → `EmbeddingService` | ✅ 已重命名 | 完成 |
| 配置源 `llm_profiles` 表 | ✅ `EmbeddingService.reloadConfig()` 读取 `LlmConfigResolver.getEmbeddingProfile()` | 完成 |
| API Key 模式支持 | ✅ `initOpenAiEmbedding()` | 完成 |
| Ollama 本地模式支持 | ✅ `initOllamaEmbedding()` | 完成 |

---

## 三、待完成工作

### ❌ Phase 3：KB 自动向量化索引

**现状**：`NoteIndexService` 已具备向量搜索和混合搜索能力，但索引依赖外部工具手动构建，笔记 CRUD 后不会自动更新。

#### 3.1 新建 `KbIndexingService`

```java
@Service
public class KbIndexingService {
    public void indexNote(Long kbId, Long noteId) { /* chunk + embedding + 写入 kb_embeddings */ }
    public void removeNoteIndex(Long noteId) { /* 删除笔记所有 embedding 块 */ }
    public void reindexKb(Long kbId) { /* 全量重索引 */ }
    @Scheduled(fixedDelay = 300_000)
    public void scheduledIncrementalIndex() { /* 5分钟增量 */ }
}
```

#### 3.2 `KbEmbeddingEntity` 增强

当前无 `noteId`、`chunkSize` 字段，需新增：

```sql
ALTER TABLE kb_embeddings
  ADD COLUMN note_id BIGINT DEFAULT 0,
  ADD COLUMN chunk_size INT DEFAULT 0;
```

#### 3.3 `KbEmbeddingDbService` 新增方法

| 方法 | 说明 |
|------|------|
| `deleteByNoteId(Long noteId)` | 删除笔记所有 embedding 块 |
| `listByNoteId(Long noteId)` | 列出笔记所有 embedding 块 |

#### 3.4 触发时机

| 时机 | 方式 | 实现 |
|------|------|------|
| 笔记新建/编辑 | 同步 | `KbNoteApiController.save()` |
| 笔记删除 | 同步 | `KbNoteApiController.delete()` |
| 定时增量 | `@Scheduled` | 每 5 分钟 |
| 全量重索引 | REST API | `POST /api/kb/{id}/reindex` |

#### 涉及文件

| 操作 | 文件 |
|------|------|
| 新建 | `KbIndexingService.java` |
| 新建 | `docs/migration_kb_embeddings_note_id.sql` |
| 修改 | `KbEmbeddingEntity.java` — 新增 `noteId`、`chunkSize` |
| 修改 | `KbEmbeddingDbService.java` — 新增 `deleteByNoteId`、`listByNoteId` |
| 修改 | `KbNoteApiController.java` — 增删改后触发索引 |
| 修改 | `KbBaseController.java` — 新增 `POST /api/kb/{id}/reindex` |

---

### ❌ Phase 4：Chat RAG 路径

**现状**：`ChatApiController.sendChat()` 调用 `NoteAssistantService.streamChat()`（全量 Agent，26 工具 + 记忆 + 追踪 + 规划），流式响应是伪流式（`.call().content()` + 按句子拆分）。

#### 4.1 新建 `ChatRagService`

```java
@Service
public class ChatRagService {
    public void streamChat(String sessionId, String userMessage, Long kbId,
                           String modelName,
                           Consumer<String> chunkCallback,
                           Consumer<String> statusCallback) {
        String context = buildContext(sessionId, userMessage, kbId, statusCallback);
        ChatClient client = getOrCreateClient(modelName); // 无工具
        client.prompt()
            .system(SYSTEM_PROMPT)
            .user(context)
            .stream().content()
            .doOnNext(chunk -> { if (chunkCallback != null) chunkCallback.accept(chunk); })
            .blockLast();
    }
}
```

#### 4.2 系统 Prompt（简洁版）

```java
private static final String SYSTEM_PROMPT = """
你是一个 AI 助手，基于知识库内容回答用户的问题。
请根据提供的知识库内容回答。如果内容不足以回答，请如实告知用户。
回答时引用信息来源的文件名。使用中文回复。
""";
```

#### 4.3 `ChatApiController.sendChat()` 改造

- 注入 `ChatRagService` 替代 `NoteAssistantService`
- **移除** heartbeat 线程
- **移除** thinkingStatus 线程
- **移除** `TaskPlannerService`、`MemoryManagerService`、`AgentTraceService` 调用
- 真流式 SSE

#### 4.4 `SessionService` 新增 `buildSimpleHistory()`

```java
public String buildSimpleHistory(String sessionId) {
    // 取最近 20 条消息，简化为 用户/AI 格式
}
```

#### 涉及文件

| 操作 | 文件 |
|------|------|
| 新建 | `ChatRagService.java` |
| 改造 | `ChatApiController.java` |
| 新增 | `SessionService.java` — `buildSimpleHistory()` |

---

### ⚠️ Phase 5：Agent/Chat 分离

**现状**：页面路由已分离（`/chat`、`/data`、`/planner`、`/tools`），但代码层面仍有问题。

#### 5.1 `NoteAssistantService` SYSTEM_PROMPT 清理

当前 prompt 列出 34 个工具，但以下 **不存在**（会导致 AI 幻觉）：

| 删除项 | 说明 |
|--------|------|
| `listDir(path)` | 不存在 |
| `readFile(path)` / `readNote(path)` | 不存在 |
| `writeFile(path, content)` | 不存在 |
| `deleteFile(path)` | 不存在 |
| `searchFiles(keyword)` | 不存在 |
| `searchNotes(query, limit)` | 不存在 |
| `runPython(code)` / `python(code)` | 不存在 |

实际注册的 @Tool 仅 26 个，prompt 须与实际一致。

#### 5.2 `AgentAnalysisService` 合并到 `NoteAssistantService`

`AgentAnalysisService` 有完全独立的 ChatClient 缓存（第 3 套），与 `NoteAssistantService` 代码高度重复。当前被 `ReportService` 引用。

| 步骤 | 操作 |
|------|------|
| 1 | `AgentAnalysisService.analyze()` 改为调用 `NoteAssistantService` |
| 2 | `ReportService` 改注入 `NoteAssistantService` |
| 3 | 删除 `AgentAnalysisService.java` |

#### 5.3 职责划分（最终目标）

| 页面 | 服务 | 方式 | 工具 |
|------|------|------|------|
| `/chat` | `ChatRagService` | 无工具，`.stream()` 真流式 | 无 |
| `/data` (数据集) | `NoteAssistantService` | 有工具，`.call()` 阻塞 | DataTools |
| `/planner` (任务/提醒) | `NoteAssistantService` | 有工具，`.call()` 阻塞 | TaskTools, ReminderTools |
| `/tools` (工具箱) | `NoteAssistantService` | 有工具，`.call()` 阻塞 | 全部 26 工具 |
| `/notes` (知识库) | `NoteAssistantService` | 有工具，`.call()` 阻塞 | NoteTools, KbTools |

#### 涉及文件

| 操作 | 文件 |
|------|------|
| 修改 | `NoteAssistantService.java` — SYSTEM_PROMPT 清理 |
| 合并 | `AgentAnalysisService.java` → 并入 NoteAssistantService |
| 修改 | `ReportService.java` — 改注入 `NoteAssistantService` |

---

### ❌ Phase 6：清理收尾

| 项 | 操作 | 优先级 |
|----|------|--------|
| 1. **MemoryManagerService UPSERT 语法错误** | `UPSERT INTO` 是 H2 语法，MySQL 需改为 `INSERT ... ON DUPLICATE KEY UPDATE` | 🔴 紧急 |
| 2. **ChatApiController 单线程 Executor** | `Executors.newSingleThreadExecutor()` 串行化所有 chat 请求，改为 `newCachedThreadPool()` | 🔴 紧急 |
| 3. **ChatClient 三套缓存** | 待 Phase 5.2 完成后验证是否仅剩两套（LlmService + NoteAssistantService） | 高 |
| 4. **ContextBuilder.build() 不搜索 KB** | 只加时间+历史，不调用 NoteIndexService，KB 搜索缺失（Agent 路径需修复） | 中 |
| 5. **NoteIndexService** | Phase 3+4 完成后由 `KbIndexingService` + `ChatRagService` 覆盖，可废弃 | 低 |
| 6. **MemoryManagerService** | Agent 路径保留，Chat 路径不再调用 | 低 |
| 7. **AgentTraceService** | Agent 路径保留，Chat 路径不再调用 | 低 |
| 8. **v1/v2 模板残留** | `templates/1.0/` 和 `templates/2.0/` 目录可能为死代码 | 低 |

---

## 四、实施顺序（推荐）

| 优先级 | 内容 | 理由 |
|--------|------|------|
| ⭐ **P0** | Phase 6.1: 修复 MemoryManagerService UPSERT 语法 | MySQL 兼容性，当前会报错 |
| ⭐ **P0** | Phase 6.2: 修复单线程 Executor | 并发场景的性能瓶颈 |
| ⭐ **P0** | Phase 4: Chat RAG 路径 | Chat 页面核心功能，真流式体验急迫 |
| ⭐ **P1** | Phase 3: KB 自动向量化索引 | 知识库搜索体验的关键依赖 |
| ⭐ **P2** | Phase 5.1: SYSTEM_PROMPT 清理 | 消除 AI 幻觉指令 |
| ⭐ **P2** | Phase 5.2: AgentAnalysisService 合并 | 消除重复代码 |
| 📌 **P3** | Phase 6 其余清理项 | 低风险，可穿插执行 |

## 五、新增发现的问题

| # | 问题 | 位置 | 严重性 |
|---|------|------|--------|
| 1 | **MemoryManagerService 使用 H2 语法** | `MemoryManagerService.java:33` `UPSERT INTO` 在 MySQL 不兼容 | 🔴 高 |
| 2 | **ChatApiController 单线程 Executor** | `ChatApiController.java:29` 所有 SSE 请求排队串行 | 🟠 中 |
| 3 | **ContextBuilder.build() 不搜索 KB** | 只加时间+历史，不调用 NoteIndexService | 🟡 低 |
| 4 | **v1/v2 模板残留** | `templates/1.0/` + `templates/2.0/` 可能为死代码 | 🟢 低 |

## 六、未纳入范围（待后续明确）

- **记忆功能**：`MemoryManagerService` 暂不在 Chat 路径启用，Agent 路径保留
- **知识图谱**：暂不考虑