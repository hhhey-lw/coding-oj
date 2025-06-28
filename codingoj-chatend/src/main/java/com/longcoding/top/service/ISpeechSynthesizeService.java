package com.longcoding.top.service;

import io.reactivex.Flowable;
import reactor.core.publisher.Flux;

public interface ISpeechSynthesizeService {

    /* 返回音频URL */
    String synthesizeSpeech(String text, String voice);

    /* 完整Text生成流式语音 */
    Flowable<String> flowableSynthesizeSpeech(String text, String voice);

    /* 流式文本生成流式语音 */
    Flowable<String> flowableSynthesizeSpeech(Flux<String> text, String voice);
}
