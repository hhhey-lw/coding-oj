package com.longcoding.top.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.longcoding.top.model.entity.Meal;
import com.longcoding.top.model.entity.UserMeal;
import com.longcoding.top.service.MealService;
import com.longcoding.top.service.UserMealService;
import com.longcoding.top.mapper.UserMealMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
* @author 韦龙
* @description 针对表【user_meal】的数据库操作Service实现
* @createDate 2025-06-25 15:48:35
*/
@Service
public class UserMealServiceImpl extends ServiceImpl<UserMealMapper, UserMeal>
    implements UserMealService{

    @Override
    public boolean addUserMealFreeVersion(Long userId) {
        UserMeal userMeal = new UserMeal();
        userMeal.setUserId(userId);
        // 1是免费版本的套餐ID
        userMeal.setMealId(1);
        userMeal.setStatus(0);
        userMeal.setCreateTime(new Date());
        userMeal.setStartDate(new Date());

        return save(userMeal);
    }

    @Override
    public Meal getMealByUserId(Long userId) {
        Meal meal = baseMapper.getMealByUserId(userId);
        if (meal == null) {
            addUserMealFreeVersion(userId);
            return baseMapper.getMealByUserId(userId);
        }
        return meal;
    }


}




