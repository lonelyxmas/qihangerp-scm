package cn.qihang.ai.assistant.controller.v3;

import cn.qihang.ai.assistant.entity.KbBaseEntity;
import cn.qihang.ai.assistant.service.KbBaseService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
//@RequestMapping("/v3")
public class ToolsController {

    private final KbBaseService kbService;

    public ToolsController(KbBaseService kbService) {
        this.kbService = kbService;
    }

    @GetMapping("/tools")
    public String toolsPage(Map<String, Object> model) {
        model.put("currentNav", "tools");
        List<KbBaseEntity> kbList = kbService.getAll();
        model.put("kbList", kbList);
        
        if (!kbList.isEmpty()) {
            model.put("defaultKbId", kbList.get(0).getId());
        }

        return "3.0/tools";
    }
}