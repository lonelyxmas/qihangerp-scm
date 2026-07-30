# 后端重构方案

## 一、问题清单

| # | 问题 | 现状 |
|---|------|------|
| 1 | **KB 未向量化** | `kb_embeddings` 表存在但为旧文件笔记系统遗留，@KB 不走 RAG，靠 Agent 工具 `switchKnowledgeBase` 搜索 |
| 2 | **Embedding 配置源错误** | `OllamaEmbeddingService` 读 `config.json`，但 `llm_profiles` 已有 `model_type='embedding'` 的配置入口，两者脱节 |
| 3 | **表前缀混乱** | 对话相关表无统一前缀：`sessions`、`messages`、`turn_embeddings`、`llm_profiles` |
| 4 | **遗留工具幻觉** | SYSTEM_PROMPT 列出 `listDir`/`readFile`/`searchFiles` 等不存在的工具，LLM 接到幻觉指令 |
| 5 | **Agent 范式溢出** | Chat 页面使用完整 ReAct Agent（27 工具 + 规划 + 记忆 + 追踪），但只需要简单 RAG |
| 6 | **流式与工具冲突** | `defaultTools()` + `.stream()` 在 Spring AI 中不兼容（ThreadLocal + Reactor 线程切换） |
| 7 | **三套 ChatClient 缓存** | `LlmService`、`NoteAssistantService`、`AgentAnalysisService` 各自管理，代码重复 |
| 8 | **Ollama 硬依赖** | 语义检索、跨会话记忆、KB 搜索全部依赖 Ollama embedding，未配置时全线降级报错 |

## 二、整体架构

```
┌──────────────────────────────────────────────────┐
│                   Chat 页面                        │
│  ChatRagService                                   │
│  ├── @KB → 搜索 kb_embeddings (RAG)              │
│  ├── 无工具 / .stream() / 真流式                   │
│  └── 简洁 prompt                                  │
└──────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────┐
│            KB 自动向量化索引 (新增)                  │
│  KbIndexingService                                │
│  ├── 笔记变更 → 自动 chunk + embedding            │
│  ├── @Scheduled 定时增量索引                       │
│  └── 全量重索引入口 (UI 按钮触发)                    │
└──────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────┐
│            Agent 功能 (保留)                        │
│  NoteAssistantService (改造)                      │
│  ├── 工具：DataTools / TaskTools / ReminderTools  │
│  ├── 阻塞 .call().content()                       │
│  ├── 清理不存在的工具引用                           │
│  └── 仅用于 数据集/任务/提醒 页面                    │
└──────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────┐
│            Embedding 服务 (统一)                    │
│  EmbeddingService (原 OllamaEmbeddingService)     │
│  ├── 配置源从 config.json → llm_profiles           │
│  ├── 推理侧：ChatRagService 查询向量               │
│  └── 索引侧：KbIndexingService 生成向量            │
└──────────────────────────────────────────────────┘
```

### 数据流

```
用户输入 "@工作笔记 帮我查一下合同"

  │
  ▼
ChatApiController.sendChat()
  │
  ├── 保存用户消息到 ai_messages
  │
  ├── ChatRagService.streamChat()
  │     │
  │     ├── 解析 @ → kbId
  │     │
  │     ├── EmbeddingService.embed(query) → float[]
  │     │
  │     ├── 向量搜索 kb_embeddings → 最相关 N 条
  │     │     └── 失败 → 关键词 LIKE 降级
  │     │
  │     ├── 构建 prompt: [时间] + [KB 结果] + [历史] + [问题]
  │     │
  │     └── client.prompt().stream().content()     ← 无工具，真流式
  │           │
  │           └── 逐 token → SSE: type=text → 前端实时渲染
  │
  ├── 保存 AI 回复到 ai_messages
  │
  └── SSE: type=done
```

## 三、分阶段实施

---

### Phase 1：表重构

**目标**：统一 `ai_` 前缀，保持 `kb_` 前缀不变

#### 变更表

| 原名 | 改为 | 原因 |
|------|------|------|
| `sessions` | `ai_sessions` | 对话会话 |
| `messages` | `ai_messages` | 对话消息 |
| `turn_embeddings` | `ai_turn_embeddings` | 对话轮次向量 |
| `llm_profiles` | `ai_llm_profiles` | LLM 配置 |

#### 保持原名

`kb_bases`、`kb_notes`、`kb_embeddings`、`kb_categories` 等知识库相关表不变。

#### 变更实体

| Entity | 当前 `@TableName` | 改为 |
|--------|-------------------|------|
| `SessionEntity` | `sessions` | `ai_sessions` |
| `MessageEntity` | `messages` | `ai_messages` |
| `TurnEmbeddingEntity` | `turn_embeddings` | `ai_turn_embeddings` |
| `LlmProfileEntity` | `llm_profiles` | `ai_llm_profiles` |

#### 数据迁移

```sql
RENAME TABLE sessions TO ai_sessions;
RENAME TABLE messages TO ai_messages;
RENAME TABLE turn_embeddings TO ai_turn_embeddings;
RENAME TABLE llm_profiles TO ai_llm_profiles;
```

#### 涉及文件

- `SessionEntity.java` — 改 `@TableName`
- `MessageEntity.java` — 改 `@TableName`
- `TurnEmbeddingEntity.java` — 改 `@TableName`
- `LlmProfileEntity.java` — 改 `@TableName`
- `docs/migration_table_prefix.sql` — 新建迁移脚本

---

### Phase 2：Embedding 服务统一

**目标**：`OllamaEmbeddingService` → `EmbeddingService`，配置源从 `config.json` 改为 `llm_profiles` 表

#### 配置源切换

```java
// 改前 (config.json)
Config cfg = configService.load();
String model = cfg.getEmbeddingModel();
String baseUrl = cfg.getEmbeddingBaseUrl();
String apiKey = cfg.getEmbeddingApiKey();

// 改后 (llm_profiles WHERE model_type='embedding')
LlmProfileEntity profile = llmConfigResolver.getEmbeddingProfile();
String model = profile.getModel();
String baseUrl = profile.getBaseUrl();
String apiKey = profile.getApiKey();
```

#### 前端配置入口

已有 `/config` 页面的 LLM 配置功能，新增一条 `model_type=embedding` 的 profile 即可。

#### `LlmConfigResolver` 新增方法

```java
public LlmProfileEntity getEmbeddingProfile() {
    return profileDbService.lambdaQuery()
        .eq(LlmProfileEntity::getModelType, LlmProfileEntity.TYPE_EMBEDDING)
        .orderByAsc(LlmProfileEntity::getSortOrder)
        .last("LIMIT 1")
        .one();
}
```

#### 服务重命名

| 当前 | 改为 |
|------|------|
| `OllamaEmbeddingService.java` | `EmbeddingService.java` |
| 类名 `OllamaEmbeddingService` | `EmbeddingService` |

内部保留两种模式不变：
- 有 `apiKey` → OpenAI 兼容 API
- 无 `apiKey` → Ollama 本地

#### 涉及文件

- `OllamaEmbeddingService.java` — 重命名 + 换配置源
- `LlmConfigResolver.java` — 新增 `getEmbeddingProfile()`
- `NoteIndexService.java` — 引用名更新
- `SessionService.java` — 引用名更新
- `ChatRagService.java` — 新建，引用 EmbeddingService

---

### Phase 3：KB 自动向量化索引（新增）

#### 3.1 KbIndexingService 设计

```java
@Service
public class KbIndexingService {

    /** 索引一篇笔记（新建/编辑后调用） */
    public void indexNote(Long kbId, Long noteId) {
        KbNoteEntity note = noteDbService.getById(noteId);
        String content = note.getContent();
        String contentHash = md5(content);

        // 检查哈希是否变化，无变化则跳过
        List<KbEmbeddingEntity> existing = embeddingDb.lambdaQuery()
            .eq(KbEmbeddingEntity::getNoteId, noteId).list();
        if (!existing.isEmpty() && existing.get(0).getContentHash().equals(contentHash)) {
            return; // 内容未变
        }

        // 清除旧索引
        embeddingDb.deleteByNoteId(noteId);  // 新增接口

        // 文本分块
        List<Chunk> chunks = splitIntoChunks(content);

        // 逐块生成 embedding
        for (Chunk chunk : chunks) {
            float[] vec = embeddingService.embed(chunk.text);
            if (vec == null) continue;

            KbEmbeddingEntity entity = new KbEmbeddingEntity();
            entity.setKbId(kbId);
            entity.setNoteId(noteId);
            entity.setFilePath(note.getFilePath());
            entity.setChunkIndex(chunk.index);
            entity.setChunkSize(chunk.text.length());
            entity.setContent(chunk.text);
            entity.setEmbedding(Base64.encode(floatToBytes(vec)));
            entity.setContentHash(contentHash);
            entity.setCreatedAt(now());
            entity.setUpdatedAt(now());
            embeddingDb.save(entity);
        }
    }

    /** 删除笔记索引 */
    public void removeNoteIndex(Long noteId) {
        embeddingDb.deleteByNoteId(noteId);
    }

    /** 全量重索引一个知识库 */
    public void reindexKb(Long kbId) {
        embeddingDb.deleteByKb(kbId);
        List<KbNoteEntity> notes = noteDbService.listByKb(kbId);
        for (KbNoteEntity note : notes) {
            if (note.getContent() != null && !note.getContent().isBlank()) {
                indexNote(kbId, note.getId());
            }
        }
    }

    /** 定时增量索引 */
    @Scheduled(fixedDelay = 300_000)  // 5 分钟
    public void scheduledIncrementalIndex() {
        List<KbNoteEntity> notes = noteDbService.listAll();
        for (KbNoteEntity note : notes) {
            String hash = md5(note.getContent());
            List<KbEmbeddingEntity> embeddings = embeddingDb.lambdaQuery()
                .eq(KbEmbeddingEntity::getNoteId, note.getId()).list();
            boolean changed = embeddings.isEmpty()
                || !embeddings.get(0).getContentHash().equals(hash);
            if (changed) {
                indexNote(note.getKbId(), note.getId());
            }
        }
    }
}
```

#### 3.2 文本分块策略

```java
private List<Chunk> splitIntoChunks(String text) {
    List<Chunk> result = new ArrayList<>();
    int chunkSize = 500;     // 每块字符数
    int overlap = 100;       // 重叠字符数
    int start = 0;
    int index = 0;
    while (start < text.length()) {
        int end = Math.min(start + chunkSize, text.length());
        // 尽量在段落/句子边界断开
        if (end < text.length()) {
            int breakPoint = findSentenceBoundary(text, end);
            if (breakPoint > start) end = breakPoint;
        }
        result.add(new Chunk(index++, text.substring(start, end)));
        start = end - overlap;
        if (start < 0) start = 0;
    }
    return result;
}
```

#### 3.3 `kb_embeddings` 表增强

```sql
ALTER TABLE kb_embeddings
  ADD COLUMN note_id BIGINT DEFAULT 0  COMMENT '关联 kb_notes.id',
  ADD COLUMN chunk_size INT DEFAULT 0  COMMENT '块大小(字符数)';
```

#### 3.4 触发时机

| 时机 | 方式 | 实现 |
|------|------|------|
| 笔记新建 | 同步触发 | `KbNoteApiController.save()` 末尾调用 `indexNote()` |
| 笔记编辑 | 同步触发 | `KbNoteApiController.save()` 中检测 `id` 存在时调用 |
| 笔记删除 | 同步触发 | `KbNoteApiController.delete()` 中调用 `removeNoteIndex()` |
| 定时增量 | @Scheduled | `KbIndexingService.scheduledIncrementalIndex()` 每 5 分钟 |
| 全量重索引 | REST API | `POST /api/kb/{id}/reindex` → 前端 KB 配置页按钮 |

#### 3.5 KbEmbeddingDbService 新增接口

```java
void deleteByNoteId(Long noteId);  // 删除笔记的所有 embedding 块
List<KbEmbeddingEntity> listByNoteId(Long noteId);
```

#### 涉及文件

| 类型 | 文件 | 操作 |
|------|------|------|
| 新建 | `KbIndexingService.java` | 主服务 |
| 新建 | `docs/migration_kb_embeddings_note_id.sql` | 表结构变更 |
| 修改 | `KbEmbeddingDbService.java` | 新增 `deleteByNoteId` |
| 修改 | `KbEmbeddingMapper.java` | 新增 SQL |
| 修改 | `KbNoteApiController.java` | 增删改笔记后触发索引 |
| 修改 | `KbBaseController.java` | 新增 `POST /api/kb/{id}/reindex` 端点 |

---

### Phase 4：Chat RAG 路径

#### 4.1 新建 `ChatRagService`

```java
@Service
public class ChatRagService {

    public void streamChat(String sessionId, String userMessage, Long kbId,
                           String modelName,
                           Consumer<String> chunkCallback,
                           Consumer<String> statusCallback) {

        // 1. 构建上下文
        String context = buildContext(sessionId, userMessage, kbId, statusCallback);

        // 2. 获取或创建 ChatClient（无工具）
        ChatClient client = getOrCreateClient(modelName);

        // 3. 流式调用
        StringBuilder reply = new StringBuilder();
        client.prompt()
            .system(SYSTEM_PROMPT)
            .user(context)
            .stream()
            .content()
            .doOnNext(chunk -> {
                reply.append(chunk);
                if (chunkCallback != null) chunkCallback.accept(chunk);
            })
            .blockLast();

        // 4. 保存回复到 DB（由调用方完成）
    }

    private String buildContext(String sessionId, String userMessage,
                                Long kbId, Consumer<String> statusCallback) {
        StringBuilder sb = new StringBuilder();

        // 当前时间
        sb.append("== 当前时间 ==\n");
        sb.append(timeStr()).append("\n\n");

        // 知识库搜索结果
        if (kbId != null) {
            if (statusCallback != null) statusCallback.accept("正在搜索知识库...");
            KbBaseEntity kb = kbService.getById(kbId);
            if (kb != null) {
                List<NoteSearchResult> results = searchKb(kbId, userMessage);
                if (!results.isEmpty()) {
                    sb.append("== 来自知识库「").append(kb.getName()).append("」的相关内容 ==\n\n");
                    for (NoteSearchResult r : results) {
                        sb.append("[").append(r.filePath()).append(" - 匹配度 ")
                          .append(String.format("%.2f", r.score())).append("]\n");
                        sb.append(r.content()).append("\n\n");
                    }
                }
            }
        }

        // 历史对话
        if (statusCallback != null) statusCallback.accept("正在加载历史对话...");
        String history = sessionService.buildSimpleHistory(sessionId);  // 简化版
        if (!history.isEmpty()) {
            sb.append("== 对话历史 ==\n").append(history).append("\n\n");
        }

        // 用户问题
        sb.append("---\n用户问题:\n").append(userMessage);
        return sb.toString();
    }

    /** 搜索知识库：向量搜索 → 关键词降级 */
    private List<NoteSearchResult> searchKb(Long kbId, String query) {
        // 优先向量搜索
        float[] queryVec = embeddingService.embed(query);
        if (queryVec != null) {
            List<KbEmbeddingEntity> all = embeddingDb.lambdaQuery()
                .eq(KbEmbeddingEntity::getKbId, kbId).list();
            return rankAndFilter(all, queryVec);
        }
        // 降级：关键词 LIKE
        return keywordSearch(kbId, query);
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

#### 4.3 `ChatApiController` 改造

```java
@PostMapping("/send")
public SseEmitter sendChat(@RequestParam String message,
                           @RequestParam(required = false) Long kbId,
                           @RequestParam(required = false) String modelName,
                           @RequestParam(required = false) String sessionId) {
    SseEmitter emitter = new SseEmitter(300_000L);

    chatExecutor.execute(() -> {
        try {
            String actualSessionId = getOrCreateSession(sessionId, kbId);

            emitter.send(event("session", actualSessionId));
            emitter.send(event("status", "正在处理..."));

            // 保存用户消息
            sessionService.saveMessage(actualSessionId, "user", message, "knowledge", "web", kbId);

            // RAG 流式调用（无工具、无 heartbeat、无 thinking status 线程）
            chatRagService.streamChat(actualSessionId, message, kbId, modelName,
                chunk -> emitter.send(event("text", chunk)),
                status -> emitter.send(event("status", status))
            );

            // 保存回复
            emitter.send(event("done"));
            emitter.complete();

        } catch (Exception e) {
            emitter.send(event("error", resolveErrorMessage(e)));
            emitter.complete();
        }
    });
    return emitter;
}
```

不再需要：
- heartbeat 线程（RAG 路径响应快）
- thinking status 线程（状态由 `statusCallback` 驱动）
- `TaskPlannerService`、`MemoryManagerService`、`AgentTraceService` 的调用

#### 涉及文件

| 文件 | 操作 |
|------|------|
| `ChatRagService.java` | 新建 |
| `ChatApiController.java` | 改造 `sendChat()`，注入 `ChatRagService` |
| `SessionService.java` | 新增 `buildSimpleHistory()` 简化版（仅取最近 N 条，不跨 session 检索） |

---

### Phase 5：Agent/Chat 分离

#### 5.1 `NoteAssistantService` 清理 SYSTEM_PROMPT

删除以下不存在的工具引用：

| 删除 | 说明 |
|------|------|
| `1. listDir(path) — 列出目录内容` | 文件读取，不需要 |
| `2. readFile(path) / readNote(path) — 读取笔记文件内容` | 文件读取，不需要 |
| `3. writeFile(path, content) — 写入/覆盖笔记文件` | 文件写入，不需要 |
| `4. deleteFile(path) — 删除笔记文件` | 文件删除，不需要 |
| `5. searchFiles(keyword) — 按文件名搜索` | 文件搜索，不需要 |
| `6. searchNotes(query, limit) — 搜索笔记文件内容` | 改用 RAG |
| `7. logRecord(notePath, noteContent, dataset, jsonData)` | 保留（实际存在） |
| `runPython(code)` / `python(code)` | 不存在 |

保留实际注册的 27 个 `@Tool` 方法。

#### 5.2 `AgentAnalysisService` 合并

`AgentAnalysisService.java` 功能并入 `NoteAssistantService`，消除第三套 ChatClient 缓存。

#### 5.3 职责划分

| 页面 | 服务 | 方式 |
|------|------|------|
| `/chat` | `ChatRagService` | 无工具，`.stream()` |
| `/data` (数据集操作) | `NoteAssistantService` | 有工具，`.call()` |
| `/planner` (任务/提醒) | `NoteAssistantService` | 有工具，`.call()` |
| `/tools` (工具箱) | `NoteAssistantService` | 有工具，`.call()` |

#### 涉及文件

- `NoteAssistantService.java` — SYSTEM_PROMPT 清理
- `AgentAnalysisService.java` — 合并到 NoteAssistantService
- `TaskPlannerService.java` — 保留给 Agent 路径

---

### Phase 6：清理收尾

| 项 | 操作 | 优先级 |
|----|------|--------|
| `config.json` 中 embedding 相关字段 | 迁移到 `llm_profiles` 后废弃 | 中 |
| `application.yml` Ollama 默认配置 | 移除 | 低 |
| `NoteIndexService.java` | `ChatRagService` 和 `KbIndexingService` 覆盖其功能后可废弃 | 低 |
| `ContextBuilder.java` | Chat 路径不再使用，Agent 路径保留 | 低 |
| `MemoryManagerService.java` | 保留但不调用（等记忆设计明确） | 低 |

---

## 四、实施顺序

| Phase | 内容 | 文件名 | 估算 |
|-------|------|--------|------|
| **1** | 表前缀 + 实体注解变更 + 迁移 SQL | 4 个 Entity、SQL | 0.5d |
| **2** | Embedding 服务统一 | `EmbeddingService`、`LlmConfigResolver` | 0.5d |
| **3** | KB 自动向量化索引 | `KbIndexingService`、分块、触发、API | 1.5d |
| **4** | Chat RAG 路径 | `ChatRagService`、`ChatApiController` 改造 | 1.5d |
| **5** | Agent/Chat 分离 | prompt 清理、`AgentAnalysisService` 合并 | 0.5d |
| **6** | 清理收尾 | 配置迁移、废弃代码删除 | 0.5d |

**合计约 5 天**。

## 五、未纳入范围（待后续明确）

- **记忆功能**：`agent_memories` 表和 `MemoryManagerService` 暂不启用，等产品设计明确后再实现
- **跨会话语义检索**：`turn_embeddings` 相关功能保留但 Chat 路径暂不使用（等 embedding 服务稳定后再启用）
- **知识图谱**：暂不考虑
