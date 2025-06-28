package com.longcoding.top.service;

import com.longcoding.top.model.entity.Meal;
import com.longcoding.top.model.entity.UserMeal;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 韦龙
* @description 针对表【user_meal】的数据库操作Service
* @createDate 2025-06-25 15:48:35
*/
public interface UserMealService extends IService<UserMeal> {
    boolean addUserMealFreeVersion(Long userId);

    Meal getMealByUserId(Long userId);
}
