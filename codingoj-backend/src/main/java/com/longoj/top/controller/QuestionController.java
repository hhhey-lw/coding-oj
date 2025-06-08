package com.longoj.top.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.longoj.top.annotation.AuthCheck;
import com.longoj.top.common.BaseResponse;
import com.longoj.top.common.DeleteRequest;
import com.longoj.top.common.ErrorCode;
import com.longoj.top.common.ResultUtils;
import com.longoj.top.constant.UserConstant;
import com.longoj.top.exception.BusinessException;
import com.longoj.top.exception.ThrowUtils;
import com.longoj.top.model.dto.question.*;
import com.longoj.top.model.entity.Question;
import com.longoj.top.model.vo.QuestionVO;
import com.longoj.top.service.QuestionService;
import com.longoj.top.service.UserService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;


@Slf4j
// @Api(tags = "题目接口")
@RequestMapping("/question")
@RestController
public class QuestionController {

    @Resource
    private QuestionService questionService;

    // region 增删改查

    /**
     * 创建题目
     *
     * @param questionAddRequest
     * @param request
     * @return
     */
    @ApiOperation("创建题目")
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request) {

        Long newQuestionId = questionService.addQuestion(questionAddRequest, request);

        return ResultUtils.success(newQuestionId);
    }

    /**
     * 删除题目
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @ApiOperation("删除题目")
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = questionService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新题目
     *
     * @param questionUpdateRequest
     * @return
     */
    @ApiOperation("更新题目")
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        // 标签
        List<String> tags = questionUpdateRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        // 判题用例
        List<JudgeCase> judgeCase = questionUpdateRequest.getJudgeCase();
        if (judgeCase != null) {
            question.setJudgeCase(JSONUtil.toJsonStr(judgeCase));
        }
        // 判题配置
        JudgeConfig judgeConfig = questionUpdateRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        }
        // 参数校验
        questionService.validQuestion(question, false);
        long id = questionUpdateRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = questionService.updateById(question);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取题目（仅管理员）
     *
     * @param id
     * @return
     */
    @ApiOperation("获取题目ById")
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Question> getQuestionById(@RequestParam("id") Long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = questionService.getById(id);
        ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(question);
    }

    /**
     * 根据 id 获取包装类
     *
     * @param id
     * @return
     */
    @ApiOperation("获取题目包装类ById")
    @GetMapping("/get/vo")
    public BaseResponse<QuestionVO> getQuestionVOById(@RequestParam("id") Long id) {
        BaseResponse<Question> response = getQuestionById(id);
        Question question = response.getData();
        return ResultUtils.success(questionService.getQuestionVO(question));
    }

    /**
     * 分页获取题目列表（仅管理员）
     *
     * @param questionQueryRequest
     * @return
     */
    @ApiOperation("分页获取题目")
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Question>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionPage);
    }

    /**
     * 分页获取题目封装列表
     *
     * @param questionQueryRequest
     * @return
     */
    @ApiOperation("分页获取题目封装列表")
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest) {
        if (questionQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        Page<QuestionVO> questionVOPage = new Page<>(current, size, questionPage.getTotal());
        List<QuestionVO> questionVO = questionService.getQuestionVOPage(questionPage.getRecords());
        questionVOPage.setRecords(questionVO);
        return ResultUtils.success(questionVOPage);
    }

    // endregion

    /**
     * 更新题目信息
     *
     * @param questionUpdateRequest
     * @return
     */
    @Deprecated
    @ApiOperation("更新题目信息")
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMyQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        if (questionUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
}
