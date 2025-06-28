package com.longcoding.top.longojchatend.interview;

import com.longcoding.top.service.ISpeechRecognizeService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SpeechRecognizeTest {

    @Resource
    private ISpeechRecognizeService speechRecognizeService;

    // @Test
    public void testSpeechRecognize() {
        // 测试语音识别功能
        // 这里可以调用ISpeechRecognizeService的实现类方法进行测试
        String result = speechRecognizeService.speechRecognize("https://dashscope.oss-cn-beijing.aliyuncs.com/samples/audio/paraformer/hello_world_female2.wav");
        System.out.println("识别结果: " + result);
    }

}
