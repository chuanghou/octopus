package com.bilanee.octopus.infrastructure.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bilanee.octopus.basic.enums.Choice;
import com.bilanee.octopus.basic.enums.QuestionType;
import com.stellariver.milky.infrastructure.base.database.ListJsonHandler;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("octopus_paper")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaperDO {

    @TableId(type = IdType.INPUT)
    Integer id;

    String name;

    @TableField(typeHandler = ListQuestionHandler.class)
    List<Question> questions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static public class Question {

        /**
         * 问题id
         */
        @NotNull
        Integer id;

        /*
         问题文本
         */
        @NotBlank
        String text;

        /**
         * 问题类型
         */
        @NotNull
        QuestionType type;

        /**
         * 选项
         */
        @NotEmpty
        List<String> options;

        /**
         * 正确答案
         */
        @NotEmpty
        List<Choice> answers;

    }

    static class ListQuestionHandler extends ListJsonHandler<Question> {}
    
}
