package com.longoj.top.model.vo;

import lombok.Data;

@Data
public class UserSubmitInfoVO extends UserVO {

    Integer passedQuestionNumber; // 通过题目数量

    Integer totalSubmitNumber; // 总提交数量

}
