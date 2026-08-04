package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.entity.LlmProfileEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Service
public class NoteAssistantService {

    private static final Logger log = LoggerFactory.getLogger(NoteAssistantService.class);

    private final LlmConfigResolver configResolver;
    private final NoteTools noteTools;
    private final DataTools dataTools;
    private final TaskTools taskTools;
    private final ReminderTools reminderTools;
    private final KbTools kbTools;
    private final WebTools webTools;
    private final SessionService sessionService;
    private final ContextBuilder contextBuilder;
    private final LlmService llmService;
    private final TaskPlannerService taskPlanner;
    private final AgentTraceService agentTrace;

    private volatile ChatClient defaultClient;
    private volatile String cachedConfigKey = "";
    private final ConcurrentHashMap<String, ChatClient> modelClients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> modelConfigKeys = new ConcurrentHashMap<>();

    public NoteAssistantService(LlmConfigResolver configResolver,
                                NoteTools noteTools, DataTools dataTools,
                                TaskTools taskTools, ReminderTools reminderTools,
                                KbTools kbTools, WebTools webTools,
                                SessionService sessionService, ContextBuilder contextBuilder,
                                LlmService llmService,
                                TaskPlannerService taskPlanner, AgentTraceService agentTrace) {
        this.configResolver = configResolver;
        this.noteTools = noteTools;
        this.dataTools = dataTools;
        this.taskTools = taskTools;
        this.reminderTools = reminderTools;
        this.kbTools = kbTools;
        this.webTools = webTools;
        this.sessionService = sessionService;
        this.contextBuilder = contextBuilder;
        this.llmService = llmService;
        this.taskPlanner = taskPlanner;
        this.agentTrace = agentTrace;
    }

    public boolean isAvailable() {
        return configResolver.isAvailable();
    }

    public boolean needsOrchestration(String userMessage) {
        return true;
    }

    public String chat(String sessionId, String userMessage, String mode) throws Exception {
        return chat(sessionId, userMessage, mode, null, null);
    }

    public String chat(String sessionId, String userMessage, String mode, Long kbId) throws Exception {
        return chat(sessionId, userMessage, mode, kbId, null);
    }

    public String chat(String sessionId, String userMessage, String mode, Long kbId, String modelName) throws Exception {
        if (!isAvailable()) {
            throw new IllegalStateException("LLM API Key 未配置，请在配置页填写");
        }

        ChatClient client = getOrCreateClient(modelName);
        if (client == null) {
            throw new IllegalStateException("ChatClient 初始化失败");
        }

        NoteTools.setCurrentKbId(kbId);
        try {
            // 构建完整上下文（历史对话 + 规则文件）
            ContextBuilder.ChatContext context = contextBuilder.build(sessionId, userMessage, kbId);
            String fullMessage = contextBuilder.merge(context, userMessage);

            log.info("[编排] 上下文构建完成，总消息长度={}", fullMessage.length());

            log.info("[编排] 用户: {} (session={}, kbId={}, model={})", userMessage, sessionId, kbId,
                    modelName != null ? modelName : "default");

            String reply = client.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(fullMessage)
                    .call()
                    .content();

            log.info("[编排] 回复长度: {}", reply != null ? reply.length() : 0);
            return reply != null ? reply : "（AI 未返回回复）";
        } finally {
            NoteTools.clearCurrentKbId();
        }
    }

    public String chat(String sessionId, String userMessage) throws Exception {
        return chat(sessionId, userMessage, "knowledge", null, null);
    }

    public String streamChat(String sessionId, String userMessage, String mode, Long kbId, String modelName, Consumer<String> chunkCallback) throws Exception {
        return streamChat(sessionId, userMessage, mode, kbId, modelName, chunkCallback, null);
    }

    public String streamChat(String sessionId, String userMessage, String mode, Long kbId, String modelName,
                             Consumer<String> chunkCallback, Consumer<String> statusCallback) throws Exception {
        if (!isAvailable()) {
            throw new IllegalStateException("LLM API Key 未配置，请在配置页填写");
        }

        ChatClient client = getOrCreateClient(modelName);
        if (client == null) {
            throw new IllegalStateException("ChatClient 初始化失败");
        }

        NoteTools.setCurrentKbId(kbId);
        try {
            // Step 0: 任务规划 — 复杂请求先生成执行计划
            String planContext = "";
            if (taskPlanner.needsPlanning(userMessage)) {
                if (statusCallback != null) statusCallback.accept("正在制定执行计划...");
                planContext = taskPlanner.buildPlanContext(sessionId, userMessage, kbId);
                if (!planContext.isEmpty()) {
                    log.info("[编排] 已生成执行计划，注入上下文");
                    agentTrace.record(sessionId, agentTrace.getNextStepIndex(sessionId), "plan",
                            "为复杂请求生成执行计划", planContext, 0);
                }
            }

            // 构建完整上下文（历史对话 + 规则文件）
            if (statusCallback != null) statusCallback.accept("正在搜索笔记库...");
            ContextBuilder.ChatContext context = contextBuilder.build(sessionId, userMessage, kbId, statusCallback);

            if (statusCallback != null) statusCallback.accept("正在构建上下文...");
            String baseMessage = contextBuilder.merge(context, userMessage);

            // 合并：计划 > 笔记上下文
            StringBuilder fullMessageBuilder = new StringBuilder();
            if (!planContext.isEmpty()) {
                fullMessageBuilder.append(planContext).append("\n");
            }
            fullMessageBuilder.append(baseMessage);
            String fullMessage = fullMessageBuilder.toString();

            log.info("[编排] 上下文构建完成，总消息长度={}", fullMessage.length());

            log.info("[编排] 用户: {} (session={}, kbId={}, model={})", userMessage, sessionId, kbId,
                    modelName != null ? modelName : "default");

            if (statusCallback != null) statusCallback.accept("AI 正在生成回复...");

            if (statusCallback != null) {
                NoteTools.setStatusCallback(statusCallback);
            }

            StringBuilder fullReply = new StringBuilder();

            String reply = client.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(fullMessage)
                    .call()
                    .content();

            if (reply != null && !reply.isEmpty()) {
                reply = reply.trim().replaceAll("\\n{3,}", "\n\n");
                fullReply.append(reply);
                if (chunkCallback != null) {
                    String[] sentences = reply.split("(?<=[。！？.!?\\n])");
                    for (String s : sentences) {
                        String trimmed = s.trim();
                        if (!trimmed.isEmpty()) {
                            chunkCallback.accept(trimmed);
                        }
                    }
                }
            } else {
                reply = "（AI 未返回回复）";
                fullReply.append(reply);
                if (chunkCallback != null) {
                    chunkCallback.accept(reply);
                }
            }

            log.info("[编排] 回复长度: {}", reply.length());

            // 记录决策追踪
            try {
                int stepIdx = agentTrace.getNextStepIndex(sessionId);
                agentTrace.record(sessionId, stepIdx++, "thought",
                        "理解用户意图: " + (userMessage.length() > 60 ? userMessage.substring(0, 60) + "..." : userMessage),
                        "用户消息: " + userMessage, 0);
                agentTrace.record(sessionId, stepIdx, "answer",
                        "AI 回复", reply.length() > 200 ? reply.substring(0, 200) + "..." : reply, 0);
            } catch (Exception e) {
                log.debug("[编排] 追踪记录跳过: {}", e.getMessage());
            }

            return reply.isEmpty() ? "（AI 未返回回复）" : reply;
        } finally {
            NoteTools.clearCurrentKbId();
            NoteTools.clearStatusCallback();
        }
    }

    // ========== ChatClient 管理 ==========

    private ChatClient getOrCreateClient(String modelName) {
        if (!isAvailable()) return null;

        if (modelName == null || modelName.isEmpty()) {
            String currentKey = buildConfigKey();
            if (defaultClient == null || !currentKey.equals(cachedConfigKey)) {
                defaultClient = createDefaultClient();
                cachedConfigKey = currentKey;
            }
            return defaultClient;
        }

        String cacheKey = modelName;
        String currentKey = buildConfigKey(modelName);
        String cachedKey = modelConfigKeys.get(cacheKey);
        if (cachedKey == null || !cachedKey.equals(currentKey)) {
            ChatClient client = createChatClientFromName(modelName);
            modelClients.put(cacheKey, client);
            modelConfigKeys.put(cacheKey, currentKey);
            log.info("NoteAssistant ChatClient 已重建 (model={})", modelName);
        }
        return modelClients.get(cacheKey);
    }

    private String buildConfigKey() {
        return configResolver.resolveBaseUrl() + "|"
                + configResolver.resolveModel() + "|"
                + configResolver.resolveApiKey().hashCode();
    }

    private String buildConfigKey(String modelName) {
        return configResolver.resolveBaseUrl(modelName) + "|"
                + configResolver.resolveModel(modelName) + "|"
                + configResolver.resolveApiKey(modelName).hashCode();
    }

    private ChatClient createDefaultClient() {
        String apiKey = configResolver.resolveApiKey();
        String baseUrl = configResolver.resolveBaseUrl();
        String model = configResolver.resolveModel();

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .restClientBuilder(configResolver.buildRestClientBuilder())
                .build();

        DeepSeekChatOptions options = DeepSeekChatOptions.builder()
                .model(model)
                .build();

        DeepSeekChatModel chatModel = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .options(options)
                .build();

        return ChatClient.builder(chatModel)
                .defaultTools(noteTools, dataTools, taskTools, reminderTools, kbTools, webTools)
                .build();
    }

    private ChatClient createChatClient(String modelName, LlmProfileEntity profile) {
        String apiKey = profile.getApiKey();
        String baseUrl = profile.getBaseUrl();
        String model = profile.getModel();

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .restClientBuilder(configResolver.buildRestClientBuilder())
                .build();

        DeepSeekChatOptions options = DeepSeekChatOptions.builder()
                .model(model)
                .build();

        DeepSeekChatModel chatModel = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .options(options)
                .build();

        return ChatClient.builder(chatModel)
                .defaultTools(noteTools, dataTools, taskTools, reminderTools, kbTools, webTools)
                .build();
    }

    private ChatClient createChatClientFromName(String modelName) {
        LlmProfileEntity profile = configResolver.getProfileByName(modelName);
        if (profile != null) {
            return createChatClient(modelName, profile);
        }
        return createDefaultClient();
    }

    private static final String SYSTEM_PROMPT = """
            你是一个拥有自主思考和工具调用能力的 AI Agent（智能体），核心使命是成为用户的笔记库助手。

            == 身份意识 ==
            - 你是一个智能体（Agent），不是简单的问答机器人
            - 你有规划能力、工具使用能力
            - 你的目标是主动帮助用户管理知识、完成任务、达成目标

            == 思维框架（ReAct：推理→行动→观察）==
            每当收到用户请求，请按以下步骤思考：

            1️⃣ 理解（Thought）: 分析用户真正想要什么
               - 用户的请求是简单查询还是复杂任务？
               - 需要调用什么工具？调用顺序是什么？
               - 有没有需要先了解的背景信息？

            2️⃣ 规划（Plan）: 对复杂任务进行拆解
               - 如果是"分析"、"总结"、"报告"、"对比"类请求，先想好执行步骤
               - 步骤之间可能有依赖关系，按顺序执行
               - 示例：用户说"分析本周工作" → ①searchRecords搜索本周记录 ②queryRecords按条件筛选 ③综合回复

            3️⃣ 行动（Action）: 调用最合适的工具
               - 数据操作：先 listDatasets 查看有哪些数据集，再用相应工具操作
               - 任务管理用 TaskTools，提醒管理用 ReminderTools
               - 知识库管理用 KbTools，互联网搜索用 WebTools
               - 记录工作进展、客户沟通等用 logRecord

            4️⃣ 观察（Observation）: 检查工具返回的结果
               - 结果是否满足用户需求？
               - 是否需要补充更多信息？
               - 如果搜索无结果，换关键词或换工具重试

            5️⃣ 回答（Answer）: 给出最终的完整回复
               - 综合所有信息给出答案
               - 引用来源（数据集名称、记录信息）
               - 如果用户指令有歧义，先确认再执行

            == 核心工具一览 ==

            【数据中心工具 - DataTools】(7)
              listDatasets() — 查看所有数据集
              searchRecords(dataset, keyword) — 搜索数据记录
              addRecord(dataset, jsonData) — 新增数据记录
              updateRecord(dataset, recordId, jsonData) — 修改数据记录
              deleteRecord(dataset, recordId) — 删除数据记录
              getRecord(dataset, recordId) — 查看记录详情
              queryRecords(dataset, filterJson) — 按条件筛选记录

            【笔记记录工具 - NoteTools】(1)
              logRecord(notePath, noteContent, dataset, jsonData) — 同时保存笔记+数据集记录

            【任务管理工具 - TaskTools】(5)
              createTask(title, description, priority, dueDate) — 创建待办任务
              listTasks(status) — 查看任务列表
              updateTask(taskId, ...) — 更新任务
              deleteTask(taskId) — 删除任务
              completeTask(taskId) — 完成任务

            【提醒管理工具 - ReminderTools】(5)
              createReminder(name, message, type, time, ...) — 创建定时提醒
              listReminders(filter) — 查看提醒列表
              toggleReminder(reminderId) — 启用/禁用提醒
              deleteReminder(reminderId) — 删除提醒
              updateReminder(reminderId, ...) — 修改提醒

            【知识库管理工具 - KbTools】(4)
              switchKnowledgeBase(kbIdentifier) — 切换知识库
              listKnowledgeBases() — 列出所有知识库
              getCurrentKnowledgeBase() — 查看当前知识库
              createKnowledgeBase(name, notesDir) — 创建知识库

            【互联网工具 - WebTools】(2)
              webSearch(query, limit) — 搜索互联网
              fetchUrl(url) — 获取网页内容

             == 工作流程 ==
            1. 注意上下文中的"当前时间"信息，以此为准理解"今天"等时间概念
            2. 理解用户意图 — 是查询、记录、分析还是管理任务？
            3. 对复杂任务进行多步规划（分析/总结/报告类请求）
            4. 数据查询：先 listDatasets 了解有哪些数据集，再用 searchRecords / queryRecords 检索
            5. 综合所有结果给出完整回复，引用来源
            6. 记录类操作（客户沟通、工作进展）→ 用 logRecord，不要分别调用其他工具
            7. 任务相关用户说"记个事"、"待办" → 用 TaskTools
            8. 提醒相关用户说"提醒我" → 用 ReminderTools
            9. 用户说"切换到XX知识库" → 用 switchKnowledgeBase
            10. 用户问最新消息、你不知道的信息 → 用 webSearch
            11. 用户要求"制定计划"、"拆解任务"、"排期"、"安排工作"时 → 除了给出计划，还必须用 createTask 将每个步骤落地为任务（合理设置 priority 和 dueDate），并在回复中说明已创建哪些任务

            == 重要原则 ==
            - AGENTS.md 的内容已包含在上下文中，无需再读取
            - 数据操作前先 listDatasets 确认数据集名称
            - 参考文件内容，但以对话历史中的用户最新说法为最高优先级
            - 如果用户明确纠正了某个信息（如"已经发布过了"），以用户说法为准
            - 引用数据时标注来源 [来源: 数据集名/记录ID]
            - 不要假设工具调用失败，检查返回结果再做判断

            == 硬性规则 ==
            - 严格执行用户最新消息中明确要求的操作，不要擅自做其他事
            - 写入 JSON 时先读取现有数据，合并后写入
            - 用中文回复
            - 用户对数据内容的纠正，应立即更新到数据集中
            - 对于敏感操作（deleteRecord, deleteTask），确认后再执行
            - 不要执行危险的 shell 命令或修改系统文件
            """;
}