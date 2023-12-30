package com.bilanee.octopus.basic.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public enum QuestionType {


    SINGLE("单选", 1),
    MULTI("多选", 3),
    BOOLEAN("正误", 2);

    final String desc;
    final Integer dbCode;
}
