package com.bilanee.octopus.domain;

import com.bilanee.octopus.adapter.tunnel.BidQuery;
import com.bilanee.octopus.adapter.tunnel.InterClearance;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.*;
import com.bilanee.octopus.basic.enums.*;
import com.bilanee.octopus.config.OctopusProperties;
import com.bilanee.octopus.infrastructure.entity.StepRecord;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.stellariver.milky.common.base.StaticWire;
import com.stellariver.milky.common.base.SysEx;
import com.stellariver.milky.common.tool.common.Clock;
import com.stellariver.milky.common.tool.common.Kit;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.common.tool.util.Json;
import com.stellariver.milky.domain.support.ErrorEnums;
import com.stellariver.milky.domain.support.base.AggregateRoot;
import com.stellariver.milky.domain.support.command.ConstructorHandler;
import com.stellariver.milky.domain.support.command.MethodHandler;
import com.stellariver.milky.domain.support.context.Context;
import com.stellariver.milky.domain.support.dependency.UniqueIdGetter;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Data
@CustomLog
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Comp extends AggregateRoot {

    Long compId;

    Integer roundTotal = 3;
    List<String> userIds;

    CompStage compStage;
    Integer roundId;
    TradeStage tradeStage;
    MarketStatus marketStatus;
    Boolean enableQuiz;

    Long endingTimeStamp;

    DelayConfig delayConfig;

    String dt;

    List<StepRecord> stepRecords = new ArrayList<>();

    @StaticWire
    static private DelayExecutor delayExecutor;
    @StaticWire
    static private OctopusProperties octopusProperties;
    @StaticWire
    static private Tunnel tunnel;
    @StaticWire
    static private UniqueIdGetter uniqueIdGetter;
    @StaticWire
    static private RestTemplate restTemplate;

    @Override
    public String getAggregateId() {
        return compId.toString();
    }


    @ConstructorHandler
    public static Comp create(CompCmd.Create command, Context context) {

        // init comp status
        Comp comp = new Comp();
        comp.setCompId(command.getCompId());
        comp.setUserIds(command.getUserIds());
        comp.setCompStage(CompStage.INIT);
        comp.setDelayConfig(command.getDelayConfig());
        long endingTimeStamp = command.getStartTimeStamp();
        comp.setEndingTimeStamp(endingTimeStamp);
        comp.setEnableQuiz(command.getEnableQuiz());
        comp.setDt(command.getDt());

        // assign metaUnit
        List<Map<String, Collection<MetaUnit>>> roundMetaUnits = IntStream.range(0, comp.getRoundTotal())
                .mapToObj(roundId -> tunnel.assignMetaUnits(roundId, command.getUserIds(), comp)).collect(Collectors.toList());

        Map<UnitType, GridLimit> priceLimits = tunnel.priceLimits();

        roundMetaUnits.stream().map(Map::values)
                .flatMap(Collection::stream).flatMap(Collection::stream)
                .forEach(metaUnit -> metaUnit.setPriceLimit(priceLimits.get(metaUnit.getUnitType())));

        // fill stage step trigger
        StageId stageId = comp.getStageId();
        CompCmd.Step stepCommand = CompCmd.Step.builder().stageId(stageId.next(comp)).build();
        pushDelayCommand(stepCommand, comp.endingTimeStamp);
        CompEvent.Created event = CompEvent.Created.builder().comp(comp).roundMetaUnits(roundMetaUnits).build();
        context.publish(event);
        return comp;

    }

    @MethodHandler
    public void clear(CompCmd.Clear command, Context context) {
        SysEx.trueThrow((tradeStage != TradeStage.AN_INTER) && (tradeStage != TradeStage.MO_INTER), ErrorEnums.SYS_EX);
        SysEx.trueThrow(marketStatus != MarketStatus.CLEAR, ErrorEnums.SYS_EX);
        BidQuery bidQuery = BidQuery.builder().compId(compId).roundId(roundId).tradeStage(tradeStage).build();
        List<Bid> bids = tunnel.listBids(bidQuery);
        Arrays.stream(TimeFrame.values()).forEach(t -> {
            List<Bid> timeFrameBids = bids.stream().filter(b -> b.getTimeFrame().equals(t)).collect(Collectors.toList());
            doClear(timeFrameBids, t);
        });

    }
    @SuppressWarnings("UnstableApiUsage")
    private void doClear(List<Bid> bids, TimeFrame timeFrame) {

        List<Bid> sortedBuyBids = bids.stream().filter(bid -> bid.getDirection() == Direction.BUY)
                .sorted(Comparator.comparing(Bid::getPrice).reversed())
                .collect(Collectors.toList());
        List<Bid> sortedSellBids = bids.stream().filter(bid -> bid.getDirection() == Direction.SELL)
                .sorted(Comparator.comparing(Bid::getPrice))
                .collect(Collectors.toList());

        RangeMap<Double, Range<Double>> buyBrokenLine = ClearUtil.buildRangeMap(sortedBuyBids, Double.MAX_VALUE, 0D);
        RangeMap<Double, Range<Double>> sellBrokenLine = ClearUtil.buildRangeMap(sortedSellBids, 0D, Double.MAX_VALUE);

        Point<Double> interPoint = ClearUtil.analyzeInterPoint(buyBrokenLine, sellBrokenLine);

        //  当没有报价的时候，此时相当于交点处于y轴上，因为成交量是0，所以此时成交价格没有意义
        if (interPoint == null) {
            interPoint = new Point<>(0D, null);
        }

        GridLimit transLimit = tunnel.transLimit(getStageId(), timeFrame);

        double nonMarketQuantity = 0D;
        if (interPoint.x <= transLimit.getLow()) { // 当出清点小于等于最小传输量限制时
            nonMarketQuantity = transLimit.getLow() - interPoint.x;
        }else if (interPoint.x > transLimit.getHigh()) { // // 当出清点大于最大传输量限制时
            interPoint.x = transLimit.getHigh();
            Range<Double> bR = buyBrokenLine.get(interPoint.x);
            Range<Double> sR = sellBrokenLine.get(interPoint.x);
            if (bR == null || sR == null) {
                throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
            }
            interPoint.y = ((bR.upperEndpoint() + bR.lowerEndpoint()) + (sR.upperEndpoint() + sR.lowerEndpoint()))/4;
        }
        double marketQuantity = interPoint.x;

        if (!Kit.eq(interPoint.x, 0D)) {
            ClearUtil.deal(sortedBuyBids, interPoint, uniqueIdGetter);
            ClearUtil.deal(sortedSellBids, interPoint, uniqueIdGetter);
        }

        InterClearance.InterClearanceBuilder interClearBOBuilder = InterClearance.builder()
                .stageId(getStageId()).timeFrame(timeFrame)
                .nonMarketQuantity(nonMarketQuantity)
                .marketQuantity(marketQuantity)
                ;

        Double buyDeclaredQuantity = sortedBuyBids.stream().map(Bid::getQuantity).reduce(0D, Double::sum);
        Double sellDeclaredQuantity = sortedSellBids.stream().map(Bid::getQuantity).reduce(0D, Double::sum);
        List<Deal> buyDeals = sortedBuyBids.stream().flatMap(bid -> bid.getDeals().stream()).collect(Collectors.toList());
        Double dealQuantity = buyDeals.stream().map(Deal::getQuantity).reduce(0D, Double::sum);
        Double dealPrice = Collect.isNotEmpty(buyDeals) ? buyDeals.get(0).getPrice() : null;
        interClearBOBuilder.buyDeclaredQuantity(buyDeclaredQuantity)
                .sellDeclaredQuantity(sellDeclaredQuantity)
                .dealQuantity(dealQuantity)
                .dealPrice(dealPrice);

        GridLimit priceLimit = tunnel.priceLimit(UnitType.GENERATOR);

        List<Section> buildSections = buildSections(sortedBuyBids);
        List<Section> sellSections = buildSections(sortedSellBids);
        interClearBOBuilder.buySections(buildSections)
                .buyTerminus(buildSections.isEmpty() ? null : new Point<>(buildSections.get(buildSections.size() - 1).getRx(), 0D))
                .sellSections(sellSections)
                .sellTerminus(sellSections.isEmpty() ? null : new Point<>(sellSections.get(sellSections.size() -  1).getRx(), priceLimit.getHigh()))
                .transLimit(transLimit);
        tunnel.persistInterClearance(interClearBOBuilder.build());
        tunnel.updateBids(sortedBuyBids);
        tunnel.updateBids(sortedSellBids);
    }

    private List<Section> buildSections(List<Bid> sortedBids) {
        Double x = 0D;
        List<Section> sections = new ArrayList<>();
        for (Bid sortedBid : sortedBids) {
            Section section = Section.builder().unitId(sortedBid.getUnitId())
                    .lx(x).y(sortedBid.getPrice()).rx(x + sortedBid.getQuantity()).build();
            x += sortedBid.getQuantity();
            sections.add(section);
        }
        return sections;
    }


    @MethodHandler
    public void step(CompCmd.Step command, Context context) {
        log.info(Json.toJson(command));
        StageId last = getStageId();
        this.compStage = command.getStageId().getCompStage();
        this.roundId = command.getStageId().getRoundId();
        this.tradeStage = command.getStageId().getTradeStage();
        this.marketStatus = command.getStageId().getMarketStatus();
        StageId now = getStageId();
        if (this.compStage != CompStage.END) {
            long l = command.getDuration() == null ? getStageId().duration(this) : command.getDuration();
            this.endingTimeStamp = System.currentTimeMillis() + l * octopusProperties.getDelayUnits();
            CompCmd.Step stepCommand = CompCmd.Step.builder().stageId(now.next(this)).build();
            pushDelayCommand(stepCommand, endingTimeStamp);
        } else {
            this.endingTimeStamp = null;
        }

        StepRecord stepRecord = StepRecord.builder().stageId(getStageId().toString())
                .startTimeStamp(Clock.currentTimeMillis()).endTimeStamp(endingTimeStamp).build();
        stepRecords.add(stepRecord);

        CompEvent.Stepped stepped = CompEvent.Stepped.builder().compId(compId).last(last).now(now).build();
        context.publish(stepped);
    }


    static private void pushDelayCommand(CompCmd.Step command, long executeTime) {
        DelayCommandWrapper delayCommandWrapper = new DelayCommandWrapper(command, new Date(executeTime));
        delayExecutor.getDelayQueue().add(delayCommandWrapper);
    }

    public StageId getStageId() {
        return StageId.builder()
                .compId(compId)
                .compStage(compStage)
                .roundId(roundId)
                .tradeStage(tradeStage)
                .marketStatus(marketStatus)
                .build();
    }


}
