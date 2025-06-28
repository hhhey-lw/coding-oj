package com.longcoding.top.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName interview_records
 */
@TableName(value ="interview_records")
@Data
public class InterviewRecords implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Long interviewId;

    /**
     * 
     */
    private Long userId;

    /**
     * 
     */
    private Integer mealId;

    /**
     * 
     */
    private Integer interviewType;

    /**
     * 
     */
    private Date startTime;

    /**
     * 
     */
    private Date endTime;

    /**
     * 
     */
    private Integer durationSeconds;

    /**
     * 
     */
    private Integer status;

    /**
     * 
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}