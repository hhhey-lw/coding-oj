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
 * @TableName user_resume_info
 */
@TableName(value ="user_resume_info")
@Data
public class UserResumeInfo implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer userResumeId;

    /**
     * 
     */
    private Long userId;

    /**
     * 
     */
    private String positionName;

    /**
     * 
     */
    private String responsibilities;

    /**
     * 
     */
    private String resumeInformation;

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