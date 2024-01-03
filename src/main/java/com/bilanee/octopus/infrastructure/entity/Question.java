package com.bilanee.octopus.infrastructure.entity;

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
public class Question {

    /**
     * 问题id
     */
    Integer id;

    /*
     问题文本
     */
    String text;

    /**
     * 问题类型
     */
    QuestionType type;

    /**
     * 选项
     */
    List<String> options;

    /**
     * 正确答案
     */
    List<Choice> answers;

}
