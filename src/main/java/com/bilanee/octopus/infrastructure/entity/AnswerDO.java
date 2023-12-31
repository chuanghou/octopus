package com.bilanee.octopus.infrastructure.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bilanee.octopus.basic.enums.Choice;
import com.bilanee.octopus.infrastructure.handlers.ListChoiceHandler;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "octopus_answer_do", autoResultMap = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnswerDO {

    Long compId;
    String userId;
    Integer questionId;
    @TableField(typeHandler = ListChoiceHandler.class)
    List<Choice> choices;

}