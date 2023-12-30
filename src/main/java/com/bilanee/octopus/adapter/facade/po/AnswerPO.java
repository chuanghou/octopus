package com.bilanee.octopus.adapter.facade.po;

import com.bilanee.octopus.basic.enums.Choice;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnswerPO {

    String stageId;
    Integer questionId;
    List<Choice> choices;

}
