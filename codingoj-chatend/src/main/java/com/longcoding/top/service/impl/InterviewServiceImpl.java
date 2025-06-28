package com.longcoding.top.service.impl;

import cn.hutool.json.JSONUtil;
import com.longcoding.top.model.entity.InterviewRecords;
import com.longcoding.top.model.entity.Meal;
import com.longcoding.top.model.entity.UserResumeInfo;
import com.longcoding.top.service.IInterviewService;
import com.longcoding.top.service.InterviewRecordsService;
import com.longcoding.top.service.UserMealService;
import com.longcoding.top.service.UserResumeInfoService;
import com.longcoding.top.utils.RedisKeyUtil;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class InterviewServiceImpl implements IInterviewService {

    @Resource
    private UserMealService userMealService;

    @Resource
    private InterviewRecordsService interviewRecordsService;

    @Resource
    private UserResumeInfoService userResumeInfoService;

    @Override
    public boolean checkUserMeal(Long userId) {
        Meal meal = userMealService.getMealByUserId(userId);
        Long interviewCount = interviewRecordsService.getRecordCountOfCurrentDayByUserId(userId);
        if (interviewCount == null) {
            interviewCount = 0L; // 如果没有记录，默认为0
        }
        return meal.getMaxInterviewsPerDay() > interviewCount;
    }

    @Override
    public Long addInterviewRecord(Long userId) {
        InterviewRecords interviewRecords = new InterviewRecords();
        interviewRecords.setUserId(userId);
        // 模拟面试
        interviewRecords.setInterviewType(0);
        interviewRecords.setMealId(userMealService.getMealByUserId(userId).getMealId());
        interviewRecords.setStartTime(new Date());
        // 进行中
        interviewRecords.setStatus(0);

        interviewRecords.setCreateTime(new Date());

        interviewRecordsService.addInterviewRecord(interviewRecords);

        return interviewRecords.getInterviewId();
    }

    @Override
    public void finishInterview(Long userId, Long interviewId) {
        InterviewRecords interviewRecords = new InterviewRecords();

        interviewRecords.setInterviewId(interviewId);
        interviewRecords.setEndTime(new Date());
        // 已完成
        interviewRecords.setStatus(1);

        interviewRecordsService.updateInterviewRecord(interviewRecords);
    }

    @Override
    public Integer getMealDurationTimeByUserId(Long userId) {
        return userMealService.getMealByUserId(userId).getMaxDurationPerInterview();
    }

    @Override
    public String getInitPromptByUserId(Long userId) {
        UserResumeInfo resumeInfo = userResumeInfoService.getByUserId(userId);

        String positionPrompt = "面试职位：" + resumeInfo.getPositionName() +
                "\n岗位职责：" + resumeInfo.getResponsibilities();
        String resumePrompt = "用户简历：" + resumeInfo.getResumeInformation();

        return positionPrompt + "\n" +
                resumePrompt + "\n" +
                "现在开始面试，请你先让面试者做自我介绍。";
    }
}
