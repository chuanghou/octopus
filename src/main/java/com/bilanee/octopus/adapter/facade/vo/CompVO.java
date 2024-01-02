package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.basic.enums.CompStage;
import com.bilanee.octopus.basic.enums.MarketStatus;
import com.bilanee.octopus.basic.enums.TradeStage;
import com.bilanee.octopus.domain.DelayCommandWrapper;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompVO {

    /**
     * 竞赛id
     */
    Long compId;

    /**
     * 是否知识竞赛
     */
    Boolean enableQuiz;

    /**
     * 竞赛阶段
     */
    CompStage compStage;

    /**
     * 交易轮次
     */
    Integer roundId;

    /**
     * 交易阶段
     */
    TradeStage tradeStage;

    /**
     * 市场状态
     */
    MarketStatus marketStatus;

    /**
     * 阶段id
     */
    String stageId;

    /**
     * 当前阶段的截止时间
     */
    Long endingTimeStamp;


    /**
     * 本次参赛的用户id列表
     */
    List<String> userIds;


    List<DelayCommandWrapper> delayCommandWrappers;

}
