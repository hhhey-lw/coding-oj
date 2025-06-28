package com.longcoding.top.service;

import com.longcoding.top.model.dto.interview.UserResumeInfoDTO;
import com.longcoding.top.model.entity.UserResumeInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.longcoding.top.model.vo.UserResumeInfoVO;

/**
* @author 韦龙
* @description 针对表【user_resume_info】的数据库操作Service
* @createDate 2025-06-25 15:48:35
*/
public interface UserResumeInfoService extends IService<UserResumeInfo> {
    UserResumeInfo getByUserIdFromDB(Long userId);
    UserResumeInfo getByUserId(Long userId);

    boolean startUploadInterviewOfUserResumeInfo(UserResumeInfoDTO userResumeInfoDTO);

    UserResumeInfoVO getResumeVOByUserId(Long userId);
}
