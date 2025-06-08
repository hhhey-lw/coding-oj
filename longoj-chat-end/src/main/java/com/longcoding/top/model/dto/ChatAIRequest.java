package com.longcoding.top.model.dto;

import lombok.Data;

@Data
public class ChatAIRequest {

    private String chatId;

    private String prompt;

    private String modelName;

    private Boolean isThinking;
}
