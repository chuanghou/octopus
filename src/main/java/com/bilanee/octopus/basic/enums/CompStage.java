package com.bilanee.octopus.basic.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CompStage {
    INT("初始化"),
    QUIT_COMPETE("知识竞答"),
    QUIT_RESULT("竞答结束"),
    TRADE("交易仿真"),
    RANKING("成绩排名");

    final String desc;
}
