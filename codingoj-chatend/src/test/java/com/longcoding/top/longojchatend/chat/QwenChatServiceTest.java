package com.longcoding.top.longojchatend.chat;

import com.longcoding.top.model.dto.chat.ChatAIRequest;
import com.longcoding.top.service.IChatAIService;
import com.longcoding.top.service.impl.QwenChatServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class QwenChatServiceTest {

    @Resource(name = "qwenChatService")
    private IChatAIService chatAIService;

    @Test
    public void testDoChat() {
        ChatAIRequest chatAIRequest = new ChatAIRequest();
        chatAIRequest.setModelName("qwen-7b-chat");
        chatAIRequest.setChatId("qwen-7b-chat");
        chatAIRequest.setPrompt("你好呀");
        chatAIRequest.setIsThinking(false);
        System.out.println(chatAIService.doChat(chatAIRequest));
    }

}
