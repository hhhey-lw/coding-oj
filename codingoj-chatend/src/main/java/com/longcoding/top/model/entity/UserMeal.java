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
 * @TableName user_meal
 */
@TableName(value ="user_meal")
@Data
public class UserMeal implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer userMealId;

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
    private Date startDate;

    /**
     * 
     */
    private Date endDate;

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