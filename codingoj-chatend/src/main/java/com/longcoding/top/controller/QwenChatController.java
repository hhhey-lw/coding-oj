package com.longcoding.top.controller;

import com.longcoding.top.model.dto.chat.ChatAIRequest;
import com.longcoding.top.service.IChatAIService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(("/chat"))
public class QwenChatController {

    @Resource(name = "qwenChatService")
    private IChatAIService chatAIService;

    @GetMapping("/qwen")
    public String doChat(ChatAIRequest chatAIRequest) {
        log.info("Received chat request with prompt: {}", chatAIRequest.getPrompt());
        return chatAIService.doChat(chatAIRequest);
    }

}
