package com.longcoding.top.longojchatend.interview;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.longcoding.top.model.entity.InterviewRecords;
import com.longcoding.top.model.entity.Meal;
import com.longcoding.top.service.InterviewRecordsService;
import com.longcoding.top.service.UserMealService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

@SpringBootTest
public class InterViewTest {

    @Resource
    private UserMealService userMealService;

    @Resource
    private InterviewRecordsService interviewRecordsService;

    // @Test
    public void testUserMeal() {
        // 测试用例逻辑
        // 这里可以添加对 UserMeal 类的具体测试逻辑
        // 例如，创建 UserMeal 实例，调用方法，断言结果等

        Meal meal = userMealService.getMealByUserId(1L);
        System.out.println(JSONUtil.toJsonStr(meal));
    }

    // @Test
    public void testInterViewRecords() throws InterruptedException {
        InterviewRecords interviewRecords = new InterviewRecords();
        interviewRecords.setUserId(1L);
        interviewRecords.setInterviewType(0);
        interviewRecords.setMealId(1);
        interviewRecords.setStartTime(new Date());
        interviewRecords.setStatus(0);

        interviewRecords.setCreateTime(new Date());

        interviewRecordsService.addInterviewRecord(interviewRecords);

        System.out.println(interviewRecordsService.getRecordCountOfCurrentDayByUserId(1L));

        for (InterviewRecords records : interviewRecordsService.getRecordsOfCurrentDayByUserId(1L)) {
            System.out.println(JSONUtil.toJsonStr(records));
        }

        Thread.sleep(2000);

        interviewRecords.setStatus(1);
        interviewRecords.setEndTime(new Date());
        interviewRecordsService.updateInterviewRecord(interviewRecords);

    }

}
