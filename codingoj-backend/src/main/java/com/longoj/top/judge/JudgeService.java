package com.longoj.top.judge;

import com.longoj.top.model.dto.questionsubmit.JudgeInfo;
import com.longoj.top.model.entity.QuestionSubmit;

public interface JudgeService {

    JudgeInfo doJudge(QuestionSubmit questionSubmit);

}
