package com.longcoding.top.longojchatend.interview;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.longcoding.top.service.impl.AliYunSpeechRecognizeServiceImpl;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import java.nio.ByteBuffer;

@SpringBootTest
public class SpeechRecognizeStreamTest {

    @Resource
    private AliYunSpeechRecognizeServiceImpl aliYunSpeechRecognizeService;

    // @Test
    public void Test() throws NoApiKeyException {
        // 创建一个Flowable<ByteBuffer>
        Flowable<ByteBuffer> audioSource =
                Flowable.create(
                        emitter -> {
                            new Thread(
                                    () -> {
                                        try {
                                            // 创建音频格式
                                            AudioFormat audioFormat = new AudioFormat(16000, 16, 1, true, false);
                                            // 根据格式匹配默认录音设备
                                            TargetDataLine targetDataLine =
                                                    AudioSystem.getTargetDataLine(audioFormat);
                                            targetDataLine.open(audioFormat);
                                            // 开始录音
                                            targetDataLine.start();
                                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                                            long start = System.currentTimeMillis();
                                            // 录音50s并进行实时转写
                                            while (System.currentTimeMillis() - start < 50000) {
                                                int read = targetDataLine.read(buffer.array(), 0, buffer.capacity());
                                                if (read > 0) {
                                                    buffer.limit(read);
                                                    // 将录音音频数据发送给流式识别服务
                                                    emitter.onNext(buffer);
                                                    buffer = ByteBuffer.allocate(1024);
                                                    // 录音速率有限，防止cpu占用过高，休眠一小会儿
                                                    Thread.sleep(20);
                                                }
                                            }
                                            // 通知结束转写
                                            emitter.onComplete();
                                        } catch (Exception e) {
                                            emitter.onError(e);
                                        }
                                    })
                                    .start();
                        },
                        BackpressureStrategy.BUFFER);

        // 创建Recognizer
        Recognition recognizer = new Recognition();
        // 创建RecognitionParam，audioFrames参数中传入上面创建的Flowable<ByteBuffer>
        RecognitionParam param = RecognitionParam.builder()
                // 若没有将API Key配置到环境变量中，需将apiKey替换为自己的API Key
                .apiKey("sk-4d9ac0285a4e45cc9adcc73115de3da3")
                .model("paraformer-realtime-v2")
                .format("pcm")
                .sampleRate(16000)
                // “language_hints”只支持paraformer-realtime-v2模型
                .parameter("language_hints", new String[]{"zh", "en"})
                .build();

        // 流式调用接口
        recognizer
                .streamCall(param, audioSource)
                .blockingForEach(
                        result -> {
                            // Subscribe to the output result
                            if (result.isSentenceEnd()) {
                                System.out.println("Final Result: " + result.getSentence().getText());
                            } else {
                                System.out.println("Intermediate Result: " + result.getSentence().getText());
                            }
                        });
        // aliYunSpeechRecognizeService.streamSpeechRecognize(audioSource)
        //         .blockingForEach(
        //                 result -> {
        //                     // Subscribe to the output result
        //                     System.out.println("Received recognition result: " + result);
        //                 });
        System.out.println(
                "[Metric] requestId: "
                        + recognizer.getLastRequestId()
                        + ", first package delay ms: "
                        + recognizer.getFirstPackageDelay()
                        + ", last package delay ms: "
                        + recognizer.getLastPackageDelay());
        System.exit(0);
    }
}