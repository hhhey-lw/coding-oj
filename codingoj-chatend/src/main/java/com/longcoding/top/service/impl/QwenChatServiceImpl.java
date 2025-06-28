package com.longcoding.top.service.impl;

import com.longcoding.top.model.dto.chat.ChatAIRequest;
import com.longcoding.top.service.IChatAIService;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import org.springframework.core.io.Resource;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Service("qwenChatService")
public class QwenChatServiceImpl implements IChatAIService {

    private final ChatClient.Builder chatClientBuilder;
    private ChatClient chatClient;

    @Value("${spring.ai.dashscope.interview.options.model}")
    private String MODEL_NAME;

    @Value("classpath:system_prompt.txt")
    private Resource SYSTEM_PROMPT;
    // private String SYSTEM_PROMPT = "你现在是一名资深的Java面试官，正在通过电话面试候选人。你的目标是评估候选人的专业能力、项目经验和软技能。";


    public QwenChatServiceImpl(ChatClient.Builder builder) {
        this.chatClientBuilder = builder;
    }

    @PostConstruct
    public void init() {
        // 此时，SYSTEM_PROMPT 和 MODEL_NAME 已经通过 @Value 成功注入。
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT) // .defaultSystem(Resource) 默认使用 UTF-8 编码
                .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .defaultOptions(ChatOptions.builder()
                        .model(MODEL_NAME)
                        .build())
                .build();
    }

    @Override
    public String doChat(ChatAIRequest chatAIRequest) {
    if (chatAIRequest == null || chatAIRequest.getModelName() == null) {
            return "请求参数异常，请仔细检查！";
    }
        return chatClient.prompt(chatAIRequest.getPrompt())
                .advisors(advisorSpec -> advisorSpec
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatAIRequest.getChatId())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 200))
                .call()
                .content();
    }

    @Override
    public Flux<String> doChatStreamWithThinking(ChatAIRequest chatAIRequest) {
        return null;
    }

    @Override
    public Flux<String> doChatStream(ChatAIRequest chatAIRequest) {
        if (chatAIRequest == null || chatAIRequest.getModelName() == null) {
            return Flux.just("请求参数异常，请仔细检查！");
        }
        return chatClient.prompt(chatAIRequest.getPrompt())
                .advisors(advisorSpec -> advisorSpec
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatAIRequest.getChatId())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 200))
                .stream()
                .content();
    }

    @Override
    public Flux<String> checkRequestCount() {
        return null;
    }
}
