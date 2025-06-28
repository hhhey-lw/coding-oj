package com.longcoding.top.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.longcoding.top.model.entity.InterviewRecords;
import com.longcoding.top.service.InterviewRecordsService;
import com.longcoding.top.mapper.InterviewRecordsMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
* @author 韦龙
* @description 针对表【interview_records】的数据库操作Service实现
* @createDate 2025-06-25 15:48:34
*/
@Service
public class InterviewRecordsServiceImpl extends ServiceImpl<InterviewRecordsMapper, InterviewRecords>
    implements InterviewRecordsService{

    @Override
    @Deprecated
    public List<InterviewRecords> getRecordsByUserId(Long userId) {
        return List.of();
    }

    @Override
    public List<InterviewRecords> getRecordsOfCurrentDayByUserId(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        return this.list(getQueryWrapper(userId));
    }

    private LambdaQueryWrapper<InterviewRecords> getQueryWrapper(Long userId) {
        // 获取当天的开始时间和结束时间
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startOfDay = calendar.getTime();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        Date endOfDay = calendar.getTime();

        // 构建查询条件
        LambdaQueryWrapper<InterviewRecords> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InterviewRecords::getUserId, userId)
                .ge(InterviewRecords::getCreateTime, startOfDay)
                .le(InterviewRecords::getCreateTime, endOfDay)
                .orderByAsc(InterviewRecords::getCreateTime);

        return queryWrapper;
    }

    @Override
    public Long getRecordCountOfCurrentDayByUserId(Long userId) {
        if (userId == null) {
            return 0L;
        }
        return this.count(getQueryWrapper(userId));
    }

    @Override
    public boolean addInterviewRecord(InterviewRecords record) {
        if (record == null || record.getUserId() == null) {
            return false; // 无效的记录
        }
        return this.save(record); // 使用 MyBatis-Plus 的 save 方法添加记录
    }

    @Override
    public boolean updateInterviewRecord(InterviewRecords record) {
        if (record == null || record.getInterviewId() == null) {
            return false; // 无效的记录
        }
        return this.updateById(record); // 使用 MyBatis-Plus 的 updateById 方法更新记录
    }
}




