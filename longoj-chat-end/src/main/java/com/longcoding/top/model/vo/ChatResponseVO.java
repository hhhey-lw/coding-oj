package com.longcoding.top.model.vo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatResponseVO {

    private Integer code;

    private String type;

    private String msg;

    public ChatResponseVO() {
    }

    public ChatResponseVO(Integer code, String type, String msg) {
        this.code = code;
        this.type = type;
        this.msg = msg;
    }

    public String strFormat() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize ChatResponseVO", e);
        }
    }
}
