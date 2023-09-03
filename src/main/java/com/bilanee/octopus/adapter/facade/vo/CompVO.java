package com.bilanee.octopus.adapter.facade.vo;

import com.bilanee.octopus.common.enums.CompStage;
import com.bilanee.octopus.common.enums.MarketStatus;
import com.bilanee.octopus.basic.StageId;
import com.bilanee.octopus.common.enums.TradeStage;
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
    StageId  stageId;

    /**
     * 当前阶段的截止时间
     */
    Long endingTimeStamp;


    /**
     * 本次参赛的用户id列表
     */
    List<String> traderIds;


}
