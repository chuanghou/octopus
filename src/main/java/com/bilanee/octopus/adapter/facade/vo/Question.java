package com.bilanee.octopus.adapter.facade.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    Integer id;
    Integer type;
    String text;
    String answers;
    String optionA;
    String optionB;
    String optionC;
    String optionD;

}
