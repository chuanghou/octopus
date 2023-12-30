package com.bilanee.octopus.adapter.facade;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilanee.octopus.adapter.facade.po.AnswerPO;
import com.bilanee.octopus.adapter.facade.vo.QuestionVO;
import com.bilanee.octopus.basic.StageId;
import com.bilanee.octopus.basic.TokenUtils;
import com.bilanee.octopus.basic.enums.Choice;
import com.bilanee.octopus.basic.enums.CompStage;
import com.bilanee.octopus.infrastructure.entity.AnswerDO;
import com.bilanee.octopus.infrastructure.entity.MarketSettingDO;
import com.bilanee.octopus.infrastructure.entity.PaperDO;
import com.bilanee.octopus.infrastructure.entity.Question;
import com.bilanee.octopus.infrastructure.mapper.AnswerDOMapper;
import com.bilanee.octopus.infrastructure.mapper.MarketSettingMapper;
import com.bilanee.octopus.infrastructure.mapper.PaperDOMapper;
import com.stellariver.milky.common.base.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/quiz")
public class QuizFacade {

    final PaperDOMapper paperDOMapper;
    final AnswerDOMapper answerDOMapper;
    final MarketSettingMapper marketSettingMapper;

    /**
     * 查看试卷详情
     * @param stageId 阶段id
     * @param token 用户token
     * @return 试卷详情
     */
    @GetMapping("/listQuestionVOs")
    public Result<List<QuestionVO>> listQuestionVOs(String stageId, @RequestHeader String token) {
        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(1);
        PaperDO paperDO = paperDOMapper.selectById(marketSettingDO.getPaperId());
        List<Question> questions = paperDO.getQuestions();
        StageId parsed = StageId.parse(stageId);
        Long compId = parsed.getCompId();
        String userId = TokenUtils.getUserId(token);
        LambdaQueryWrapper<AnswerDO> eq = new LambdaQueryWrapper<AnswerDO>().eq(AnswerDO::getCompId, compId).eq(AnswerDO::getUserId, userId);
        Map<Integer, List<Choice>> answerMap = answerDOMapper.selectList(eq).stream().collect(Collectors.toMap(AnswerDO::getQuestionId, AnswerDO::getChoices));
        List<QuestionVO> questionVOs = questions.stream().map(q -> {
            List<String> options = q.getOptions();
            return QuestionVO.builder()
                    .paperId(marketSettingDO.getPaperId())
                    .questionId(q.getId())
                    .questionContent(q.getText())
                    .questionType(q.getType())
                    .options(options)
                    .submitChoices(answerMap.get(q.getId()))
                    .rightChoices(q.getAnswers())
                    .build();
        }).collect(Collectors.toList());

        CompStage compStage = parsed.getCompStage();
        if (compStage == CompStage.QUIT_COMPETE) {
            questionVOs.forEach(q -> q.getRightChoices().clear());
        }
        return Result.success(questionVOs);
    }



    /**
     * 查看试卷分数
     * @param stageId 阶段id
     * @param token 用户token
     * @return 试卷得分
     */
    @GetMapping("/getScore")
    public Result<Integer> getScore(String stageId, @RequestHeader String token) {
        List<QuestionVO> questionVOs = listQuestionVOs(stageId, token).getData();
        if (StageId.parse(stageId).getCompStage() == CompStage.QUIT_COMPETE) {
            return Result.success(null);
        }
        Integer score = questionVOs.stream().map(q -> {
            List<Choice> sChoices = q.getSubmitChoices().stream().sorted(Comparator.comparing(Choice::ordinal)).collect(Collectors.toList());
            List<Choice> rChoices = q.getRightChoices().stream().sorted(Comparator.comparing(Choice::ordinal)).collect(Collectors.toList());
            return sChoices.equals(rChoices) ? 2 : 0;
        }).reduce(0, Integer::sum);
        return Result.success(score);
    }


    /**
     * 提交作答
     */
    @PostMapping("/submitAnswers")
    public Result<Void> submitAnswers(@RequestHeader String token, @RequestBody List<AnswerPO> answerPOs) {
        String userId = TokenUtils.getUserId(token);
        answerPOs.forEach(a -> {
            StageId stageId = StageId.parse(a.getStageId());
            LambdaQueryWrapper<AnswerDO> eq = new LambdaQueryWrapper<AnswerDO>().eq(AnswerDO::getCompId, stageId.getCompId())
                    .eq(AnswerDO::getUserId, userId)
                    .eq(AnswerDO::getQuestionId, a.getQuestionId());
            AnswerDO answerDO = AnswerDO.builder().userId(userId)
                    .compId(stageId.getCompId()).questionId(a.getQuestionId()).choices(a.getChoices()).build();
            answerDOMapper.insertOrUpdateOnDuplicate(answerDO);
        });
        return Result.success();
    }



}
