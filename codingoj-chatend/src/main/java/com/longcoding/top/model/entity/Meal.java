package com.longcoding.top.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName meal
 */
@TableName(value ="meal")
@Data
public class Meal implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer mealId;

    /**
     * 
     */
    private String name;

    /**
     * 
     */
    private String description;

    /**
     * 
     */
    private BigDecimal price;

    /**
     * 
     */
    private Integer maxInterviewsPerDay;

    /**
     * 
     */
    private Integer maxDurationPerInterview;

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