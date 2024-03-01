package com.bilanee.octopus.adapter.facade;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilanee.octopus.adapter.facade.po.*;
import com.bilanee.octopus.adapter.facade.vo.*;
import com.bilanee.octopus.adapter.repository.UnitAdapter;
import com.bilanee.octopus.adapter.tunnel.BidQuery;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.*;
import com.bilanee.octopus.basic.enums.*;
import com.bilanee.octopus.domain.Comp;
import com.bilanee.octopus.domain.IntraSymbol;
import com.bilanee.octopus.domain.Unit;
import com.bilanee.octopus.domain.UnitCmd;
import com.bilanee.octopus.infrastructure.entity.*;
import com.bilanee.octopus.infrastructure.mapper.*;
import com.google.common.collect.ListMultimap;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.base.SysEx;
import com.stellariver.milky.common.tool.common.Kit;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.domain.support.base.DomainTunnel;
import com.stellariver.milky.domain.support.command.CommandBus;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.tuple.Pair;
import org.mapstruct.Builder;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    final UnitBasicMapper unitBasicMapper;
    final SpotUnitClearedMapper spotUnitClearedMapper;
    final SpotLoadClearedMapper spotLoadClearedMapper;
    final IntraOfferMapper intraOfferMapper;

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
        StageId currentStageId = tunnel.runningComp().getStageId();
        boolean realTime = currentStageId.equals(parsedStageId);

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
                        .map(eee -> new BalanceVO(eee.getKey(), Double.parseDouble(String.format("%.2f", eee.getValue()))))
                        .filter(b -> b.getBalance() > 0).collect(Collectors.toList());
                return InterBidVO.builder().timeFrame(timeFrame)
                        .capacity(capacity)
                        .bidVOs(Collect.transfer(ee.getValue(), Convertor.INST::to))
                        .balanceVOs(realTime ? balanceVOs : Collections.EMPTY_LIST)
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

        List<BidPO> bidPOs = interBidsPO.getBidPOs().stream()
                .filter(bidPO -> bidPO.getDirection() != null)
                .filter(bidPO -> bidPO.getQuantity() != null && bidPO.getQuantity() > 0)
                .filter(bidPO -> bidPO.getPrice() != null).collect(Collectors.toList());
        BizEx.trueThrow(Collect.isEmpty(bidPOs), PARAM_FORMAT_WRONG.message("无有效报单"));
        Long unitId = interBidsPO.getBidPOs().get(0).getUnitId();
        UnitType unitType = domainTunnel.getByAggregateId(Unit.class, unitId).getMetaUnit().getUnitType();
        GridLimit gridLimit = tunnel.priceLimit(unitType);
        bidPOs.forEach(bidPO -> gridLimit.check(bidPO.getPrice()));
        StageId pStageId = StageId.parse(interBidsPO.getStageId());
        StageId cStageId = tunnel.runningComp().getStageId();
        BizEx.falseThrow(pStageId.equals(cStageId), PARAM_FORMAT_WRONG.message("已经进入下一阶段不可报单"));
        BizEx.trueThrow(cStageId.getTradeStage().getTradeType() != TradeType.INTER,
                PARAM_FORMAT_WRONG.message("当前为中长期省省间报价阶段"));
        BizEx.trueThrow(cStageId.getMarketStatus() != MarketStatus.BID,
                PARAM_FORMAT_WRONG.message("当前竞价阶段已经关闭"));

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

        Comp comp = tunnel.runningComp();
        StepRecord stepRecord = comp.getStepRecords().get(comp.getStepRecords().size() - 1);


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

        boolean notCurrentStage = !tunnel.runningComp().getStageId().toString().equals(stageId);

        List<IntraSymbolBidVO> intraSymbolBidVOs = IntraSymbol.intraSymbols().stream().map(intraSymbol -> {
            IntraSymbolBidVO.IntraSymbolBidVOBuilder builder = IntraSymbolBidVO.builder()
                    .province(intraSymbol.getProvince()).timeFrame(intraSymbol.getTimeFrame());
            IntraInstantDO intraInstantDO = instantDOMap.get(intraSymbol);

            boolean b1 =  System.currentTimeMillis() > stepRecord.getStartTimeStamp() + 60_000L;
            if (intraInstantDO != null) {
                if (notCurrentStage || b1) {
                    builder.latestPrice(intraInstantDO.getPrice());
                    builder.buyAsks(intraInstantDO.getBuyAsks());
                    builder.sellAsks(intraInstantDO.getSellAsks());
                    builder.buyVolumes(intraInstantDO.getBuyVolumes());
                    builder.sellVolumes(intraInstantDO.getSellVolumes());
                }
            }
            List<Unit> us = units.stream().filter(u -> u.getMetaUnit().getProvince().equals(intraSymbol.getProvince())).collect(Collectors.toList());
            builder.unitIntraBidVOs(to(us, StageId.parse(stageId), intraSymbol, comp));

            List<IntraQuotationDO> intraQuotationDOs = quotationDOMap.get(intraSymbol);
            List<QuotationVO> quotationVOs = intraQuotationDOs.stream()
                    .map(i -> new QuotationVO(i.getTimeStamp(), i.getLatestPrice(), i.getBuyQuantity(), i.getSellQuantity()))
                    .sorted(Comparator.comparing(QuotationVO::getTimeStamp)).collect(Collectors.toList());
            builder.quotationVOs(quotationVOs);
            builder.stepRecord(stepRecord);
            return builder.build();
        }).collect(Collectors.toList());
        return Result.success(intraSymbolBidVOs);
    }

    private List<UnitIntraBidVO> to(List<Unit> units, StageId stageId, IntraSymbol intraSymbol, Comp comp) {
        Set<Long> unitIds = units.stream().map(Unit::getUnitId).collect(Collectors.toSet());
        String currentStageId = comp.getStageId().toString();
        StepRecord stepRecord = comp.getStepRecords().stream().filter(s -> s.getStageId().equals(stageId.toString())).findFirst().orElseThrow(SysEx::unreachable);

        BidQuery bidQuery = BidQuery.builder().unitIds(unitIds)
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
            builder.position(Double.parseDouble(String.format("%.2f", general - opposite)));

            bids = bids.stream().filter(bid -> bid.getTradeStage().equals(stageId.getTradeStage())).collect(Collectors.toList());
            Double transit = bids.stream().map(Bid::getTransit).reduce(0D, Double::sum);
            builder.transit(transit);

            List<BalanceVO> balanceVOs;
            // 持仓限制
            if (stageId.getTradeStage() != TradeStage.MO_INTRA) {
                Double balance = unit.getBalance().get(intraSymbol.getTimeFrame()).get(unitType.generalDirection());
                balance = Double.parseDouble(String.format("%.2f", balance));
                BalanceVO balanceVO = BalanceVO.builder().direction(unitType.generalDirection()).balance(balance).build();
                balanceVOs = Collect.asList(balanceVO);
            } else {
                if (unit.getMoIntraDirection().get(intraSymbol.getTimeFrame()) == null) {
                    Double balance0 = unit.getBalance().get(intraSymbol.getTimeFrame()).get(unitType.generalDirection());
                    balance0 = Double.parseDouble(String.format("%.2f", balance0));
                    BalanceVO balanceVO0 = BalanceVO.builder().direction(unitType.generalDirection()).balance(balance0).build();
                    Double balance1 = unit.getBalance().get(intraSymbol.getTimeFrame()).get(unitType.generalDirection().opposite());
                    balance1 = Double.parseDouble(String.format("%.2f", balance1));
                    BalanceVO balanceVO1 = BalanceVO.builder().direction(unitType.generalDirection().opposite()).balance(balance1).build();
                    balanceVOs = Collect.asList(balanceVO0, balanceVO1);
                } else {
                    Double balance = unit.getBalance().get(intraSymbol.getTimeFrame()).get(unit.getMoIntraDirection().get(intraSymbol.getTimeFrame()));
                    balance = Double.parseDouble(String.format("%.2f", balance));
                    BalanceVO balanceVO = BalanceVO.builder().direction(unit.getMoIntraDirection().get(intraSymbol.getTimeFrame())).balance(balance).build();
                    balanceVOs = Collect.asList(balanceVO);
                }
            }


            List<Direction> directions = enableDirections(stepRecord);

            if (Objects.equals(stageId.toString(), currentStageId)) {
                balanceVOs = balanceVOs.stream().filter(b -> directions.contains(b.getDirection())).collect(Collectors.toList());
            } else {
                balanceVOs = new ArrayList<>();
            }
            builder.balanceVOs(balanceVOs);

            // 报单内容
            List<IntraBidVO> intraBidVOs = bids.stream().map(bid -> IntraBidVO.builder()
                    .bidId(bid.getBidId())
                    .quantity(bid.getQuantity())
                    .transit(bid.getTransit())
                    .cancelled(bid.getCancelled())
                    .direction(bid.getDirection())
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
        Comp comp = tunnel.runningComp();
        StageId cStageId = comp.getStageId();
        Long unitId = intraBidPO.getBidPO().getUnitId();
        UnitType unitType = domainTunnel.getByAggregateId(Unit.class, unitId).getMetaUnit().getUnitType();
        GridLimit gridLimit = tunnel.priceLimit(unitType);
        gridLimit.check(intraBidPO.getBidPO().getPrice());
        BizEx.falseThrow(pStageId.equals(cStageId), PARAM_FORMAT_WRONG.message("已经进入下一阶段不可报单"));
        BizEx.trueThrow(cStageId.getTradeStage().getTradeType() != TradeType.INTRA,
                PARAM_FORMAT_WRONG.message("当前为中长期省省内报价阶段"));
        BizEx.trueThrow(cStageId.getMarketStatus() != MarketStatus.BID,
                PARAM_FORMAT_WRONG.message("当前竞价阶段已经关闭"));

        StepRecord stepRecord = comp.getStepRecords().stream().filter(s -> s.getStageId().equals(intraBidPO.getStageId())).findFirst().orElseThrow(SysEx::unreachable);
        List<Direction> directions = enableDirections(stepRecord);
        BizEx.falseThrow(directions.contains(intraBidPO.getBidPO().getDirection()), PARAM_FORMAT_WRONG.message("当前不允许此方向报单"));
        UnitCmd.IntraBidDeclare command = UnitCmd.IntraBidDeclare.builder()
                .bid(Convertor.INST.to(intraBidPO.getBidPO())).stageId(pStageId).build();
        CommandBus.accept(command, new HashMap<>());

        return Result.success();
    }


    public List<Direction> enableDirections(StepRecord stepRecord) {
        long now = System.currentTimeMillis();
        Long startTimeStamp = stepRecord.getStartTimeStamp();
        Long endTimeStamp = stepRecord.getEndTimeStamp();
        if (now >= startTimeStamp && now < startTimeStamp + 60_000) {
            return Collect.asList(Direction.BUY);
        } else if (now >= startTimeStamp + 60_000 && now < startTimeStamp + 120_000) {
            return Collect.asList(Direction.SELL);
        } else {
            return Collect.asList(Direction.BUY, Direction.SELL);
        }
    }

    /**
     * 省内撤单接口
     * @param intraCancelPO 省内撤单请求结构体
     * @return 报单结果
     */
    @PostMapping("submitIntraCancelPO")
    public Result<Void> submitIntraCancelPO(@RequestBody IntraCancelPO intraCancelPO) {
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

    final IntraCostMapper intraCostMapper;

    private IntraDaBidVO build(Unit unit, StageId stageId) {
        UnitType unitType = unit.getMetaUnit().getUnitType();
        GeneratorType generatorType = unit.getMetaUnit().getGeneratorType();
        MetaUnit metaUnit = unit.getMetaUnit();
        GridLimit priceLimit = metaUnit.getPriceLimit();
        IntraDaBidVO.IntraDaBidVOBuilder builder = IntraDaBidVO.builder()
                .unitId(unit.getUnitId()).unitName(unit.getMetaUnit().getName())
                .unitType(unitType).generatorType(generatorType)
                .sourceId(metaUnit.getSourceId())
                .priceLimit(priceLimit);
        if (unitType == UnitType.GENERATOR) {

            Double costStart = tunnel.cost(unit.getUnitId(), 0D);
            Double costEnd = tunnel.cost(unit.getUnitId(), metaUnit.getMaxCapacity());
            builder.costStart(Point.<Double>builder().x(0D).y(costStart).build());
            builder.costEnd(Point.<Double>builder().x(metaUnit.getMaxCapacity()).y(costEnd).build());

            if (generatorType == GeneratorType.CLASSIC) {
                LambdaQueryWrapper<IntraOffer> eq = new LambdaQueryWrapper<IntraOffer>()
                        .eq(IntraOffer::getUnitId, unit.getMetaUnit().getSourceId()).eq(IntraOffer::getRoundId, stageId.getRoundId() + 1);
                LambdaQueryWrapper<IntraCost> eq1 = new LambdaQueryWrapper<IntraCost>().eq(IntraCost::getUnitId, unit.getMetaUnit().getSourceId());
                IntraCost intraCost = intraCostMapper.selectOne(eq1);
                IntraOffer intraOffer = intraOfferMapper.selectOne(eq);
                builder.coldStartupOffer(intraOffer.getColdStartupOffer());
                builder.coldStartupOfferLimit(GridLimit.builder().low(0D).high(intraCost.getColdStartupOfferCap()).build());
                builder.warmStartupOffer(intraOffer.getWarmStartupOffer());
                builder.warmStartupOfferLimit(GridLimit.builder().low(0D).high(intraCost.getWarmStartupOfferCap()).build());
                builder.hotStartupOffer(intraOffer.getHotStartupOffer());
                builder.hotStartupOfferLimit(GridLimit.builder().low(0D).high(intraCost.getHotStartupOfferCap()).build());
                builder.noLoadOffer(intraOffer.getNoLoadOffer());
                builder.noLoadOfferOfferLimit(GridLimit.builder().low(0D).high(intraCost.getNoLoadOfferCap()).build());
            }

            LambdaQueryWrapper<GeneratorDaSegmentBidDO> eq0 = new LambdaQueryWrapper<GeneratorDaSegmentBidDO>()
                    .eq(GeneratorDaSegmentBidDO::getRoundId, stageId.getRoundId() + 1)
                    .eq(GeneratorDaSegmentBidDO::getUnitId, metaUnit.getSourceId());
            List<GeneratorDaSegmentBidDO> gDOs = generatorDaSegmentMapper.selectList(eq0).stream()
                    .sorted(Comparator.comparing(GeneratorDaSegmentBidDO::getOfferId))
                    .skip(generatorType == GeneratorType.RENEWABLE ? 1 : 0)
                    .collect(Collectors.toList());
            if (generatorType == GeneratorType.CLASSIC) {
                gDOs.get(0).setOfferMw(metaUnit.getMinCapacity());
            }
            List<Segment> segments = new ArrayList<>();

            Double start = 0D;
            for (GeneratorDaSegmentBidDO gDO : gDOs) {
                Segment segment = Segment.builder().start(start).end(start + gDO.getOfferMw()).price(gDO.getOfferPrice()).build();
                segments.add(segment);
                start = start + gDO.getOfferMw();
            }
            segments.get(segments.size() - 1).setEnd(metaUnit.getMaxCapacity());

            builder.segments(segments);
            if (generatorType == GeneratorType.RENEWABLE) {

                LambdaQueryWrapper<GeneratorForecastValueDO> eq1 = new LambdaQueryWrapper<GeneratorForecastValueDO>()
                        .eq(GeneratorForecastValueDO::getUnitId, unit.getMetaUnit().getSourceId());
                List<Double> forecasts = generatorForecastValueMapper.selectList(eq1).stream()
                        .sorted(Comparator.comparing(GeneratorForecastValueDO::getPrd))
                        .map(GeneratorForecastValueDO::getDaPForecast)
                        .collect(Collectors.toList());

                builder.forecasts(forecasts);
                LambdaQueryWrapper<GeneratorDaForecastBidDO> eq2 = new LambdaQueryWrapper<GeneratorDaForecastBidDO>()
                        .eq(GeneratorDaForecastBidDO::getRoundId, stageId.getRoundId() + 1)
                        .eq(GeneratorDaForecastBidDO::getUnitId, unit.getMetaUnit().getSourceId());
                List<Double> declares = generatorDaForecastBidMapper.selectList(eq2)
                        .stream().sorted(Comparator.comparing(GeneratorDaForecastBidDO::getPrd))
                        .map(GeneratorDaForecastBidDO::getForecastMw)
                        .collect(Collectors.toList());
                builder.declares(declares);
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
    public Result<Void> submitDaBidVO(@NotBlank String stageId,
                                      @RequestBody IntraDaBidPO intraDaBidPO, @RequestHeader String token) {
        StageId parsed = StageId.parse(stageId);
        boolean equals = tunnel.runningComp().getStageId().equals(parsed);
        BizEx.falseThrow(equals, PARAM_FORMAT_WRONG.message("已经进入下一阶段"));
        Long unitId = intraDaBidPO.getUnitId();
        Unit unit = domainTunnel.getByAggregateId(Unit.class, unitId);
        UnitType unitType = unit.getMetaUnit().getUnitType();
        GeneratorType generatorType = unit.getMetaUnit().getGeneratorType();

        boolean b0 = unitType == UnitType.GENERATOR;
        boolean b1 = generatorType == GeneratorType.CLASSIC;
        boolean b2 = intraDaBidPO.getWarmStartupOffer() == null;
        boolean b3 = intraDaBidPO.getHotStartupOffer() == null;
        boolean b4 = intraDaBidPO.getNoLoadOffer() == null;
        boolean b5 = intraDaBidPO.getColdStartupOffer() == null;
        boolean b = (b0 && b1) && (b2 || b3 || b4 || b5);
        BizEx.trueThrow(b, PARAM_FORMAT_WRONG.message("火电机组启动费用不可为空"));

        if (unitType == UnitType.GENERATOR) {
            LambdaQueryWrapper<GeneratorDaSegmentBidDO> eq0 = new LambdaQueryWrapper<GeneratorDaSegmentBidDO>()
                    .eq(GeneratorDaSegmentBidDO::getRoundId, parsed.getRoundId() + 1)
                    .eq(GeneratorDaSegmentBidDO::getUnitId, unit.getMetaUnit().getSourceId());
            List<GeneratorDaSegmentBidDO> gSegmentBidDOs = generatorDaSegmentMapper.selectList(eq0).stream()
                    .sorted(Comparator.comparing(GeneratorDaSegmentBidDO::getOfferId)).collect(Collectors.toList());
            List<Segment> segments = intraDaBidPO.getSegments();
            int offset = (generatorType == GeneratorType.RENEWABLE) ? 1 : 0;
            IntStream.range(0, gSegmentBidDOs.size() - 1).forEach(i -> {
                Segment segment = intraDaBidPO.getSegments().get(i);
                GeneratorDaSegmentBidDO generatorDaSegmentBidDO = gSegmentBidDOs.get(i + offset);
                double v = segments.get(i).getEnd() - segments.get(i).getStart();
                generatorDaSegmentBidDO.setOfferMw(v);
                generatorDaSegmentBidDO.setOfferPrice(segment.getPrice());
                generatorDaSegmentBidDO.setOfferCost(tunnel.cost(unitId, segment.getStart(), segment.getEnd()));
            });
            gSegmentBidDOs.forEach(generatorDaSegmentMapper::updateById);

            if (generatorType == GeneratorType.RENEWABLE) {
                LambdaQueryWrapper<GeneratorDaForecastBidDO> eq2 = new LambdaQueryWrapper<GeneratorDaForecastBidDO>()
                        .eq(GeneratorDaForecastBidDO::getRoundId, parsed.getRoundId() + 1)
                        .eq(GeneratorDaForecastBidDO::getUnitId, unit.getMetaUnit().getSourceId());
                List<GeneratorDaForecastBidDO> gForecastDOs = generatorDaForecastBidMapper.selectList(eq2).stream()
                        .sorted(Comparator.comparing(GeneratorDaForecastBidDO::getPrd)).collect(Collectors.toList());
                Double maxCapacity = unit.getMetaUnit().getMaxCapacity();
                List<Double> declares = intraDaBidPO.getDeclares().stream().map(dec -> Math.min(dec, maxCapacity)).collect(Collectors.toList());
                IntStream.range(0, gForecastDOs.size()).forEach(i -> gForecastDOs.get(i).setForecastMw(declares.get(i)));
                gForecastDOs.forEach(generatorDaForecastBidMapper::updateById);
            } else {
                LambdaQueryWrapper<IntraOffer> eq = new LambdaQueryWrapper<IntraOffer>()
                        .eq(IntraOffer::getRoundId, StageId.parse(stageId).getRoundId() + 1)
                        .eq(IntraOffer::getUnitId, unit.getMetaUnit().getSourceId());
                IntraOffer intraOffer = intraOfferMapper.selectOne(eq);

                LambdaQueryWrapper<IntraCost> eq1 = new LambdaQueryWrapper<IntraCost>().eq(IntraCost::getUnitId, unit.getMetaUnit().getSourceId());
                IntraCost intraCost = intraCostMapper.selectOne(eq1);
                BizEx.trueThrow(intraDaBidPO.getColdStartupOffer() > intraCost.getColdStartupOfferCap(), PARAM_FORMAT_WRONG.message("冷启动费用超过上限"));
                BizEx.trueThrow(intraDaBidPO.getWarmStartupOffer() > intraCost.getWarmStartupOfferCap(), PARAM_FORMAT_WRONG.message("温启动费用超过上限"));
                BizEx.trueThrow(intraDaBidPO.getHotStartupOffer() > intraCost.getHotStartupOfferCap(), PARAM_FORMAT_WRONG.message("热启动费用超过上限"));
                BizEx.trueThrow(intraDaBidPO.getNoLoadOffer() > intraCost.getNoLoadOfferCap(), PARAM_FORMAT_WRONG.message("空载费用超过上限"));

                intraOffer.setColdStartupOffer(intraDaBidPO.getColdStartupOffer());
                intraOffer.setWarmStartupOffer(intraDaBidPO.getWarmStartupOffer());
                intraOffer.setHotStartupOffer(intraDaBidPO.getHotStartupOffer());
                intraOffer.setNoLoadOffer(intraDaBidPO.getNoLoadOffer());
                intraOfferMapper.updateById(intraOffer);
            }
        } else if (unitType == UnitType.LOAD) {
            LambdaQueryWrapper<LoadDaForecastBidDO> eq1 = new LambdaQueryWrapper<LoadDaForecastBidDO>()
                    .eq(LoadDaForecastBidDO::getRoundId, parsed.getRoundId() + 1)
                    .eq(LoadDaForecastBidDO::getLoadId, unit.getMetaUnit().getSourceId());
            List<LoadDaForecastBidDO> lForecastBidDOs = loadDaForecastBidMapper.selectList(eq1).stream()
                    .sorted(Comparator.comparing(LoadDaForecastBidDO::getPrd)).collect(Collectors.toList());
            Double maxCapacity = unit.getMetaUnit().getMaxCapacity();
            List<Double> declares = intraDaBidPO.getDeclares().stream().map(dec -> Math.min(dec, maxCapacity)).collect(Collectors.toList());
            IntStream.range(0, lForecastBidDOs.size()).forEach(i -> lForecastBidDOs.get(i).setBidMw(declares.get(i)));
            lForecastBidDOs.forEach(loadDaForecastBidMapper::updateById);
        } else {
            throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
        }

        return Result.success();
    }


    /**
     * 现货阶段成本计算接口
     * @param unitId 待计算报价单元id
     * @param start 待计算报价段起点
     * @param end 待计算报价段终点
     */
    @GetMapping("calculateDaCost")
    public Result<Double> calculateDaCost(@NotNull @Positive Long unitId,
                                  @NotNull @PositiveOrZero Double start,
                                  @NotNull @Positive Double end) {
        BizEx.trueThrow(end <= start, PARAM_FORMAT_WRONG.message("报价段右端点应该小于左端点"));
        double v = tunnel.cost(unitId, start, end);
        v = BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
        return Result.success(v);
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


    /**
     * 现货中标量价曲线，机组列表，负荷列表
     * @param stageId 阶段id
     * @param unitType 机组列表，或者负荷列表
     */
    @GetMapping("listClearedUnitVOs")
    public Result<List<UnitVO>> listClearedUnitVOs(@NotBlank String stageId, @NotBlank String unitType, @RequestHeader String token) {
        UnitType uType = Kit.enumOf(UnitType::name, unitType).orElse(null);
        boolean equals = tunnel.review();
        StageId parsed = StageId.parse(stageId);
        Long compId = parsed.getCompId();
        Integer roundId = parsed.getRoundId();
        LambdaQueryWrapper<UnitDO> queryWrapper = new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getCompId, compId)
                .eq(UnitDO::getRoundId, roundId)
                .eq(!equals, UnitDO::getUserId, TokenUtils.getUserId(token));
        List<UnitDO> unitDOs = unitDOMapper.selectList(queryWrapper).stream()
                .filter(u -> u.getMetaUnit().getUnitType().equals(uType)).collect(Collectors.toList());
        List<UnitVO> unitVOs = Collect.transfer(unitDOs,
                unitDO -> new UnitVO(unitDO.getUnitId(), unitDO.getMetaUnit().getName(), unitDO.getMetaUnit()));
        return Result.success(unitVOs);
    }

    final NodalPriceVoltageMapper nodalPriceVoltageMapper;

    /**
     * 现货中标量量价曲线-分机组
     * @param stageId 阶段id
     * @param unitId 待查看的机组unitId
     */
    @GetMapping("listGeneratorClearances")
    public Result<GeneratorClearVO> listGeneratorClearances(@NotBlank String stageId, @NotNull @Positive Long unitId) {
        Integer roundId = StageId.parse(stageId).getRoundId();
        Unit unit = domainTunnel.getByAggregateId(Unit.class, unitId);
        Integer sourceId = unit.getMetaUnit().getSourceId();
        LambdaQueryWrapper<GeneratorBasic> eq = new LambdaQueryWrapper<GeneratorBasic>().eq(GeneratorBasic::getUnitId, sourceId);
        Integer nodeId = unitBasicMapper.selectOne(eq).getNodeId();
        LambdaQueryWrapper<NodalPriceVoltage> eq1 = new LambdaQueryWrapper<NodalPriceVoltage>().eq(NodalPriceVoltage::getRoundId, roundId + 1)
                .eq(NodalPriceVoltage::getNodeId, nodeId);
        List<NodalPriceVoltage> nodalPriceVoltages = nodalPriceVoltageMapper.selectList(eq1).stream()
                .sorted(Comparator.comparing(NodalPriceVoltage::getPrd)).collect(Collectors.toList());
        List<Double> daPrices = Collect.transfer(nodalPriceVoltages, NodalPriceVoltage::getDaLmp);
        List<Double> rtPrices = Collect.transfer(nodalPriceVoltages, NodalPriceVoltage::getRtLmp);

        GeneratorClearVO.GeneratorClearVOBuilder builder = GeneratorClearVO.builder().daPrice(daPrices).rtPrice(rtPrices);

        LambdaQueryWrapper<GeneratorDaSegmentBidDO> eq2 = new LambdaQueryWrapper<GeneratorDaSegmentBidDO>()
                .eq(GeneratorDaSegmentBidDO::getRoundId, roundId + 1)
                .eq(GeneratorDaSegmentBidDO::getUnitId, sourceId);

        List<GeneratorDaSegmentBidDO> generatorDaSegmentBidDOs = generatorDaSegmentMapper.selectList(eq2).stream()
                .sorted(Comparator.comparing(GeneratorDaSegmentBidDO::getOfferId))
                .skip(GeneratorType.RENEWABLE == unit.getMetaUnit().getGeneratorType() ? 1 : 0)
                .collect(Collectors.toList());

        LambdaQueryWrapper<SpotUnitCleared> eq3 = new LambdaQueryWrapper<SpotUnitCleared>()
                .eq(SpotUnitCleared::getRoundId, roundId + 1)
                .eq(SpotUnitCleared::getUnitId, sourceId);
        List<SpotUnitCleared> spotUnitCleareds = spotUnitClearedMapper.selectList(eq3)
                .stream().sorted(Comparator.comparing(SpotUnitCleared::getPrd)).collect(Collectors.toList());

        List<Double> daCleared = Collect.transfer(spotUnitCleareds, SpotUnitCleared::getDaClearedMw);
        List<Double> rtCleared = Collect.transfer(spotUnitCleareds, SpotUnitCleared::getRtClearedMw);

        List<Double> daDeclares;
        List<Double> rtDeclares;
        if (GeneratorType.RENEWABLE.equals(unit.getMetaUnit().getGeneratorType())) {
            LambdaQueryWrapper<GeneratorDaForecastBidDO> eq4 = new LambdaQueryWrapper<GeneratorDaForecastBidDO>()
                    .eq(GeneratorDaForecastBidDO::getRoundId, roundId + 1)
                    .eq(GeneratorDaForecastBidDO::getUnitId, unit.getMetaUnit().getSourceId());
            daDeclares = generatorDaForecastBidMapper.selectList(eq4)
                    .stream().sorted(Comparator.comparing(GeneratorDaForecastBidDO::getPrd))
                    .map(GeneratorDaForecastBidDO::getForecastMw)
                    .collect(Collectors.toList());
            LambdaQueryWrapper<GeneratorForecastValueDO> eq5 = new LambdaQueryWrapper<GeneratorForecastValueDO>()
                    .eq(GeneratorForecastValueDO::getUnitId, unit.getMetaUnit().getSourceId());
            rtDeclares = generatorForecastValueMapper.selectList(eq5).stream()
                    .sorted(Comparator.comparing(GeneratorForecastValueDO::getPrd))
                    .map(GeneratorForecastValueDO::getRtP)
                    .collect(Collectors.toList());
        } else {
            daDeclares = new ArrayList<>();
            rtDeclares = new ArrayList<>();
        }


        List<Pair<List<ClearedVO>, List<ClearedVO>>> clearedSections = IntStream.range(0, 24).mapToObj(i -> {

            List<ClearedVO> daBids = new ArrayList<>();
            List<ClearedVO> das = new ArrayList<>();
            Double daTotal = daCleared.get(i);

            if (GeneratorType.CLASSIC.equals(unit.getMetaUnit().getGeneratorType())) {
                generatorDaSegmentBidDOs.forEach(gDO -> {
                    daBids.add(new ClearedVO(gDO.getOfferCost(), gDO.getOfferMw(), gDO.getOfferPrice(), gDO.getOfferMw()));
                });
            } else {
                double renewableAccumulate = 0D;
                Double declareTotal = daDeclares.get(i);
                for (GeneratorDaSegmentBidDO gDO : generatorDaSegmentBidDOs) {
                    renewableAccumulate += gDO.getOfferMw();
                    if (renewableAccumulate < declareTotal) {
                        daBids.add(new ClearedVO(gDO.getOfferCost(), gDO.getOfferMw(), gDO.getOfferPrice(), gDO.getOfferMw()));
                    } else {
                        double v = declareTotal - (renewableAccumulate - gDO.getOfferMw());
                        daBids.add(new ClearedVO(gDO.getOfferCost(), v, gDO.getOfferPrice(), v));
                        break;
                    }
                }
            }

            Double daAccumulate = 0D;
            if (!daTotal.equals(0D)) {
                for (ClearedVO bid : daBids) {
                    daAccumulate += bid.getQuantity();
                    if (daAccumulate >= daTotal) {
                        double v = daTotal - (daAccumulate - bid.getQuantity());
                        das.add(new ClearedVO(bid.getCost(), v, bid.getPrice(), bid.getDeclared()));
                        break;
                    }
                    das.add(bid);
                }
            }

            // 实时
            List<ClearedVO> rtBids = new ArrayList<>();
            List<ClearedVO> rts = new ArrayList<>();
            Double rtTotal = rtCleared.get(i);

            if (GeneratorType.CLASSIC.equals(unit.getMetaUnit().getGeneratorType())) {
                generatorDaSegmentBidDOs.forEach(gDO -> {
                    rtBids.add(new ClearedVO(gDO.getOfferCost(), gDO.getOfferMw(), gDO.getOfferPrice(), gDO.getOfferMw()));
                });
            } else {
                double renewableAccumulate = 0D;
                Double declareTotal = rtDeclares.get(i);
                for (GeneratorDaSegmentBidDO gDO : generatorDaSegmentBidDOs) {
                    renewableAccumulate += gDO.getOfferMw();
                    if (renewableAccumulate < declareTotal) {
                        rtBids.add(new ClearedVO(gDO.getOfferCost(), gDO.getOfferMw(), gDO.getOfferPrice(), gDO.getOfferMw()));
                    } else {
                        double v = declareTotal - (renewableAccumulate - gDO.getOfferMw());
                        rtBids.add(new ClearedVO(gDO.getOfferCost(), v, gDO.getOfferPrice(), v));
                        break;
                    }
                }
            }

            Double rtAccumulate = 0D;
            if (!rtTotal.equals(0D)) {
                for (ClearedVO bid : rtBids) {
                    rtAccumulate += bid.getQuantity();
                    if (rtAccumulate >= rtTotal) {
                        double v = rtTotal - (rtAccumulate - bid.getQuantity());
                        rts.add(new ClearedVO(bid.getCost(), v, bid.getPrice(), bid.getDeclared()));
                        break;
                    }
                    rts.add(bid);
                }
            }

            return Pair.of(das, rts);
        }).collect(Collectors.toList());

        List<List<ClearedVO>> daSections = clearedSections.stream().map(Pair::getLeft).collect(Collectors.toList());
        List<List<ClearedVO>> rtSections = clearedSections.stream().map(Pair::getRight).collect(Collectors.toList());
        if (GeneratorType.CLASSIC.equals(unit.getMetaUnit().getGeneratorType())) {
            List<Pair<ClearedVO, List<ClearedVO>>> daPs = daSections.stream().map(ds -> Pair.of(ds.get(0), ds.subList(1, ds.size()))).collect(Collectors.toList());
            List<Pair<ClearedVO, List<ClearedVO>>> rtPs = rtSections.stream().map(ds -> Pair.of(ds.get(0), ds.subList(1, ds.size()))).collect(Collectors.toList());
            builder.daMinClears(daPs.stream().map(Pair::getLeft).collect(Collectors.toList()));
            builder.daClearedSections(daPs.stream().map(Pair::getRight).collect(Collectors.toList()));
            builder.rtMinClears(rtPs.stream().map(Pair::getLeft).collect(Collectors.toList()));
            builder.rtClearedSections(rtPs.stream().map(Pair::getRight).collect(Collectors.toList()));
        } else {
            builder.daClearedSections(daSections);
            builder.rtClearedSections(rtSections);
        }
        GeneratorClearVO generatorClearVO = builder.build();
        return Result.success(generatorClearVO);
    }

    /**
     * 现货中标量量价曲线-分负荷
     * @param stageId 阶段id
     * @param unitId 待查看的负荷unitId
     */
    @GetMapping("listLoadClearances")
    public Result<LoadClearVO> listLoadClearances(@NotBlank String stageId, @NotNull @Positive Long unitId) {
        Integer roundId = StageId.parse(stageId).getRoundId();
        Unit unit = domainTunnel.getByAggregateId(Unit.class, unitId);
        Integer sourceId = unit.getMetaUnit().getSourceId();
        LambdaQueryWrapper<SpotLoadCleared> eq = new LambdaQueryWrapper<SpotLoadCleared>()
                .eq(SpotLoadCleared::getRoundId, roundId + 1)
                .eq(SpotLoadCleared::getLoadId, sourceId);
        List<Double> daCleared = spotLoadClearedMapper.selectList(eq).stream()
                .sorted(Comparator.comparing(SpotLoadCleared::getPrd)).map(SpotLoadCleared::getDaClearedMw).collect(Collectors.toList());
        LoadClearVO.LoadClearVOBuilder builder = LoadClearVO.builder().daCleared(daCleared);
        LambdaQueryWrapper<LoadForecastValueDO> eq1 = new LambdaQueryWrapper<LoadForecastValueDO>()
                .eq(LoadForecastValueDO::getLoadId, sourceId);
        List<Double> rtCleared = loadForecastValueMapper.selectList(eq1).stream()
                .sorted(Comparator.comparing(LoadForecastValueDO::getPrd)).map(LoadForecastValueDO::getRtP).collect(Collectors.toList());
        builder.rtCleared(rtCleared);


        Integer nodeId = loadBasicMapper.selectById(sourceId).getNodeId();
        LambdaQueryWrapper<NodalPriceVoltage> eqx = new LambdaQueryWrapper<NodalPriceVoltage>()
                .eq(NodalPriceVoltage::getRoundId, roundId + 1)
                .eq(NodalPriceVoltage::getNodeId, nodeId);
        List<NodalPriceVoltage> nodalPriceVoltages = nodalPriceVoltageMapper.selectList(eqx)
                .stream().sorted(Comparator.comparing(NodalPriceVoltage::getPrd)).collect(Collectors.toList());
        builder.daPrice(Collect.transfer(nodalPriceVoltages, NodalPriceVoltage::getDaLmp));
        builder.rtPrice(Collect.transfer(nodalPriceVoltages, NodalPriceVoltage::getRtLmp));

        LoadClearVO loadClearVO = builder.build();

        return Result.success(loadClearVO);
    }

    final LoadBasicMapper loadBasicMapper;
    final UnmetDemandMapper unmetDemandMapper;
    final InterSpotUnitOfferDOMapper interSpotUnitOfferDOMapper;
    final StackDiagramDOMapper stackDiagramDOMapper;

    /**
     *  省间现货回填接口
     * @param stageId 阶段id
     */
    @GetMapping("listSpotInterBidVO")
    public Result<List<SpotInterBidVO>> listSpotInterBidVO(@NotBlank String stageId, @RequestHeader String token) {
        StageId parsedStageId = StageId.parse(stageId);
        Long compId = parsedStageId.getCompId();
        Integer roundId = parsedStageId.getRoundId();
        LambdaQueryWrapper<TieLinePowerDO> eq = new LambdaQueryWrapper<TieLinePowerDO>().eq(TieLinePowerDO::getRoundId, roundId + 1);
        List<TieLinePowerDO> tieLinePowerDOS = tieLinePowerDOMapper.selectList(eq).stream().sorted(Comparator.comparing(TieLinePowerDO::getPrd)).collect(Collectors.toList());
        List<UnmetDemand> unmetDemands = unmetDemandMapper.selectList(null).stream().sorted(Comparator.comparing(UnmetDemand::getPrd)).collect(Collectors.toList());
        List<Double> availablePrds = IntStream.range(0, 24).mapToObj(i -> {
            TieLinePowerDO tieLinePowerDO = tieLinePowerDOS.get(i);
            double v = tieLinePowerDO.getAnnualTielinePower() + tieLinePowerDO.getMonthlyTielinePower();
            return unmetDemands.get(i).getDaReceivingMw() - v;
        }).collect(Collectors.toList());

        LambdaQueryWrapper<UnitDO> queryWrapper = new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getCompId, compId).eq(UnitDO::getRoundId, roundId).eq(UnitDO::getUserId, TokenUtils.getUserId(token));
        List<UnitDO> generatorUnitDOs = unitDOMapper.selectList(queryWrapper).stream()
                .filter(u -> u.getMetaUnit().getUnitType().equals(UnitType.GENERATOR))
                .filter(u -> u.getMetaUnit().getProvince().equals(Province.TRANSFER))
                .collect(Collectors.toList());
        Map<Integer, List<Double>> maxCapacities = generatorUnitDOs.stream().collect(Collectors.toMap(u -> u.getMetaUnit().getSourceId(), this::highLimit));
        List<Integer> sourceIds = Collect.transfer(generatorUnitDOs, u -> u.getMetaUnit().getSourceId());
        LambdaQueryWrapper<SpotUnitCleared> in = new LambdaQueryWrapper<SpotUnitCleared>()
                .eq(SpotUnitCleared::getRoundId, roundId + 1).in(Collect.isNotEmpty(sourceIds), SpotUnitCleared::getUnitId, sourceIds);
        Map<Integer, List<SpotUnitCleared>> clearResult = spotUnitClearedMapper.selectList(in)
                .stream().collect(Collectors.groupingBy(SpotUnitCleared::getUnitId));

        LambdaQueryWrapper<InterSpotUnitOfferDO> in1 = new LambdaQueryWrapper<InterSpotUnitOfferDO>()
                .eq(InterSpotUnitOfferDO::getRoundId, roundId + 1)
                .in(Collect.isNotEmpty(sourceIds), InterSpotUnitOfferDO::getUnitId, sourceIds);
        ListMultimap<Integer, InterSpotUnitOfferDO> spotOfferMap = interSpotUnitOfferDOMapper.selectList(in1).stream().collect(Collect.listMultiMap(InterSpotUnitOfferDO::getUnitId));

        GridLimit priceLimit = tunnel.priceLimit(UnitType.GENERATOR);

        LambdaQueryWrapper<StackDiagramDO> eq1 = new LambdaQueryWrapper<StackDiagramDO>()
                .eq(StackDiagramDO::getRoundId, parsedStageId.getRoundId() + 1);
        List<StackDiagramDO> stackDiagramDOS = stackDiagramDOMapper.selectList(eq1)
                .stream().sorted(Comparator.comparing(StackDiagramDO::getPrd)).collect(Collectors.toList());


        List<SpotInterBidVO> spotInterBidVOs = generatorUnitDOs.stream().map(unitDO -> {
            Integer sourceId = unitDO.getMetaUnit().getSourceId();
            SpotInterBidVO.SpotInterBidVOBuilder builder = SpotInterBidVO.builder().sourceId(sourceId)
                    .unitId(unitDO.getUnitId()).unitName(unitDO.getMetaUnit().getName()).priceLimit(priceLimit);
            Map<Integer, SpotUnitCleared> unitClearedMap = Collect.toMap(clearResult.get(sourceId), SpotUnitCleared::getPrd);
            List<Double> capacities = maxCapacities.get(sourceId);
            Map<Integer, InterSpotUnitOfferDO> collect = spotOfferMap.get(sourceId).stream().collect(Collectors.toMap(InterSpotUnitOfferDO::getPrd, i -> i));
            List<InstantSpotBidVO> instantSpotBidVOs = IntStream.range(0, 24).mapToObj(i -> {
                Double available = availablePrds.get(i);
                SpotUnitCleared spotUnitCleared = unitClearedMap.get(i);

                if (available > 0) {
                    InterSpotUnitOfferDO interSpotUnitOfferDO = collect.get(i);
                    if (interSpotUnitOfferDO != null) {
                        InterSpotBid interSpotBid1 = InterSpotBid.builder()
                                .quantity(interSpotUnitOfferDO.getSpotOfferMw1()).price(interSpotUnitOfferDO.getSpotOfferPrice1()).build();
                        InterSpotBid interSpotBid2 = InterSpotBid.builder()
                                .quantity(interSpotUnitOfferDO.getSpotOfferMw2()).price(interSpotUnitOfferDO.getSpotOfferPrice2()).build();
                        InterSpotBid interSpotBid3 = InterSpotBid.builder()
                                .quantity(interSpotUnitOfferDO.getSpotOfferMw3()).price(interSpotUnitOfferDO.getSpotOfferPrice3()).build();
                        return InstantSpotBidVO.builder().instant(i)
                                .maxCapacity(capacities.get(i)).preCleared(spotUnitCleared.getPreclearClearedMw())
                                .interSpotBids(Collect.asList(interSpotBid1, interSpotBid2, interSpotBid3))
                                .build();
                    } else {
                        return InstantSpotBidVO.builder().instant(i)
                                .maxCapacity(capacities.get(i)).preCleared(spotUnitCleared.getPreclearClearedMw())
                                .interSpotBids(Collect.asList(null, null, null))
                                .build();
                    }

                } else {
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
            return builder.instantSpotBidVOs(instantSpotBidVOs).build();
        }).collect(Collectors.toList());
        return Result.success(spotInterBidVOs);

    }

    private List<Double> highLimit(UnitDO unitDO) {
        if (GeneratorType.CLASSIC.equals(unitDO.getMetaUnit().getGeneratorType())) {
            return IntStream.range(0, 24).mapToObj(i -> unitDO.getMetaUnit().getMaxCapacity()).collect(Collectors.toList());
        } else {
            LambdaQueryWrapper<GeneratorForecastValueDO> eq = new LambdaQueryWrapper<GeneratorForecastValueDO>()
                    .eq(GeneratorForecastValueDO::getUnitId, unitDO.getMetaUnit().getSourceId());
            return generatorForecastValueMapper.selectList(eq).stream().sorted(Comparator.comparing(GeneratorForecastValueDO::getPrd))
                    .map(GeneratorForecastValueDO::getDaPForecast).collect(Collectors.toList());
        }
    }


    /**
     * 省间现货报价
     * @param spotBidPO 省间现货报价结构体
     */
    @PostMapping("submitInterSpotBid")
    public Result<Void> submitInterSpotBid(@RequestBody SpotBidPO spotBidPO, @RequestHeader String token) {

        String cStageId = tunnel.runningComp().getStageId().toString();
        StageId stageId = StageId.parse(spotBidPO.getStageId());
        BizEx.trueThrow(Kit.notEq(cStageId, stageId.toString()), PARAM_FORMAT_WRONG.message("已经进入下一个阶段"));
        Long unitId = spotBidPO.getUnitId();
        Integer sourceId = domainTunnel.getByAggregateId(Unit.class, unitId).getMetaUnit().getSourceId();
        Long compId = stageId.getCompId();
        Integer roundId = stageId.getRoundId();

        List<SpotInterBidVO> spotInterBidVOs = listSpotInterBidVO(cStageId, token).getData();
        SpotInterBidVO spotInterBidVO = spotInterBidVOs.stream().filter(s -> s.getUnitId().equals(unitId)).findFirst().orElseThrow(SysEx::unreachable);
        Map<Integer, InstantSpotBidVO> instantSpotBidVOs = spotInterBidVO.getInstantSpotBidVOs().stream().collect(Collectors.toMap(InstantSpotBidVO::getInstant, i -> i));


        for (InstantSpotBidPO instantSpotBidPO : spotBidPO.getInstantSpotBidPOs()) {
            Integer instant = instantSpotBidPO.getInstant();
            InstantSpotBidVO instantSpotBidVO = instantSpotBidVOs.get(instant);
            double balance = instantSpotBidVO.getMaxCapacity() - instantSpotBidVO.getPreCleared();
            double sum = instantSpotBidPO.getInterSpotBids().stream().collect(Collectors.summarizingDouble(InterSpotBid::getQuantity)).getSum();
            BizEx.trueThrow(sum > balance, PARAM_FORMAT_WRONG.message("三段报价超过剩留量！"));
        }

        for (InstantSpotBidPO instantSpotBidPO : spotBidPO.getInstantSpotBidPOs()) {
            Integer instant = instantSpotBidPO.getInstant();
            LambdaQueryWrapper<InterSpotUnitOfferDO> eq = new LambdaQueryWrapper<InterSpotUnitOfferDO>()
                    .eq(InterSpotUnitOfferDO::getRoundId, roundId + 1)
                    .eq(InterSpotUnitOfferDO::getUnitId, sourceId)
                    .eq(InterSpotUnitOfferDO::getPrd, instant);
            InterSpotUnitOfferDO interSpotUnitOfferDO = interSpotUnitOfferDOMapper.selectOne(eq);
            List<InterSpotBid> bids = instantSpotBidPO.getInterSpotBids();
            interSpotUnitOfferDO.setSpotOfferMw1(bids.get(0).getQuantity());
            interSpotUnitOfferDO.setSpotOfferPrice1(bids.get(0).getPrice());
            interSpotUnitOfferDO.setSpotOfferMw2(bids.get(1).getQuantity());
            interSpotUnitOfferDO.setSpotOfferPrice2(bids.get(1).getPrice());
            interSpotUnitOfferDO.setSpotOfferMw3(bids.get(2).getQuantity());
            interSpotUnitOfferDO.setSpotOfferPrice3(bids.get(2).getPrice());
            interSpotUnitOfferDOMapper.updateById(interSpotUnitOfferDO);
        }
        return Result.success();
    }









}
