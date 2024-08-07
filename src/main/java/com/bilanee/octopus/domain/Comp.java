package com.bilanee.octopus.domain;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilanee.octopus.adapter.repository.UnitAdapter;
import com.bilanee.octopus.adapter.tunnel.BidQuery;
import com.bilanee.octopus.adapter.tunnel.InterClearance;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.*;
import com.bilanee.octopus.basic.enums.*;
import com.bilanee.octopus.config.OctopusProperties;
import com.bilanee.octopus.infrastructure.entity.MarketSettingDO;
import com.bilanee.octopus.infrastructure.entity.MultiYearUnitOfferDO;
import com.bilanee.octopus.infrastructure.entity.StepRecord;
import com.bilanee.octopus.infrastructure.entity.UnitDO;
import com.bilanee.octopus.infrastructure.mapper.MarketSettingMapper;
import com.bilanee.octopus.infrastructure.mapper.MultiYearUnitOfferDOMapper;
import com.bilanee.octopus.infrastructure.mapper.UnitDOMapper;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.stellariver.milky.common.base.BeanUtil;
import com.stellariver.milky.common.base.StaticWire;
import com.stellariver.milky.common.base.SysEx;
import com.stellariver.milky.common.tool.common.Clock;
import com.stellariver.milky.common.tool.util.Collect;
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

    @StaticWire
    static private MultiYearUnitOfferDOMapper multiYearUnitOfferDOMapper;

    @StaticWire
    static ProcessorManager processorManager;

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
            doClear(timeFrameBids, t, null);
        });
        context.publishPlaceHolderEvent(getAggregateId());
    }

    @MethodHandler
    public void clear(CompCmd.ClearMulti command, Context context) {

        SysEx.trueThrow(tradeStage != TradeStage.MULTI_ANNUAL, ErrorEnums.SYS_EX);
        SysEx.trueThrow(marketStatus != MarketStatus.CLEAR, ErrorEnums.SYS_EX);

        LambdaQueryWrapper<MultiYearUnitOfferDO> eq0 = new LambdaQueryWrapper<MultiYearUnitOfferDO>()
                .eq(MultiYearUnitOfferDO::getRoundId, roundId + 1);
        List<MultiYearUnitOfferDO> unitOffers = multiYearUnitOfferDOMapper.selectList(eq0);
        LambdaQueryWrapper<UnitDO> eq1 = new LambdaQueryWrapper<UnitDO>().eq(UnitDO::getCompId, compId).eq(UnitDO::getRoundId, roundId);
        List<UnitDO> unitDOs = BeanUtil.getBean(UnitDOMapper.class).selectList(eq1).stream()
                .filter(u -> GeneratorType.RENEWABLE.equals(u.getMetaUnit().getGeneratorType())).collect(Collectors.toList());
        List<Unit> units = Collect.transfer(unitDOs, UnitAdapter.Convertor.INST::to);
        Map<Integer, Unit> unitMap = Collect.toMap(units, u -> u.getMetaUnit().getSourceId());

        unitOffers.forEach(unitOffer -> {
            Unit unit = unitMap.get(unitOffer.getUnitId());
            Bid templateBid = Bid.builder()
                    .userId(unit.getUserId())
                    .compId(compId)
                    .unitId(unit.getUnitId())
                    .province(unit.getMetaUnit().getProvince())
                    .renewableType(unit.getMetaUnit().getRenewableType())
                    .roundId(unit.getRoundId())
                    .tradeStage(TradeStage.MULTI_ANNUAL)
                    .declareTimeStamp(System.currentTimeMillis())
                    .deals(new ArrayList<>())
                    .bidStatus(BidStatus.NEW_DECELERATED).build();

            Bid bid1 = templateBid.toBuilder().bidId(uniqueIdGetter.get())
                    .quantity(unitOffer.getOfferMwh1()).price(unitOffer.getOfferPrice1()).direction(Direction.SELL).build();
            tunnel.insertBid(bid1);
            Bid bid2 = templateBid.toBuilder().bidId(uniqueIdGetter.get())
                    .quantity(unitOffer.getOfferMwh2()).price(unitOffer.getOfferPrice2()).direction(Direction.SELL).build();
            tunnel.insertBid(bid2);
            Bid bid3 = templateBid.toBuilder().bidId(uniqueIdGetter.get())
                    .quantity(unitOffer.getOfferMwh3()).price(unitOffer.getOfferPrice3()).direction(Direction.SELL).build();
            tunnel.insertBid(bid3);
        });



        MarketSettingDO marketSettingDO = BeanUtil.getBeanLoader().getBean(MarketSettingMapper.class).selectById(1);
        String renewableSpecialTransactionDemand = marketSettingDO.getRenewableSpecialTransactionDemand();
        String[] split = renewableSpecialTransactionDemand.split(":");
        Double transferWind = Double.valueOf(split[2 * 0 + roundId]);
        Double transferSolar = Double.valueOf(split[2 * 1 + roundId]);
        Double receiverWind = Double.valueOf(split[2 * 2 + roundId]);
        Double receiverSolar = Double.valueOf(split[2 * 3 + roundId]);
        Double solarSpecificPriceCap = marketSettingDO.getSolarSpecificPriceCap();
        Double windSpecificPriceCap = marketSettingDO.getWindSpecificPriceCap();

        Bid systemTemplateBid = Bid.builder()
                .userId("system")
                .compId(compId)
                .unitId(-1L)
                .roundId(roundId)
                .tradeStage(TradeStage.MULTI_ANNUAL)
                .direction(Direction.BUY)
                .declareTimeStamp(System.currentTimeMillis())
                .deals(new ArrayList<>())
                .bidStatus(BidStatus.NEW_DECELERATED)
                .build();

        Bid transferWindBid = systemTemplateBid.toBuilder().bidId(uniqueIdGetter.get())
                .province(Province.TRANSFER)
                .renewableType(RenewableType.WIND)
                .quantity(transferWind)
                .price(windSpecificPriceCap).build();
        tunnel.insertBid(transferWindBid);

        Bid transferSolarBid = systemTemplateBid.toBuilder().bidId(uniqueIdGetter.get())
                .province(Province.TRANSFER)
                .renewableType(RenewableType.SOLAR)
                .quantity(transferSolar)
                .price(solarSpecificPriceCap).build();
        tunnel.insertBid(transferSolarBid);
        Bid receiverWindBid = systemTemplateBid.toBuilder().bidId(uniqueIdGetter.get())
                .province(Province.RECEIVER)
                .renewableType(RenewableType.WIND)
                .quantity(receiverWind)
                .price(windSpecificPriceCap).build();
        tunnel.insertBid(receiverWindBid);
        Bid receiverSolarBid = systemTemplateBid.toBuilder().bidId(uniqueIdGetter.get())
                .province(Province.RECEIVER)
                .renewableType(RenewableType.SOLAR)
                .quantity(receiverSolar)
                .price(solarSpecificPriceCap).build();
        tunnel.insertBid(receiverSolarBid);

        BidQuery bidQuery = BidQuery.builder().compId(compId).roundId(roundId).tradeStage(tradeStage).build();
        List<Bid> bids = tunnel.listBids(bidQuery);

        Arrays.stream(MultiYearFrame.values()).forEach(m -> {
            List<Bid> multiBids = bids.stream()
                    .filter(b -> b.getProvince().equals(m.getProvince()) && b.getRenewableType().equals(m.getRenewableType()))
                    .collect(Collectors.toList());
            doClear(multiBids, null, m);
        });

        context.publishPlaceHolderEvent(getAggregateId());
    }




    @SuppressWarnings("UnstableApiUsage")
    private void doClear(List<Bid> bids, TimeFrame timeFrame, MultiYearFrame multiYearFrame) {

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
        GridLimit transLimit = tunnel.transLimit(getStageId(), timeFrame, multiYearFrame);
        double nonMarketQuantity = 0D, marketQuantity = 0D;
        if (interPoint != null && interPoint.getX() > 0D) {
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
        } else if (transLimit.getLow() > 0){
            nonMarketQuantity = transLimit.getLow();
        }


        if (interPoint != null && interPoint.getX() > 0D) {
            ClearUtil.deal(sortedBuyBids, interPoint, false);
            ClearUtil.deal(sortedSellBids, interPoint, true);
        }

        InterClearance.InterClearanceBuilder interClearBOBuilder = InterClearance.builder()
                .stageId(getStageId())
                .timeFrame(timeFrame)
                .multiYearFrame(multiYearFrame)
                .nonMarketQuantity(nonMarketQuantity)
                .marketQuantity(marketQuantity)
                ;

        Double buyDeclaredQuantity = sortedBuyBids.stream().map(Bid::getQuantity).reduce(0D, Double::sum);
        Double sellDeclaredQuantity = sortedSellBids.stream().map(Bid::getQuantity).reduce(0D, Double::sum);
        interClearBOBuilder.buyDeclaredQuantity(buyDeclaredQuantity)
                .sellDeclaredQuantity(sellDeclaredQuantity)
                .dealQuantity(marketQuantity)
                .dealPrice((interPoint == null || interPoint.getX() == 0D) ? null : interPoint.y);

        GridLimit priceLimit = tunnel.priceLimit(UnitType.GENERATOR, timeFrame, multiYearFrame);

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
    public void step(CompCmd.Step command, Context context) throws InterruptedException {
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

        if (last.getMarketStatus() == MarketStatus.BID) {
            Thread.sleep(3_000L);
            while (processorManager.processors.values().stream().anyMatch(p -> !p.empty())) {
                Thread.sleep(1000L);
            }
        }

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
