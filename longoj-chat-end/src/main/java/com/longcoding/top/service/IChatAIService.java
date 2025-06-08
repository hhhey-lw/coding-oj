package com.longcoding.top.service;

import com.longcoding.top.model.dto.ChatAIRequest;
import reactor.core.publisher.Flux;

public interface IChatAIService {

    Flux<String> doChatStreamWithThinking(ChatAIRequest chatAIRequest);

    Flux<String> doChatStream(ChatAIRequest chatAIRequest);

    Flux<String> checkRequestCount();
}
