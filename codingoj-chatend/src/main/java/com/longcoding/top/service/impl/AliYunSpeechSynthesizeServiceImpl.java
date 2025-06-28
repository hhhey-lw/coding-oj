package com.longcoding.top.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.longcoding.top.model.enums.SpeechResponseStatusEnum;
import com.longcoding.top.model.vo.SpeechResponseVO;
import com.longcoding.top.service.ISpeechSynthesizeService;
import io.reactivex.Flowable;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.concurrent.Semaphore;

@Service
public class AliYunSpeechSynthesizeServiceImpl implements ISpeechSynthesizeService {

    private static final Logger log = LoggerFactory.getLogger(AliYunSpeechSynthesizeServiceImpl.class);

    @Value("${spring.ai.dashscope.api-key}")
    private String API_KEY;

    @Value("${spring.ai.dashscope.speech-synthesis.options.model}")
    private String TTS_MODEL;

    @Value("${spring.ai.dashscope.speech-synthesis.options.voice}")
    private String TTS_VOICE;

    @Override
    public String synthesizeSpeech(String text, String voice) {
        try {
            MultiModalConversation conv = new MultiModalConversation();
            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .model(TTS_MODEL)
                    .text(text)
                    .voice(AudioParameters.Voice.ETHAN)
                    .build();
            MultiModalConversationResult result = conv.call(param);
            log.info("QWen TTS synthesis result: {}", JsonUtils.toJson(result));
            return result.getOutput().getAudio().getUrl();
        } catch (NoApiKeyException e) {
            log.info("QWen TTS API key is not set, please configure it in application.yml, {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (UploadFileException e) {
            log.info("QWen TTS upload file error, please check the file size and format, {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public Flowable<String> flowableSynthesizeSpeech(String text, String voice) {
        try {
            MultiModalConversation conv = new MultiModalConversation();
            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .model(TTS_MODEL)
                    .text(text)
                    .voice(AudioParameters.Voice.ETHAN)
                    .build();
            Flowable<MultiModalConversationResult> multiModalConversationResultFlowable = conv.streamCall(param);
            return multiModalConversationResultFlowable.map(result -> {
                String data = result.getOutput().getAudio().getData();
                if (StrUtil.isNotBlank(data)) {
                    SpeechResponseVO response = new SpeechResponseVO();
                    response.setCode(SpeechResponseStatusEnum.SPEECH_DATA.getCode());
                    response.setMessage(SpeechResponseStatusEnum.SPEECH_DATA.getMessage());
                    response.setData(data);
                    return JsonUtils.toJson(response);
                }
                return StrUtil.EMPTY;
            }).filter(StrUtil::isNotBlank);
        } catch (NoApiKeyException e) {
            log.info("QWen TTS API key is not set, please configure it in application.yml, {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (UploadFileException e) {
            log.info("QWen TTS upload file error, please check the file size and format, {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public Flowable<String> flowableSynthesizeSpeech(Flux<String> text, String voice) {
        /* 这里支持流式调用的模型好贵 */
        return null;
    }

    @PostConstruct
    private void init() {
        Constants.apiKey = API_KEY;
    }
}
