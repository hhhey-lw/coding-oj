package com.longcoding.top.service;

public interface IInterviewService {

    boolean checkUserMeal(Long userId);

    Long addInterviewRecord(Long userId);

    void finishInterview(Long userId, Long interviewId);

    Integer getMealDurationTimeByUserId(Long userId);

    String getInitPromptByUserId(Long userId);
}
