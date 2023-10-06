package com.bilanee.octopus.adapter.facade;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilanee.octopus.adapter.facade.po.*;
import com.bilanee.octopus.adapter.facade.vo.*;
import com.bilanee.octopus.adapter.repository.UnitAdapter;
import com.bilanee.octopus.adapter.tunnel.BidQuery;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.*;
import com.bilanee.octopus.basic.enums.*;
import com.bilanee.octopus.domain.IntraSymbol;
import com.bilanee.octopus.domain.Unit;
import com.bilanee.octopus.domain.UnitCmd;
import com.bilanee.octopus.infrastructure.entity.*;
import com.bilanee.octopus.infrastructure.mapper.*;
import com.google.common.collect.ListMultimap;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.base.SysEx;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.domain.support.base.DomainTunnel;
import com.stellariver.milky.domain.support.command.CommandBus;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.mapstruct.Builder;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.stellariver.milky.common.base.ErrorEnumsBase.PARAM_FORMAT_WRONG;

/**
 * 单元信息
 */

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequestMapping("/api/unit")
public class UnitFacade {

    final Tunnel tunnel;
    final UnitDOMapper unitDOMapper;
    final BidDOMapper bidDOMapper;
    final IntraQuotationDOMapper intraQuotationDOMapper;
    final IntraInstantDOMapper intraInstantDOMapper;
    final GeneratorDaSegmentMapper generatorDaSegmentMapper;
    final GeneratorForecastValueMapper generatorForecastValueMapper;
    final GeneratorDaForecastBidMapper generatorDaForecastBidMapper;
    final LoadForecastValueMapper loadForecastValueMapper;
    final LoadDaForecastBidMapper loadDaForecastBidMapper;
    final ThermalCostDOMapper thermalCostDOMapper;
    final DomainTunnel domainTunnel;
    final TieLinePowerDOMapper tieLinePowerDOMapper;
    final MarketSettingMapper marketSettingMapper;
    final SprDOMapper sprDOMapper;

    final Executor executor = Executors.newFixedThreadPool(100);

    /**
     * 本轮被分配的机组信息
     * @param stageId 阶段id
     * @param token 前端携带的token
     * @return 本轮被分配的机组信息
     */
    @GetMapping("listAssignUnitVOs")
    public Result<List<UnitVO>> listAssignUnitVOs(@NotBlank String stageId, @RequestHeader String token) {
        StageId parsedStageId = StageId.parse(stageId);
        List<Unit> units = tunnel.listUnits(parsedStageId.getCompId(), parsedStageId.getRoundId(), TokenUtils.getUserId(token));
        List<UnitVO> unitVOs = Collect.transfer(units, unit -> UnitVO.builder()
                .unitId(unit.getUnitId())
                .unitName(unit.getMetaUnit().getName())
                .metaUnit(unit.getMetaUnit()).build());
        return Result.success(unitVOs);
    }


    /**
     * 省间交易员报价页面，包含省间年度，省间月度
     * @param stageId 阶段id
     * @param token 前端携带的token
     * @return 省间报价回填输入内容
     */
    @SuppressWarnings("unchecked")
    @GetMapping("listInterBidsVOs")
    public Result<List<UnitInterBidVO>> listInterBidsVOs(@NotBlank String stageId, @RequestHeader String token) {

        String userId = TokenUtils.getUserId(token);
        StageId parsedStageId = StageId.parse(stageId);


        LambdaQueryWrapper<UnitDO> queryWrapper = new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getCompId, parsedStageId.getCompId())
                .eq(UnitDO::getRoundId, parsedStageId.getRoundId())
                .eq(UnitDO::getUserId, userId);

        Map<Long, Unit> unitMap = Collect.transfer(unitDOMapper.selectList(queryWrapper), UnitAdapter.Convertor.INST::to).stream()
                .filter(unit -> unit.getMetaUnit().getProvince().interDirection() == unit.getMetaUnit().getUnitType().generalDirection())
                .collect(Collectors.toMap(Unit::getUnitId, u -> u));


        BidQuery bidQuery = BidQuery.builder().compId(parsedStageId.getCompId())
                .unitIds(Collect.transfer(unitMap.values(), Unit::getUnitId, HashSet::new))
                .roundId(parsedStageId.getRoundId())
                .tradeStage(parsedStageId.getTradeStage())
                .build();


        ListMultimap<Long, Bid> groupedByUnitId = tunnel.listBids(bidQuery).stream().collect(Collect.listMultiMap(Bid::getUnitId));

        List<UnitInterBidVO> interBidsVOs = unitMap.entrySet().stream().map(e -> {
            Long uId = e.getKey();
            Unit unit = e.getValue();
            Collection<Bid> bs = groupedByUnitId.get(uId);

            GridLimit priceLimit = unit.getMetaUnit().getPriceLimit();

            Map<TimeFrame, Collection<Bid>> map = bs.stream().collect(Collect.listMultiMap(Bid::getTimeFrame)).asMap();
            Map<TimeFrame, Collection<Bid>> newMap = new HashMap<>(map);
            Collect.subtract(new HashSet<>(TimeFrame.list()), map.keySet()).forEach(t -> newMap.put(t, Collections.EMPTY_LIST));
            List<InterBidVO> interBidVOS = newMap.entrySet().stream().map(ee -> {
                TimeFrame timeFrame = ee.getKey();
                Double capacity = unit.getMetaUnit().getCapacity().get(timeFrame).get(unit.getMetaUnit().getUnitType().generalDirection());
                List<BalanceVO> balanceVOs = unit.getBalance().get(timeFrame).entrySet().stream()
                        .map(eee -> new BalanceVO(eee.getKey(), eee.getValue())).collect(Collectors.toList());
                return InterBidVO.builder().timeFrame(timeFrame)
                        .capacity(capacity)
                        .bidVOs(Collect.transfer(ee.getValue(), Convertor.INST::to))
                        .balanceVOs(balanceVOs)
                        .build();
            }).collect(Collectors.toList());
            return UnitInterBidVO.builder().unitId(unit.getUnitId())
                    .unitType(unit.getMetaUnit().getUnitType())
                    .province(unit.getMetaUnit().getProvince())
                    .unitName(unit.getMetaUnit().getName())
                    .sourceId(unit.getMetaUnit().getSourceId())
                    .priceLimit(priceLimit)
                    .interBidVOS(interBidVOS)
                    .build();
        }).collect(Collectors.toList());

        return Result.success(interBidsVOs);
    }

    /**
     * 省间报价接口
     * @param interBidsPO 省间报价请求结构体
     * @return 报单结果
     */
    @PostMapping("submitInterBidsPO")
    public Result<Void> submitInterBidsPO(@RequestBody InterBidsPO interBidsPO) {

        StageId pStageId = StageId.parse(interBidsPO.getStageId());
        StageId cStageId = tunnel.runningComp().getStageId();
        BizEx.falseThrow(pStageId.equals(cStageId), PARAM_FORMAT_WRONG.message("已经进入下一阶段不可报单"));
        BizEx.trueThrow(cStageId.getTradeStage().getTradeType() != TradeType.INTER,
                PARAM_FORMAT_WRONG.message("当前为中长期省省间报价阶段"));
        BizEx.trueThrow(cStageId.getMarketStatus() != MarketStatus.BID,
                PARAM_FORMAT_WRONG.message("当前竞价阶段已经关闭"));
        List<BidPO> bidPOs = interBidsPO.getBidPOs();
        UnitCmd.InterBids command = UnitCmd.InterBids.builder().stageId(pStageId)
                .bids(Collect.transfer(bidPOs, Convertor.INST::to))
                .build();
        CommandBus.accept(command, new HashMap<>());
        return Result.success();
    }

    /**
     * 省内报价交易员报价页面，包含省间年度，省间月度
     * @param stageId 阶段id
     * @param token 前端携带的token
     * @return 省内报价交易员报价页面
     */
    @SneakyThrows
    @GetMapping("listIntraSymbolBidVOs")
    public Result<List<IntraSymbolBidVO>> listIntraSymbolBidVOs(@NotBlank String stageId, @RequestHeader String token) {


        CompletableFuture<Map<IntraSymbol, IntraInstantDO>> future0 = CompletableFuture.supplyAsync(() -> {
            // prepare instant
            LambdaQueryWrapper<IntraInstantDO> eq0 = new LambdaQueryWrapper<IntraInstantDO>().eq(IntraInstantDO::getStageId, stageId);
            List<IntraInstantDO> intraInstantDOs = intraInstantDOMapper.selectList(eq0);
            return Collect.toMap(intraInstantDOs, i -> new IntraSymbol(i.getProvince(), i.getTimeFrame()));
        }, executor);

        CompletableFuture<ListMultimap<IntraSymbol, IntraQuotationDO>> future1 = CompletableFuture.supplyAsync(() -> {
            // prepare quotation
            LambdaQueryWrapper<IntraQuotationDO> eq1 = new LambdaQueryWrapper<IntraQuotationDO>().eq(IntraQuotationDO::getStageId, stageId);
            List<IntraQuotationDO> intraQuotationDOs = intraQuotationDOMapper.selectList(eq1);
            return intraQuotationDOs.stream().collect(Collect.listMultiMap(i -> new IntraSymbol(i.getProvince(), i.getTimeFrame())));
        }, executor);

        CompletableFuture<List<Unit>> future2 = CompletableFuture.supplyAsync(() -> {
            StageId parsedStageId = StageId.parse(stageId);
            return tunnel.listUnits(parsedStageId.getCompId(), parsedStageId.getRoundId(), TokenUtils.getUserId(token));
        });

        Map<IntraSymbol, IntraInstantDO> instantDOMap = future0.get();
        ListMultimap<IntraSymbol, IntraQuotationDO> quotationDOMap = future1.get();
        List<Unit> units = future2.get();


        List<IntraSymbolBidVO> intraSymbolBidVOs = IntraSymbol.intraSymbols().stream().map(intraSymbol -> {
            IntraSymbolBidVO.IntraSymbolBidVOBuilder builder = IntraSymbolBidVO.builder()
                    .province(intraSymbol.getProvince()).timeFrame(intraSymbol.getTimeFrame());
            IntraInstantDO intraInstantDO = instantDOMap.get(intraSymbol);

            if (intraInstantDO != null) {
                builder.latestPrice(intraInstantDO.getPrice());
                builder.buyAsks(intraInstantDO.getBuyAsks());
                builder.sellAsks(intraInstantDO.getSellAsks());
                builder.buyVolumes(intraInstantDO.getBuyVolumes());
                builder.sellVolumes(intraInstantDO.getSellVolumes());
            }

            builder.unitIntraBidVOs(to(units, StageId.parse(stageId), intraSymbol));

            List<IntraQuotationDO> intraQuotationDOs = quotationDOMap.get(intraSymbol);
            List<QuotationVO> quotationVOs = intraQuotationDOs.stream()
                    .map(i -> new QuotationVO(i.getTimeStamp(), i.getLatestPrice(), i.getBuyQuantity(), i.getSellQuantity()))
                    .sorted(Comparator.comparing(QuotationVO::getTimeStamp)).collect(Collectors.toList());
            builder.quotationVOs(quotationVOs);
            return builder.build();
        }).collect(Collectors.toList());
        return Result.success(intraSymbolBidVOs);
    }

    private List<UnitIntraBidVO> to(List<Unit> units, StageId stageId, IntraSymbol intraSymbol) {
        Set<Long> unitIds = units.stream().map(Unit::getUnitId).collect(Collectors.toSet());

        BidQuery bidQuery = BidQuery.builder().unitIds(unitIds).tradeStage(stageId.getTradeStage())
                .province(intraSymbol.getProvince()).timeFrame(intraSymbol.getTimeFrame()).build();
        ListMultimap<Long, Bid> bidMap = tunnel.listBids(bidQuery).stream().collect(Collect.listMultiMap(Bid::getUnitId));
        return units.stream().map(unit -> {
            UnitIntraBidVO.UnitIntraBidVOBuilder builder = UnitIntraBidVO.builder().unitId(unit.getUnitId())
                    .priceLimit(unit.getMetaUnit().getPriceLimit())
                    .unitName(unit.getMetaUnit().getName())
                    .unitType(unit.getMetaUnit().getUnitType())
                    .sourceId(unit.getMetaUnit().getSourceId());
            List<Bid> bids = bidMap.get(unit.getUnitId());
            UnitType unitType = unit.getMetaUnit().getUnitType();
            Double general = bids.stream().filter(bid -> bid.getDirection() == unitType.generalDirection())
                    .flatMap(b -> b.getDeals().stream()).map(Deal::getQuantity).reduce(0D, Double::sum);
            Double opposite = bids.stream().filter(bid -> bid.getDirection() == unitType.generalDirection().opposite())
                    .flatMap(b -> b.getDeals().stream()).map(Deal::getQuantity).reduce(0D, Double::sum);
            builder.position(general - opposite);
            Double transit = bids.stream().map(Bid::getTransit).reduce(0D, Double::sum);
            builder.transit(transit);

            // 持仓限制
            if (stageId.getTradeStage() != TradeStage.MO_INTRA) {
                Double balance = unit.getBalance().get(intraSymbol.getTimeFrame()).get(unitType.generalDirection());
                BalanceVO balanceVO = BalanceVO.builder().direction(unitType.generalDirection()).balance(balance).build();
                builder.balanceVOs(Collect.asList(balanceVO));
            } else {
                if (unit.getMoIntraDirection() == null) {
                    Double balance0 = unit.getBalance().get(intraSymbol.getTimeFrame()).get(unitType.generalDirection());
                    BalanceVO balanceVO0 = BalanceVO.builder().direction(unitType.generalDirection()).balance(balance0).build();
                    Double balance1 = unit.getBalance().get(intraSymbol.getTimeFrame()).get(unitType.generalDirection().opposite());
                    BalanceVO balanceVO1 = BalanceVO.builder().direction(unitType.generalDirection().opposite()).balance(balance1).build();
                    builder.balanceVOs(Collect.asList(balanceVO0, balanceVO1));
                } else {
                    Double balance = unit.getBalance().get(intraSymbol.getTimeFrame()).get(unit.getMoIntraDirection());
                    BalanceVO balanceVO = BalanceVO.builder().direction(unit.getMoIntraDirection()).balance(balance).build();
                    builder.balanceVOs(Collect.asList(balanceVO));
                }
            }

            // 报单内容
            List<IntraBidVO> intraBidVOs = bids.stream().map(bid -> IntraBidVO.builder().quantity(bid.getQuantity())
                    .transit(bid.getTransit())
                    .bidStatus(bid.getBidStatus())
                    .price(bid.getPrice())
                    .declareTimeStamp(bid.getDeclareTimeStamp())
                    .cancelTimeStamp(bid.getCancelledTimeStamp())
                    .operations(bid.getBidStatus().operations())
                    .intraDealVOs(Collect.transfer(bid.getDeals(), d -> new IntraDealVO(d.getQuantity(), d.getPrice(), d.getTimeStamp())))
                    .build()).collect(Collectors.toList());

            return builder.intraBidVOs(intraBidVOs).build();
        }).collect(Collectors.toList());

    }


    /**
     * 省内报价接口
     * @param intraBidPO 省内报价请求结构体
     * @return 报单结果
     */
    @PostMapping("submitIntraBidPO")
    public Result<Void> submitIntraBidPO(@RequestBody IntraBidPO intraBidPO) {
        StageId pStageId = StageId.parse(intraBidPO.getStageId());
        StageId cStageId = tunnel.runningComp().getStageId();

        BizEx.falseThrow(pStageId.equals(cStageId), PARAM_FORMAT_WRONG.message("已经进入下一阶段不可报单"));
        BizEx.trueThrow(cStageId.getTradeStage().getTradeType() != TradeType.INTRA,
                PARAM_FORMAT_WRONG.message("当前为中长期省省内报价阶段"));
        BizEx.trueThrow(cStageId.getMarketStatus() != MarketStatus.BID,
                PARAM_FORMAT_WRONG.message("当前竞价阶段已经关闭"));

        UnitCmd.IntraBidDeclare command = UnitCmd.IntraBidDeclare.builder()
                .bid(Convertor.INST.to(intraBidPO.getBidPO())).stageId(pStageId).build();
        CommandBus.accept(command, new HashMap<>());

        return Result.success();
    }

    /**
     * 省内撤单接口
     * @param intraCancelPO 省内撤单请求结构体
     * @return 报单结果
     */
    @PostMapping("submitIntraCancelPO")
    public Result<Void> submitIntraCancelPO(IntraCancelPO intraCancelPO) {
        StageId pStageId = StageId.parse(intraCancelPO.getStageId());
        StageId cStageId = tunnel.runningComp().getStageId();

        BizEx.falseThrow(pStageId.equals(cStageId), PARAM_FORMAT_WRONG.message("已经进入下一阶段不可报单"));
        BizEx.trueThrow(cStageId.getTradeStage().getTradeType() != TradeType.INTRA,
                PARAM_FORMAT_WRONG.message("当前为中长期省省内报价阶段"));
        BizEx.trueThrow(cStageId.getMarketStatus() != MarketStatus.BID,
                PARAM_FORMAT_WRONG.message("竞价阶段已经关闭，未达成挂牌，将由系统自动撤单"));

        BidDO bidDO = bidDOMapper.selectById(intraCancelPO.getBidId());
        boolean b0 = bidDO.getBidStatus() == BidStatus.NEW_DECELERATED;
        boolean b1 = bidDO.getBidStatus() == BidStatus.PART_DEAL;
        BizEx.falseThrow(b0 || b1, PARAM_FORMAT_WRONG.message("当前报单处于处于不可撤状态"));
        Long unitId = bidDO.getUnitId();
        UnitCmd.IntraBidCancel command = UnitCmd.IntraBidCancel.builder().unitId(unitId).cancelBidId(intraCancelPO.getBidId()).build();
        CommandBus.accept(command, new HashMap<>());
        return Result.success();
    }

    /**
     * 省内现货报单页面回填接口
     * @param stageId 当前阶段id
     */
    @GetMapping("listDaBidVOs")
    public Result<List<IntraDaBidVO>> listDaBidVOs(@NotBlank String stageId, @RequestHeader String token) {
        String userId = TokenUtils.getUserId(token);
        StageId parsed = StageId.parse(stageId);
        List<Unit> units = tunnel.listUnits(parsed.getCompId(), parsed.getRoundId(), userId);
        List<IntraDaBidVO> intraDaBidVOs = Collect.transfer(units,u -> this.build(u, parsed));
        return Result.success(intraDaBidVOs);
    }

    private IntraDaBidVO build(Unit unit, StageId stageId) {
        UnitType unitType = unit.getMetaUnit().getUnitType();
        GeneratorType generatorType = unit.getMetaUnit().getGeneratorType();
        MetaUnit metaUnit = unit.getMetaUnit();
        GridLimit priceLimit = metaUnit.getPriceLimit();
        IntraDaBidVO.IntraDaBidVOBuilder builder = IntraDaBidVO.builder()
                .unitId(unit.getUnitId()).unitName(unit.getMetaUnit().getName())
                .unitType(unitType).generatorType(generatorType)
                .priceLimit(priceLimit);
        if (unitType == UnitType.GENERATOR) {

            if (generatorType == GeneratorType.CLASSIC) {
                Segment segment = Segment.builder().start(0D).end(metaUnit.getMinCapacity())
                        .price(metaUnit.getMinOutputPrice()).build();
                builder.minSegment(segment);
            }

            double start = generatorType == GeneratorType.CLASSIC ? metaUnit.getMinOutputPrice(): 0D;
            LambdaQueryWrapper<GeneratorDaSegmentBidDO> eq0 = new LambdaQueryWrapper<GeneratorDaSegmentBidDO>()
                    .eq(GeneratorDaSegmentBidDO::getRoundId, stageId.getRoundId() + 1)
                    .eq(GeneratorDaSegmentBidDO::getUnitId, metaUnit.getSourceId());
            List<GeneratorDaSegmentBidDO> gDOs = generatorDaSegmentMapper.selectList(eq0).stream()
                    .sorted(Comparator.comparing(GeneratorDaSegmentBidDO::getOfferId)).collect(Collectors.toList());
            List<Segment> segments = new ArrayList<>();

            for (GeneratorDaSegmentBidDO gDO : gDOs) {
                Double price = gDO.getOfferPrice().equals(0D) ? null : gDO.getOfferPrice();
                Segment segment = Segment.builder().start(start).end(start + gDO.getOfferMw()).price(price).build();
                segments.add(segment);
                start = start + gDO.getOfferMw();
            }
            segments.get(segments.size() - 1).setEnd(metaUnit.getMaxCapacity());

            for (int i = 1; i < segments.size(); i++) {
                if (segments.get(i).getStart().equals(start)) {
                    segments.get(i).setStart(null);
                }
            }

            for (int i = 0; i < segments.size() - 1; i++) {
                if (segments.get(i).getEnd().equals(start)) {
                    segments.get(i).setEnd(null);
                }
            }

            builder.segments(segments);
            if (generatorType == GeneratorType.RENEWABLE) {

                LambdaQueryWrapper<GeneratorForecastValueDO> eq1 = new LambdaQueryWrapper<GeneratorForecastValueDO>()
                        .eq(GeneratorForecastValueDO::getUnitId, unit.getMetaUnit().getSourceId());
                List<Double> declares = generatorForecastValueMapper.selectList(eq1).stream()
                        .sorted(Comparator.comparing(GeneratorForecastValueDO::getPrd))
                        .map(GeneratorForecastValueDO::getDaPForecast)
                        .collect(Collectors.toList());

                builder.declares(declares);
                LambdaQueryWrapper<GeneratorDaForecastBidDO> eq2 = new LambdaQueryWrapper<GeneratorDaForecastBidDO>()
                        .eq(GeneratorDaForecastBidDO::getRoundId, stageId.getRoundId() + 1)
                        .eq(GeneratorDaForecastBidDO::getUnitId, unit.getMetaUnit().getSourceId());
                List<Double> forecasts = generatorDaForecastBidMapper.selectList(eq2)
                        .stream().sorted(Comparator.comparing(GeneratorDaForecastBidDO::getPrd))
                        .map(GeneratorDaForecastBidDO::getForecastMw)
                        .collect(Collectors.toList());
                builder.forecasts(forecasts);
            }

        } else if (unitType == UnitType.LOAD){
            LambdaQueryWrapper<LoadForecastValueDO> eq0 = new LambdaQueryWrapper<LoadForecastValueDO>()
                    .eq(LoadForecastValueDO::getLoadId, unit.getMetaUnit().getSourceId());
            List<Double> forecasts = loadForecastValueMapper.selectList(eq0).stream()
                    .sorted(Comparator.comparing(LoadForecastValueDO::getPrd)).map(LoadForecastValueDO::getDaPForecast).collect(Collectors.toList());
            builder.forecasts(forecasts);
            LambdaQueryWrapper<LoadDaForecastBidDO> eq1 = new LambdaQueryWrapper<LoadDaForecastBidDO>()
                    .eq(LoadDaForecastBidDO::getRoundId, stageId.getRoundId() + 1)
                    .eq(LoadDaForecastBidDO::getLoadId, unit.getMetaUnit().getSourceId());
            List<Double> declares = loadDaForecastBidMapper.selectList(eq1).stream().sorted(Comparator.comparing(LoadDaForecastBidDO::getPrd))
                    .map(LoadDaForecastBidDO::getBidMw).collect(Collectors.toList());
            builder.declares(declares);
        } else {
            throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
        }

        return builder.build();
    }

    /**
     * 省内现货报单页面报单接口
     * @param stageId 当前阶段id
     * @param intraDaBidPO 省内现货报单结构体
     */
    @PostMapping("submitDaBidVO")
    public Result<List<IntraDaBidVO>> submitDaBidVO(@NotBlank String stageId,
                                                    @RequestBody IntraDaBidPO intraDaBidPO, @RequestHeader String token) {
        StageId parsed = StageId.parse(stageId);
        boolean equals = tunnel.runningComp().getStageId().equals(parsed);
        BizEx.falseThrow(equals, PARAM_FORMAT_WRONG.message("已经进入下一阶段"));
        Long unitId = intraDaBidPO.getUnitId();
        Unit unit = domainTunnel.getByAggregateId(Unit.class, unitId);
        UnitType unitType = unit.getMetaUnit().getUnitType();
        GeneratorType generatorType = unit.getMetaUnit().getGeneratorType();
        if (unitType == UnitType.GENERATOR) {
            LambdaQueryWrapper<GeneratorDaSegmentBidDO> eq0 = new LambdaQueryWrapper<GeneratorDaSegmentBidDO>()
                    .eq(GeneratorDaSegmentBidDO::getRoundId, parsed.getRoundId() + 1)
                    .eq(GeneratorDaSegmentBidDO::getUnitId, unit.getMetaUnit().getSourceId());
            List<GeneratorDaSegmentBidDO> gSegmentBidDOs = generatorDaSegmentMapper.selectList(eq0);
            List<Segment> segments = intraDaBidPO.getSegments();
            IntStream.range(0, gSegmentBidDOs.size()).forEach(i -> {
                Segment segment = intraDaBidPO.getSegments().get(i);
                GeneratorDaSegmentBidDO generatorDaSegmentBidDO = gSegmentBidDOs.get(i);
                double v = segments.get(i).getEnd() - segments.get(i).getStart();
                generatorDaSegmentBidDO.setOfferMw(v);
                generatorDaSegmentBidDO.setOfferPrice(segment.getPrice());
            });
            gSegmentBidDOs.forEach(generatorDaSegmentMapper::updateById);

            if (generatorType == GeneratorType.RENEWABLE) {
                LambdaQueryWrapper<GeneratorDaForecastBidDO> eq2 = new LambdaQueryWrapper<GeneratorDaForecastBidDO>()
                        .eq(GeneratorDaForecastBidDO::getRoundId, parsed.getRoundId() + 1)
                        .eq(GeneratorDaForecastBidDO::getUnitId, unit.getMetaUnit().getSourceId());
                List<GeneratorDaForecastBidDO> gForecastDOs = generatorDaForecastBidMapper.selectList(eq2).stream()
                        .sorted(Comparator.comparing(GeneratorDaForecastBidDO::getPrd)).collect(Collectors.toList());
                List<Double> declares = intraDaBidPO.getDeclares();
                IntStream.range(0, gForecastDOs.size()).forEach(i -> gForecastDOs.get(i).setForecastMw(declares.get(i)));
                gForecastDOs.forEach(generatorDaForecastBidMapper::updateById);
            }
        } else if (unitType == UnitType.LOAD) {
            LambdaQueryWrapper<LoadDaForecastBidDO> eq1 = new LambdaQueryWrapper<LoadDaForecastBidDO>()
                    .eq(LoadDaForecastBidDO::getRoundId, parsed.getRoundId() + 1)
                    .eq(LoadDaForecastBidDO::getLoadId, unit.getMetaUnit().getSourceId());
            List<LoadDaForecastBidDO> lForecastBidDOs = loadDaForecastBidMapper.selectList(eq1).stream()
                    .sorted(Comparator.comparing(LoadDaForecastBidDO::getPrd)).collect(Collectors.toList());
            IntStream.range(0, lForecastBidDOs.size()).forEach(i -> lForecastBidDOs.get(i).setBidMw(intraDaBidPO.getDeclares().get(i)));
            lForecastBidDOs.forEach(loadDaForecastBidMapper::updateById);
        } else {
            throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
        }

        return null;
    }


    /**
     * 现货阶段成本计算接口
     * @param unitId 待计算报价单元id
     * @param start 待计算报价段起点
     * @param end 待计算报价段终点
     */
    @GetMapping("calculateDaCost")
    public double calculateDaCost(@NotNull @Positive Long unitId,
                                  @NotNull @Positive Double start,
                                  @NotNull @Positive Double end) {
        BizEx.trueThrow(end <= start, PARAM_FORMAT_WRONG.message("报价段右端点应该小于左端点"));
        Unit unit = domainTunnel.getByAggregateId(Unit.class, unitId);
        if (unit.getMetaUnit().getGeneratorType() == GeneratorType.RENEWABLE) {
            return -400;
        }
        Double minCapacity = unit.getMetaUnit().getMinCapacity();
        start = start - minCapacity;
        end = end - minCapacity;
        LambdaQueryWrapper<ThermalCostDO> eq = new LambdaQueryWrapper<ThermalCostDO>().eq(ThermalCostDO::getUnitId, unit.getMetaUnit().getSourceId());
        List<ThermalCostDO> thermalCostDOs = thermalCostDOMapper.selectList(eq).stream()
                .sorted(Comparator.comparing(ThermalCostDO::getSpotCostId)).collect(Collectors.toList());

        List<CostSection> sections = buildCostSections(thermalCostDOs);

        double accumulate = 0D;
        for (CostSection section : sections) {
            if (start >= section.getLeft() && start < section.getRight())
                accumulate += (section.getRight() - start) * section.getPrice();
            else if (end > section.getLeft() && end <= section.getRight()) {
                accumulate += (end - section.getLeft()) * section.getPrice();
            } else if (start < section.getLeft() || end > section.getRight()){
                accumulate += (section.getRight() - section.getLeft()) * section.getPrice();
            }
        }
        return accumulate/(end - start);
    }

    private List<CostSection> buildCostSections(List<ThermalCostDO> thermalCostDOs) {
        Double start = 0D;
        List<CostSection> sections = new ArrayList<>();
        for (ThermalCostDO thermalCostDO : thermalCostDOs) {
            CostSection section = CostSection.builder().left(start)
                    .right(start + thermalCostDO.getSpotCostMw()).price(thermalCostDO.getSpotCostMarginalCost()).build();
            sections.add(section);
            start += thermalCostDO.getSpotCostMw();
        }
        return sections;
    }


    @Data
    @lombok.Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static private class CostSection {
        Double left;
        Double right;
        Double price;
    }


    @Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public interface Convertor extends BasicConvertor {

        Convertor INST = Mappers.getMapper(Convertor.class);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        Bid to(BidPO bidPO);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        BidVO to(Bid bid);

    }

}
