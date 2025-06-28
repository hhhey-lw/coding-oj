package com.longcoding.top.mapper;

import com.longcoding.top.model.entity.Meal;
import com.longcoding.top.model.entity.UserMeal;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author 韦龙
* @description 针对表【user_meal】的数据库操作Mapper
* @createDate 2025-06-25 15:48:35
* @Entity com.longcoding.top.model.entity.UserMeal
*/
public interface UserMealMapper extends BaseMapper<UserMeal> {

    Meal getMealByUserId(Long userId);
}




