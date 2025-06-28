package com.longcoding.top.service;

import com.alibaba.dashscope.exception.NoApiKeyException;
import io.reactivex.Flowable;

import java.nio.ByteBuffer;

public interface ISpeechRecognizeService {

    boolean stopRecognize(String sessionId);

    String speechRecognize(String speechFileURL);

    // String speechRecognizeRealTime(File speechFile, String fileType);

    Flowable<String> streamSpeechRecognize(String sessionId, Flowable<ByteBuffer> audioSource) throws NoApiKeyException;
}
