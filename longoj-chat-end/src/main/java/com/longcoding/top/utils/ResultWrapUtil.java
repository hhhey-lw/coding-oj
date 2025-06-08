package com.longcoding.top.utils;

import cn.hutool.json.JSONUtil;
import com.longcoding.top.model.enums.ChatResponseStatus;
import com.longcoding.top.model.enums.ChatResponseType;
import com.longcoding.top.model.vo.ChatResponseVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;
import cn.hutool.core.util.StrUtil;

public class ResultWrapUtil<T> {

    private static final String REASONING_CONTENT = "reasoningContent";
    private static final Logger log = LoggerFactory.getLogger(ResultWrapUtil.class);

    public static Flux<String> wrapContentOrThinking(Flux<ChatResponse> chatResponseFlux) {
        return chatResponseFlux.map(chatResponse -> {
            if (StrUtil.isNotBlank(chatResponse.getResult().getOutput().getMetadata().get(REASONING_CONTENT).toString())) {
                ChatResponseVO response = new ChatResponseVO(ChatResponseStatus.SUCCESS, ChatResponseType.THINKING,
                        chatResponse.getResult().getOutput().getMetadata().get(REASONING_CONTENT).toString());
                return response.strFormat();
            }
            if (StrUtil.isNotBlank(chatResponse.getResult().getOutput().getText())) {
                ChatResponseVO response = new ChatResponseVO(ChatResponseStatus.SUCCESS, ChatResponseType.CONTENT,
                        chatResponse.getResult().getOutput().getText());
                return response.strFormat();
            }
            return StrUtil.EMPTY;
        }).filter(StrUtil::isNotBlank);
    }

    public static Flux<String> wrapContent(Flux<String> content) {
        return content.map(str -> {
            ChatResponseVO response = new ChatResponseVO(ChatResponseStatus.SUCCESS, ChatResponseType.CONTENT, str);
            return response.strFormat();
        });
    }

    public static Flux<String> error() {
        return Flux.just(new ChatResponseVO(ChatResponseStatus.FAIL, ChatResponseType.ERROR, StrUtil.EMPTY).strFormat());
    }
}
