package com.longcoding.top.controller;

import com.longcoding.top.common.BaseResponse;
import com.longcoding.top.utils.ResultUtils;
import com.longcoding.top.service.ISpeechRecognizeService;
import com.longcoding.top.service.ISpeechSynthesizeService;
import io.reactivex.Flowable;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/speech")
public class SpeechController {

    @Resource
    private ISpeechSynthesizeService ttsService;

    @Resource
    private ISpeechRecognizeService speechRecognizeService;

    @GetMapping("/gen")
    public Flowable<String> generateSpeech(@RequestParam("text") String text,
                                           @RequestParam(value = "voice", required = false) String voice) {
        log.info("Generating speech...");
        // Here you would typically call a service to generate speech
        // For demonstration, we return a simple string
        return ttsService.flowableSynthesizeSpeech(text, voice);
    }

    @GetMapping("/recognize")
    public BaseResponse<String> recognizeSpeech(@RequestParam("audioUrl") String audioUrl) {
        log.info("Recognizing speech from audio URL: {}", audioUrl);
        // Call the speech recognition service
        return ResultUtils.success(speechRecognizeService.speechRecognize(audioUrl));
    }

    // @GetMapping("/recognize/stream")
    // public BaseResponse<String> recognizeSpeechStream(@RequestParam("audioUrl") String audioUrl) {
    //     log.info("Recognizing speech from audio URL: {}", audioUrl);
    //     // Call the speech recognition service
    //     return ResultUtils.success(speechRecognizeService.streamSpeechRecognize(audioUrl));
    // }

}
