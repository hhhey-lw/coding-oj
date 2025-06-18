package com.longoj.top.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.longoj.top.common.ErrorCode;
import com.longoj.top.common.RedisKeyUtil;
import com.longoj.top.constant.CommonConstant;
import com.longoj.top.exception.BusinessException;
import com.longoj.top.exception.ThrowUtils;
import com.longoj.top.model.dto.question.*;
import com.longoj.top.model.entity.*;
import com.longoj.top.model.vo.QuestionVO;
import com.longoj.top.model.vo.UserVO;
import com.longoj.top.service.QuestionPassedService;
import com.longoj.top.service.QuestionService;
import com.longoj.top.mapper.QuestionMapper;
import com.longoj.top.service.QuestionTagService;
import com.longoj.top.service.UserService;
import com.longoj.top.utils.SqlUtils;
import com.longoj.top.utils.UserContext;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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

    @Resource
    private QuestionTagService questionTagService;

    @Resource
    private QuestionPassedService questionPassedService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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

        // 这里补充一下 Tag
        if (tags != null) {
            questionTagService.setTagsByQuestionId(question.getId(), tags);
        }

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
    @Transactional(propagation = Propagation.REQUIRED)
    public Boolean updateQuestion(QuestionUpdateRequest questionUpdateRequest) {
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        // 标签
        List<String> tags = questionUpdateRequest.getTags();
        if (tags != null) {
            question.setTags(JSONUtil.toJsonStr(tags));
            questionTagService.delAndSetTagsByQuestionId(questionUpdateRequest.getId(), tags);
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
        validQuestion(question, false);
        long id = questionUpdateRequest.getId();
        // 判断是否存在
        Question oldQuestion = getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        return updateById(question);
    }

    @Override
    public boolean isPassed(Long questionId, Long userId) {
        // 1. 参数校验
        if (questionId == null || userId == null || userId <= 0) {
            return false;
        }
        String userPassedQuestionKey = RedisKeyUtil.getUserPassedQuestionKey(userId);

        // 2. 检查缓存是否存在
        Boolean hasKey = stringRedisTemplate.hasKey(userPassedQuestionKey);

        if (BooleanUtil.isFalse(hasKey)) {
            // 3. 缓存不存在，使用分布式锁防止缓存击穿
            String lockKey = RedisKeyUtil.LOCK + userPassedQuestionKey;
            Boolean lockAcquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, java.util.concurrent.TimeUnit.SECONDS);

            if (BooleanUtil.isFalse(lockAcquired)) {
                // 未获取到锁，说明有其他线程在重建，直接返回false，避免用户等待
                return false;
            }

            try {
                // 4. 获取锁成功，进行双重检查
                hasKey = stringRedisTemplate.hasKey(userPassedQuestionKey);
                if (BooleanUtil.isFalse(hasKey)) {
                    // 从数据库查询该用户所有通过的题目ID
                    List<Long> questionIds = questionPassedService.getQuestionIdsByUserId(userId);
                    if (CollectionUtil.isNotEmpty(questionIds)) {
                        // 修复类型问题：将 Long 列表转为 String 数组
                        String[] idArray = questionIds.stream().map(String::valueOf).toArray(String[]::new);
                        stringRedisTemplate.opsForSet().add(userPassedQuestionKey, idArray);
                        stringRedisTemplate.expire(userPassedQuestionKey, 24, java.util.concurrent.TimeUnit.HOURS);
                    } else {
                        // 防止缓存穿透：如果用户未通过任何题目，也缓存一个空标记
                        stringRedisTemplate.opsForSet().add(userPassedQuestionKey, "");
                        stringRedisTemplate.expire(userPassedQuestionKey, 5, java.util.concurrent.TimeUnit.MINUTES);
                    }
                }
            } finally {
                // 5. 修复：在 finally 块中释放锁
                stringRedisTemplate.delete(lockKey);
            }
        }

        // 6. 修复：从缓存查询并返回正确结果
        Boolean isPassed = stringRedisTemplate.opsForSet().isMember(userPassedQuestionKey, String.valueOf(questionId));
        return Boolean.TRUE.equals(isPassed);
    }

    @Override
    public Page<QuestionVO> getQuestionVO(QuestionQueryRequest questionQueryRequest, HttpServletRequest request) {
        List<String> tags = questionQueryRequest.getTags();
        String title = questionQueryRequest.getTitle();
        if (StrUtil.isBlank(title) && CollectionUtil.isEmpty(tags)) {
            // 如果没有搜索条件，直接返回空结果
            return getQuestionVOBYRedis(questionQueryRequest, request);
        }
        return getQuestionVOByTagAndSearch(questionQueryRequest, request);
    }

    public Page<QuestionVO> getQuestionVOByTagAndSearch(QuestionQueryRequest questionQueryRequest, HttpServletRequest request) {
        if (questionQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        Page<Question> questionPage = page(new Page<>(current, size),
                getQueryWrapper(questionQueryRequest));
        List<Question> records = questionPage.getRecords();
        List<QuestionVO> questionVOList = getQuestionVOPage(records);

        Page<QuestionVO> questionVOPage = new Page<>(current, size, questionPage.getTotal());

        try {
            User loginUser = userService.getLoginUser(request);
            // 设置题目通过状态
            questionVOList.forEach(questionVOItem -> questionVOItem.setPassed(isPassed(questionVOItem.getId(), loginUser.getId())));
        } catch (Exception e) {
            log.error("获取登录用户失败, 不设置题目通过状态", e);
        }
        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }

    public Page<QuestionVO> getQuestionVOBYRedis(QuestionQueryRequest questionQueryRequest, HttpServletRequest request) {
        if (questionQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        List<QuestionVO> questionList;
        Long total;

        // 先从Redis中获取题目列表
        String questionListKey = RedisKeyUtil.getQuestionListKey(current, size);
        String questionListTotalKey = RedisKeyUtil.getQuestionListTotalKey(current, size);

        String questionJSONStr = stringRedisTemplate.opsForValue().get(questionListKey);

        // 缓存命中
        if (StrUtil.isNotBlank(questionJSONStr)) {
            // 空值 - 避免缓存穿透
            if (questionJSONStr.equals("[]")) {
                // 如果缓存中有空字符串，说明没有数据，直接返回空结果
                return new Page<QuestionVO>(current, size, 0).setRecords(Collections.emptyList());
            }
            log.debug("Redis中有题目列表，查询缓存");
            questionList = JSONUtil.toList(questionJSONStr, QuestionVO.class);
            String totalStr = stringRedisTemplate.opsForValue().get(questionListTotalKey);
            if (StrUtil.isNotBlank(totalStr)) {
                total = Long.valueOf(totalStr);
            } else {
                total = 0L;
            }
        } else {
            // 缓存未命中，使用分布式锁防止缓存击穿
            Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(RedisKeyUtil.LOCK + questionListKey, "1", 10, TimeUnit.SECONDS);

            if (BooleanUtil.isTrue(lock)) {
                // 获取锁成功，进行双重检查
                questionJSONStr = stringRedisTemplate.opsForValue().get(questionListKey);
                if (StrUtil.isNotBlank(questionJSONStr)) {
                    // 如果缓存中有数据，直接返回
                    questionList = JSONUtil.toList(questionJSONStr, QuestionVO.class);
                    String totalStr = stringRedisTemplate.opsForValue().get(questionListTotalKey);
                    if (StrUtil.isNotBlank(totalStr)) {
                        total = Long.valueOf(totalStr);
                    } else {
                        total = 0L;
                    }
                } else {
                    log.debug("Redis中没有题目列表，查询数据库，并更新缓存");
                    Page<Question> questionPage = page(new Page<>(current, size),
                            getQueryWrapper(questionQueryRequest));
                    List<Question> records = questionPage.getRecords();

                    if (CollectionUtils.isEmpty(records)) {
                        // 如果没有题目，直接返回空结果
                        questionList = Collections.emptyList();
                        total = 0L;

                        stringRedisTemplate.opsForValue().set(questionListKey, "[]", 1, TimeUnit.MINUTES);
                        stringRedisTemplate.opsForValue().set(questionListTotalKey, "0", 1, TimeUnit.MINUTES);
                    } else {
                        questionList = getQuestionVOPage(records);
                        total = questionPage.getTotal();

                        stringRedisTemplate.opsForValue().set(questionListKey, JSONUtil.toJsonStr(records), 24, TimeUnit.HOURS);
                        stringRedisTemplate.opsForValue().set(questionListTotalKey, String.valueOf(questionPage.getTotal()), 24, TimeUnit.HOURS);
                    }
                }
            } else {
                // 未获取到锁，直接返回空结果
                return new Page<QuestionVO>(current, size, 0).setRecords(Collections.emptyList());
            }
        }

        Page<QuestionVO> questionVOPage = new Page<>(current, size, total);

        try {
            User loginUser = userService.getLoginUser(request);
            // 设置题目通过状态
            questionList.forEach(questionVOItem -> questionVOItem.setPassed(isPassed(questionVOItem.getId(), loginUser.getId())));
        } catch (Exception e) {
            log.error("获取登录用户失败, 不设置题目通过状态", e);
        }

        return questionVOPage.setRecords(questionList);
    }

    @Override
    public QuestionVO getQuestionVO(Question question) {
        QuestionVO questionVO = QuestionVO.objToVo(question);

        Long userId = question.getUserId();
        User user = userService.getById(userId);
        if (user != null && user.getId() >= 0) {
            questionVO.setUserVO(BeanUtil.copyProperties(user, UserVO.class));
        }

        // List<String> tags = questionTagService.getTagsByQuestionId(questionVO.getId());
        // if (tags != null && CollectionUtil.isNotEmpty(tags)){
        //     questionVO.setTags(tags);
        // }

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

        // for (QuestionVO re : res) {
        //     List<String> tags = questionTagService.getTagsByQuestionId(re.getId());
        //     if (tags != null && CollectionUtil.isNotEmpty(tags)){
        //         re.setTags(tags);
        //     }
        // }

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

        queryWrapper.eq(ObjectUtils.isNotEmpty(userId) && userId != 0, "user_id", userId);
        queryWrapper.eq("is_delete", 0);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }
}
