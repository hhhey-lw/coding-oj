package com.longcoding.top.utils;

import com.longcoding.top.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Date;

/**
 * Cos 对象存储操作
 *
 */
@Component
public class CosUtil {

    public final static String COS_HOST = "http://longoj-1350136079.cos.ap-nanjing.myqcloud.com";

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key 唯一键
     * @param localFilePath 本地文件路径
     * @return
     */
    public PutObjectResult putObject(String key, String localFilePath) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                new File(localFilePath));
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 上传对象
     *
     * @param key 唯一键
     * @param file 文件
     * @return
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        return cosClient.putObject(putObjectRequest);
    }

    public PutObjectResult putObjectExpireOneDay(String key, File file) {

        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);

        // 创建对象元数据
        ObjectMetadata metadata = new ObjectMetadata();
        // 设置 HTTP 头部的 Expires 字段，即过期时间
        metadata.setHttpExpiresDate(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24)); // 1天后过期
        putObjectRequest.setMetadata(metadata);

        return cosClient.putObject(putObjectRequest);
    }
}
