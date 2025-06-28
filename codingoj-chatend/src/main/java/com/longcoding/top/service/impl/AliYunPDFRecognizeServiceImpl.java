package com.longcoding.top.service.impl;

import cn.hutool.core.util.StrUtil;
import com.aliyun.ocr20191230.Client;
import com.aliyun.ocr20191230.models.RecognizePdfRequest;
import com.aliyun.ocr20191230.models.RecognizePdfResponse;
import com.aliyun.ocr20191230.models.RecognizePdfResponseBody;
import com.aliyun.teautil.models.RuntimeOptions;
import com.longcoding.top.common.ErrorCode;
import com.longcoding.top.exception.BusinessException;
import com.longcoding.top.service.IPDFRecognizeService;
import com.longcoding.top.utils.RedisKeyUtil;
import com.longcoding.top.utils.UserContext;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AliYunPDFRecognizeServiceImpl implements IPDFRecognizeService {

    @Resource
    private Client client;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static Integer MAX_RECOGNIZE_COUNT_PER_DAY = 3;

    @Override
    public String recognizeTextFromPDF(String fileUrl) {
        checkRecognizeCountPerDay();

        RecognizePdfRequest recognizePdfRequest = new RecognizePdfRequest()
                .setFileURL(fileUrl);
        RuntimeOptions runtime = new RuntimeOptions();
        try {
            RecognizePdfResponse recognizePdfResponse = client.recognizePdfWithOptions(recognizePdfRequest, runtime);

            String pdfText = analysisPdfResponse(recognizePdfResponse);

            return pdfText;
        } catch (com.aliyun.tea.TeaException teaException) {
            // 获取并打印关键错误信息
            System.out.println("An API error occurred. Code: " + teaException.getCode() + ", Message: " + teaException.getMessage());

            // teaException.getData() 返回包含详细错误信息的Map，这是安全的序列化来源
            String errorDetails = com.aliyun.teautil.Common.toJSONString(teaException.getData());
            System.out.println("Error details from API: " + errorDetails);

            // 返回从API获取的详细错误信息，而不是序列化整个异常
            return errorDetails;
        } catch (Exception e) {
            System.out.println("Error occurred while recognizing PDF: " + e.getMessage());
            // 对于通用异常，返回一个简单的错误消息
            return "{\"error\": \"An unexpected error occurred\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }

    private void checkRecognizeCountPerDay() {
        Long userId = UserContext.getUserId();
        String countStr = stringRedisTemplate.opsForValue().get(RedisKeyUtil.USER_RECOGNIZE_RESUME_COUNT_KEY(userId));
        if (StrUtil.isBlank(countStr)) {
            // 如果没有记录，则初始化为0
            stringRedisTemplate.opsForValue().set(RedisKeyUtil.USER_RECOGNIZE_RESUME_COUNT_KEY(userId), "1");
        } else {
            int count = Integer.parseInt(countStr);
            if (count >= MAX_RECOGNIZE_COUNT_PER_DAY) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "超过每日识别次数限制，请明天再试");
            }
        }
    }

    private String analysisPdfResponse(RecognizePdfResponse recognizePdfResponse) {
        List<RecognizePdfResponseBody.RecognizePdfResponseBodyDataWordsInfo> wordsInfo = recognizePdfResponse.getBody().getData().getWordsInfo();

        String recognizedText = wordsInfo.stream()
                .map(RecognizePdfResponseBody.RecognizePdfResponseBodyDataWordsInfo::getWord)
                .collect(java.util.stream.Collectors.joining("\n"));
        return recognizedText;
    }

}
