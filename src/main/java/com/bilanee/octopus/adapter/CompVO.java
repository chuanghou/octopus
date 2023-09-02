package com.bilanee.octopus.adapter;

import com.bilanee.octopus.basic.CompStage;
import com.bilanee.octopus.basic.MarketStatus;
import com.bilanee.octopus.basic.TradeStage;
import lombok.*;
import lombok.experimental.FieldDefaults;

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

    CompStage compStage;

    /**
     * 交易轮次
     */
    Integer roundId;

    /**
     * 交易阶段
     * @see TradeStage
     */
    TradeStage tradeStage;

    /**
     * 市场状态
     * @see MarketStatus
     */
    MarketStatus marketStatus;

    /**
     * 当前阶段的截止时间
     */
    Long endingTimeStamp;

}
