package com.longcoding.top.utils;

public class RedisKeyUtil {

    private static final String USER_INTERVIEW_KEY_PREFIX = "user:interview:info:";
    private static final String USER_RECOGNIZE_RESUME_COUNT_KEY = "user:recognize:resume:";

    public static String USER_RESUME_INFO_KEY(Long userId) {
        return USER_INTERVIEW_KEY_PREFIX + userId;
    }

    public static String USER_RECOGNIZE_RESUME_COUNT_KEY(Long userId) {
        return USER_RECOGNIZE_RESUME_COUNT_KEY + userId;
    }

}
