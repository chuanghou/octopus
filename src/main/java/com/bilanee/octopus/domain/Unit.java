package com.bilanee.octopus.domain;

import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.*;
import com.google.common.collect.ListMultimap;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.StaticWire;
import com.stellariver.milky.common.tool.common.Clock;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.domain.support.base.AggregateRoot;
import com.stellariver.milky.domain.support.command.ConstructorHandler;
import com.stellariver.milky.domain.support.command.MethodHandler;
import com.stellariver.milky.domain.support.context.Context;
import com.stellariver.milky.domain.support.dependency.UniqueIdGetter;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.mapstruct.Builder;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@CustomLog
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Unit extends AggregateRoot {

    Long unitId;
    Long compId;
    Integer roundId;
    String userId;
    MetaUnit metaUnit;
    Map<TimeFrame, Map<Direction, Double>> balance;

    @StaticWire
    static private UniqueIdGetter uniqueIdGetter;
    @StaticWire
    static private Tunnel tunnel;

    @Override
    public String getAggregateId() {
        return unitId.toString();
    }


    @ConstructorHandler
    public static Unit create(UnitCmd.Create command, Context context) {
        Unit unit = Convertor.INST.to(command);
        context.publishPlaceHolderEvent(unit.getAggregateId());
        return unit;
    }


    @MethodHandler
    public void handle(UnitCmd.CentralizedBids command, Context context) {

        List<Bid> bids = command.getBids();
        ListMultimap<TimeFrame, Bid> collect = bids.stream().collect(Collect.listMultiMap(Bid::getTimeFrame));
        collect.asMap().forEach((t, vs) -> {
            Double declareQuantity = vs.stream().map(Bid::getQuantity).reduce(0D, Double::sum);
            Bid bid = vs.iterator().next();
            Double tBalance = balance.get(t).get(bid.getDirection());
            BizEx.trueThrow(declareQuantity > tBalance, ErrorEnums.PARAM_FORMAT_WRONG.message("超过持仓限制"));
        });

        StageId stageId = command.getStageId();
        bids.forEach(bid -> {
            bid.setBidId(uniqueIdGetter.get());
            bid.setCompId(stageId.getCompId());
            bid.setProvince(metaUnit.getProvince());
            bid.setTradeStage(stageId.getTradeStage());
            bid.setDeclareTimeStamp(Clock.currentTimeMillis());
            bid.setBidStatus(BidStatus.NEW_DECELERATED);
        });
        tunnel.coverBids(bids);

        context.publishPlaceHolderEvent(getAggregateId());
    }



    @MethodHandler
    public void handle(UnitCmd.RealtimeBid command, Context context) {

    }


    @Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public interface Convertor extends BasicConvertor {

        Convertor INST = Mappers.getMapper(Convertor.class);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        Unit to(UnitCmd.Create command);


    }
}
