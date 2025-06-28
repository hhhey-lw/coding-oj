package com.longcoding.top.longojchatend.interview;

import com.longcoding.top.service.ISpeechSynthesizeService;
import jakarta.annotation.Resource;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SpeechSynthesizeTest {

    @Resource
    private ISpeechSynthesizeService ttsService;

    // @Test
    public void testSynthesizeSpeech() {
        String result = ttsService.synthesizeSpeech("同学，你好。请你先介绍一下自己。", "zh-CN-XiaoxiaoNeural");
        System.out.println("合成语音结果: " + result);
    }

}
