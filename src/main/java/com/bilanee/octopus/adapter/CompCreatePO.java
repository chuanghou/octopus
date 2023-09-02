package com.bilanee.octopus.adapter;

import com.bilanee.octopus.basic.TradeStage;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompCreatePO {

    /**
     * 竞赛初始化时长，单位分钟
     */
    @NotNull @Positive
    Integer compInitLength;

    /**
     * 知识问答环节时长，单位分钟
     */
    @NotNull @Positive
    Integer quitCompeteLength;


    /**
     * 知识问答查看结果时长，单位分钟
     */
    @NotNull @Positive
    Integer quitResultLength;

    /**
     * 各个市场的竞价时长，单位分钟
     */
    @NotNull @NotEmpty
    Map<TradeStage, Integer> marketStageBidLengths;

    /**
     * 各个市场的查看结果时长，单位分钟
     */
    @NotNull @NotEmpty
    Map<TradeStage, Integer> marketStageClearLengths;

    /**
     * 各个市场的查看结果时长，单位分钟
     */
    @NotNull @Positive
    Integer tradeResultLength;

}
