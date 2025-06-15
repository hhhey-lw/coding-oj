package com.longoj.top.job.cycle;

import cn.hutool.json.JSONUtil;
import com.longoj.top.job.publisher.JudgeServicePublisher;
import com.longoj.top.model.entity.MqLocalMessage;
import com.longoj.top.model.entity.QuestionSubmit;
import com.longoj.top.model.enums.MQMessageStatusEnum;
import com.longoj.top.service.MqLocalMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Component
public class JudgeScheduledTask {

    private final Integer MAX_RETRY_COUNT = 3; // 最大重试次数

    @Resource
    private MqLocalMessageService mqLocalMessageService;

    @Resource
    private JudgeServicePublisher judgeServicePublisher;

    /**
     * 每10分钟扫描一次本地消息表，重新发送失败或待处理的任务
     */
    @Scheduled(cron = "0 */10 * * * ?") // 每10分钟执行一次
    public void resendFailedMessages() {
        log.info("开始执行定时任务：扫描并重发本地消息表中的失败/待处理消息");

        List<MqLocalMessage> messagesToResend = mqLocalMessageService.lambdaQuery()
                .eq(MqLocalMessage::getStatus, MQMessageStatusEnum.FAILED)
                .list();

        if (CollectionUtils.isEmpty(messagesToResend)) {
            log.info("没有需要重发的消息");
            return;
        }

        log.info("发现 {} 条需要重发的消息", messagesToResend.size());

        for (MqLocalMessage localMessage : messagesToResend) {
            try {
                // 增加重试次数
                int currentRetryCount = localMessage.getRetryCount() == null ? 0 : localMessage.getRetryCount();
                if (currentRetryCount >= MAX_RETRY_COUNT) {
                    log.warn("消息重试次数已达上限，ID: {}", localMessage.getMessageId());
                    // 可以选择将状态更新为最终失败
                    mqLocalMessageService.lambdaUpdate()
                            .set(MqLocalMessage::getStatus, MQMessageStatusEnum.FINAL_FAILED)
                            .set(MqLocalMessage::getErrorCause, "Max retry count reached")
                            .eq(MqLocalMessage::getMessageId, localMessage.getMessageId())
                            .update();
                    continue;
                }

                localMessage.setRetryCount(currentRetryCount + 1);
                // 更新状态为待发送，并增加重试次数
                mqLocalMessageService.lambdaUpdate()
                        .set(MqLocalMessage::getStatus, MQMessageStatusEnum.PENDING)
                        .set(MqLocalMessage::getRetryCount, localMessage.getRetryCount())
                        .eq(MqLocalMessage::getMessageId, localMessage.getMessageId())
                        .update();

                QuestionSubmit questionSubmit = JSONUtil.toBean(localMessage.getPayload(), QuestionSubmit.class);
                judgeServicePublisher.retrySendJudgeMessage(localMessage.getPayload(), questionSubmit.getId());
                log.info("消息重发请求已提交，ID: {}", localMessage.getMessageId());
            } catch (Exception e) {
                log.error("重发消息失败，ID: {}, 错误: {}", localMessage.getMessageId(), e.getMessage(), e);
                // 如果重发仍然失败，可以考虑更新状态为最终失败，或根据重试次数决定
                mqLocalMessageService.lambdaUpdate()
                        .set(MqLocalMessage::getStatus, MQMessageStatusEnum.FINAL_FAILED) // 或者根据重试次数判断
                        .set(MqLocalMessage::getErrorCause, "Resend failed: " + e.getMessage())
                        .eq(MqLocalMessage::getMessageId, localMessage.getMessageId())
                        .update();
            }
        }
        log.info("定时任务执行完毕：扫描并重发本地消息表中的失败消息");
    }
}
