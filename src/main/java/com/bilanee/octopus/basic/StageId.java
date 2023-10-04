package com.bilanee.octopus.basic;

import com.bilanee.octopus.basic.enums.CompStage;
import com.bilanee.octopus.basic.enums.MarketStatus;
import com.bilanee.octopus.basic.enums.TradeStage;
import com.bilanee.octopus.domain.Comp;
import com.bilanee.octopus.domain.DelayConfig;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.SysEx;
import com.stellariver.milky.common.tool.common.Kit;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

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

    public StageId next(Comp comp) {
        StageId stageId = Convertor.INST.to(this);
        if (compStage == CompStage.INIT) {
            if (comp.getEnableQuiz()) {
                stageId.setCompStage(CompStage.QUIT_COMPETE);
            } else {
                stageId.setCompStage(CompStage.TRADE);
                stageId.setRoundId(0);
                stageId.setTradeStage(TradeStage.AN_INTER);
                stageId.setMarketStatus(MarketStatus.BID);
            }
        } else if (compStage == CompStage.QUIT_COMPETE) {
            stageId.setCompStage(CompStage.QUIT_RESULT);
        } else if (compStage == CompStage.QUIT_RESULT) {
            stageId.setCompStage(CompStage.TRADE);
            stageId.setRoundId(0);
            stageId.setTradeStage(TradeStage.AN_INTER);
            stageId.setMarketStatus(MarketStatus.BID);
        } else if (compStage == CompStage.RANKING) {
            throw new BizEx(ErrorEnums.PARAM_FORMAT_WRONG.message("已经到了最后阶段"));
        } else if (compStage == CompStage.TRADE) {
            if (tradeStage == TradeStage.END) {
                if (stageId.getRoundId() == comp.getRoundTotal() - 1) {
                    stageId.setCompStage(CompStage.RANKING);
                    stageId.setRoundId(null);
                    stageId.setTradeStage(null);
                    stageId.setMarketStatus(null);
                } else {
                    stageId.setRoundId(stageId.getRoundId() + 1);
                    stageId.setTradeStage(TradeStage.AN_INTER);
                    stageId.setMarketStatus(MarketStatus.BID);
                }
            } else if (marketStatus == MarketStatus.BID){
                // 省内现货的查看阶段直接略过
                if (tradeStage == TradeStage.DA_INTRA) {
                    stageId.setTradeStage(TradeStage.DA_INTER);
                } else {
                    stageId.setMarketStatus(MarketStatus.CLEAR);
                }
            } else if (marketStatus == MarketStatus.CLEAR) {
                stageId.setTradeStage(TradeStage.values()[stageId.getTradeStage().ordinal() + 1]);
                stageId.setMarketStatus(MarketStatus.BID);
            }
        }
        return stageId;
    }


    public Integer duration(Comp comp) {
        DelayConfig delayConfig = comp.getDelayConfig();
        if (compStage == CompStage.QUIT_COMPETE) {
            return delayConfig.getQuitCompeteLength();
        } else if (compStage == CompStage.QUIT_RESULT) {
            return delayConfig.getQuitResultLength();
        } else if (compStage == CompStage.TRADE) {
            if (tradeStage != TradeStage.END) {
                if (marketStatus == MarketStatus.BID) {
                    return delayConfig.getMarketStageBidLengths().get(tradeStage);
                } else if (marketStatus == MarketStatus.CLEAR){
                    return delayConfig.getMarketStageClearLengths().get(tradeStage);
                }
            } else {
                return delayConfig.getTradeResultLength();
            }
        }
        throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
    }

    @Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public interface Convertor extends BasicConvertor {

        Convertor INST = Mappers.getMapper(Convertor.class);

        @BeanMapping(builder = @org.mapstruct.Builder(disableBuilder = true))
        StageId to(StageId stageId);

    }

}
