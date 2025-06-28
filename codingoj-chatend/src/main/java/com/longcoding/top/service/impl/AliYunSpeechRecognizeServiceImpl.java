package com.longcoding.top.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.transcription.*;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.nacos.shaded.com.google.gson.*;
import com.longcoding.top.common.ErrorCode;
import com.longcoding.top.exception.BusinessException;
import com.longcoding.top.model.enums.SpeechResponseStatusEnum;
import com.longcoding.top.model.vo.SpeechResponseVO;
import com.longcoding.top.service.ISpeechRecognizeService;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AliYunSpeechRecognizeServiceImpl implements ISpeechRecognizeService {

    private static final Logger log = LoggerFactory.getLogger(AliYunSpeechRecognizeServiceImpl.class);

    @Value("${spring.ai.dashscope.api-key}")
    private String DASH_SCOPE_API_KEY;
    @Value("${spring.ai.dashscope.speech-recognize.options.model}")
    private String SPEECH_RECOGNITION_MODEL;

    private Map<String, Recognition> recognitionMap;

    public AliYunSpeechRecognizeServiceImpl() {
        this.recognitionMap = new ConcurrentHashMap<>();
    }

    @Override
    public boolean stopRecognize(String sessionId) {
        if (recognitionMap.containsKey(sessionId)) {
            Recognition recognition = recognitionMap.remove(sessionId);
            recognition.stop();
            recognition = null;
            return true;
        }
        return false;
    }

    @Override
    public String speechRecognize(String speechFileURL) {
        // 创建转写请求参数
        TranscriptionParam param =
                TranscriptionParam.builder()
                        .apiKey(DASH_SCOPE_API_KEY)
                        .model(SPEECH_RECOGNITION_MODEL)
                        .parameter("language_hints", new String[]{"zh", "en"})
                        .fileUrls(Arrays.asList(speechFileURL))
                        .build();
        try {
            Transcription transcription = new Transcription();
            // 提交转写请求
            TranscriptionResult result = transcription.asyncCall(param);
            System.out.println("RequestId: " + result.getRequestId());
            // 阻塞等待任务完成并获取结果
            result = transcription.wait(
                    TranscriptionQueryParam.FromTranscriptionParam(param, result.getTaskId()));
            // 解析转写结果
            List<TranscriptionTaskResult> taskResultList = result.getResults();
            if (taskResultList != null && !taskResultList.isEmpty()) {
                // 这里只支持单文件 语音转文本服务 get(0)
                String transcriptionUrl = taskResultList.get(0).getTranscriptionUrl();
                HttpURLConnection connection =
                        (HttpURLConnection) new URL(transcriptionUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder resultBuilder = new StringBuilder();

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
                JsonArray transcripts = jsonObject.getAsJsonArray("transcripts");
                for (JsonElement transcript : transcripts) {
                    if (transcript != null && transcript.isJsonObject()) {
                        JsonObject transcriptObj = transcript.getAsJsonObject();
                        String text = transcriptObj.get("text").getAsString();
                        log.info("Transcription: {}", text);
                        resultBuilder.append(text).append("\n");
                    } else {
                        log.info("No valid transcription found.");
                    }
                }
                return resultBuilder.toString().trim();
            }
            log.error("No transcription results found for the provided audio file.");
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "数据解析失败！");
        } catch (Exception e) {
            log.error("Error during speech recognition: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, e.getMessage());
        }
    }

    /* 实时语音识别，这里暂未测试流式输入输出 */
    // @Override
    // public String speechRecognizeRealTime(File speechFile, String FileType) {
    //     RecognitionParam param =
    //             RecognitionParam.builder()
    //                     .model("paraformer-realtime-v2")
    //                     .format(FileType) // 'pcm'、'wav'、'opus'、'speex'、'aac'、'amr', you
    //                     // can check the supported formats in the document
    //                     .sampleRate(16000) // supported 8000、16000
    //                     .apiKey(DASH_SCOPE_API_KEY) // set your apikey in config.Environments.yourApikey
    //                     .build();
    //
    //     Recognition recognizer = new Recognition();
    //     String result = recognizer.call(param, speechFile);
    //
    //     log.info("Full recognition result: {}", result);
    //
    //     JSONArray sentencesArray = new JSONObject(result).getJSONArray("sentences");
    //     StringBuilder recognizedText = new StringBuilder();
    //     for (Object sentence : sentencesArray) {
    //         JSONObject sentenceObject = (JSONObject) sentence;
    //         recognizedText.append(sentenceObject.getStr("text")).append("\n");
    //     }
    //
    //     return recognizedText.toString().trim();
    // }

    @Override
    public Flowable<String> streamSpeechRecognize(String sessionId, Flowable<ByteBuffer> audioSource) throws NoApiKeyException {
        Flowable<ByteBuffer> audioSourceCompleted = audioSource.subscribeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .doOnError(e -> log.error("Error in audio source: {}", e.getMessage(), e))
                .doOnComplete(() -> log.info("Audio source completed"));

        Recognition recognizer = recognitionMap.computeIfAbsent(sessionId, k -> new Recognition());
        // 创建RecognitionParam，audioFrames参数中传入上面创建的Flowable<ByteBuffer>
        RecognitionParam param = RecognitionParam.builder()
                // 若没有将API Key配置到环境变量中，需将apiKey替换为自己的API Key
                .apiKey(DASH_SCOPE_API_KEY)
                .model("paraformer-realtime-v2")
                .format("pcm")
                .sampleRate(16000)
                // “language_hints”只支持paraformer-realtime-v2模型
                .parameter("language_hints", new String[]{"zh", "en"})
                // .parameter("heartbeat", true)
                // .parameter("task_group", UUID.randomUUID().toString()) // ⭐ 添加这一行
                .build();
        log.info("Starting real-time speech recognition with model: {}", SPEECH_RECOGNITION_MODEL);
        // 流式调用接口
        return recognizer.streamCall(param, audioSourceCompleted).map(
                result -> {
                    log.info("Received recognition result: {}", result.getSentence().getText());
                    if (StrUtil.isBlank(result.getSentence().getText())) {
                        return StrUtil.EMPTY;
                    }

                    SpeechResponseVO responseVO = new SpeechResponseVO();
                    responseVO.setCode(SpeechResponseStatusEnum.SUCCESS.getCode());
                    if (result.isSentenceEnd()) {
                        System.out.println("Final Result: " + result.getSentence().getText());
                        responseVO.setMessage(SpeechResponseStatusEnum.RECOGNIZE_FINAL_DATA.getMessage());
                        responseVO.setData(result.getSentence().getText());
                    } else {
                        System.out.println("Intermediate Result: " + result.getSentence().getText());
                        responseVO.setMessage(SpeechResponseStatusEnum.RECOGNIZE_INTERMEDIATE_DATA.getMessage());
                        responseVO.setData(result.getSentence().getText());
                    }
                    return JSONUtil.toJsonStr(responseVO);
                }).filter(StrUtil::isNotBlank);
    }
}
