package com.bilanee.octopus.adapter.facade.po;

import com.bilanee.octopus.basic.enums.TradeStage;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompCreatePO {

    /**
     * 竞赛开始时间戳，可以为空，为空的时候，默认5分钟之后开始
     */
    @Positive
    Long startTimeStamp;

    /**
     * 知识问答环节时长，单位分钟
     */
    @Positive
    Integer quitCompeteLength;


    /**
     * 知识问答查看结果时长，单位分钟
     */
    @Positive
    Integer quitResultLength;

    /**
     * 各个市场的竞价时长, Key为交易阶段枚举列表中前6阶段
     */
    @NotNull @NotEmpty
    Map<TradeStage, Integer> marketStageBidLengths;

    /**
     * 各个市场的查看结果时长，Key为交易阶段枚举列表中前6阶段
     */
    @NotNull @NotEmpty
    Map<TradeStage, Integer> marketStageClearLengths;

    /**
     * 第7阶段的查看结果时长，单位分钟
     */
    @NotNull @Positive
    Integer tradeResultLength;

    /**
     * 本次参赛的用户id列表
     */
    @NotNull @NotEmpty
    List<String> userIds;

    /**
     * 是否进行知识竞赛
     */
    @NotNull
    Boolean enableQuiz;

    /**
     * 比赛日期
     */
    String dt;



}
