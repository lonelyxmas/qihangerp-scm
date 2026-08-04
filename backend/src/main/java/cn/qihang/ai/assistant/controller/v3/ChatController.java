package cn.qihang.ai.assistant.controller.v3;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/")
public class ChatController {

//    @GetMapping("")
//    public String index(Map<String, Object> model) {
//        return chatPage(model);
//    }

    @GetMapping("/chat")
    public String chatPage(Map<String, Object> model) {
        model.put("currentNav", "chat");
        return "3.0/chat";
    }
}