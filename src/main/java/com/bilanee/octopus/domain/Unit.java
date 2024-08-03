package com.bilanee.octopus.domain;

import com.bilanee.octopus.adapter.tunnel.BidQuery;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.*;
import com.bilanee.octopus.basic.enums.BidStatus;
import com.bilanee.octopus.basic.enums.Direction;
import com.bilanee.octopus.basic.enums.TimeFrame;
import com.bilanee.octopus.basic.enums.TradeStage;
import com.bilanee.octopus.infrastructure.mapper.BidDOMapper;
import com.google.common.collect.ListMultimap;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.StaticWire;
import com.stellariver.milky.common.base.SysEx;
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

import java.util.*;
import java.util.stream.Collectors;

import static com.stellariver.milky.common.base.ErrorEnumsBase.PARAM_FORMAT_WRONG;

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
    Map<Integer, Boolean> rollBidden;
    Map<TimeFrame, Direction> moIntraDirection;

    @StaticWire
    static private UniqueIdGetter uniqueIdGetter;
    @StaticWire
    static private Tunnel tunnel;
    @StaticWire
    static private ProcessorManager processorManager;
    @StaticWire
    static private BidDOMapper bidDOMapper;

    @Override
    public String getAggregateId() {
        return unitId.toString();
    }


    @ConstructorHandler
    public static Unit create(UnitCmd.Create command, Context context) {
        Unit unit = Convertor.INST.to(command);
        unit.setMoIntraDirection(new HashMap<>());
        UnitEvent.Created created = UnitEvent.Created.builder().unit(unit).build();
        context.publish(created);
        return unit;
    }


    @MethodHandler
    public void handle(UnitCmd.InterBids command, Context context) {

        List<Bid> bids = command.getBids();

        // 报单方向
        Direction direction = bids.get(0).getDirection();

        // 方向限制
        BizEx.trueThrow(direction != metaUnit.getUnitType().generalDirection(), PARAM_FORMAT_WRONG.message("买卖方向错误"));

        // 价格限制
        GridLimit gridLimit = tunnel.priceLimit(metaUnit.getUnitType());
        bids.forEach(bid -> gridLimit.check(bid.getPrice()));

        // 持仓限制
        bids.stream().collect(Collect.listMultiMap(Bid::getTimeFrame)).asMap().forEach((t, vs) -> {
            Double declareQuantity = vs.stream().map(Bid::getQuantity).reduce(0D, Double::sum);
            Double tBalance = balance.get(t).get(direction);
            BizEx.trueThrow(declareQuantity > tBalance, PARAM_FORMAT_WRONG.message("超过持仓限制"));
        });
        Comp comp = tunnel.runningComp();
        // 填充委托其他参数
        bids.forEach(bid -> {
            bid.setBidId(uniqueIdGetter.get());
            bid.setUserId(userId);
            bid.setCompId(command.getStageId().getCompId());
            bid.setProvince(metaUnit.getProvince());
            bid.setRoundId(command.getStageId().getRoundId());
            bid.setTradeStage(command.getStageId().getTradeStage());
            bid.setDeclareTimeStamp(Clock.currentTimeMillis());
            bid.setBidStatus(BidStatus.NEW_DECELERATED);
        });

        // 数据库覆盖
        tunnel.coverBids(bids);

        context.publishPlaceHolderEvent(getAggregateId());
    }

    @MethodHandler
    public void handle(UnitCmd.InterDeduct command, Context context) {
        StageId stageId = tunnel.runningComp().getStageId();
        BidQuery bidQuery = BidQuery.builder().compId(stageId.getCompId())
                .roundId(stageId.getRoundId())
                .tradeStage(stageId.getTradeStage())
                .unitIds(Collect.asSet(unitId)).build();
        List<Bid> bids = tunnel.listBids(bidQuery);
        bids.stream().collect(Collect.listMultiMap(Bid::getTimeFrame)).asMap().forEach((timeFrame, bs) -> bs.forEach(bid -> {
            Double unitBalance = balance.get(timeFrame).get(bid.getDirection());
            Double dealed = bid.getDeals().stream().map(Deal::getQuantity).reduce(0D, Double::sum);
            balance.get(timeFrame).put(bid.getDirection(), unitBalance - dealed);
        }));
        context.publishPlaceHolderEvent(getAggregateId());
    }



    @MethodHandler
    public void handle(UnitCmd.IntraBidDeclare command, Context context) {

        Bid bid = command.getBid();
        bid.setBidId(uniqueIdGetter.get());
        bid.setUserId(userId);
        bid.setCompId(command.getStageId().getCompId());
        bid.setProvince(metaUnit.getProvince());
        bid.setRoundId(tunnel.runningComp().getRoundId());
        bid.setTradeStage(command.getStageId().getTradeStage());
        bid.setDeclareTimeStamp(Clock.currentTimeMillis());
        bid.setBidStatus(BidStatus.NEW_DECELERATED);

        TradeStage tradeStage = tunnel.runningComp().getStageId().getTradeStage();
        if (tradeStage == TradeStage.MO_INTRA) {
            log.info("intra bid {}", bid);
            Direction direction = moIntraDirection.computeIfAbsent(bid.getTimeFrame(), k -> bid.getDirection());
            log.info("moIntraDirection {}", moIntraDirection);
            BizEx.trueThrow(bid.getDirection() != direction, PARAM_FORMAT_WRONG.message("省内月度报单必须保持同一个方向"));
        } else {
            BizEx.trueThrow(bid.getDirection() != metaUnit.getUnitType().generalDirection(), PARAM_FORMAT_WRONG.message("省内年度报单方向错误"));
        }


        Double unitBalance = balance.get(bid.getTimeFrame()).get(bid.getDirection());
        BizEx.trueThrow(unitBalance < bid.getTransit(), PARAM_FORMAT_WRONG.message("报单超过持仓量"));

        balance.get(bid.getTimeFrame()).put(bid.getDirection(), unitBalance - bid.getTransit());

        processorManager.declare(bid);
        context.publishPlaceHolderEvent(getAggregateId());
    }

    @MethodHandler
    public void handle(UnitCmd.IntraBidCancel command, Context context) {
        processorManager.cancel(command.getCancelBidId());
        context.publishPlaceHolderEvent(getAggregateId());
    }

    @MethodHandler
    public void handle(UnitCmd.RollBidDeclare command, Context context) {
        Bid bid = command.getBid();
        boolean bidden = Boolean.TRUE.equals(rollBidden.get(bid.getInstant()));
        BizEx.trueThrow(bidden, PARAM_FORMAT_WRONG.message("时刻" + bid.getInstant() + "已有报单"));
        bid.setBidId(uniqueIdGetter.get());
        bid.setUserId(userId);
        bid.setCompId(command.getStageId().getCompId());
        bid.setProvince(metaUnit.getProvince());
        bid.setRoundId(tunnel.runningComp().getRoundId());
        bid.setTradeStage(command.getStageId().getTradeStage());
        bid.setDeclareTimeStamp(Clock.currentTimeMillis());
        bid.setBidStatus(BidStatus.NEW_DECELERATED);
        Integer instant = bid.getInstant();
        TimeFrame timeFrame = Arrays.stream(TimeFrame.values()).filter(t -> t.getPrds().contains(instant)).findFirst().orElseThrow(SysEx::unreachable);
        Double unitBalance = balance.get(TimeFrame.getByInstant(bid.getInstant())).get(bid.getDirection());
        BizEx.trueThrow(unitBalance < bid.getTransit(), PARAM_FORMAT_WRONG.message("报单超过持仓量"));

        log.info("processorManager.declare(bid) is {}", bid);
        processorManager.declare(bid);
        rollBidden.put(instant, true);
        context.publishPlaceHolderEvent(getAggregateId());
    }

    @MethodHandler
    public void handle(UnitCmd.RollBidCancelled command, Context context) {
        Bid bid = tunnel.getByBidId(command.getCancelBidId());
        if (Collect.isEmpty(bid.getDeals())) {
            rollBidden.remove(bid.getInstant());
        }
        context.publishPlaceHolderEvent(getAggregateId());
    }

    @MethodHandler
    public void handle(UnitCmd.RollBidCancel command, Context context) {
        processorManager.cancel(command.getCancelBidId());
        context.publishPlaceHolderEvent(getAggregateId());
    }

    @MethodHandler
    public void handle(UnitCmd.IntraBidCancelled command, Context context) {
        Bid bid = tunnel.getByBidId(command.getCancelBidId());
        Double unitBalance = balance.get(bid.getTimeFrame()).get(bid.getDirection());
        double returnBalance = bid.getQuantity() - bid.getDeals().stream().map(Deal::getQuantity).reduce(0D, Double::sum);
        balance.get(bid.getTimeFrame()).put(bid.getDirection(), unitBalance + returnBalance);
        context.publishPlaceHolderEvent(getAggregateId());
    }

    @MethodHandler
    public void handle(UnitCmd.FillBalance command, Context context) {
        if (command.getTradeStage() == TradeStage.MO_INTRA) {
            balance.forEach(((timeFrame, balances) -> {
                Direction generalDirection = metaUnit.getUnitType().generalDirection();
                Double reverseBalance = metaUnit.getCapacity().get(timeFrame).get(generalDirection) - balances.get(generalDirection);
                balances.put(generalDirection.opposite(), reverseBalance);
            }));
            context.publishPlaceHolderEvent(getAggregateId());
        } else if (command.getTradeStage() == TradeStage.ROLL) {
            BidQuery bidQuery = BidQuery.builder().unitIds(Collect.asSet(unitId))
                    .tradeStage(TradeStage.MO_INTRA)
                    .compId(compId)
                    .roundId(roundId)
                    .build();
            ListMultimap<TimeFrame, Deal> deals = tunnel.listBids(bidQuery).stream().flatMap(l -> l.getDeals().stream()).collect(Collect.listMultiMap(Deal::getTimeFrame));
            Arrays.stream(TimeFrame.values()).filter(t -> !deals.get(t).isEmpty()).forEach(t -> {
                List<Deal> tfDeals = deals.get(t);
                double sum = tfDeals.stream().collect(Collectors.summarizingDouble(Deal::getQuantity)).getSum();
                if (Objects.equals(tfDeals.get(0).getBuyUnitId(), unitId)) {
                    balance.get(t).put(Direction.SELL, balance.get(t).get(Direction.SELL) + sum);
                } else if (Objects.equals(tfDeals.get(0).getSellUnitId(), unitId)) {
                    balance.get(t).put(Direction.BUY, balance.get(t).get(Direction.BUY) + sum);
                } else {
                    throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
                }
            });
            context.publishPlaceHolderEvent(getAggregateId());
        } else {
            throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
        }
    }

    @Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public interface Convertor extends BasicConvertor {

        Convertor INST = Mappers.getMapper(Convertor.class);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        Unit to(UnitCmd.Create command);

    }

}
