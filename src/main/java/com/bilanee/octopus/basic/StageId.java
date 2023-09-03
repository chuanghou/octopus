package com.bilanee.octopus.basic;

import com.stellariver.milky.common.tool.common.Kit;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StageId {

    Long compId;
    CompStage compStage;
    Integer roundId;
    TradeStage tradeStage;
    MarketStatus marketStatus;

    @Override
    public String toString() {
        return String.format("%s.%s.%s.%s.%s", compId, compStage, roundId, tradeStage, marketStatus);
    }

    static public StageId parse(String stageId) {
        String[] split = StringUtils.split(stageId, ".");
        return StageId.builder()
                .compId(Long.parseLong(split[0]))
                .compStage(Kit.enumOfMightEx(CompStage::name, split[1]))
                .roundId(Integer.parseInt(split[2]))
                .tradeStage(Kit.enumOf(TradeStage::name, split[3]).orElse(null))
                .marketStatus(Kit.enumOf(MarketStatus::name, split[4]).orElse(null))
                .build();
    }

}
