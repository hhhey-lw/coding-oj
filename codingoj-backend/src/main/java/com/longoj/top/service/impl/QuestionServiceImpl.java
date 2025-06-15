package com.longoj.top.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.longoj.top.common.ErrorCode;
import com.longoj.top.constant.CommonConstant;
import com.longoj.top.exception.BusinessException;
import com.longoj.top.exception.ThrowUtils;
import com.longoj.top.model.dto.question.JudgeCase;
import com.longoj.top.model.dto.question.JudgeConfig;
import com.longoj.top.model.dto.question.QuestionAddRequest;
import com.longoj.top.model.dto.question.QuestionQueryRequest;
import com.longoj.top.model.entity.*;
import com.longoj.top.model.vo.QuestionVO;
import com.longoj.top.model.vo.UserVO;
import com.longoj.top.service.QuestionService;
import com.longoj.top.mapper.QuestionMapper;
import com.longoj.top.service.UserService;
import com.longoj.top.utils.SqlUtils;
import com.longoj.top.utils.UserContext;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author 韦龙
* @description 针对表【question(题目)】的数据库操作Service实现
* @createDate 2025-05-15 00:13:26
*/
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService{

    @Resource
    private UserService userService;

    @Override
    public void validQuestion(Question question, boolean add) {
        if (question == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // question.allget快捷获取所有属性
        String title = question.getTitle();
        String content = question.getContent();
        String tags = question.getTags();
        String answer = question.getAnswer();
        String judgeCase = question.getJudgeCase();
        String judgeConfig = question.getJudgeConfig();


        // 创建时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(StringUtils.isAnyBlank(title, content, tags), ErrorCode.PARAMS_ERROR);
        }
        // 有参数则校验
        if (StringUtils.isNotBlank(title) && title.length() > 80) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题过长");
        }
        if (StringUtils.isNotBlank(content) && content.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容过长");
        }
        if (StringUtils.isNotBlank(answer) && content.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "答案过长");
        }
        if (StringUtils.isNotBlank(judgeCase) && content.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "判题用例过长");
        }
        if (StringUtils.isNotBlank(judgeConfig) && content.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "判题配置过长");
        }

    }

    @Override
    public Long addQuestion(QuestionAddRequest questionAddRequest, HttpServletRequest request) {
        if (questionAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        List<String> tags = questionAddRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
        }
        List<JudgeCase> judgeCase = questionAddRequest.getJudgeCase();
        if (judgeCase != null) {
            question.setJudgeCase(JSONUtil.toJsonStr(judgeCase));
        }
        JudgeConfig judgeConfig = questionAddRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(judgeConfig));
        }
        validQuestion(question, true);
        User loginUser = userService.getLoginUser(request);
        // User loginUser = UserContext.getUser();
        question.setUserId(loginUser.getId());
        question.setFavourNum(0);
        question.setThumbNum(0);
        boolean result = save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return question.getId();
    }

    @Override
    public int updateQuestionSubmitNum(Long questionId) {
        return baseMapper.updateSubmitNum(questionId);
    }

    @Override
    public int updateQuestionAcceptedNum(Long questionId) {
        return baseMapper.updateAcceptedNum(questionId);
    }

    @Override
    public QuestionVO getQuestionVO(Question question) {
        QuestionVO questionVO = QuestionVO.objToVo(question);

        Long userId = question.getUserId();
        User user = userService.getById(userId);
        if (user != null && user.getId() >= 0) {
            questionVO.setUserVO(BeanUtil.copyProperties(user, UserVO.class));
        }
        return questionVO;
    }

    @Override
    public List<QuestionVO> getQuestionVOPage(List<Question> questionList) {

        Set<Long> userIds = questionList.stream().map(Question::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userList = userService.listByIds(userIds).stream().collect(Collectors.groupingBy(User::getId));

        List<QuestionVO> res = questionList.stream().map(question -> {
            QuestionVO questionVO = QuestionVO.objToVo(question);
            questionVO.setUserVO(BeanUtil.copyProperties(userList.get(question.getUserId()).get(0), UserVO.class));
            return questionVO;
        }).collect(Collectors.toList());

        return res;
    }

    @Override
    public Wrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null)
            return queryWrapper;

        Long id = questionQueryRequest.getId();
        String title = questionQueryRequest.getTitle();
        String content = questionQueryRequest.getContent();
        List<String> tags = questionQueryRequest.getTags();
        Long userId = questionQueryRequest.getUserId();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();

        queryWrapper.like(StringUtils.isNotEmpty(title) && StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotEmpty(content) && StringUtils.isNotBlank(content), "content", content);
        //
        if (CollectionUtils.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }

        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "user_id", userId);
        queryWrapper.eq("is_delete", 0);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }
}
