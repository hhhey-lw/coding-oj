package com.longcoding.top.controller;

import com.longcoding.top.common.BaseResponse;
import com.longcoding.top.utils.ResultUtils;
import com.longcoding.top.utils.JwtTokenUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/ping")
    public BaseResponse<String> ping() {
        return ResultUtils.success("pong");
    }

    @GetMapping("/token")
    public BaseResponse<String> getToken() {
        return ResultUtils.success(JwtTokenUtil.generateToken(1L));
    }

}
