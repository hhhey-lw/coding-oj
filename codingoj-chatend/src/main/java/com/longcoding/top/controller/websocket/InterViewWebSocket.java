package com.longcoding.top.controller.websocket;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.longcoding.top.common.ErrorCode;
import com.longcoding.top.service.IInterviewService;
import com.longcoding.top.utils.JwtTokenUtil;
import com.longcoding.top.utils.ResultUtils;
import com.longcoding.top.model.dto.chat.ChatAIRequest;
import com.longcoding.top.model.dto.chat.SpeechMessageDTO;
import com.longcoding.top.model.enums.SpeechMessageTypeEnum;
import com.longcoding.top.model.enums.SpeechResponseStatusEnum;
import com.longcoding.top.model.vo.SpeechResponseVO;
import com.longcoding.top.service.IChatAIService;
import com.longcoding.top.service.ISpeechRecognizeService;
import com.longcoding.top.service.ISpeechSynthesizeService;
import com.longcoding.top.utils.UserContext;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.processors.ReplayProcessor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
* 单例模式，因此，
 * 一个 InterViewWebSocket 实例处理所有连接
 * => 每个连接有唯一的 WebSocketSession 对象 <=
 * 每个会话都需要独立的状态，因此需要 Map 来区分
* */

@Slf4j
@Component
public class InterViewWebSocket extends TextWebSocketHandler {
    // 语音合成，顺序进行
    private static final Map<String, Semaphore> semaphoreMap = new ConcurrentHashMap<>();
    // 控制文本和音频数据的互斥输出，文本语音数据并行输出会出现异常
    private static final Semaphore singleOutput = new Semaphore(1);

    // 线程安全的Map存放
    private static final Map<String, CompositeDisposable> disposableMap = new ConcurrentHashMap<>();
    private static final Map<String, ReplayProcessor<ByteBuffer>> audioSourceMap = new ConcurrentHashMap<>();

    // 在类中定义一个全局计数器
    private static final AtomicInteger sessionCount = new AtomicInteger(0);
    private static final int MAX_SESSIONS = 32; // 根据需求调整

    // 在类中定义一个线程池映射，限制每个会话的线程池大小
    private ExecutorService threadPool = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final AtomicInteger round = new AtomicInteger(0);
    // 存储每个会话对应的面试ID
    private Map<String, Long> interviewIdMap = new ConcurrentHashMap<>();

    @Resource(name = "qwenChatService")
    private IChatAIService chatAIService;

    @Resource
    private ISpeechRecognizeService speechRecognizeService;

    @Resource
    private ISpeechSynthesizeService speechSynthesizeService;

    @Resource
    private IInterviewService interviewService;

    private ChatAIRequest getChatAIRequest(WebSocketSession session, String prompt) {
        log.info("Received message from session {}: {}", session.getId(), prompt);
        ChatAIRequest chatAIRequest = new ChatAIRequest();
        chatAIRequest.setChatId(session.getId());
        chatAIRequest.setModelName("qwen-mx");
        chatAIRequest.setPrompt(prompt);
        chatAIRequest.setIsThinking(false);
        return chatAIRequest;
    }

    // 处理连接逻辑
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        if (sessionCount.incrementAndGet() > MAX_SESSIONS) {
            sessionCount.decrementAndGet();
            session.sendMessage(new TextMessage(JSONUtil.toJsonStr(ResultUtils.error(ErrorCode.FORBIDDEN_ERROR, "连接过多，暂时不能访问！"))));
            session.close();
            throw new RuntimeException("Maximum number of sessions reached");
        }
    }

    // 处理断开连接逻辑
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        log.error("WebSocket session {} closed with status: {}", session.getId(), status);
        semaphoreMap.remove(session.getId());

        disposableMap.remove(session.getId());

        sessionCount.decrementAndGet();

        speechRecognizeService.stopRecognize(session.getId());

        interviewService.finishInterview(UserContext.getUserId(), interviewIdMap.get(session.getId()));

        session.close();
    }

    // 处理消息逻辑
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        if (StrUtil.isNotBlank(message.getPayload())) {
            SpeechMessageDTO messageDTO = JSONUtil.toBean(message.getPayload(), SpeechMessageDTO.class);

            if (messageDTO.getType().equals(SpeechMessageTypeEnum.TEXT_DATA)) {
                log.info("Received text data from session {}: {}", session.getId(), messageDTO.getData());
                if (StrUtil.isBlank(messageDTO.getData())) {
                    return;
                }
                ChatAIRequest request = getChatAIRequest(session, messageDTO.getData());
                Flux<String> reply = chatAIService.doChatStream(request);

                sendReplyTextToClientStream(session, reply);
            }
            else if (messageDTO.getType().equals(SpeechMessageTypeEnum.SPEECH_DATA)) {
                log.info("Received speech data from session {}: {}", session.getId(), messageDTO.getData().substring(0, 32));

                if (StrUtil.isBlank(messageDTO.getData())) {
                    return;
                }

                // 解析Base64音频数据
                byte[] audioBytes = java.util.Base64.getDecoder().decode(messageDTO.getData());
                ByteBuffer audioBuffer = ByteBuffer.wrap(audioBytes);

                // 获取音频源
                ReplayProcessor<ByteBuffer> audioSource = audioSourceMap.get(session.getId());
                audioSource.onNext(audioBuffer);
            }
            else if (messageDTO.getType().equals(SpeechMessageTypeEnum.SPEECH_START)) {
                log.info("Received speech start from session {}: {}", session.getId(), messageDTO.getData());
                connectSpeechRecognition(session);
            }
            else if (messageDTO.getType().equals(SpeechMessageTypeEnum.SPEECH_END)) {
                log.info("Received speech end from session {}: {}", session.getId(), messageDTO.getData());
                scheduledStopRecognize(session, 5, TimeUnit.SECONDS);
            }
            else if (messageDTO.getType().equals(SpeechMessageTypeEnum.TOKEN)) {
                log.info("Received interview start from session {}: {}", session.getId(), messageDTO.getData());
                startInterview(session, messageDTO.getData());
            }
            else {
                // 处理语音数据
                log.info("Received speech data from session {}: {} - {}", session.getId(), messageDTO.getType(), messageDTO.getData());
            }
        }
    }

    private void startInterview(WebSocketSession session, String token) throws IOException {
        if (!JwtTokenUtil.validateToken(token)) {
            session.sendMessage(new TextMessage(JSONUtil.toJsonStr(ResultUtils.error(ErrorCode.FORBIDDEN_ERROR, "无效的令牌！请重新登录！"))));
            session.close();
            return;
        }
        Long userId = JwtTokenUtil.getUserIdFromToken(token);

        if (!interviewService.checkUserMeal(userId)) {
            session.sendMessage(new TextMessage(JSONUtil.toJsonStr(ResultUtils.error(ErrorCode.FORBIDDEN_ERROR, "你当日的次数已用完！请明天再来！"))));
            session.close();
            throw new RuntimeException("User meal not expired");
        }
        Long _interviewId = interviewService.addInterviewRecord(userId);
        interviewIdMap.put(session.getId(), _interviewId);

        stopSession(session, interviewService.getMealDurationTimeByUserId(userId).longValue());

        log.info("WebSocket session {} established. Current session count: {}", session.getId(), sessionCount.get());

        semaphoreMap.put(session.getId(), new Semaphore(1));

        Flux<String> reply = chatAIService.doChatStream(getChatAIRequest(session, interviewService.getInitPromptByUserId(userId)));

        sendReplyTextToClientStream(session, reply);
    }

    private void sendReplyTextToClientStream(WebSocketSession session, Flux<String> modelReply) {
        final int BUFFER_THRESHOLD = 128; // 设置缓冲字数阈值，根据需求调整
        StringBuffer stringBuilder = new StringBuffer(256); // 线程安全的缓冲区
        modelReply.subscribe(msg -> {
                    SpeechResponseVO speechResponseVO = new SpeechResponseVO();
                    speechResponseVO.setCode(SpeechResponseStatusEnum.SPEECH_TEXT.getCode());
                    speechResponseVO.setMessage(SpeechResponseStatusEnum.SPEECH_TEXT.getMessage());
                    speechResponseVO.setData(msg);
                    try {
                        singleOutput.acquire(); // 确保单次输出
                        session.sendMessage(new TextMessage(JSONUtil.toJsonStr(speechResponseVO)));
                        singleOutput.release(); // 释放单次输出锁

                        stringBuilder.append(msg);

                        // 检查缓冲长度是否达到阈值
                        if (stringBuilder.length() >= BUFFER_THRESHOLD) {
                            String textToSynthesize = stringBuilder.toString();
                            stringBuilder.setLength(0); // 清空缓冲

                            threadPool.submit(() -> {
                                try {
                                    log.info("Processing text for synthesis: {}", textToSynthesize);
                                    semaphoreMap.get(session.getId()).acquire();

                                    Flowable<String> flowable = speechSynthesizeService.flowableSynthesizeSpeech(textToSynthesize, "Longwei");

                                    flowable.blockingForEach(
                                            speech -> {
                                                try {
                                                    singleOutput.acquire(); // 确保单次输出
                                                    session.sendMessage(new TextMessage(speech));
                                                    singleOutput.release(); // 释放单次输出锁
                                                } catch (IOException e) {
                                                    log.error("Error sending message to session {}: {}", session.getId(), e.getMessage());
                                                }
                                            }
                                    );
                                } catch (InterruptedException e) {
                                    log.error("InterruptedException {} {}", session.getId(), e.getMessage());
                                    throw new RuntimeException(e);
                                } finally {
                                    semaphoreMap.get(session.getId()).release();
                                }
                            });
                        }
                    } catch (IOException e) {
                        log.error("Error sending message to session {}: {}", session.getId(), e.getMessage());
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        log.error("InterruptedException {} {}", session.getId(), e.getMessage());
                        throw new RuntimeException(e);
                    }
                },
                throwable -> {
                    log.error("Error in model reply stream for session {}: {}", session.getId(), throwable.getMessage());
                },
                () -> {
                    log.info("Model reply stream completed for session {}", session.getId());
                    // 处理完成后，检查是否有剩余文本需要合成
                    if (stringBuilder.length() > 0) {
                        String textToSynthesize = stringBuilder.toString();
                        threadPool.submit(() -> {
                            try {
                                semaphoreMap.get(session.getId()).acquire();

                                Flowable<String> flowable = speechSynthesizeService.flowableSynthesizeSpeech(textToSynthesize, "Longwei");

                                flowable.blockingForEach(
                                        speech -> {
                                            try {
                                                singleOutput.acquire(); // 确保单次输出
                                                session.sendMessage(new TextMessage(speech));
                                                singleOutput.release(); // 释放单次输出锁
                                            } catch (IOException e) {
                                                log.error("Error sending message to session {}: {}", session.getId(), e.getMessage());
                                            }
                                        }
                                );
                            } catch (InterruptedException e) {
                                log.error("InterruptedException {} {}", session.getId(), e.getMessage());
                                throw new RuntimeException(e);
                            } finally {
                                semaphoreMap.get(session.getId()).release();
                            }
                        });
                    }
                });
    }

    private void startSpeechRecognition(WebSocketSession session, ReplayProcessor<ByteBuffer> audioSource) throws NoApiKeyException {
        CompositeDisposable disposables = disposableMap.get(session.getId());
        Disposable disposable = speechRecognizeService.streamSpeechRecognize(session.getId(), audioSource)
                .subscribe(
                        res -> {
                            log.info("Received transcription: {}", res);
                            try {
                                session.sendMessage(new TextMessage(res));
                            } catch (IOException e) {
                                log.error("Error sending message to session {}: {}", session.getId(), e.getMessage());
                            }
                        },
                        error -> {
                            log.error("Error in speech recognition for session {}: {}", session.getId(), error.getMessage());
                            // try {
                            //     session.sendMessage(new TextMessage(getErrorMessage("语音识别服务出错")));
                            //     // connectSpeechRecognition(session); // 重新连接语音识别服务
                            //     // session.close();
                            // } catch (IOException e) {
                            //     log.error("Error closing session {}: {}", session.getId(), e.getMessage());
                            // }
                        },
                        () -> {
                            log.info("Speech recognition completed for session {}", session.getId());
                        }
                );

        // 将订阅添加到CompositeDisposable中，以便在会话关闭时进行清理
        if (disposables != null) {
            disposables.add(disposable);
        }

        scheduledStopRecognize(session, 60, TimeUnit.SECONDS);
    }

    private void scheduledStopRecognize(WebSocketSession session, Integer delay, TimeUnit timeUnit) {
        // 最长识别60秒
        scheduler.schedule(() -> {
            log.info("Stopping speech recognition for session {} after {} {}", session.getId(), delay, timeUnit);
            boolean isStop = speechRecognizeService.stopRecognize(session.getId());
            if (isStop) {
                log.info("Speech recognition stopped for session {}", session.getId());
                SpeechResponseVO speechResponseVO = new SpeechResponseVO();
                speechResponseVO.setCode(SpeechResponseStatusEnum.RECOGNIZE_OUT_MAX_TIME.getCode());
                speechResponseVO.setMessage(SpeechResponseStatusEnum.RECOGNIZE_OUT_MAX_TIME.getMessage());
                speechResponseVO.setData(null);
                try {
                    session.sendMessage(new TextMessage(JSONUtil.toJsonStr(speechResponseVO)));
                } catch (IOException e) {
                    log.error("Error sending message to session {}: {}", session.getId(), e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        }, delay, timeUnit);
    }

    private void stopSession(WebSocketSession session, Long durationTime) {
        scheduler.schedule(() -> {
            try {
                if (session != null && session.isOpen()) {
                    session.sendMessage(new TextMessage(getErrorMessage("模拟面试时间到了，自动关闭！")));
                    session.close();
                }
            } catch (IOException e) {
                log.error("Error closing session {}: {}", session.getId(), e.getMessage());
            }
        }, durationTime, TimeUnit.MINUTES);
    }

    private void connectSpeechRecognition(WebSocketSession session) throws NoApiKeyException {
        speechRecognizeService.stopRecognize(session.getId());
        if (disposableMap.get(session.getId()) != null) {
            disposableMap.get(session.getId()).dispose();
        }

        audioSourceMap.put(session.getId(), ReplayProcessor.create());
        // 启动语音识别
        startSpeechRecognition(session, audioSourceMap.get(session.getId()));
    }

    private String getErrorMessage(String message) {
        SpeechResponseVO responseVO = new SpeechResponseVO();
        responseVO.setData(null);
        responseVO.setCode(SpeechResponseStatusEnum.FAIL.getCode());
        responseVO.setMessage(message);
        return JSONUtil.toJsonStr(responseVO);
    }

}
