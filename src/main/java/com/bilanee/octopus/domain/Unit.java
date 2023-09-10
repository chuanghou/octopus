package com.bilanee.octopus.domain;

import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.*;
import com.bilanee.octopus.basic.enums.BidStatus;
import com.bilanee.octopus.basic.enums.Direction;
import com.bilanee.octopus.basic.enums.TimeFrame;
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
    public void handle(UnitCmd.InterBids command, Context context) {

        List<Bid> bids = command.getBids();

        // 报单方向
        Direction direction = bids.get(0).getDirection();

        // 方向限制
        BizEx.trueThrow(direction != metaUnit.getUnitType().generalDirection(), ErrorEnums.PARAM_FORMAT_WRONG.message("买卖方向错误"));

        // 价格限制
        GridLimit gridLimit = tunnel.priceLimit(metaUnit.getUnitType());
        bids.forEach(bid -> gridLimit.check(bid.getPrice()));

        // 持仓限制
        bids.stream().collect(Collect.listMultiMap(Bid::getTimeFrame)).asMap().forEach((t, vs) -> {
            Double declareQuantity = vs.stream().map(Bid::getQuantity).reduce(0D, Double::sum);
            Double tBalance = balance.get(t).get(direction);
            BizEx.trueThrow(declareQuantity > tBalance, ErrorEnums.PARAM_FORMAT_WRONG.message("超过持仓限制"));
        });
        Comp comp = tunnel.runningComp();
        // 填充委托其他参数
        bids.forEach(bid -> {
            bid.setBidId(uniqueIdGetter.get());
            bid.setUserId(userId);
            bid.setCompId(command.getStageId().getCompId());
            bid.setProvince(metaUnit.getProvince());
            bid.setRoundId(comp.getRoundId());
            bid.setTradeStage(command.getStageId().getTradeStage());
            bid.setDeclareTimeStamp(Clock.currentTimeMillis());
            bid.setBidStatus(BidStatus.NEW_DECELERATED);
        });

        // 数据库覆盖
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
