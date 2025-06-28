package com.longcoding.top.model.enums;

public enum SpeechResponseStatusEnum {

    SUCCESS(200, "Success"),
    FAIL(500, "Fail"),

    SPEECH_DATA(200, "speech"),
    RECOGNIZE_INTERMEDIATE_DATA(200, "recognize-intermediate"),
    RECOGNIZE_FINAL_DATA(200, "recognize-final"),
    SPEECH_TEXT(200, "text"),

    RECOGNIZE_OUT_MAX_TIME(408, "recognize-out-max-time"),;

    private final Integer code;
    private final String message;

    SpeechResponseStatusEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
