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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Data
@CustomLog
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Comp extends AggregateRoot {

    Long compId;

    Integer roundTotal;
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
        comp.setRoundTotal(command.getRoundTotal());

        Map<UnitType, GridLimit> priceLimits = tunnel.priceLimits();

        command.getRoundMetaUnits().stream().map(Map::values)
                .flatMap(Collection::stream).flatMap(Collection::stream)
                .forEach(metaUnit -> metaUnit.setPriceLimit(priceLimits.get(metaUnit.getUnitType())));

        // fill stage step trigger
        StageId stageId = comp.getStageId();
        CompCmd.Step stepCommand = CompCmd.Step.builder().stageId(stageId.next(comp)).build();
        pushDelayCommand(stepCommand, comp.endingTimeStamp);
        CompEvent.Created event = CompEvent.Created.builder().comp(comp).roundMetaUnits(command.getRoundMetaUnits()).build();
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

        bids = bids.stream().filter(bid -> bid.getQuantity() > 0).collect(Collectors.toList());
        List<Bid> sortedBuyBids = bids.stream().filter(bid -> bid.getDirection() == Direction.BUY)
                .sorted(Comparator.comparing(Bid::getPrice).reversed())
                .collect(Collectors.toList());
        List<Bid> sortedSellBids = bids.stream().filter(bid -> bid.getDirection() == Direction.SELL)
                .sorted(Comparator.comparing(Bid::getPrice))
                .collect(Collectors.toList());

        RangeMap<Double, Range<Double>> buyBrokenLine = ClearUtil.buildRangeMap(sortedBuyBids, Double.MAX_VALUE, 0D);
        RangeMap<Double, Range<Double>> sellBrokenLine = ClearUtil.buildRangeMap(sortedSellBids, 0D, Double.MAX_VALUE);

        Point<Double> interPoint = ClearUtil.analyzeInterPoint(buyBrokenLine, sellBrokenLine);
        GridLimit transLimit = tunnel.transLimit(getStageId(), timeFrame);
        double nonMarketQuantity = 0D, marketQuantity = 0D;
        if (interPoint != null) {
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
            marketQuantity = interPoint.x;
        }


        if (interPoint != null) {
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
        interClearBOBuilder.buyDeclaredQuantity(buyDeclaredQuantity)
                .sellDeclaredQuantity(sellDeclaredQuantity)
                .dealQuantity(marketQuantity)
                .dealPrice(interPoint == null ? null : interPoint.y);

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
                    .lx(x).y(sortedBid.getPriceAfterTariff()).rx(x + sortedBid.getQuantity()).build();
            x += sortedBid.getQuantity();
            sections.add(section);
        }
        return sections.stream().map(s -> new Section(s.getUnitId(), point2(s.getLx()), point2(s.getRx()), point2(s.getY()))).collect(Collectors.toList());
    }

    static private final DecimalFormat df = new DecimalFormat("0.00");

    private Double point2(Double value) {
        BigDecimal bigDecimal = new BigDecimal(String.valueOf(value));
        df.setRoundingMode(RoundingMode.HALF_UP);
        String format = df.format(bigDecimal);
        return Double.parseDouble(format);
    }

    @MethodHandler
    public void step(CompCmd.Step command, Context context) {
        StageId last = getStageId();
        this.compStage = command.getStageId().getCompStage();
        this.roundId = command.getStageId().getRoundId();
        this.tradeStage = command.getStageId().getTradeStage();
        this.marketStatus = command.getStageId().getMarketStatus();
        StageId now = getStageId();

        if (this.tradeStage == TradeStage.END || compStage == CompStage.RANKING) {
            this.endingTimeStamp = null;
        } else if (!Arrays.asList(CompStage.REVIEW, CompStage.RANKING, CompStage.END).contains(this.compStage)) {
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
        delayExecutor.schedule(command, executeTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
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
