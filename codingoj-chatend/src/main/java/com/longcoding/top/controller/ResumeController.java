package com.longcoding.top.controller;

import com.longcoding.top.common.BaseResponse;
import com.longcoding.top.model.entity.UserResumeInfo;
import com.longcoding.top.model.vo.UserResumeInfoVO;
import com.longcoding.top.service.UserResumeInfoService;
import com.longcoding.top.utils.ResultUtils;
import com.longcoding.top.model.dto.interview.UserResumeInfoDTO;
import com.longcoding.top.service.IPDFRecognizeService;
import com.longcoding.top.utils.UserContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/resume")
public class ResumeController {

    @Resource
    private IPDFRecognizeService pdfRecognizeService;

    @Resource
    private UserResumeInfoService userResumeInfoService;

    @PostMapping("/start/upload/info")
    public BaseResponse<String> startUploadInfo(UserResumeInfoDTO userResumeInfoDTO) {
        log.info("startUploadInfo for resume");
        // Here you would typically return some information about the upload process
        // For demonstration, we return a simple string
        return ResultUtils.success(userResumeInfoService.startUploadInterviewOfUserResumeInfo(userResumeInfoDTO)
        ? "Upload started successfully" : "Failed to start upload");
    }

    @GetMapping("/get/info")
    public BaseResponse<UserResumeInfoVO> getResumeInfo() {
        log.info("Getting resume info");
        return ResultUtils.success(userResumeInfoService.getResumeVOByUserId(UserContext.getUserId()));
    }

    @GetMapping("/recognize/pdf")
    public BaseResponse<String> recognizeResume(String resumeFileUrl) {
        log.info("Recognizing resume from file URL: {}", resumeFileUrl);
        // Here you would typically call a service to recognize the resume
        // For demonstration, we return a simple string
        return ResultUtils.success(pdfRecognizeService.recognizeTextFromPDF(resumeFileUrl));
    }

}
