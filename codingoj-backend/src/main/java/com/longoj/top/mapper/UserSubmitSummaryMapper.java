package com.longoj.top.mapper;

import com.longoj.top.model.entity.UserSubmitSummary;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserSubmitSummaryMapper {

    UserSubmitSummary querySubmitSummary(Long userId, String yyyyMMDD);

    boolean updateSubmitSummary(Long userId, String yyyyMMDD, Integer submitCount, Integer acceptedCount);

    boolean addSubmitSummary(Long userId, String yyyyMMDD);
}
