package com.longcoding.top.utils;

public class UserContext {

    private static final ThreadLocal<Long> userIdHolder = new ThreadLocal<>();

    // 设置当前线程的 Token
    public static void setUserId(Long userId) {
        userIdHolder.set(userId);
    }

    // 获取当前线程的 Token
    public static Long getUserId() {
        return userIdHolder.get();
    }

    // 清除当前线程的 Token（防止内存泄漏）
    public static void clearToken() {
        userIdHolder.remove();
    }

}
