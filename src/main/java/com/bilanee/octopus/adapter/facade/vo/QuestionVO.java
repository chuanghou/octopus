package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.basic.enums.Choice;
import com.bilanee.octopus.basic.enums.QuestionType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuestionVO {

    /**
     * 试卷Id
     */
    Integer paperId;
    /**
     * 问题Id
     */
    Integer questionId;

    /**
     * 问题内容
     */
    String questionContent;

    /**
     * 问题类型
     */
    QuestionType questionType;

    /**
     * 选项
     */
    List<String> options;

    /**
     * 提交的选项
     */
    List<Choice> submitChoices;

    /**
     * 提交的选项
     */
    List<Choice> rightChoices;

}
