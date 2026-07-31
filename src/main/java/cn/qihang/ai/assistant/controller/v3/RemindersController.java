package cn.qihang.ai.assistant.controller.v3;

import cn.qihang.ai.assistant.entity.KbBaseEntity;
import cn.qihang.ai.assistant.service.KbBaseService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class RemindersController {

    private final KbBaseService kbService;

    public RemindersController(KbBaseService kbService) {
        this.kbService = kbService;
    }

    @GetMapping("/reminders")
    public String remindersPage(@RequestParam(required = false) Long kbId, Map<String, Object> model) {
        model.put("currentNav", "reminders");
        List<KbBaseEntity> kbList = kbService.getAll();
        model.put("kbList", kbList);

        if (!kbList.isEmpty()) {
            model.put("defaultKbId", kbList.get(0).getId());
        }

        if (kbId != null) {
            KbBaseEntity kb = kbService.getById(kbId);
            if (kb != null) {
                model.put("selectedKb", kb);
            }
        }

        return "3.0/reminders";
    }

    /** 旧地址 /planner 重定向到 /reminders */
//    @GetMapping("/planner")
//    public String plannerRedirect(@RequestParam(required = false) Long kbId) {
//        return "redirect:/reminders" + (kbId != null ? "?kbId=" + kbId : "");
//    }
}
