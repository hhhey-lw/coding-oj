package com.longcoding.top.service;

import com.longcoding.top.model.dto.chat.ChatAIRequest;
import reactor.core.publisher.Flux;

public interface IChatAIService {

    String doChat(ChatAIRequest chatAIRequest);

    Flux<String> doChatStreamWithThinking(ChatAIRequest chatAIRequest);

    Flux<String> doChatStream(ChatAIRequest chatAIRequest);

    Flux<String> checkRequestCount();
}
