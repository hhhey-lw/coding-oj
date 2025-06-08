package com.longcoding.top.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.longcoding.top.model.dto.ChatAIRequest;
import com.longcoding.top.model.enums.ChatResponseStatus;
import com.longcoding.top.model.enums.ChatResponseType;
import com.longcoding.top.model.vo.ChatResponseVO;
import com.longcoding.top.service.IChatAIService;
import com.longcoding.top.utils.ResultWrapUtil;
import com.longcoding.top.utils.UserContext;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import cn.hutool.json.JSONUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Service("deepSeekChatService")
public class DeepSeekChatServiceImpl implements IChatAIService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekChatServiceImpl.class);

    private final Integer perDayChatCount = 20;

    private final Set<String> modelList = Set.of("deepseek-r1", "deepseek-v3");

    private final ChatClient chatClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public DeepSeekChatServiceImpl(@Qualifier("dashscopeChatModel") DashScopeChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .build();
    }

    @Override
    public Flux<String> doChatStreamWithThinking(ChatAIRequest chatAIRequest) {
        if (chatAIRequest == null || !modelList.contains(chatAIRequest.getModelName())) {
            return ResultWrapUtil.error();
        }

        Flux<ChatResponse> chatResponseFlux = chatClient.prompt(chatAIRequest.getPrompt())
                .options(DashScopeChatOptions.builder()
                        .withModel(chatAIRequest.getModelName())
                        .build())
                .advisors(advisorSpec -> advisorSpec
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatAIRequest.getChatId())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
                .stream()
                .chatResponse();

        return ResultWrapUtil.wrapContentOrThinking(chatResponseFlux);
    }

    @Override
    public Flux<String> doChatStream(ChatAIRequest chatAIRequest) {
        if (chatAIRequest == null || !modelList.contains(chatAIRequest.getModelName())) {
            return ResultWrapUtil.error();
        }

        Flux<String> content = chatClient.prompt(chatAIRequest.getPrompt())
                .options(DashScopeChatOptions.builder()
                        .withModel(chatAIRequest.getModelName())
                        .build())
                .advisors(advisorSpec -> advisorSpec
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatAIRequest.getChatId())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
                .stream()
                .content();

        return ResultWrapUtil.wrapContent(content);
    }

    @Override
    public Flux<String> checkRequestCount() {
        Long userId = UserContext.getUserId();

        // 1. 获取当前日期（年月日）
        LocalDate currentDate = LocalDate.now();
        // 2. 定义格式化器（yyyyMMdd）
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        // 3. 格式化日期
        String formattedDate = currentDate.format(formatter);

        String chatCountKey = "chat:count:" + formattedDate + ":" + userId;
        String count = stringRedisTemplate.opsForValue().get(chatCountKey);
        if (StrUtil.isBlank(count)) {
            synchronized (userId.toString().intern()) {
                count = stringRedisTemplate.opsForValue().get(chatCountKey);
                if (StrUtil.isBlank(count))
                    stringRedisTemplate.opsForValue().set(chatCountKey, perDayChatCount.toString(), 24, TimeUnit.HOURS);
            }
        } else {
            if (Integer.valueOf(count) <= 0){
                return Flux.just(new ChatResponseVO(ChatResponseStatus.FAIL, ChatResponseType.CONTENT, "今日次数已耗尽").strFormat());
            }
            Long decrement = stringRedisTemplate.opsForValue().decrement(chatCountKey);
            if (decrement < 0) {
                return Flux.just(new ChatResponseVO(ChatResponseStatus.FAIL, ChatResponseType.CONTENT, "今日次数已耗尽").strFormat());
            }
        }
        return null;
    }
}
