package com.longcoding.top.controller;

import cn.hutool.json.JSONUtil;
import com.longcoding.top.model.dto.ChatAIRequest;
import com.longcoding.top.service.IChatAIService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;


@RestController
public class ChatClientController {

    private static final Logger log = LoggerFactory.getLogger(ChatClientController.class);

    @Resource(name = "deepSeekChatService")
    private IChatAIService iChatAIService;

    @PostMapping(value = "/chat", produces = "text/event-stream")
    public Flux<String> streamChat(
            @RequestBody ChatAIRequest chatAIRequest,
            HttpServletResponse response
    ) {
        response.setCharacterEncoding("UTF-8");
        log.debug(JSONUtil.toJsonStr(chatAIRequest));

        Flux<String> res = iChatAIService.checkRequestCount();
        if (res != null)
            return res;

        if (chatAIRequest.getIsThinking()) {
            return iChatAIService.doChatStreamWithThinking(chatAIRequest);
        } else {
            return iChatAIService.doChatStream(chatAIRequest);
        }
    }

}
