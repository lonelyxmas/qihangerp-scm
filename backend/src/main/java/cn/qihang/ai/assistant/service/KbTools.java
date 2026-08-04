package cn.qihang.ai.assistant.service;

import cn.qihang.ai.assistant.entity.KbBaseEntity;
import cn.qihang.ai.assistant.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KbTools {

    private static final Logger log = LoggerFactory.getLogger(KbTools.class);

    private final KbBaseService kbService;

    public KbTools(KbBaseService kbService) {
        this.kbService = kbService;
    }

    @Tool(description = "切换当前操作的知识库。当用户说\"切换到XX知识库\"、\"转到XX笔记库\"时必须使用此工具。切换后后续所有笔记操作将针对新知识库")
    public String switchKnowledgeBase(
            @ToolParam(description = "知识库名称或ID") String kbIdentifier) {
        List<KbBaseEntity> all = kbService.getAll();
        KbBaseEntity target = null;

        try {
            Long id = Long.valueOf(kbIdentifier);
            target = kbService.getById(id);
        } catch (NumberFormatException ignored) {}

        if (target == null) {
            for (KbBaseEntity kb : all) {
                if (kb.getName().equals(kbIdentifier) || kb.getName().contains(kbIdentifier)) {
                    target = kb;
                    break;
                }
            }
        }

        if (target == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("❌ 未找到知识库: ").append(kbIdentifier).append("\n");
            sb.append("可用的知识库：\n");
            for (KbBaseEntity kb : all) {
                sb.append("  - ").append(kb.getName()).append(" (ID: ").append(kb.getId()).append(")\n");
            }
            return sb.toString();
        }

        NoteTools.setCurrentKbId(target.getId());
        log.info("[KbTools] 切换到知识库: {} (ID={})", target.getName(), target.getId());
        return "✅ 已切换到知识库：「" + target.getName() + "」";
    }

    @Tool(description = "列出所有可用的知识库。当用户不确定当前在哪个知识库、或想知道有哪些知识库可用时必须使用此工具")
    public String listKnowledgeBases() {
        List<KbBaseEntity> all = kbService.getAll();
        if (all.isEmpty()) {
            return "暂无知识库，请先在配置页创建";
        }

        Long currentId = NoteTools.getCurrentKbId();
        StringBuilder sb = new StringBuilder();
        sb.append("📚 共 ").append(all.size()).append(" 个知识库：\n\n");
        for (KbBaseEntity kb : all) {
            String marker = kb.getId().equals(currentId) ? " ◀ 当前" : "";
            sb.append("  ").append(kb.getId()).append(". ").append(kb.getName()).append(marker).append("\n");
        }
        sb.append("\n提示：使用 switchKnowledgeBase 可以切换知识库");
        return sb.toString();
    }

    @Tool(description = "获取当前知识库的信息。当用户问\"我在哪个知识库\"、\"当前知识库是什么\"时必须使用此工具")
    public String getCurrentKnowledgeBase() {
        Long currentId = NoteTools.getCurrentKbId();
        if (currentId == null) {
            return "当前未选择任何知识库";
        }
        KbBaseEntity kb = kbService.getById(currentId);
        if (kb == null) {
            return "当前知识库 (ID=" + currentId + ") 不存在";
        }
        return "📚 当前知识库：「" + kb.getName() + "」";
    }

    @Tool(description = "创建新的知识库。当用户说\"创建知识库\"、\"新建笔记库\"时必须使用此工具")
    public String createKnowledgeBase(
            @ToolParam(description = "知识库名称") String name) {
        if (name == null || name.isEmpty()) return "❌ 知识库名称不能为空";

        KbBaseEntity kb = new KbBaseEntity();
        kb.setName(name);
        kb.setLabels("{}");
        kb.setSortOrder(0);
        kb.setCreatedAt(TimeUtil.nowStr());
        kbService.save(Map.of("name", name));

        log.info("[KbTools] 创建知识库: {}", name);
        return "✅ 已创建知识库：「" + name + "」";
    }
}