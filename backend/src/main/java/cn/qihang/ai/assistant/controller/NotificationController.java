package cn.qihang.ai.assistant.controller;

import cn.qihang.ai.assistant.service.db.NotificationDbService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NotificationController {

    private final NotificationDbService notificationDbService;

    public NotificationController(NotificationDbService notificationDbService) {
        this.notificationDbService = notificationDbService;
    }

    @GetMapping("/notifications")
    public String notificationsPage(Model model) {
        model.addAttribute("currentNav", "notifications");
        model.addAttribute("currentUserId", 1L);
        model.addAttribute("unreadCount", notificationDbService.countUnread(1L));
        return "3.0/notifications";
    }
}
