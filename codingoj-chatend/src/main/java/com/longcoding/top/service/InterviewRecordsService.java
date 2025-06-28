package com.longcoding.top.service;

import com.longcoding.top.model.entity.InterviewRecords;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 韦龙
* @description 针对表【interview_records】的数据库操作Service
* @createDate 2025-06-25 15:48:34
*/
public interface InterviewRecordsService extends IService<InterviewRecords> {

    /**
     * 根据用户ID查询面试记录
     * @param userId 用户ID
     * @return 面试记录列表
     */
    List<InterviewRecords> getRecordsByUserId(Long userId);

    List<InterviewRecords> getRecordsOfCurrentDayByUserId(Long userId);

    Long getRecordCountOfCurrentDayByUserId(Long userId);

    /**
     * 添加新的面试记录
     * @param record 面试记录实体
     * @return 是否添加成功
     */
    boolean addInterviewRecord(InterviewRecords record);

    boolean updateInterviewRecord(InterviewRecords record);
}
