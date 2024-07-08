package com.bilanee.octopus.adapter.facade;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilanee.octopus.adapter.facade.po.*;
import com.bilanee.octopus.adapter.facade.vo.*;
import com.bilanee.octopus.adapter.repository.UnitAdapter;
import com.bilanee.octopus.adapter.tunnel.BidQuery;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.*;
import com.bilanee.octopus.basic.enums.*;
import com.bilanee.octopus.domain.*;
import com.bilanee.octopus.infrastructure.entity.*;
import com.bilanee.octopus.infrastructure.mapper.*;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.base.SysEx;
import com.stellariver.milky.common.tool.common.ConcurrentTool;
import com.stellariver.milky.common.tool.common.Kit;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.domain.support.base.DomainTunnel;
import com.stellariver.milky.domain.support.command.CommandBus;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.stellariver.milky.common.base.ErrorEnumsBase.PARAM_FORMAT_WRONG;

/**
 * 单元信息
 */

@CustomLog
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
    final NodeBasicDOMapper nodeBasicDOMapper;
    final UnitBasicMapper unitBasicMapper;
    final SpotUnitClearedMapper spotUnitClearedMapper;
    final SpotLoadClearedMapper spotLoadClearedMapper;
    final IntraOfferMapper intraOfferMapper;

    final Executor executor = Executors.newFixedThreadPool(100);

    static private Cache<String, Object> shortCache = CacheBuilder.newBuilder().expireAfterWrite(5L, TimeUnit.SECONDS).maximumSize(1000L).build();
    static private Cache<String, Object> longCache = CacheBuilder.newBuilder().expireAfterWrite(5L, TimeUnit.MINUTES).maximumSize(1000L).build();

    /**
     * 本轮被分配的机组信息
     * @param stageId 阶段id
     * @param token 前端携带的token
     * @return 本轮被分配的机组信息
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    @GetMapping("listAssignUnitVOs")
    public Result<List<UnitVO>> listAssignUnitVOs(@NotBlank String stageId, @RequestHeader String token) {
        List<UnitVO> unitVOs = (List<UnitVO>) shortCache.get("listAssignUnitVOs" + stageId + TokenUtils.getUserId(token), () -> {
            StageId parsedStageId = StageId.parse(stageId);
            List<Unit> units = tunnel.listUnits(parsedStageId.getCompId(), parsedStageId.getRoundId(), TokenUtils.getUserId(token));
            return Collect.transfer(units, unit -> UnitVO.builder()
                    .unitId(unit.getUnitId())
                    .unitName(unit.getMetaUnit().getName())
                    .metaUnit(unit.getMetaUnit()).build());
        });
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
        Comp comp = tunnel.runningComp();
        boolean realTime = comp.getStageId().equals(parsedStageId);

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


        boolean b = comp.getStageId().getTradeStage() == TradeStage.AN_INTER;

        ListMultimap<Long, Bid> groupedByUnitId = Collect.isEmpty(unitMap) ? ArrayListMultimap.create() :
                tunnel.listBids(bidQuery).stream().collect(Collect.listMultiMap(Bid::getUnitId));

        List<UnitInterBidVO> interBidsVOs = unitMap.entrySet().stream().map(e -> {
            Long uId = e.getKey();
            Unit unit = e.getValue();
            Collection<Bid> bs = groupedByUnitId.get(uId);
            Map<TimeFrame, List<BalanceVO>> sumCap = new HashMap<>();
            Map<TimeFrame, Double> capacityMap = new HashMap<>();
            GridLimit priceLimit = unit.getMetaUnit().getPriceLimit();
            if (unit.getMetaUnit().getUnitType() == UnitType.GENERATOR) {
                LambdaQueryWrapper<ForwardUnitOffer> eq = new LambdaQueryWrapper<ForwardUnitOffer>().eq(ForwardUnitOffer::getRoundId, parsedStageId.getRoundId() + 1)
                        .eq(ForwardUnitOffer::getUnitId, unit.getMetaUnit().getSourceId());
                forwardUnitOfferMapper.selectList(eq).stream().collect(Collectors.groupingBy(
                        k -> Kit.enumOfMightEx(TimeFrame::getDbCode, k.getPfvPrd())
                )).forEach((t, fs) -> {
                    Double maxCleared = b ? fs.get(0).getMaxAnnualClearedMw() : fs.get(0).getMaxMonthlyClearedMw();
                    List<BalanceVO> balanceVOs = Collections.singletonList(new BalanceVO(Direction.SELL, maxCleared));
                    sumCap.put(t, balanceVOs);
                    capacityMap.put(t, maxCleared);
                });

            } else {
                LambdaQueryWrapper<ForwardLoadBid> eq = new LambdaQueryWrapper<ForwardLoadBid>().eq(ForwardLoadBid::getRoundId, parsedStageId.getRoundId() + 1)
                        .eq(ForwardLoadBid::getLoadId, unit.getMetaUnit().getSourceId());
                forwardLoadBidMapper.selectList(eq).stream().collect(Collectors.groupingBy(
                        k -> Kit.enumOfMightEx(TimeFrame::getDbCode, k.getPfvPrd())
                )).forEach((t, fs) -> {
                    Double maxCleared = b ? fs.get(0).getMaxAnnualClearedMw() : fs.get(0).getMaxMonthlyClearedMw();
                    List<BalanceVO> balanceVOs = Collections.singletonList(new BalanceVO(Direction.BUY, maxCleared));
                    sumCap.put(t, balanceVOs);
                    capacityMap.put(t, maxCleared);
                });
            }

            Map<TimeFrame, Collection<Bid>> map = bs.stream().collect(Collect.listMultiMap(Bid::getTimeFrame)).asMap();
            Map<TimeFrame, Collection<Bid>> newMap = new HashMap<>(map);
            Collect.subtract(new HashSet<>(TimeFrame.list()), map.keySet()).forEach(t -> newMap.put(t, Collections.EMPTY_LIST));
            List<InterBidVO> interBidVOS = newMap.entrySet().stream().map(ee -> {
                TimeFrame timeFrame = ee.getKey();
                Double capacity = unit.getMetaUnit().getCapacity().get(timeFrame).get(unit.getMetaUnit().getUnitType().generalDirection());
                return InterBidVO.builder().timeFrame(timeFrame)
                        .capacity(capacityMap.get(timeFrame))
                        .bidVOs(Collect.transfer(ee.getValue(), Convertor.INST::to))
                        .balanceVOs(realTime ? sumCap.get(timeFrame) : Collections.EMPTY_LIST)
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

    final ForwardUnitOfferMapper forwardUnitOfferMapper;
    final ForwardLoadBidMapper forwardLoadBidMapper;

    /**
     * 省间报价接口
     * @param interBidsPO 省间报价请求结构体
     * @return 报单结果
     */
    @ToBid
    @PostMapping("submitInterBidsPO")
    public Result<Void> submitInterBidsPO(@RequestBody InterBidsPO interBidsPO, @RequestHeader String token) {

        List<BidPO> bidPOs = interBidsPO.getBidPOs().stream()
                .filter(bidPO -> bidPO.getDirection() != null)
                .filter(bidPO -> bidPO.getQuantity() != null && bidPO.getQuantity() >= 0)
                .filter(bidPO -> bidPO.getPrice() != null).collect(Collectors.toList());
        BizEx.trueThrow(Collect.isEmpty(bidPOs), PARAM_FORMAT_WRONG.message("无有效报单"));
        Long unitId = interBidsPO.getBidPOs().get(0).getUnitId();
        MetaUnit metaUnit = domainTunnel.getByAggregateId(Unit.class, unitId).getMetaUnit();
        Integer sourceId = metaUnit.getSourceId();
        UnitType unitType = metaUnit.getUnitType();
        GridLimit gridLimit = tunnel.priceLimit(unitType);
        bidPOs.forEach(bidPO -> gridLimit.check(bidPO.getPrice()));
        StageId pStageId = StageId.parse(interBidsPO.getStageId());
        StageId cStageId = tunnel.runningComp().getStageId();
        BizEx.falseThrow(pStageId.equals(cStageId), PARAM_FORMAT_WRONG.message("已经进入下一阶段不可报单"));
        BizEx.trueThrow(cStageId.getTradeStage().getTradeType() != TradeType.INTER,
                PARAM_FORMAT_WRONG.message("当前为中长期省省间报价阶段"));
        BizEx.trueThrow(cStageId.getMarketStatus() != MarketStatus.BID,
                PARAM_FORMAT_WRONG.message("当前竞价阶段已经关闭"));


        if (unitType == UnitType.GENERATOR) {
            LambdaQueryWrapper<ForwardUnitOffer> eq = new LambdaQueryWrapper<ForwardUnitOffer>().eq(ForwardUnitOffer::getRoundId, cStageId.getRoundId() + 1)
                    .eq(ForwardUnitOffer::getUnitId, sourceId);
            forwardUnitOfferMapper.selectList(eq).stream().collect(Collectors.groupingBy(
                    k -> Kit.enumOfMightEx(TimeFrame::getDbCode, k.getPfvPrd())
            )).forEach((t, fs) -> {
                Double maxCleared = cStageId.getTradeStage() == TradeStage.AN_INTER ? fs.get(0).getMaxAnnualClearedMw() : fs.get(0).getMaxMonthlyClearedMw();
                double sum = bidPOs.stream().filter(bidPO -> bidPO.getTimeFrame().equals(t)).collect(Collectors.summarizingDouble(BidPO::getQuantity)).getSum();
                BizEx.trueThrow(sum > maxCleared, PARAM_FORMAT_WRONG.message(t.getDesc() + "报价总量超过限制" + maxCleared));
            });

        } else {
            LambdaQueryWrapper<ForwardLoadBid> eq = new LambdaQueryWrapper<ForwardLoadBid>().eq(ForwardLoadBid::getRoundId, cStageId.getRoundId() + 1)
                    .eq(ForwardLoadBid::getLoadId, sourceId);
            forwardLoadBidMapper.selectList(eq).stream().collect(Collectors.groupingBy(
                    k -> Kit.enumOfMightEx(TimeFrame::getDbCode, k.getPfvPrd())
            )).forEach((t, fs) -> {
                Double maxCleared =  cStageId.getTradeStage() == TradeStage.AN_INTER ? fs.get(0).getMaxAnnualClearedMw() : fs.get(0).getMaxMonthlyClearedMw();
                double sum = bidPOs.stream().filter(bidPO -> bidPO.getTimeFrame().equals(t)).collect(Collectors.summarizingDouble(BidPO::getQuantity)).getSum();
                BizEx.trueThrow(sum > maxCleared, PARAM_FORMAT_WRONG.message(t.getDesc() + "报价总量超过限制" + maxCleared));
            });
        }

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
        StepRecord stepRecord = comp.getStepRecords().stream().filter(s -> s.getStageId().equals(stageId)).findFirst().orElseThrow(SysEx::unreachable);


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

            boolean b1 =  System.currentTimeMillis() > stepRecord.getStartTimeStamp() + 180_000L;
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
            builder.unitIntraBidVOs(toUnitIntraBidVOs(us, StageId.parse(stageId), intraSymbol, comp));

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

    /**
     * 滚动报价交易员报价页面
     * @param stageId 阶段id
     * @param token 前端携带的token
     * @return 滚动报价交易员报价页面
     */
    @SneakyThrows
    @GetMapping("listRollSymbolBidVOs")
    public Result<List<RollSymbolBidVO>> listRollSymbolBidVOs(@NotBlank String stageId, @RequestHeader String token) {

        Comp comp = tunnel.runningComp();
        StepRecord stepRecord = comp.getStepRecords().stream().filter(s -> s.getStageId().equals(stageId)).findFirst().orElseThrow(SysEx::unreachable);

        CompletableFuture<Map<RollSymbol, IntraInstantDO>> future0 = CompletableFuture.supplyAsync(() -> {
            // prepare instant
            LambdaQueryWrapper<IntraInstantDO> eq0 = new LambdaQueryWrapper<IntraInstantDO>().eq(IntraInstantDO::getStageId, stageId);
            List<IntraInstantDO> intraInstantDOs = intraInstantDOMapper.selectList(eq0);
            return Collect.toMap(intraInstantDOs, i -> new RollSymbol(i.getProvince(), i.getInstant()));
        }, executor);

        CompletableFuture<ListMultimap<RollSymbol, IntraQuotationDO>> future1 = CompletableFuture.supplyAsync(() -> {
            // prepare quotation
            LambdaQueryWrapper<IntraQuotationDO> eq1 = new LambdaQueryWrapper<IntraQuotationDO>().eq(IntraQuotationDO::getStageId, stageId);
            List<IntraQuotationDO> intraQuotationDOs = intraQuotationDOMapper.selectList(eq1);
            return intraQuotationDOs.stream().collect(Collect.listMultiMap(i -> new RollSymbol(i.getProvince(), i.getInstant())));
        }, executor);

        CompletableFuture<List<Unit>> future2 = CompletableFuture.supplyAsync(() -> {
            StageId parsedStageId = StageId.parse(stageId);
            return tunnel.listUnits(parsedStageId.getCompId(), parsedStageId.getRoundId(), TokenUtils.getUserId(token));
        });

        Map<RollSymbol, IntraInstantDO> instantDOMap = future0.get();
        ListMultimap<RollSymbol, IntraQuotationDO> quotationDOMap = future1.get();
        List<Unit> units = future2.get();

        boolean notCurrentStage = !tunnel.runningComp().getStageId().toString().equals(stageId);

        List<RollSymbolBidVO> rollSymbolBidVOs = new ArrayList<>(ConcurrentTool.batchCall(RollSymbol.rollSymbols(), rollSymbol -> {
            RollSymbolBidVO.RollSymbolBidVOBuilder builder = RollSymbolBidVO.builder()
                    .province(rollSymbol.getProvince()).instant(rollSymbol.getInstant());
            IntraInstantDO intraInstantDO = instantDOMap.get(rollSymbol);

            boolean b1 = System.currentTimeMillis() > stepRecord.getStartTimeStamp() + 180_000L;
            if (intraInstantDO != null) {
                if (notCurrentStage || b1) {
                    builder.latestPrice(intraInstantDO.getPrice());
                    builder.buyAsks(intraInstantDO.getBuyAsks());
                    builder.sellAsks(intraInstantDO.getSellAsks());
                    builder.buyVolumes(intraInstantDO.getBuyVolumes());
                    builder.sellVolumes(intraInstantDO.getSellVolumes());
                    builder.buyHighestPrice(intraInstantDO.getBuyAsks().stream().max(Comparator.comparing(Ask::getPrice)).map(Ask::getPrice).orElse(null));
                    builder.buyLowestPrice(intraInstantDO.getBuyAsks().stream().min(Comparator.comparing(Ask::getPrice)).map(Ask::getPrice).orElse(null));
                    builder.sellHighestPrice(intraInstantDO.getSellAsks().stream().max(Comparator.comparing(Ask::getPrice)).map(Ask::getPrice).orElse(null));
                    builder.sellLowestPrice(intraInstantDO.getSellAsks().stream().min(Comparator.comparing(Ask::getPrice)).map(Ask::getPrice).orElse(null));
                }
            }
            List<Unit> us = units.stream().filter(u -> u.getMetaUnit().getProvince().equals(rollSymbol.getProvince())).collect(Collectors.toList());
            builder.unitRollBidVOs(toUnitRollBidVOs(us, StageId.parse(stageId), rollSymbol, comp));

            List<IntraQuotationDO> intraQuotationDOs = quotationDOMap.get(rollSymbol);
            List<QuotationVO> quotationVOs = intraQuotationDOs.stream()
                    .map(i -> new QuotationVO(i.getTimeStamp(), i.getLatestPrice(), i.getBuyQuantity(), i.getSellQuantity()))
                    .sorted(Comparator.comparing(QuotationVO::getTimeStamp)).collect(Collectors.toList());
            builder.quotationVOs(quotationVOs);
            builder.stepRecord(stepRecord);
            return builder.build();
        }, executor).values());
        return Result.success(rollSymbolBidVOs);
    }



    private List<UnitIntraBidVO> toUnitIntraBidVOs(List<Unit> units, StageId stageId, IntraSymbol intraSymbol, Comp comp) {
        Set<Long> unitIds = units.stream().map(Unit::getUnitId).collect(Collectors.toSet());
        String currentStageId = comp.getStageId().toString();
        StepRecord stepRecord = comp.getStepRecords().stream().filter(s -> s.getStageId().equals(stageId.toString())).findFirst().orElseThrow(SysEx::unreachable);

        BidQuery bidQuery = BidQuery.builder().unitIds(unitIds)
                .province(intraSymbol.getProvince()).timeFrame(intraSymbol.getTimeFrame()).build();
        ListMultimap<Long, Bid> bidMap = Collect.isEmpty(unitIds) ? ArrayListMultimap.create() :
                tunnel.listBids(bidQuery).stream().collect(Collect.listMultiMap(Bid::getUnitId));
        return units.stream().map(unit -> {
            UnitIntraBidVO.UnitIntraBidVOBuilder builder = UnitIntraBidVO.builder().unitId(unit.getUnitId())
                    .capacity(unit.getMetaUnit().getMaxCapacity())
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

    private List<UnitRollBidVO> toUnitRollBidVOs(List<Unit> units, StageId stageId, RollSymbol rollSymbol, Comp comp) {
        Set<Long> unitIds = units.stream().map(Unit::getUnitId).collect(Collectors.toSet());
        String currentStageId = comp.getStageId().toString();
        StepRecord stepRecord = comp.getStepRecords().stream().filter(s -> s.getStageId().equals(stageId.toString())).findFirst().orElseThrow(SysEx::unreachable);

        BidQuery bidQuery = BidQuery.builder().unitIds(unitIds)
                .province(rollSymbol.getProvince()).instant(rollSymbol.getInstant()).build();
        Map<Long, Bid> instantBidMap = Collect.isEmpty(unitIds) ? new HashMap<>() :
                tunnel.listBids(bidQuery).stream().collect(Collectors.groupingBy(Bid::getInstant)).values().stream()
                        .map(instantBids -> instantBids.stream().max(Comparator.comparing(Bid::getDeclareTimeStamp)).orElseThrow(SysEx::unreachable))
                        .collect(Collectors.toMap(Bid::getUnitId, Function.identity()));

        bidQuery = BidQuery.builder().unitIds(unitIds)
                .province(rollSymbol.getProvince()).timeFrame(TimeFrame.getByInstant(rollSymbol.getInstant())).build();
        ListMultimap<Long, Bid> bidMap = Collect.isEmpty(unitIds) ? ArrayListMultimap.create() :
                tunnel.listBids(bidQuery).stream().collect(Collect.listMultiMap(Bid::getUnitId));

        return units.stream().map(unit -> {
            UnitRollBidVO.UnitRollBidVOBuilder builder = UnitRollBidVO.builder().unitId(unit.getUnitId())
                    .capacity(unit.getMetaUnit().getMaxCapacity())
                    .priceLimit(unit.getMetaUnit().getPriceLimit())
                    .unitName(unit.getMetaUnit().getName())
                    .unitType(unit.getMetaUnit().getUnitType())
                    .sourceId(unit.getMetaUnit().getSourceId());

            // 持仓限制
            List<BalanceVO> balanceVOs = new ArrayList<>();
            TimeFrame timeFrame = TimeFrame.getByInstant(rollSymbol.getInstant());
            Map<Direction, Double> balanceMap = unit.getBalance().get(timeFrame);
            Boolean bidden = unit.getRollBidden().get(rollSymbol.getInstant());
            if (!Boolean.TRUE.equals(bidden)) {
                balanceVOs = Arrays.stream(Direction.values()).map(d -> new BalanceVO(d, balanceMap.get(d))).collect(Collectors.toList());
            }

            List<Direction> directions = enableDirectionsOfRollStage(stepRecord);

            if (Objects.equals(stageId.toString(), currentStageId)) {
                balanceVOs = balanceVOs.stream().filter(b -> directions.contains(b.getDirection())).collect(Collectors.toList());
            } else {
                balanceVOs = new ArrayList<>();
            }
            builder.balanceVOs(balanceVOs);

            List<Bid> bids = bidMap.get(unit.getUnitId());
            UnitType unitType = unit.getMetaUnit().getUnitType();
            double general = bids.stream().filter(bid -> bid.getDirection() == unitType.generalDirection())
                    .flatMap(b -> b.getDeals().stream()).map(Deal::getQuantity).reduce(0D, Double::sum);
            double opposite = bids.stream().filter(bid -> bid.getDirection() == unitType.generalDirection().opposite())
                    .flatMap(b -> b.getDeals().stream()).map(Deal::getQuantity).reduce(0D, Double::sum);

            Bid bid = instantBidMap.get(unit.getUnitId());
            List<Operation> operations = Collect.asList(Operation.DECLARE);
            if (bid != null) {
                double quantity = bid.getDeals().stream().map(Deal::getQuantity).reduce(0D, Double::sum);
                if (bid.getDirection() == unitType.generalDirection()) {
                    general += quantity;
                } else {
                    opposite += quantity;
                }
                builder.transit(bid.getTransit());
                if (bid.getBidStatus() == BidStatus.MANUAL_CANCELLED) {
                    if (Collect.isEmpty(bid.getDeals())) {
                        operations = Collect.asList(Operation.DECLARE);
                    } else {
                        operations = Collections.emptyList();
                    }

                } else if (bid.getBidStatus() == BidStatus.SYSTEM_CANCELLED){
                    operations = Collections.EMPTY_LIST;
                } else {
                    operations = bid.getBidStatus().operations();
                }
                // 报单内容
                RollBidVO rollBidVO = RollBidVO.builder()
                        .bidId(bid.getBidId())
                        .quantity(bid.getQuantity())
                        .transit(bid.getTransit())
                        .cancelled(bid.getCancelled())
                        .direction(bid.getDirection())
                        .bidStatus(bid.getBidStatus())
                        .price(bid.getPrice())
                        .declareTimeStamp(bid.getDeclareTimeStamp())
                        .cancelTimeStamp(bid.getCancelledTimeStamp())
                        .rollDealVOs(Collect.transfer(bid.getDeals(), d -> new RollDealVO(d.getQuantity(), d.getPrice(), d.getTimeStamp())))
                        .build();
                builder.rollBidVO(rollBidVO);
            }
            builder.operations(operations);
            builder.position(Double.parseDouble(String.format("%.2f", general - opposite)));
            return builder.build();
        }).collect(Collectors.toList());

    }


    /**
     * 省内报价接口
     * @param intraBidPO 省内报价请求结构体
     * @return 报单结果
     */
    @ToBid
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

    /**
     * 滚动报价接口
     * @param rollBidPO 滚动报价请求结构体
     * @return 报单结果
     */
    @ToBid
    @PostMapping("submitRollBidPO")
    public Result<Void> submitRollBidPO(@RequestBody RollBidPO rollBidPO) {
        StageId pStageId = StageId.parse(rollBidPO.getStageId());
        Comp comp = tunnel.runningComp();
        StageId cStageId = comp.getStageId();
        Long unitId = rollBidPO.getBidPO().getUnitId();
        UnitType unitType = domainTunnel.getByAggregateId(Unit.class, unitId).getMetaUnit().getUnitType();
        GridLimit gridLimit = tunnel.priceLimit(unitType);
        gridLimit.check(rollBidPO.getBidPO().getPrice());
        BizEx.falseThrow(pStageId.equals(cStageId), PARAM_FORMAT_WRONG.message("已经进入下一阶段不可报单"));
        BizEx.trueThrow(cStageId.getTradeStage().getTradeType() != TradeType.ROLL,
                PARAM_FORMAT_WRONG.message("当前为日前滚动报价阶段"));
        BizEx.trueThrow(cStageId.getMarketStatus() != MarketStatus.BID,
                PARAM_FORMAT_WRONG.message("当前竞价阶段已经关闭"));

        StepRecord stepRecord = comp.getStepRecords().stream().filter(s -> s.getStageId().equals(rollBidPO.getStageId())).findFirst().orElseThrow(SysEx::unreachable);
        List<Direction> directions = enableDirectionsOfRollStage(stepRecord);
        BizEx.falseThrow(directions.contains(rollBidPO.getBidPO().getDirection()), PARAM_FORMAT_WRONG.message("当前不允许此方向报单"));
        UnitCmd.RollBidDeclare command = UnitCmd.RollBidDeclare.builder()
                .bid(Convertor.INST.to(rollBidPO.getBidPO())).stageId(pStageId).build();
        log.info("command is {}", command);
        CommandBus.accept(command, new HashMap<>());

        return Result.success();
    }


    public List<Direction> enableDirections(StepRecord stepRecord) {
        long now = System.currentTimeMillis();
        Long startTimeStamp = stepRecord.getStartTimeStamp();
        Long endTimeStamp = stepRecord.getEndTimeStamp();
        if (now >= startTimeStamp && now < startTimeStamp + 180_000) {
            return Collect.asList(Direction.BUY);
        } else if (now >= startTimeStamp + 180_000 && now < startTimeStamp + 210_000) {
            return Collections.EMPTY_LIST;
        } else if (now >= startTimeStamp + 210_000 && now < startTimeStamp + 300_000) {
            return Collect.asList(Direction.SELL);
        } else {
            return Collect.asList(Direction.BUY, Direction.SELL);
        }
    }

    public List<Direction> enableDirectionsOfRollStage(StepRecord stepRecord) {
        long now = System.currentTimeMillis();
        Long startTimeStamp = stepRecord.getStartTimeStamp();
        Long endTimeStamp = stepRecord.getEndTimeStamp();
        if (now >= startTimeStamp && now < startTimeStamp + 180_000) {
            return Collect.asList(Direction.BUY);
        } else if (now >= startTimeStamp + 180_000 && now < startTimeStamp + 240_000) {
            return Collections.EMPTY_LIST;
        } else if (now >= startTimeStamp + 240_000 && now < startTimeStamp + 360_000) {
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
    @ToBid
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
     * 滚动撤单接口
     * @param rollCancelPO 滚动撤单请求结构体
     * @return 报单结果
     */
    @ToBid
    @PostMapping("submitRollCancelPO")
    public Result<Void> submitRollCancelPO(@RequestBody RollCancelPO rollCancelPO) {
        StageId pStageId = StageId.parse(rollCancelPO.getStageId());
        StageId cStageId = tunnel.runningComp().getStageId();

        BizEx.falseThrow(pStageId.equals(cStageId), PARAM_FORMAT_WRONG.message("已经进入下一阶段不可报单"));
        BizEx.trueThrow(cStageId.getTradeStage().getTradeType() != TradeType.ROLL,
                PARAM_FORMAT_WRONG.message("当前为滚动报价阶段"));
        BizEx.trueThrow(cStageId.getMarketStatus() != MarketStatus.BID,
                PARAM_FORMAT_WRONG.message("竞价阶段已经关闭，未达成挂牌，将由系统自动撤单"));

        BidDO bidDO = bidDOMapper.selectById(rollCancelPO.getBidId());
        boolean b0 = bidDO.getBidStatus() == BidStatus.NEW_DECELERATED;
        boolean b1 = bidDO.getBidStatus() == BidStatus.PART_DEAL;
        BizEx.falseThrow(b0 || b1, PARAM_FORMAT_WRONG.message("当前报单处于处于不可撤状态"));
        Long unitId = bidDO.getUnitId();
        UnitCmd.RollBidCancel command = UnitCmd.RollBidCancel.builder().unitId(unitId).cancelBidId(rollCancelPO.getBidId()).build();
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
    final SubRegionBasicMapper subRegionBasicMapper;

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
    @ToBid
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
            IntStream.range(0, gSegmentBidDOs.size() - offset).forEach(i -> {
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
    @SneakyThrows
    @SuppressWarnings("unchecked")
    @GetMapping("listClearedUnitVOs")
    public Result<List<UnitVO>> listClearedUnitVOs(@NotBlank String stageId, @NotBlank String unitType, @RequestHeader String token) {
        List<UnitVO> unitVOs = (List<UnitVO>) shortCache.get("listClearedUnitVOs" + stageId + unitType + TokenUtils.getUserId(token), () -> {
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
                    .filter(u -> u.getMetaUnit().getUnitType().equals(uType))
                    .sorted(Comparator.comparing(u -> u.getMetaUnit().getSourceId()))
                    .collect(Collectors.toList());
            return Collect.transfer(unitDOs,
                    unitDO -> new UnitVO(unitDO.getUnitId(), unitDO.getMetaUnit().getName(), unitDO.getMetaUnit()));
        });
        return Result.success(unitVOs);
    }

    final NodalPriceVoltageMapper nodalPriceVoltageMapper;
    final SubregionPriceMapper subregionPriceMapper;

    /**
     * 现货中标量量价曲线-分机组
     * @param stageId 阶段id
     * @param unitId 待查看的机组unitId
|     */
    @SneakyThrows
    @GetMapping("listGeneratorClearances")
    public Result<GeneratorClearVO> listGeneratorClearances(@NotBlank String stageId, @NotNull @Positive Long unitId) {
        GeneratorClearVO clearVO = (GeneratorClearVO) shortCache.get("listGeneratorClearances" + stageId + unitId, () -> doListGeneratorClearances(stageId, unitId));
        return Result.success(clearVO);
    }

    private GeneratorClearVO doListGeneratorClearances(String stageId, Long unitId) {
        Integer roundId = StageId.parse(stageId).getRoundId();
        Unit unit = domainTunnel.getByAggregateId(Unit.class, unitId);
        Integer sourceId = unit.getMetaUnit().getSourceId();
        LambdaQueryWrapper<GeneratorBasic> eq = new LambdaQueryWrapper<GeneratorBasic>().eq(GeneratorBasic::getUnitId, sourceId);
        Integer nodeId = unitBasicMapper.selectOne(eq).getNodeId();
        NodeBasicDO nodeBasicDO = nodeBasicDOMapper.selectById(nodeId);
        LambdaQueryWrapper<SubregionPrice> eq6 = new LambdaQueryWrapper<SubregionPrice>().eq(SubregionPrice::getRoundId, roundId + 1)
                .eq(SubregionPrice::getSubregionId, nodeBasicDO.getSubregionId());
        List<SubregionPrice> subregionPrices = subregionPriceMapper.selectList(eq6).stream()
                .sorted(Comparator.comparing(SubregionPrice::getPrd)).collect(Collectors.toList());
        List<Double> daPrices = Collect.transfer(subregionPrices, SubregionPrice::getDaLmp);
        List<Double> rtPrices = Collect.transfer(subregionPrices, SubregionPrice::getRtLmp);

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
            List<Pair<ClearedVO, List<ClearedVO>>> daPs = daSections.stream().map(ds -> {
                if (ds.size() >0) {
                    return Pair.<ClearedVO, List<ClearedVO>>of(ds.get(0), ds.subList(1, ds.size()));
                } else {
                    return Pair.<ClearedVO, List<ClearedVO>>of(null, new ArrayList<>());
                }
            }).collect(Collectors.toList());
            List<Pair<ClearedVO, List<ClearedVO>>> rtPs = rtSections.stream().map(ds -> {
                if (ds.size() > 0) {
                    return Pair.<ClearedVO, List<ClearedVO>>of(ds.get(0), ds.subList(1, ds.size()));
                } else {
                    return Pair.<ClearedVO, List<ClearedVO>>of(null, new ArrayList<>());
                }
            }).collect(Collectors.toList());
            builder.daMinClears(daPs.stream().map(Pair::getLeft).collect(Collectors.toList()));
            builder.daClearedSections(daPs.stream().map(Pair::getRight).collect(Collectors.toList()));
            builder.rtMinClears(rtPs.stream().map(Pair::getLeft).collect(Collectors.toList()));
            builder.rtClearedSections(rtPs.stream().map(Pair::getRight).collect(Collectors.toList()));
        } else {
            builder.daClearedSections(daSections);
            builder.rtClearedSections(rtSections);
        }
        return builder.build();
    }

    /**
     * 现货中标量量价曲线-分负荷
     * @param stageId 阶段id
     * @param unitId 待查看的负荷unitId
     */

    @SneakyThrows
    @GetMapping("listLoadClearances")
    public Result<LoadClearVO> listLoadClearances(@NotBlank String stageId, @NotNull @Positive Long unitId) {
        LoadClearVO loadClearVO = (LoadClearVO) shortCache.get("listLoadClearances" + stageId + unitId, () -> doListLoadClearances(stageId, unitId));
        return Result.success(loadClearVO);
    }
    public LoadClearVO doListLoadClearances(String stageId, Long unitId) {
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
        NodeBasicDO nodeBasicDO = nodeBasicDOMapper.selectById(nodeId);
        LambdaQueryWrapper<SubregionPrice> eqx = new LambdaQueryWrapper<SubregionPrice>()
                .eq(SubregionPrice::getRoundId, roundId + 1)
                .eq(SubregionPrice::getSubregionId, nodeBasicDO.getSubregionId());
        List<SubregionPrice> subregionPrices = subregionPriceMapper.selectList(eqx)
                .stream().sorted(Comparator.comparing(SubregionPrice::getPrd)).collect(Collectors.toList());
        builder.daPrice(Collect.transfer(subregionPrices, SubregionPrice::getDaLmp));
        builder.rtPrice(Collect.transfer(subregionPrices, SubregionPrice::getRtLmp));

        return builder.build();
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
                .eq(SpotUnitCleared::getRoundId, roundId + 1).in(SpotUnitCleared::getUnitId, sourceIds);
        Map<Integer, List<SpotUnitCleared>> clearResult = Collect.isEmpty(sourceIds) ? new HashMap<>() : spotUnitClearedMapper.selectList(in)
                .stream().collect(Collectors.groupingBy(SpotUnitCleared::getUnitId));

        LambdaQueryWrapper<InterSpotUnitOfferDO> in1 = new LambdaQueryWrapper<InterSpotUnitOfferDO>()
                .eq(InterSpotUnitOfferDO::getRoundId, roundId + 1)
                .in(InterSpotUnitOfferDO::getUnitId, sourceIds);
        ListMultimap<Integer, InterSpotUnitOfferDO> spotOfferMap = Collect.isEmpty(sourceIds) ?
                ArrayListMultimap.create() : interSpotUnitOfferDOMapper.selectList(in1).stream().collect(Collect.listMultiMap(InterSpotUnitOfferDO::getUnitId));

        GridLimit priceLimit = tunnel.priceLimit(UnitType.GENERATOR);

        LambdaQueryWrapper<StackDiagramDO> eq1 = new LambdaQueryWrapper<StackDiagramDO>()
                .eq(StackDiagramDO::getRoundId, parsedStageId.getRoundId() + 1);
        List<StackDiagramDO> stackDiagramDOS = stackDiagramDOMapper.selectList(eq1)
                .stream().sorted(Comparator.comparing(StackDiagramDO::getPrd)).collect(Collectors.toList());


        List<SpotInterBidVO> spotInterBidVOs = generatorUnitDOs.stream().map(unitDO -> {
            Integer sourceId = unitDO.getMetaUnit().getSourceId();
            SpotInterBidVO.SpotInterBidVOBuilder builder = SpotInterBidVO.builder().sourceId(sourceId)
                    .unitId(unitDO.getUnitId())
                    .unitType(unitDO.getMetaUnit().getUnitType())
                    .generatorType(unitDO.getMetaUnit().getGeneratorType())
                    .unitName(unitDO.getMetaUnit().getName()).priceLimit(priceLimit);
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
    @ToBid
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
