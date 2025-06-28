package com.longcoding.top.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.longcoding.top.model.dto.interview.UserResumeInfoDTO;
import com.longcoding.top.model.entity.UserResumeInfo;
import com.longcoding.top.model.vo.UserResumeInfoVO;
import com.longcoding.top.service.UserResumeInfoService;
import com.longcoding.top.mapper.UserResumeInfoMapper;
import com.longcoding.top.utils.RedisKeyUtil;
import com.longcoding.top.utils.UserContext;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
* @author 韦龙
* @description 针对表【user_resume_info】的数据库操作Service实现
* @createDate 2025-06-25 15:48:35
*/
@Service
public class UserResumeInfoServiceImpl extends ServiceImpl<UserResumeInfoMapper, UserResumeInfo>
    implements UserResumeInfoService{

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public UserResumeInfo getByUserIdFromDB(Long userId) {
        return lambdaQuery()
                .eq(UserResumeInfo::getUserId, userId)
                .one();
    }

    @Override
    public UserResumeInfo getByUserId(Long userId) {
        String jsonStr = stringRedisTemplate.opsForValue().get(RedisKeyUtil.USER_RESUME_INFO_KEY(userId));
        if (jsonStr != null) {
            return JSONUtil.toBean(jsonStr, UserResumeInfo.class);
        }
        return getByUserIdFromDB(userId);
    }


    @Override
    public boolean startUploadInterviewOfUserResumeInfo(UserResumeInfoDTO userResumeInfoDTO) {
        Long userId = UserContext.getUserId();
        UserResumeInfo userResumeInfo = new UserResumeInfo();
        userResumeInfo.setUserId(userId);
        userResumeInfo.setPositionName(userResumeInfoDTO.getPositionName());
        userResumeInfo.setResponsibilities(userResumeInfoDTO.getResponsibilities());
        userResumeInfo.setResumeInformation(userResumeInfoDTO.getResumeInformation());
        userResumeInfo.setCreateTime(new Date());
        userResumeInfo.setUpdateTime(new Date());
        UserResumeInfo resumeInfo = getByUserIdFromDB(userId);
        boolean ok;
        if (resumeInfo == null) {
            ok = save(userResumeInfo);
        } else {
            ok = lambdaUpdate()
                    .set(UserResumeInfo::getPositionName, userResumeInfo.getPositionName())
                    .set(UserResumeInfo::getResponsibilities, userResumeInfo.getResponsibilities())
                    .set(UserResumeInfo::getResumeInformation, userResumeInfo.getResumeInformation())
                    .eq(UserResumeInfo::getUserId, userId)
                    .update();
        }
        if (ok) {
            stringRedisTemplate.opsForValue().set(RedisKeyUtil.USER_RESUME_INFO_KEY(userId), JSONUtil.toJsonStr(userResumeInfoDTO));
            return true;
        }
        return false;
    }

    @Override
    public UserResumeInfoVO getResumeVOByUserId(Long userId) {
        UserResumeInfo resumeInfo = getByUserId(userId);
        UserResumeInfoVO userResumeInfoVO = new UserResumeInfoVO();
        userResumeInfoVO.setPositionName(resumeInfo.getPositionName());
        userResumeInfoVO.setResponsibilities(resumeInfo.getResponsibilities());
        userResumeInfoVO.setResumeInformation(resumeInfo.getResumeInformation());
        return userResumeInfoVO;
    }
}




