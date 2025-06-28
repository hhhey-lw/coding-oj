package com.longcoding.top.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AliyunClientConfig {

    @Value("${aliyun.ocr.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.ocr.accessKeySecret}")
    private String accessKeySecret;

    @Bean
    public com.aliyun.ocr20191230.Client createClient() throws Exception {
        /*
          初始化配置对象com.aliyun.teaopenapi.models.Config
          Config对象存放 AccessKeyId、AccessKeySecret、endpoint等配置
         */
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret);
        // 访问的域名
        config.endpoint = "ocr.cn-shanghai.aliyuncs.com";
        return new com.aliyun.ocr20191230.Client(config);
    }

}
