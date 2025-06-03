package com.longoj.top.model.dto.questionsubmit;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// @ApiModel("题目提交实体")
public class QuestionSubmitAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("编程语言")
    private String language;

    @ApiModelProperty("提交代码")
    private String code;

    @ApiModelProperty("问题Id")
    private Long questionId;

}
