package cn.qihang.ai.assistant.controller.v3;

import cn.qihang.ai.assistant.entity.KbBaseEntity;
import cn.qihang.ai.assistant.entity.SysUser;
import cn.qihang.ai.assistant.security.common.SecurityUtils;
import cn.qihang.ai.assistant.service.KbBaseService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
//@RequestMapping("/v3")
public class DataController {

    private final KbBaseService kbService;

    public DataController(KbBaseService kbService) {
        this.kbService = kbService;
    }

    @GetMapping("/data")
    public String dataPage(@RequestParam(required = false) Long kbId, Map<String, Object> model) {
        model.put("currentNav", "data");
        List<KbBaseEntity> kbList = kbService.getAll();
        model.put("kbList", kbList);
        
        if (!kbList.isEmpty()) {
            model.put("defaultKbId", kbList.get(0).getId());
        }

        if (kbId == null && !kbList.isEmpty()) {
            kbId = kbList.get(0).getId();
        }

        if (kbId != null) {
            KbBaseEntity kb = kbService.getById(kbId);
            if (kb != null) {
                model.put("selectedKb", kb);
            }
        }

        try {
            model.put("isAdmin", SysUser.isAdmin(SecurityUtils.getUserId()));
        } catch (Exception e) {
            model.put("isAdmin", false);
        }

        return "3.0/data";
    }
}