package cn.qihang.ai.assistant.controller;

import cn.qihang.ai.assistant.service.db.ApprovalRequestDbService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ApprovalController {

    private final ApprovalRequestDbService approvalRequestDbService;

    public ApprovalController(ApprovalRequestDbService approvalRequestDbService) {
        this.approvalRequestDbService = approvalRequestDbService;
    }

    @GetMapping("/approvals")
    public String approvalsPage(Model model) {
        model.addAttribute("currentNav", "approvals");
        model.addAttribute("currentUserId", 1L);
        return "3.0/approvals";
    }
}
