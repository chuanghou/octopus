package com.bilanee.octopus.adapter.facade.po;

import com.bilanee.octopus.basic.ErrorEnums;
import com.stellariver.milky.common.base.AfterValidation;
import com.stellariver.milky.common.base.BizEx;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizSetting {


    /**
     *  是否开启知识问答
     */
    @NotNull
    Boolean enableQuiz;

    /**
     * 试卷id
     */
    Integer quizId;

    /**
     * 知识问答时长
     */
    Integer quizCompeteDuration;

    /**
     * 竞赛结果查看时长
     */
    Integer quizResultDuration;


    @AfterValidation
    public void after() {
        BizEx.trueThrow(enableQuiz && quizId == null, ErrorEnums.PARAM_FORMAT_WRONG.message("开启知识问答，"));
    }

}
