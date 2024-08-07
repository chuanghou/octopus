package com.bilanee.octopus.adapter.facade;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilanee.octopus.adapter.facade.vo.*;
import com.bilanee.octopus.adapter.repository.UnitAdapter;
import com.bilanee.octopus.adapter.tunnel.BidQuery;
import com.bilanee.octopus.adapter.tunnel.InterClearance;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.*;
import com.bilanee.octopus.basic.enums.*;
import com.bilanee.octopus.domain.*;
import com.bilanee.octopus.infrastructure.entity.*;
import com.bilanee.octopus.infrastructure.mapper.*;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ListMultimap;
import com.stellariver.milky.common.base.*;
import com.stellariver.milky.common.tool.common.Kit;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.common.tool.util.Json;
import com.stellariver.milky.domain.support.base.DomainTunnel;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.tuple.Pair;
import org.mapstruct.Builder;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.stellariver.milky.common.base.ErrorEnumsBase.PARAM_FORMAT_WRONG;

/**
 * 竞赛相关
 */
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequestMapping("/api/comp")
public class CompFacade {

    // test
    final Tunnel tunnel;
    final CompDOMapper compDOMapper;
    final UnitDOMapper unitDOMapper;
    final DomainTunnel domainTunnel;
    final ClearanceDOMapper clearanceDOMapper;
    final GeneratorDaSegmentMapper generatorDaSegmentMapper;
    final GeneratorForecastValueMapper generatorForecastValueMapper;
    final GeneratorDaForecastBidMapper generatorDaForecastBidMapper;
    final LoadForecastValueMapper loadForecastValueMapper;
    final LoadDaForecastBidMapper loadDaForecastBidMapper;
    final ThermalCostDOMapper thermalCostDOMapper;
    final TieLinePowerDOMapper tieLinePowerDOMapper;
    final MarketSettingMapper marketSettingMapper;
    final SprDOMapper sprDOMapper;
    final UnitBasicMapper unitBasicMapper;
    final SpotUnitClearedMapper spotUnitClearedMapper;


    static private final Cache<String, Object> cache = CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.MINUTES).maximumSize(10000L).build();

    /**
     * 当前运行竞赛查看
     * 返回result
     * @return 当前运行竞赛概况
     */
    @GetMapping("/runningCompVO")
    public Result<CompVO> runningCompVO(@RequestHeader String token) {
        String userId = TokenUtils.getUserId(token);
        Comp comp = tunnel.runningComp();
        if (comp == null || !comp.getUserIds().contains(userId) || comp.getCompStage() == CompStage.END) {
            return Result.error(ErrorEnums.COMP_NOT_EXISTED, ExceptionType.BIZ);
        }
        return Result.success(Convertor.INST.to(comp));
    }


    /**
     * 省间/多年出清结果
     * @param stageId 阶段id
     * @param token 访问者token
     * @return 省间出清结果
     */

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @GetMapping("/interClearanceVO")
    public Result<List<InterClearanceVO>> interClearanceVO(@NotBlank String stageId, @RequestHeader String token) {
        List<InterClearanceVO> interClearanceVOs = (List<InterClearanceVO>)
                cache.get("interClearanceVO" + stageId + TokenUtils.getUserId(token), () -> doInterClearanceVO(stageId, token));
        return Result.success(interClearanceVOs);
    }
    public List<InterClearanceVO> doInterClearanceVO(String stageId ,String token) {
        StageId parsedStageId = StageId.parse(stageId);

        // 清算值
        LambdaQueryWrapper<ClearanceDO> eq = new LambdaQueryWrapper<ClearanceDO>().eq(ClearanceDO::getStageId, stageId);
        List<ClearanceDO> clearanceDOs = clearanceDOMapper.selectList(eq);
        List<InterClearance> interClearances = Collect.transfer(clearanceDOs, clearanceDO -> Json.parse(clearanceDO.getClearance(), InterClearance.class));
        List<InterClearanceVO> interClearVOs = interClearances.stream().map(Convertor.INST::to).collect(Collectors.toList());

        boolean ranking = tunnel.review();

        // 单元信息
        LambdaQueryWrapper<UnitDO> queryWrapper = new LambdaQueryWrapper<UnitDO>().eq(UnitDO::getCompId, parsedStageId.getCompId())
                .eq(UnitDO::getRoundId, parsedStageId.getRoundId())
                .eq(!ranking, UnitDO::getUserId, TokenUtils.getUserId(token));
        List<UnitDO> unitDOs = unitDOMapper.selectList(queryWrapper);
        List<Unit> units;
        if (parsedStageId.getTradeStage() != TradeStage.MULTI_ANNUAL) {
            units = Collect.transfer(unitDOs, UnitAdapter.Convertor.INST::to).stream()
                    .filter(unit -> unit.getMetaUnit().getProvince().interDirection() == unit.getMetaUnit().getUnitType().generalDirection()).collect(Collectors.toList());
        } else {
            units = Collect.transfer(unitDOs, UnitAdapter.Convertor.INST::to).stream()
                    .filter(unit -> unit.getMetaUnit().getRenewableType() != null).collect(Collectors.toList());
        }
        List<UnitVO> unitVOs = Collect.transfer(units, u -> new UnitVO(u.getUnitId(), u.getMetaUnit().getName(), u.getMetaUnit()))
                .stream().filter(u -> parsedStageId.getTradeStage() != TradeStage.MULTI_ANNUAL || GeneratorType.RENEWABLE.equals(u.getMetaUnit().getGeneratorType()))
                .sorted(Comparator.comparing(u -> u.getMetaUnit().getSourceId()))
                .collect(Collectors.toList());
        interClearVOs.forEach(interClearanceVO -> {
            if (parsedStageId.getTradeStage() != TradeStage.MULTI_ANNUAL) {
                interClearanceVO.setUnitVOs(unitVOs);
            } else {
                MultiYearFrame multiYearFrame = interClearanceVO.getMultiYearFrame();
                List<UnitVO> filterUnitVOs = unitVOs.stream().filter(u -> u.getMetaUnit().getProvince().equals(multiYearFrame.getProvince()))
                        .filter(u -> multiYearFrame.getRenewableType().equals(u.getMetaUnit().getRenewableType()))
                        .collect(Collectors.toList());
                interClearanceVO.setUnitVOs(filterUnitVOs);
            }
        });

        // 委托及成交信息
        BidQuery bidQuery = BidQuery.builder()
                .compId(parsedStageId.getCompId())
                .roundId(parsedStageId.getRoundId())
                .tradeStage(parsedStageId.getTradeStage())
                .userId(ranking ? null : TokenUtils.getUserId(token))
                .build();

        Set<Long> unitIds = unitVOs.stream().map(UnitVO::getUnitId).collect(Collectors.toSet());

        List<Bid> bids = tunnel.listBids(bidQuery);
        interClearVOs.forEach(interClearanceVO -> {
            List<Bid> subBids = bids.stream().filter(b -> unitIds.contains(b.getUnitId())).filter(b -> {
                if (parsedStageId.getTradeStage() == TradeStage.MULTI_ANNUAL) {
                    return b.getRenewableType() == interClearanceVO.getMultiYearFrame().getRenewableType()
                            && b.getProvince() == interClearanceVO.getMultiYearFrame().getProvince();
                } else if (parsedStageId.getTradeStage() == TradeStage.AN_INTER || parsedStageId.getTradeStage() == TradeStage.MO_INTER) {
                    return b.getTimeFrame() == interClearanceVO.getTimeFrame();
                } else {
                    throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
                }
            }).collect(Collectors.toList());
            List<UnitDealVO> unitDealVOS = subBids.stream().collect(Collect.listMultiMap(Bid::getUnitId)).asMap().entrySet().stream().map(e -> {
                Long unitId = e.getKey();
                Collection<Bid> unitBids = e.getValue();
                List<Deal> deals = unitBids.stream().flatMap(bid -> bid.getDeals().stream()).collect(Collectors.toList());
                Double totalQuantity = deals.stream().map(Deal::getQuantity).reduce(0D, Double::sum);
                Double totalVolume = deals.stream().map(deal -> deal.getQuantity() * deal.getPrice()).reduce(0D, Double::sum);
                return UnitDealVO.builder()
                        .unitId(unitId)
                        .averagePrice(totalVolume.equals(0D) ? null : totalVolume / totalQuantity)
                        .totalQuantity(totalQuantity)
                        .deals(deals)
                        .build();
            }).collect(Collectors.toList());
            interClearanceVO.setUnitDealVOS(unitDealVOS);
        });

        return interClearVOs;
    }

    /**
     * 省内结算结果
     * @param stageId 阶段id
     * @param token 访问者token
     * @return 省间出清结果
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    @GetMapping("/intraClearanceVO")
    public Result<List<IntraClearanceVO>> intraClearanceVO(@NotBlank String stageId, @RequestHeader String token) {
        List<IntraClearanceVO> intraClearanceVOs = (List<IntraClearanceVO>)
                cache.get("intraClearanceVO" + stageId + TokenUtils.getUserId(token), () -> doIntraClearanceVO(stageId, token));
        return Result.success(intraClearanceVOs);
    }

    public List<IntraClearanceVO> doIntraClearanceVO(@NotBlank String stageId, @RequestHeader String token) {

        Comp comp = tunnel.runningComp();
        StageId parsed = StageId.parse(stageId);
        boolean review = tunnel.review();

        String userId = TokenUtils.getUserId(token);

        BidQuery bidQuery = BidQuery.builder().compId(parsed.getCompId())
                .roundId(parsed.getRoundId()).tradeStage(parsed.getTradeStage())
                .build();

        LambdaQueryWrapper<UnitDO> queryWrapper = new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getCompId, parsed.getCompId())
                .eq(UnitDO::getRoundId, parsed.getRoundId());
        List<UnitDO> unitDOs = unitDOMapper.selectList(queryWrapper);


        ListMultimap<IntraSymbol, Bid> groupedBids = tunnel.listBids(bidQuery).stream()
                .collect(Collect.listMultiMap(i -> new IntraSymbol(i.getProvince(), i.getTimeFrame())));
        return IntraSymbol.intraSymbols().stream().map(intraSymbol -> {
            List<Bid> bids = groupedBids.get(intraSymbol);
            List<Deal> deals = bids.stream().flatMap(b -> b.getDeals().stream()).collect(Collectors.toList());
            Double maxPrice = deals.stream().max(Comparator.comparing(Deal::getPrice)).map(Deal::getPrice).orElse(null);
            Double minPrice = deals.stream().min(Comparator.comparing(Deal::getPrice)).map(Deal::getPrice).orElse(null);
            Double totalVolume = deals.stream().map(d -> d.getPrice() * d.getQuantity()).reduce(0D, Double::sum);
            Double totalQuantity = deals.stream().map(Deal::getQuantity).reduce(0D, Double::sum);
            Double averagePrice = totalQuantity.equals(0D) ? null : (totalVolume / totalQuantity);
            Double buyTransit = bids.stream().filter(bid -> bid.getDirection() == Direction.BUY)
                    .map(Bid::getCloseBalance).filter(Objects::nonNull).reduce(0D, Double::sum);
            Double sellTransit = bids.stream().filter(bid -> bid.getDirection() == Direction.SELL)
                    .map(Bid::getCloseBalance).filter(Objects::nonNull).reduce(0D, Double::sum);


            List<Pair<Double, Double>> cDeals = bids.stream().filter(bid -> bid.getDirection() == Direction.BUY).flatMap(bid -> bid.getDeals().stream())
                    .collect(Collectors.groupingBy(Deal::getPrice)).entrySet().stream()
                    .map(ee -> Pair.of(ee.getKey(), ee.getValue().stream().collect(Collectors.summarizingDouble(Deal::getQuantity)).getSum()))
                    .collect(Collectors.toList());

            cDeals = cDeals.stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList());

            List<DealHist> dealHists = new ArrayList<>();
            if (cDeals.size() > 10) {
                double min = cDeals.stream().min(Map.Entry.comparingByKey()).orElseThrow(SysEx::unreachable).getLeft() - 1;
                double max = cDeals.stream().max(Map.Entry.comparingByKey()).orElseThrow(SysEx::unreachable).getLeft() + 1;
                double v = (max - min) / 10;
                for (int i = 0; i < 10; i++) {
                    Double left = min + i * v;
                    Double right = min + (i + 1) * v;
                    List<Pair<Double, Double>> collectDeals = cDeals.stream()
                            .filter(cDeal -> cDeal.getLeft() >= left && cDeal.getLeft() < right)
                            .collect(Collectors.toList());
                    double sum = collectDeals.stream().collect(Collectors.summarizingDouble(Pair::getRight)).getSum();
                    DealHist dealHist = DealHist.builder().left(left).right(right).value(sum).build();
                    dealHists.add(dealHist);
                }
            } else {
                dealHists = cDeals.stream().sorted(Map.Entry.comparingByKey())
                        .map(d -> DealHist.builder().left(d.getLeft()).right(d.getLeft()).value(d.getRight()).build()).collect(Collectors.toList());
            }

            List<Unit> units = Collect.transfer(unitDOs, UnitAdapter.Convertor.INST::to).stream()
                    .filter(unit -> review || unit.getUserId().equals(userId))
                    .filter(unit -> unit.getMetaUnit().getProvince().equals(intraSymbol.getProvince())).collect(Collectors.toList());
            List<UnitVO> unitVOs = Collect.transfer(units, u -> new UnitVO(u.getUnitId(), u.getMetaUnit().getName(), u.getMetaUnit()))
                    .stream().sorted(Comparator.comparing(u -> u.getMetaUnit().getSourceId())).collect(Collectors.toList());

            List<UnitDealVO> unitDealVOs = bids.stream().filter(unit -> review || unit.getUserId().equals(userId))
                    .collect(Collect.listMultiMap(Bid::getUnitId)).asMap().entrySet().stream().map(ee -> {
                        Long unitId = ee.getKey();
                        Collection<Bid> unitBids = ee.getValue();
                        List<Deal> unitDeals = unitBids.stream().flatMap(bid -> bid.getDeals().stream()).collect(Collectors.toList());
                        Double unitTotalVolume = unitDeals.stream().map(deal -> deal.getQuantity() * deal.getPrice()).reduce(0D, Double::sum);
                        Double unitTotalQuantity = unitDeals.stream().map(Deal::getQuantity).reduce(0D, Double::sum);
                        return UnitDealVO.builder()
                                .unitId(unitId)
                                .averagePrice(unitTotalVolume.equals(0D) ? null : unitTotalVolume / unitTotalQuantity)
                                .totalQuantity(unitTotalQuantity)
                                .deals(unitDeals)
                                .build();
                    }).collect(Collectors.toList());


            return IntraClearanceVO.builder()
                    .province(intraSymbol.getProvince())
                    .timeFrame(intraSymbol.getTimeFrame())
                    .averageDealPrice(averagePrice)
                    .maxDealPrice(maxPrice)
                    .minDealPrice(minPrice)
                    .buyTotalTransit(buyTransit)
                    .sellTotalTransit(sellTransit)
                    .totalDealQuantity(totalQuantity/2)
                    .unitVOs(unitVOs)
                    .unitDealVOS(unitDealVOs)
                    .dealHists(dealHists)
                    .build();
        }).collect(Collectors.toList());
    }


    /**
     * 滚动结算结果
     * @param stageId 阶段id
     * @param token 访问者token
     * @return 滚动出清结果
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    @GetMapping("/rollClearanceVO")
    public Result<List<RollClearanceVO>> rollClearanceVO(@NotBlank String stageId, @RequestHeader String token) {
        List<RollClearanceVO> intraClearanceVOs = (List<RollClearanceVO>)
                cache.get("rollClearanceVO" + stageId + TokenUtils.getUserId(token), () -> doRollClearanceVO(stageId, token));
        return Result.success(intraClearanceVOs);
    }

    public List<RollClearanceVO> doRollClearanceVO(String stageId, String token) {

        Comp comp = tunnel.runningComp();
        StageId parsed = StageId.parse(stageId);
        boolean review = tunnel.review();

        String userId = TokenUtils.getUserId(token);

        BidQuery bidQuery = BidQuery.builder().compId(parsed.getCompId())
                .roundId(parsed.getRoundId()).tradeStage(parsed.getTradeStage())
                .build();

        LambdaQueryWrapper<UnitDO> queryWrapper = new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getCompId, parsed.getCompId())
                .eq(UnitDO::getRoundId, parsed.getRoundId());
        List<UnitDO> unitDOs = unitDOMapper.selectList(queryWrapper);


        ListMultimap<RollSymbol, Bid> groupedBids = tunnel.listBids(bidQuery).stream()
                .collect(Collect.listMultiMap(i -> new RollSymbol(i.getProvince(), i.getInstant())));
        return RollSymbol.rollSymbols().stream().map(rollSymbol -> {
            List<Bid> bids = groupedBids.get(rollSymbol);
            List<Deal> deals = bids.stream().flatMap(b -> b.getDeals().stream()).collect(Collectors.toList());
            Double maxPrice = deals.stream().max(Comparator.comparing(Deal::getPrice)).map(Deal::getPrice).orElse(null);
            Double minPrice = deals.stream().min(Comparator.comparing(Deal::getPrice)).map(Deal::getPrice).orElse(null);
            Double totalVolume = deals.stream().map(d -> d.getPrice() * d.getQuantity()).reduce(0D, Double::sum);
            Double totalQuantity = deals.stream().map(Deal::getQuantity).reduce(0D, Double::sum);
            Double averagePrice = totalQuantity.equals(0D) ? null : (totalVolume / totalQuantity);
            Double buyTransit = bids.stream().filter(bid -> bid.getDirection() == Direction.BUY)
                    .map(Bid::getCloseBalance).filter(Objects::nonNull).reduce(0D, Double::sum);
            Double sellTransit = bids.stream().filter(bid -> bid.getDirection() == Direction.SELL)
                    .map(Bid::getCloseBalance).filter(Objects::nonNull).reduce(0D, Double::sum);


            List<Pair<Double, Double>> cDeals = bids.stream().filter(bid -> bid.getDirection() == Direction.BUY).flatMap(bid -> bid.getDeals().stream())
                    .collect(Collectors.groupingBy(Deal::getPrice)).entrySet().stream()
                    .map(ee -> Pair.of(ee.getKey(), ee.getValue().stream().collect(Collectors.summarizingDouble(Deal::getQuantity)).getSum()))
                    .collect(Collectors.toList());

            cDeals = cDeals.stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList());

            List<DealHist> dealHists = new ArrayList<>();
            if (cDeals.size() > 10) {
                double min = cDeals.stream().min(Map.Entry.comparingByKey()).orElseThrow(SysEx::unreachable).getLeft() - 1;
                double max = cDeals.stream().max(Map.Entry.comparingByKey()).orElseThrow(SysEx::unreachable).getLeft() + 1;
                double v = (max - min) / 10;
                for (int i = 0; i < 10; i++) {
                    Double left = min + i * v;
                    Double right = min + (i + 1) * v;
                    List<Pair<Double, Double>> collectDeals = cDeals.stream()
                            .filter(cDeal -> cDeal.getLeft() >= left && cDeal.getLeft() < right)
                            .collect(Collectors.toList());
                    double sum = collectDeals.stream().collect(Collectors.summarizingDouble(Pair::getRight)).getSum();
                    DealHist dealHist = DealHist.builder().left(left).right(right).value(sum).build();
                    dealHists.add(dealHist);
                }
            } else {
                dealHists = cDeals.stream().sorted(Map.Entry.comparingByKey())
                        .map(d -> DealHist.builder().left(d.getLeft()).right(d.getLeft()).value(d.getRight()).build()).collect(Collectors.toList());
            }

            List<Unit> units = Collect.transfer(unitDOs, UnitAdapter.Convertor.INST::to).stream()
                    .filter(unit -> review || unit.getUserId().equals(userId))
                    .filter(unit -> unit.getMetaUnit().getProvince().equals(rollSymbol.getProvince())).collect(Collectors.toList());
            List<UnitVO> unitVOs = Collect.transfer(units, u -> new UnitVO(u.getUnitId(), u.getMetaUnit().getName(), u.getMetaUnit()))
                    .stream().sorted(Comparator.comparing(u -> u.getMetaUnit().getSourceId())).collect(Collectors.toList());

            List<UnitDealVO> unitDealVOs = bids.stream().filter(unit -> review || unit.getUserId().equals(userId))
                    .collect(Collect.listMultiMap(Bid::getUnitId)).asMap().entrySet().stream().map(ee -> {
                        Long unitId = ee.getKey();
                        Collection<Bid> unitBids = ee.getValue();
                        List<Deal> unitDeals = unitBids.stream().flatMap(bid -> bid.getDeals().stream()).collect(Collectors.toList());
                        Double unitTotalVolume = unitDeals.stream().map(deal -> deal.getQuantity() * deal.getPrice()).reduce(0D, Double::sum);
                        Double unitTotalQuantity = unitDeals.stream().map(Deal::getQuantity).reduce(0D, Double::sum);
                        return UnitDealVO.builder()
                                .unitId(unitId)
                                .averagePrice(unitTotalVolume.equals(0D) ? null : unitTotalVolume / unitTotalQuantity)
                                .totalQuantity(unitTotalQuantity)
                                .deals(unitDeals)
                                .build();
                    }).collect(Collectors.toList());


            return RollClearanceVO.builder()
                    .province(rollSymbol.getProvince())
                    .instant(rollSymbol.getInstant())
                    .averageDealPrice(averagePrice)
                    .maxDealPrice(maxPrice)
                    .minDealPrice(minPrice)
                    .buyTotalTransit(buyTransit)
                    .sellTotalTransit(sellTransit)
                    .totalDealQuantity(totalQuantity/2)
                    .unitVOs(unitVOs)
                    .unitDealVOS(unitDealVOs)
                    .dealHists(dealHists)
                    .build();
        }).collect(Collectors.toList());
    }


    private List<Section> buildSections(List<Bid> bids, Comparator<Bid> comparator) {
        List<Bid> sortedBids = bids.stream().sorted(comparator).collect(Collectors.toList());
        Double x = 0D;
        List<Section> sections = new ArrayList<>();
        for (Bid sortedBid : sortedBids) {
            Section section = Section.builder().unitId(sortedBid.getUnitId())
                    .lx(x).y(sortedBid.getPrice()).rx(x + sortedBid.getQuantity()).build();
            x += section.getRx();
            sections.add(section);
        }
        return sections;
    }

    final SubRegionBasicMapper subRegionBasicMapper;
    final SubregionPriceMapper subregionPriceMapper;



    /**
     * 省内现货市场：市场成交概况，市场供需曲线，分时间点取
     * @param stageId 当前页面所处stageId
     * @param province 查看省份
     * @return 现货供需曲线
     */
    @SneakyThrows
    @GetMapping("listSpotMarketVOByInstant")
    public Result<InstantSpotMarketVO> getSpotMarketVOs(String stageId, String province, Integer instant, @RequestHeader String token) {


        Comp comp = tunnel.runningComp();
        BizEx.nullThrow(comp, ErrorEnums.COMP_NOT_EXISTED);

        Long compId = StageId.parse(stageId).getCompId();
        BizEx.falseThrow(Objects.equals(compId, comp.getCompId()), PARAM_FORMAT_WRONG.message("该场次已经结束，请重新进入"));


        SpotMarketVO spotMarketVO = (SpotMarketVO) cache.get(
                "listSpotMarketVOs" + stageId + province + TokenUtils.getUserId(token), () -> doListSpotMarketVOs(stageId, province, token));
        InstantSpotMarketVO instantSpotMarketVO = InstantSpotMarketVO.builder()
                .daEntityVO(spotMarketVO.getDaEntityVOs().get(instant))
                .rtEntityVO(spotMarketVO.getRtEntityVOs().get(instant))
                .daIntraSpotDealVO(spotMarketVO.getDaIntraSpotDealVO())
                .rtIntraSpotDealVO(spotMarketVO.getRtIntraSpotDealVO())
                .unitVOs(spotMarketVO.getUnitVOs())
                .build();
        return Result.success(instantSpotMarketVO);
    }

    /**
     * 省内现货市场：市场成交概况，市场供需曲线
     * @param stageId 当前页面所处stageId
     * @param province 查看省份
     * @return 现货供需曲线
     */

    @SneakyThrows
    @GetMapping("listSpotMarketVOs")
    public Result<SpotMarketVO> listSpotMarketVOs(String stageId, String province, @RequestHeader String token) {
        SpotMarketVO spotMarketVO = (SpotMarketVO) cache.get(
                "listSpotMarketVOs" + stageId + province + TokenUtils.getUserId(token), () -> doListSpotMarketVOs(stageId, province, token));
        return Result.success(spotMarketVO);
    }

    @SneakyThrows
    public SpotMarketVO doListSpotMarketVOs(String stageId, String province, String token) {
        StageId parsed = StageId.parse(stageId);
        Province parsedProvince = Kit.enumOfMightEx(Province::name, province);
        // 搜索的单元列表
        boolean equals = tunnel.review();
        LambdaQueryWrapper<UnitDO> queryWrapper1 = new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getCompId, parsed.getCompId())
                .eq(UnitDO::getRoundId, parsed.getRoundId())
                .eq(!equals, UnitDO::getUserId, TokenUtils.getUserId(token));
        List<UnitDO> unitDOs = unitDOMapper.selectList(queryWrapper1);
        List<Unit> units = Collect.transfer(unitDOs, UnitAdapter.Convertor.INST::to).stream()
                .filter(unit -> unit.getMetaUnit().getProvince().equals(parsedProvince))
                .filter(unit -> unit.getMetaUnit().getUnitType().equals(UnitType.GENERATOR))
                .collect(Collectors.toList());
        List<UnitVO> unitVOs = Collect.transfer(units, u -> new UnitVO(u.getUnitId(), u.getMetaUnit().getName(), u.getMetaUnit()));
        SpotMarketVO spotMarketVO = prepareCacheSpotMarketVO(stageId, province);
        spotMarketVO.setUnitVOs(unitVOs);
        return spotMarketVO;
    }

    @SneakyThrows
    public SpotMarketVO prepareCacheSpotMarketVO(String stageId, String province) {
        return (SpotMarketVO) cache.get("doListSpotMarketVOs" + stageId + province, () -> doListSpotMarketVOs(stageId, province));
    }

    public SpotMarketVO doListSpotMarketVOs(String stageId, String province) {
        StageId parsed = StageId.parse(stageId);
        Province parsedProvince = Kit.enumOfMightEx(Province::name, province);

        SpotMarketVO.SpotMarketVOBuilder builder = SpotMarketVO.builder();

        LambdaQueryWrapper<SubRegionBasic> eq1 =
                new LambdaQueryWrapper<SubRegionBasic>().eq(SubRegionBasic::getProv, parsedProvince.getDbCode());

        List<Integer> subRegionIds = subRegionBasicMapper.selectList(eq1).stream().map(SubRegionBasic::getSubregionId).collect(Collectors.toList());

        //  表头的4个值
        LambdaQueryWrapper<SubregionPrice> eq = new LambdaQueryWrapper<SubregionPrice>()
                .eq(SubregionPrice::getRoundId, parsed.getRoundId() + 1)
                .in(SubregionPrice::getSubregionId, subRegionIds);
        List<SubregionPrice> subregionPrices = Collect.isEmpty(subRegionIds) ? new ArrayList<>() : subregionPriceMapper.selectList(eq);
        Double maxDaPrice = subregionPrices.stream().max(Comparator.comparing(SubregionPrice::getDaLmp)).map(SubregionPrice::getDaLmp).orElseThrow(SysEx::unreachable);
        Double minDaPrice = subregionPrices.stream().min(Comparator.comparing(SubregionPrice::getDaLmp)).map(SubregionPrice::getDaLmp).orElseThrow(SysEx::unreachable);
        Double maxRtPrice = subregionPrices.stream().max(Comparator.comparing(SubregionPrice::getRtLmp)).map(SubregionPrice::getRtLmp).orElseThrow(SysEx::unreachable);
        Double minRtPrice = subregionPrices.stream().min(Comparator.comparing(SubregionPrice::getRtLmp)).map(SubregionPrice::getRtLmp).orElseThrow(SysEx::unreachable);

        LambdaQueryWrapper<UnitDO> queryWrapper0 = new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getCompId, parsed.getCompId())
                .eq(UnitDO::getRoundId, parsed.getRoundId());
        List<Integer> loadSourceIds = unitDOMapper.selectList(queryWrapper0).stream()
                .filter(u -> u.getMetaUnit().getProvince().equals(parsedProvince))
                .filter(u -> u.getMetaUnit().getUnitType().equals(UnitType.LOAD))
                .map(u -> u.getMetaUnit().getSourceId()).collect(Collectors.toList());

        LambdaQueryWrapper<LoadDaForecastBidDO> in = new LambdaQueryWrapper<LoadDaForecastBidDO>().eq(LoadDaForecastBidDO::getRoundId, parsed.getRoundId() + 1)
                .in(LoadDaForecastBidDO::getLoadId, loadSourceIds);
        List<Double> daInstantLoadBids = Collect.isEmpty(loadSourceIds) ? new ArrayList<>() : loadDaForecastBidMapper.selectList(in).stream().collect(Collectors.groupingBy(LoadDaForecastBidDO::getPrd)).values()
                .stream().map(bids -> bids.stream().collect(Collectors.summarizingDouble(LoadDaForecastBidDO::getBidMw)).getSum()).collect(Collectors.toList());

        LambdaQueryWrapper<LoadForecastValueDO> in1 = new LambdaQueryWrapper<LoadForecastValueDO>()
                .in(LoadForecastValueDO::getLoadId, loadSourceIds)
                .eq(LoadForecastValueDO::getRoundId, parsed.getRoundId() + 1);
        List<Double> rtInstantLoadBids = Collect.isEmpty(loadSourceIds) ? new ArrayList<>() : loadForecastValueMapper.selectList(in1).stream().collect(Collectors.groupingBy(LoadForecastValueDO::getPrd)).values()
                .stream().map(vs -> vs.stream().collect(Collectors.summarizingDouble(LoadForecastValueDO::getRtP)).getSum()).collect(Collectors.toList());

        // 日前的
        IntraSpotDealVO daIntraSpotDealVO = IntraSpotDealVO.builder().maxDealPrice(maxDaPrice).minDealPrice(minDaPrice)
                .minLoad(daInstantLoadBids.stream().min(Double::compareTo).orElse(null))
                .maxLoad(daInstantLoadBids.stream().max(Double::compareTo).orElse(null)).build();
        builder.daIntraSpotDealVO(daIntraSpotDealVO);

        // 实时的
        IntraSpotDealVO rtIntraSpotDealVO = IntraSpotDealVO.builder().maxDealPrice(maxRtPrice).minDealPrice(minRtPrice)
                .minLoad(rtInstantLoadBids.stream().min(Double::compareTo).orElse(null))
                .maxLoad(rtInstantLoadBids.stream().max(Double::compareTo).orElse(null)).build();
        builder.rtIntraSpotDealVO(rtIntraSpotDealVO);

        Qps supply = supply(parsed, parsedProvince);
        Qps demand = demand(parsed, parsedProvince);
        List<List<Qp>> supplyDa = supply.getDa();
        List<List<Qp>> supplyRt = supply.getRt();
        List<List<Qp>> demandDa = demand.getDa();
        List<List<Qp>> demandRt = demand.getRt();
        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(1);
        Double offerPriceCap = marketSettingDO.getOfferPriceCap();
        List<SpotMarketEntityVO> daEntityVOs = IntStream.range(0, 24).mapToObj(i -> {
            List<Qp> supplyQps = supplyDa.get(i);
            List<Section> supplySections = toSections(supplyQps, Qp::getPrice, Comparator.comparing(Qp::getPrice));
            Point<Double> supplyTerminus = new Point<>(supplySections.get(supplySections.size() - 1).getRx(), offerPriceCap);

            List<Section> costSections = toSections(supplyQps, Qp::getCost, Comparator.comparing(Qp::getCost));
            Point<Double> costTerminus = new Point<>(supplySections.get(supplySections.size() - 1).getRx(), offerPriceCap);

            List<Qp> demandQps = demandDa.get(i);
            List<Section> demandSections = toSections(demandQps, Qp::getPrice, Comparator.comparing(Qp::getPrice).reversed());
            Point<Double> demandTerminus = new Point<>(demandSections.get(demandSections.size() - 1).getRx(), 0D);
            return SpotMarketEntityVO.builder()
                    .costSections(costSections)
                    .costTerminus(costTerminus)
                    .supplySections(supplySections)
                    .supplyTerminus(supplyTerminus)
                    .demandSections(demandSections)
                    .demandTerminus(demandTerminus)
                    .build();
        }).collect(Collectors.toList());

        builder.daEntityVOs(daEntityVOs);


        List<SpotMarketEntityVO> rtEntityVOs = IntStream.range(0, 24).mapToObj(i -> {
            List<Qp> supplyQps = supplyRt.get(i);
            List<Section> supplySections = toSections(supplyQps, Qp::getPrice, Comparator.comparing(Qp::getPrice));
            Point<Double> supplyTerminus = new Point<>(supplySections.get(supplySections.size() - 1).getRx(), offerPriceCap);

            List<Section> costSections = toSections(supplyQps, Qp::getCost, Comparator.comparing(Qp::getCost));
            Point<Double> costTerminus = new Point<>(supplySections.get(supplySections.size() - 1).getRx(), offerPriceCap);


            List<Qp> demandQps = demandRt.get(i);
            List<Section> demandSections = toSections(demandQps, Qp::getPrice, Comparator.comparing(Qp::getPrice).reversed());
            Point<Double> demandTerminus = new Point<>(demandSections.get(demandSections.size() - 1).getRx(), 0D);
            return SpotMarketEntityVO.builder()
                    .costSections(costSections)
                    .costTerminus(costTerminus)
                    .supplySections(supplySections)
                    .supplyTerminus(supplyTerminus)
                    .demandSections(demandSections)
                    .demandTerminus(demandTerminus)
                    .build();
        }).collect(Collectors.toList());

        return builder.rtEntityVOs(rtEntityVOs).build();
    }

    List<Section> toSections(List<Qp> qps, Function<Qp, Double> getter, Comparator<Qp> comparator) {
        qps = qps.stream().sorted(comparator).collect(Collectors.toList());
        Double accumulate = 0D;
        List<Section> sections = new ArrayList<>();
        for (Qp qp : qps) {
            Section section = Section.builder()
                    .unitId(qp.getUnitId()).lx(accumulate).rx(accumulate + qp.getQuantity()).y(getter.apply(qp)).build();
            sections.add(section);
            accumulate += qp.getQuantity();
        }
        return sections;
    }

    @Data
    @lombok.Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static class Qps {
        List<List<Qp>> da;
        List<List<Qp>> rt;
    }

    @SuppressWarnings("unchecked")
    private Qps supply(StageId stageId, Province province) {

        LambdaQueryWrapper<UnitDO> queryWrapper = new LambdaQueryWrapper<UnitDO>().eq(UnitDO::getCompId, stageId.getCompId()).eq(UnitDO::getRoundId, stageId.getRoundId());
        List<UnitDO> unitDOs = unitDOMapper.selectList(queryWrapper).stream().filter(unitDO -> unitDO.getMetaUnit().getProvince().equals(province)).collect(Collectors.toList());
        List<UnitDO> classicUnitDOs = unitDOs.stream().filter(unitDO -> GeneratorType.CLASSIC.equals(unitDO.getMetaUnit().getGeneratorType())).collect(Collectors.toList());
        List<UnitDO> renewableUnitDOs = unitDOs.stream().filter(unitDO -> GeneratorType.RENEWABLE.equals(unitDO.getMetaUnit().getGeneratorType())).collect(Collectors.toList());
        List<UnitDO> loadUnitDOs = unitDOs.stream().filter(unitDO -> UnitType.LOAD.equals(unitDO.getMetaUnit().getUnitType())).collect(Collectors.toList());

        // 2. 火电机组5段量价
        Map<Integer, Long> classicUnitIds = classicUnitDOs.stream().collect(Collectors.toMap(unitDO -> unitDO.getMetaUnit().getSourceId(), UnitDO::getUnitId));
        LambdaQueryWrapper<GeneratorDaSegmentBidDO> in0 = new LambdaQueryWrapper<GeneratorDaSegmentBidDO>()
                .eq(GeneratorDaSegmentBidDO::getRoundId, stageId.getRoundId() + 1)
                .in(GeneratorDaSegmentBidDO::getUnitId, classicUnitIds.keySet());

        List<GeneratorDaSegmentBidDO> generatorDaSegmentBidDOs = Collect.isEmpty(classicUnitIds) ? Collections.EMPTY_LIST : generatorDaSegmentMapper.selectList(in0);

        List<Qp> classicQps = generatorDaSegmentBidDOs.stream()
                .map(gDO -> new Qp(null, classicUnitIds.get(gDO.getUnitId()), gDO.getOfferMw(), gDO.getOfferPrice(), gDO.getOfferCost())).collect(Collectors.toList());

        // 3. 新能源机组的根据预测调整量价段，区分日前和实时
        Map<Integer, Long> renewableUnitIds = renewableUnitDOs.stream()
                .collect(Collectors.toMap(unitDO -> unitDO.getMetaUnit().getSourceId(), UnitDO::getUnitId));
        LambdaQueryWrapper<GeneratorDaSegmentBidDO> in1 = new LambdaQueryWrapper<GeneratorDaSegmentBidDO>()
                .eq(GeneratorDaSegmentBidDO::getRoundId, stageId.getRoundId() + 1)
                .in(GeneratorDaSegmentBidDO::getUnitId, renewableUnitIds.keySet());
        generatorDaSegmentBidDOs = Collect.isEmpty(renewableUnitIds) ? Collections.EMPTY_LIST : generatorDaSegmentMapper.selectList(in1);
        Map<Long, List<GeneratorDaSegmentBidDO>> groupSegments = generatorDaSegmentBidDOs.stream()
                .collect(Collectors.groupingBy(gDO -> renewableUnitIds.get(gDO.getUnitId())));

        // da qps;
        LambdaQueryWrapper<GeneratorDaForecastBidDO> in2 = new LambdaQueryWrapper<GeneratorDaForecastBidDO>()
                .eq(GeneratorDaForecastBidDO::getRoundId, stageId.getRoundId() + 1)
                .in(GeneratorDaForecastBidDO::getUnitId, renewableUnitIds.keySet());
        Collector<GeneratorDaForecastBidDO, List<GeneratorDaForecastBidDO>, List<Double>> collector0 = Collector.of(
                ArrayList::new, List::add, (ls0, ls1) -> { ls0.addAll(ls1); return ls0; },
                ls -> ls.stream().sorted(Comparator.comparing(GeneratorDaForecastBidDO::getPrd)).map(GeneratorDaForecastBidDO::getForecastMw).collect(Collectors.toList()));
        List<GeneratorDaForecastBidDO> generatorDaForecastBidDOs = Collect.isEmpty(renewableUnitIds) ? Collections.EMPTY_LIST : generatorDaForecastBidMapper.selectList(in2);
        Map<Long, List<Double>> daCutOffs = generatorDaForecastBidDOs.stream()
                .collect(Collectors.groupingBy(gBid -> renewableUnitIds.get(gBid.getUnitId()), HashMap::new, collector0));
        List<Qp> daRenewableQps = renewableUnitIds.values().stream()
                .map(unitId -> instantQps(groupSegments, daCutOffs, unitId)).flatMap(Collection::stream).collect(Collectors.toList());

        // rt qps
        LambdaQueryWrapper<GeneratorForecastValueDO> in3 = new LambdaQueryWrapper<GeneratorForecastValueDO>()
                .in(GeneratorForecastValueDO::getUnitId, renewableUnitIds.keySet()).in(GeneratorForecastValueDO::getRoundId, stageId.getRoundId() + 1);
        Collector<GeneratorForecastValueDO, List<GeneratorForecastValueDO>, List<Double>> collector1 = Collector.of(
                ArrayList::new, List::add, (ls0, ls1) -> { ls0.addAll(ls1); return ls0; },
                ls -> ls.stream().sorted(Comparator.comparing(GeneratorForecastValueDO::getPrd)).map(GeneratorForecastValueDO::getRtP).collect(Collectors.toList()));
        List<GeneratorForecastValueDO> generatorForecastValueDOs = Collect.isEmpty(renewableUnitIds) ? Collections.EMPTY_LIST : generatorForecastValueMapper.selectList(in3);
        Map<Long, List<Double>> rtCutOffs = generatorForecastValueDOs
                .stream().collect(Collectors.groupingBy(gValue -> renewableUnitIds.get(gValue.getUnitId()), HashMap::new, collector1));
        List<Qp> rtRenewableQps = renewableUnitIds.values().stream()
                .map(unitId -> instantQps(groupSegments, rtCutOffs, unitId)).flatMap(Collection::stream).collect(Collectors.toList());



        List<Qp> tielineQps = new ArrayList<>();
        if (province == Province.RECEIVER) {
            MarketSettingDO marketSettingDO = marketSettingMapper.selectById(1);
            LambdaQueryWrapper<TieLinePowerDO> eq = new LambdaQueryWrapper<TieLinePowerDO>().eq(TieLinePowerDO::getRoundId, stageId.getRoundId() + 1);
            tielineQps = tieLinePowerDOMapper.selectList(eq).stream().map(t -> {
                double tielinePower = t.getAnnualTielinePower() + t.getMonthlyTielinePower() + t.getDaTielinePower();
                return new Qp(t.getPrd(), null, tielinePower, marketSettingDO.getOfferPriceFloor(), marketSettingDO.getOfferPriceFloor());
            }).collect(Collectors.toList());
        }


        ListMultimap<Integer, Qp> groupDaQps = Stream.of(daRenewableQps, tielineQps)
                .flatMap(Collection::stream).collect(Collect.listMultiMap(Qp::getInstant));
        List<List<Qp>> daInstantQps = IntStream.range(0, 24).mapToObj(i -> new ArrayList<>(groupDaQps.get(i))).collect(Collectors.toList());
        daInstantQps.forEach(qps -> qps.addAll(classicQps));

        ListMultimap<Integer, Qp> groupRtQps = Stream.of(rtRenewableQps, tielineQps).flatMap(Collection::stream).collect(Collect.listMultiMap(Qp::getInstant));
        List<List<Qp>> rtInstantQps = IntStream.range(0, 24).mapToObj(i -> new ArrayList<>(groupRtQps.get(i))).collect(Collectors.toList());
        rtInstantQps.forEach(qps -> qps.addAll(classicQps));
        return Qps.builder().rt(rtInstantQps).da(daInstantQps).build();
    }

    private Qps demand(StageId stageId, Province province) {

        LambdaQueryWrapper<UnitDO> queryWrapper = new LambdaQueryWrapper<UnitDO>().eq(UnitDO::getCompId, stageId.getCompId()).eq(UnitDO::getRoundId, stageId.getRoundId());
        Map<Integer, Long> loadIds = unitDOMapper.selectList(queryWrapper).stream()
                .filter(unitDO -> unitDO.getMetaUnit().getUnitType().equals(UnitType.LOAD))
                .filter(unitDO -> unitDO.getMetaUnit().getProvince().equals(province))
                .collect(Collectors.toMap(unitDO -> unitDO.getMetaUnit().getSourceId(), UnitDO::getUnitId));

        //  日前
        LambdaQueryWrapper<LoadDaForecastBidDO> in = new LambdaQueryWrapper<LoadDaForecastBidDO>()
                .eq(LoadDaForecastBidDO::getRoundId, stageId.getRoundId() + 1)
                .in(LoadDaForecastBidDO::getLoadId, loadIds.keySet());
        List<LoadDaForecastBidDO> loadDaForecastBidDOS = Collect.isEmpty(loadIds.keySet()) ? Collections.EMPTY_LIST : loadDaForecastBidMapper.selectList(in);
        List<Qp> daInstantQps = loadDaForecastBidDOS.stream()
                .collect(Collectors.groupingBy(LoadDaForecastBidDO::getPrd))
                .entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> e.getValue().stream().map(b -> new Qp(e.getKey(), loadIds.get(b.getLoadId()), b.getBidMw(), b.getBidPrice(), null)).collect(Collectors.toList()))
                .flatMap(Collection::stream).collect(Collectors.toList());

        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(1);
        LambdaQueryWrapper<SprDO> eq0 = new LambdaQueryWrapper<SprDO>().eq(SprDO::getProv, province.getDbCode()).eq(SprDO::getRoundId, stageId.getRoundId() + 1);
        List<Qp> rtInstantQps = sprDOMapper.selectList(eq0).stream()
                .sorted(Comparator.comparing(SprDO::getPrd))
                .map(sprDO -> new Qp(sprDO.getPrd(), null, sprDO.getRtLoad(), marketSettingDO.getClearedPriceCap(), null)).collect(Collectors.toList());

        List<Qp> tielineQps = new ArrayList<>();
        if (province == Province.TRANSFER) {
            LambdaQueryWrapper<TieLinePowerDO> eq1 = new LambdaQueryWrapper<TieLinePowerDO>().eq(TieLinePowerDO::getRoundId, stageId.getRoundId() + 1);
            tielineQps = tieLinePowerDOMapper.selectList(eq1).stream().sorted(Comparator.comparing(TieLinePowerDO::getPrd)).map(t -> {
                double tielinePower = t.getAnnualTielinePower() + t.getMonthlyTielinePower() + t.getDaTielinePower();
                return new Qp(t.getPrd(), null, tielinePower, marketSettingDO.getClearedPriceCap(),null);
            }).collect(Collectors.toList());
        }

        List<List<Qp>> daQps = Stream.of(daInstantQps, tielineQps)
                .flatMap(Collection::stream).collect(Collectors.groupingBy(Qp::getInstant)).entrySet()
                .stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).collect(Collectors.toList());

        List<List<Qp>> rtQps = Stream.of(rtInstantQps, tielineQps)
                .flatMap(Collection::stream).collect(Collectors.groupingBy(Qp::getInstant)).entrySet()
                .stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).collect(Collectors.toList());

        return Qps.builder().rt(rtQps).da(daQps).build();
    }

    private static List<Qp> instantQps(Map<Long, List<GeneratorDaSegmentBidDO>> groupSegments, Map<Long, List<Double>> cutoffGroups, Long unitId) {
        List<GeneratorDaSegmentBidDO> segmentBidDOs = groupSegments.get(unitId).stream()
                .sorted(Comparator.comparing(GeneratorDaSegmentBidDO::getOfferId)).collect(Collectors.toList());
        List<Qp> qps = new ArrayList<>();
        List<Double> cutoffs = cutoffGroups.get(unitId);
        for (int i = 0; i < cutoffs.size(); i++) {
            Double cutoff = cutoffs.get(i);
            if (cutoff.equals(0D)) {
                continue;
            }
            double accumulate = 0D;
            for (GeneratorDaSegmentBidDO segmentBidDO : segmentBidDOs) {
                accumulate += segmentBidDO.getOfferMw();
                if (accumulate >= cutoff) {
                    double lastQuantity = cutoff - (accumulate - segmentBidDO.getOfferMw());
                    qps.add(new Qp(i, unitId, lastQuantity, segmentBidDO.getOfferPrice(), segmentBidDO.getOfferCost()));
                    break;
                } else {
                    qps.add(new Qp(i, unitId, segmentBidDO.getOfferMw(), segmentBidDO.getOfferPrice(), segmentBidDO.getOfferCost()));
                }
            }
        }
        return qps;
    }


    @Data
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static private class Qp{
        Integer instant;
        Long unitId;
        Double quantity;
        Double price;
        Double cost;
    }




    /**
     *  省内现货市场：中标电源结构
     * @param stageId 界面阶段id
     * @param province 查看省份
     *
     */

    @SneakyThrows
    @GetMapping ("listSpotBiddenEntityVOs")
    public Result<SpotBiddenVO> listSpotBiddenEntityVOs(@NotBlank String stageId, @NotBlank String province) {

        Comp comp = tunnel.runningComp();
        BizEx.nullThrow(comp, ErrorEnums.COMP_NOT_EXISTED);

        Long compId = StageId.parse(stageId).getCompId();
        BizEx.falseThrow(Objects.equals(compId, comp.getCompId()), PARAM_FORMAT_WRONG.message("该场次已经结束，请重新进入"));

        SpotBiddenVO spotBiddenVO = (SpotBiddenVO) cache.get("listSpotBiddenEntityVOs" + province, () -> doListSpotBiddenEntityVOs(stageId, province));
        return Result.success(spotBiddenVO);
    }

    public SpotBiddenVO doListSpotBiddenEntityVOs(String stageId, String province)  {
        StageId parsedStageId = StageId.parse(stageId);
        Long compId = parsedStageId.getCompId();
        Integer roundId = parsedStageId.getRoundId();
        Province parsedProvince = Kit.enumOf(Province::name, province).orElse(null);
        LambdaQueryWrapper<UnitDO> queryWrapper = new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getCompId, compId).eq(UnitDO::getRoundId, roundId);
        List<UnitDO> unitDOs = unitDOMapper.selectList(queryWrapper).stream()
                .filter(unitDO -> unitDO.getMetaUnit().getProvince().equals(parsedProvince))
                .filter(unitDO -> unitDO.getMetaUnit().getUnitType().equals(UnitType.GENERATOR))
                .collect(Collectors.toList());

        SpotBiddenEntityVO daSpotBiddenEntityVO = buildSpotBiddenEntity(parsedStageId, roundId, parsedProvince, unitDOs, true);
        SpotBiddenEntityVO rtSpotBiddenEntityVO = buildSpotBiddenEntity(parsedStageId, roundId, parsedProvince, unitDOs, false);
        return SpotBiddenVO.builder().daSpotBiddenEntityVO(daSpotBiddenEntityVO).rtSpotBiddenEntityVO(rtSpotBiddenEntityVO).build();
    }

    final SpotLoadClearedMapper spotLoadClearedMapper;

    private SpotBiddenEntityVO buildSpotBiddenEntity(StageId parsedStageId, Integer roundId, Province parsedProvince, List<UnitDO> unitDOs, boolean da) {
        Map<Integer, Long> unitIds = unitDOs.stream().collect(Collectors.toMap(unitDO -> unitDO.getMetaUnit().getSourceId(), UnitDO::getUnitId));

        Map<Integer, Long> classicUnitIds = unitDOs.stream()
                .filter(u -> GeneratorType.CLASSIC.equals(u.getMetaUnit().getGeneratorType()))
                .collect(Collectors.toMap(unitDO -> unitDO.getMetaUnit().getSourceId(), UnitDO::getUnitId));

        Map<Integer, Long> renewableUnitIds = unitDOs.stream()
                .filter(u -> GeneratorType.RENEWABLE.equals(u.getMetaUnit().getGeneratorType()))
                .collect(Collectors.toMap(unitDO -> unitDO.getMetaUnit().getSourceId(), UnitDO::getUnitId));


        SpotBiddenEntityVO.SpotBiddenEntityVOBuilder builder = SpotBiddenEntityVO.builder();

        List<Double> intraLoads;
        // 日前
        if (da) {
            LambdaQueryWrapper<SpotLoadCleared> wrapper0 = new LambdaQueryWrapper<SpotLoadCleared>()
                    .eq(SpotLoadCleared::getRoundId, roundId + 1)
                    .in(SpotLoadCleared::getLoadId, unitIds.keySet());
            List<SpotLoadCleared> loadDaForecastBidDOs = Collect.isEmpty(unitIds.keySet()) ? Collections.EMPTY_LIST : spotLoadClearedMapper.selectList(wrapper0);
                intraLoads = loadDaForecastBidDOs.stream().collect(Collectors.groupingBy(SpotLoadCleared::getPrd))
                    .entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue)
                    .map(bs -> bs.stream().collect(Collectors.summarizingDouble(SpotLoadCleared::getDaClearedMw)).getSum())
                    .collect(Collectors.toList());
        } else {
            LambdaQueryWrapper<SprDO> eq = new LambdaQueryWrapper<SprDO>().eq(SprDO::getProv, parsedProvince.getDbCode()).eq(SprDO::getRoundId, roundId + 1);
            intraLoads = sprDOMapper.selectList(eq).stream().sorted(Comparator.comparing(SprDO::getPrd)).map(SprDO::getRtLoad).collect(Collectors.toList());
        }


        LambdaQueryWrapper<TieLinePowerDO> eq = new LambdaQueryWrapper<TieLinePowerDO>().eq(TieLinePowerDO::getRoundId, roundId + 1);
        List<Double> tielinePowers = tieLinePowerDOMapper.selectList(eq).stream().collect(Collectors.groupingBy(TieLinePowerDO::getPrd))
                .entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue)
                .map(ds -> ds.stream().collect(Collectors.summarizingDouble(d -> d.getAnnualTielinePower() + d.getMonthlyTielinePower() + d.getDaTielinePower())).getSum())
                .collect(Collectors.toList());


        if (Province.TRANSFER.equals(parsedProvince)) {
            List<Double> intraLoadPlusInterOut = IntStream.range(0, 24)
                    .mapToObj(i -> intraLoads.get(i) + tielinePowers.get(i)).collect(Collectors.toList());
            builder.intraLoadPlusInterOut(intraLoadPlusInterOut);
        } else if (Province.RECEIVER.equals(parsedProvince)){
            List<Double> intraLoadMinusInterIn = IntStream.range(0, 24)
                    .mapToObj(i -> intraLoads.get(i) - tielinePowers.get(i)).collect(Collectors.toList());
            builder.intraLoadMinusInterIn(intraLoadMinusInterIn);
        } else {
            throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
        }

        LambdaQueryWrapper<GeneratorBasic> in0 = new LambdaQueryWrapper<GeneratorBasic>()
                .in(GeneratorBasic::getUnitId, unitIds.keySet());

        List<GeneratorBasic> generatorBasics = Collect.isEmpty(unitIds) ? Collections.EMPTY_LIST : unitBasicMapper.selectList(in0);

        double classicTotal = generatorBasics.stream().filter(generatorBasic -> classicUnitIds.containsKey(generatorBasic.getUnitId()))
                .collect(Collectors.summarizingDouble(GeneratorBasic::getMaxP)).getSum();

        List<Double> renewableTotals = IntStream.range(0, 24).mapToObj(i -> 0D).collect(Collectors.toList());
        if (!Collect.isEmpty(renewableUnitIds)) {
            if (da) {
                LambdaQueryWrapper<GeneratorDaForecastBidDO> in = new LambdaQueryWrapper<GeneratorDaForecastBidDO>().eq(GeneratorDaForecastBidDO::getRoundId, roundId + 1)
                        .in(GeneratorDaForecastBidDO::getUnitId, unitIds.keySet());
                renewableTotals = Collect.isEmpty(unitIds.keySet()) ? Collections.EMPTY_LIST : generatorDaForecastBidMapper.selectList(in).stream()
                        .collect(Collectors.groupingBy(GeneratorDaForecastBidDO::getPrd))
                        .entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue)
                        .map(ls -> ls.stream().collect(Collectors.summarizingDouble(GeneratorDaForecastBidDO::getForecastMw)).getSum()).collect(Collectors.toList());
            } else {
                LambdaQueryWrapper<SprDO> eqx = new LambdaQueryWrapper<SprDO>().eq(SprDO::getProv, parsedProvince.getDbCode()).eq(SprDO::getRoundId, parsedStageId.getRoundId() + 1);
                renewableTotals = sprDOMapper.selectList(eqx).stream().sorted(Comparator.comparing(SprDO::getPrd)).map(SprDO::getRtRenewable).collect(Collectors.toList());
            }
        }
        LambdaQueryWrapper<SpotUnitCleared> in1 = new LambdaQueryWrapper<SpotUnitCleared>()
                .eq(SpotUnitCleared::getRoundId, parsedStageId.getRoundId() + 1)
                .in(SpotUnitCleared::getUnitId, unitIds.keySet());
        List<SpotUnitCleared> spotUnitCleareds = Collect.isEmpty(unitIds.keySet()) ? Collections.EMPTY_LIST : spotUnitClearedMapper.selectList(in1);
        List<Double> classicBidden = IntStream.range(0, 24).mapToObj(i -> 0D).collect(Collectors.toList());
        if (Collect.isNotEmpty(classicUnitIds)) {
            classicBidden = spotUnitCleareds.stream().filter(c -> classicUnitIds.containsKey(c.getUnitId()))
                    .collect(Collectors.groupingBy(SpotUnitCleared::getPrd)).entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue)
                    .map(cs -> cs.stream().collect(Collectors.summarizingDouble(
                            c -> da ? c.getDaClearedMw() : c.getRtClearedMw()
                    )).getSum())
                    .collect(Collectors.toList());
        }

        builder.classicBidden(classicBidden);
        builder.classicNotBidden(classicBidden.stream().map(c -> classicTotal - c).collect(Collectors.toList()));
        List<Double> renewableBidden = IntStream.range(0, 24).mapToObj(i -> 0D).collect(Collectors.toList());

        if (Collect.isNotEmpty(renewableUnitIds)) {
            renewableBidden = spotUnitCleareds.stream().filter(c -> renewableUnitIds.containsKey(c.getUnitId()))
                    .collect(Collectors.groupingBy(SpotUnitCleared::getPrd)).entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue)
                    .map(cs -> cs.stream().collect(Collectors.summarizingDouble(
                            c -> da ? c.getDaClearedMw() : c.getRtClearedMw()
                    )).getSum())
                    .collect(Collectors.toList());
            builder.renewableBidden(renewableBidden);
        }

        List<Double> finalRenewableTotals = renewableTotals;
        List<Double> finalRenewableBidden = renewableBidden;
        builder.renewableNotBidden(IntStream.range(0, 24).mapToObj(i -> finalRenewableTotals.get(i) - finalRenewableBidden.get(i)).collect(Collectors.toList()));


        // 分报价区间
        LambdaQueryWrapper<GeneratorDaSegmentBidDO> in = new LambdaQueryWrapper<GeneratorDaSegmentBidDO>()
                .eq(GeneratorDaSegmentBidDO::getRoundId, parsedStageId.getRoundId() + 1)
                .in(GeneratorDaSegmentBidDO::getUnitId, unitIds.keySet());
        List<GeneratorDaSegmentBidDO> generatorDaSegmentBidDOs = Collect.isEmpty(unitIds) ? Collections.EMPTY_LIST : generatorDaSegmentMapper.selectList(in);

        List<List<SpotUnitCleared>> indexedSpotUnitCleared = spotUnitCleareds.stream().collect(Collectors.groupingBy(SpotUnitCleared::getPrd))
                .entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).collect(Collectors.toList());

        List<List<Double>> priceStatistics = IntStream.range(0, 24)
                .mapToObj(i -> process(generatorDaSegmentBidDOs, indexedSpotUnitCleared.get(i), unitDOs, da)).collect(Collectors.toList());
        builder.priceStatistics(priceStatistics);

        return builder.build();
    }

    private List<Double> process(List<GeneratorDaSegmentBidDO> segments, List<SpotUnitCleared> clears, List<UnitDO> unitDOs, boolean da) {
        ListMultimap<Integer, GeneratorDaSegmentBidDO> groupedByUnitIds = segments.stream().collect(Collect.listMultiMap(GeneratorDaSegmentBidDO::getUnitId));
        Map<Integer, SpotUnitCleared> unitClearedMap = Collect.toMapMightEx(clears, SpotUnitCleared::getUnitId);
        List<Pair<Double, Double>> collectQps = unitDOs.stream().map(unitDO -> {
            Integer sourceId = unitDO.getMetaUnit().getSourceId();
            List<GeneratorDaSegmentBidDO> generatorDaSegmentBidDOs = groupedByUnitIds.get(sourceId).stream()
                    .sorted(Comparator.comparing(GeneratorDaSegmentBidDO::getOfferId)).collect(Collectors.toList());
            SpotUnitCleared spotUnitCleared = unitClearedMap.get(sourceId);
            List<Pair<Double, Double>> qps = new ArrayList<>();
            generatorDaSegmentBidDOs.forEach(gDO -> qps.add(Pair.of(gDO.getOfferMw(), gDO.getOfferPrice())));
            List<Pair<Double, Double>> clearedQps = new ArrayList<>();
            if (da && spotUnitCleared.getDaClearedMw().equals(0D)) {
                return clearedQps;
            } else if ((!da) && spotUnitCleared.getRtClearedMw().equals(0D)) {
                return clearedQps;
            }

            double accumulate = 0D;
            double clear = da ? spotUnitCleared.getDaClearedMw() : spotUnitCleared.getRtClearedMw();
            for (Pair<Double, Double> qp : qps) {
                accumulate += qp.getLeft();
                if (accumulate >= clear) {
                    double v = clear - (accumulate - (qp.getLeft()));
                    clearedQps.add(Pair.of(v, qp.getRight()));
                    break;
                } else {
                    clearedQps.add(qp);
                }
            }
            return clearedQps;
        }).flatMap(Collection::stream).collect(Collectors.toList());

        return collectQps.stream().collect(Collect.select(
                qp -> qp.getRight() <= 200D,
                qp -> qp.getRight() > 200D && qp.getRight() <= 400,
                qp -> qp.getRight() > 400D && qp.getRight() <= 600,
                qp -> qp.getRight() > 600D && qp.getRight() <= 800,
                qp -> qp.getRight() > 800D
        )).stream().map(ls -> ls.stream().collect(Collectors.summarizingDouble(Pair::getLeft)).getSum()).collect(Collectors.toList());

    }

    final InterSpotTransactionDOMapper interSpotTransactionDOMapper;

    /**
     * 省间现货市场，全天市场成交情况
     * @param stageId 阶段id
     */

    @SneakyThrows
    @GetMapping("getSpotInterClearanceVO")
    public Result<SpotInterClearanceVO> getSpotInterClearanceVO(String stageId, @RequestHeader String token) {
        Comp comp = tunnel.runningComp();
        BizEx.nullThrow(comp, ErrorEnums.COMP_NOT_EXISTED);

        Long compId = StageId.parse(stageId).getCompId();
        BizEx.falseThrow(Objects.equals(compId, comp.getCompId()), PARAM_FORMAT_WRONG.message("该场次已经结束，请重新进入"));

        SpotInterClearanceVO spotInterClearanceVO = (SpotInterClearanceVO) cache.get(
                "getSpotInterClearanceVO" + stageId + TokenUtils.getUserId(token), () -> doGetSpotInterClearanceVO(stageId, token));
        return Result.success(spotInterClearanceVO);
    }

    public SpotInterClearanceVO doGetSpotInterClearanceVO(String stageId, @RequestHeader String token) {
        Integer roundId = StageId.parse(stageId).getRoundId();
        Long compId = StageId.parse(stageId).getCompId();
        String userId = TokenUtils.getUserId(token);
        List<Unit> units = tunnel.listUnits(compId, roundId, userId).stream()
                .filter(u -> u.getMetaUnit().getUnitType().equals(UnitType.GENERATOR))
                .filter(u -> u.getMetaUnit().getProvince().equals(Province.TRANSFER))
                .collect(Collectors.toList());
        LambdaQueryWrapper<InterSpotTransactionDO> eq3 = new LambdaQueryWrapper<InterSpotTransactionDO>()
                .eq(InterSpotTransactionDO::getRoundId, roundId + 1);
        Map<Integer, List<InterSpotTransactionDO>> spotTransactionDOs = interSpotTransactionDOMapper
                .selectList(eq3).stream().collect(Collectors.groupingBy(InterSpotTransactionDO::getPrd));
        List<Double> dealPrices = IntStream.range(0, 24).mapToObj(i -> {
            List<InterSpotTransactionDO> interSpotTransactionDOS = Kit.whenNull(spotTransactionDOs.get(i), (List<InterSpotTransactionDO>) Collections.EMPTY_LIST);
            return interSpotTransactionDOS.stream().filter(t -> !t.getClearedPrice().equals(0D))
                    .findFirst().map(InterSpotTransactionDO::getClearedPrice).orElse(null);
        }).collect(Collectors.toList());
        LambdaQueryWrapper<TieLinePowerDO> eq = new LambdaQueryWrapper<TieLinePowerDO>().eq(TieLinePowerDO::getRoundId, roundId + 1);
        List<Double> dealTotals = tieLinePowerDOMapper.selectList(eq).stream().sorted(Comparator.comparing(TieLinePowerDO::getPrd))
                .map(TieLinePowerDO::getDaMarketTielinePower).collect(Collectors.toList());
        Map<String, List<Double>> generatorDeals = new HashMap<>();
        units.forEach(u -> {
            String unitName = u.getMetaUnit().getName();
            LambdaQueryWrapper<InterSpotTransactionDO> eqs = new LambdaQueryWrapper<InterSpotTransactionDO>()
                    .eq(InterSpotTransactionDO::getRoundId, roundId + 1)
                    .eq(InterSpotTransactionDO::getSellerId, u.getMetaUnit().getSourceId());
            Map<Integer, Double> collect = interSpotTransactionDOMapper.selectList(eqs).stream()
                    .collect(Collectors.toMap(InterSpotTransactionDO::getPrd, InterSpotTransactionDO::getClearedMw));
            List<Double> deals = IntStream.range(0, 24).mapToObj(collect::get).collect(Collectors.toList());
            generatorDeals.put(unitName, deals);
        });
        SpotInterClearanceVO spotInterClearanceVO = SpotInterClearanceVO.builder()
                .dealTotals(dealTotals).dealPrices(dealPrices).generatorDeals(generatorDeals).build();
        return spotInterClearanceVO;
    }

    final InterSpotUnitOfferDOMapper interSpotUnitOfferDOMapper;
    final UnmetDemandMapper unmetDemandMapper;


    /**
     * 省间现货分时供需曲线:可选择的时刻
     * @param stageId : 阶段id
     */

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @GetMapping("interSpotMarketVOAvailableInstants")
    public Result<List<Integer>> interSpotMarketVOAvailableInstants(String stageId) {

        Comp comp = tunnel.runningComp();
        BizEx.nullThrow(comp, ErrorEnums.COMP_NOT_EXISTED);

        Long compId = StageId.parse(stageId).getCompId();
        BizEx.falseThrow(Objects.equals(compId, comp.getCompId()), PARAM_FORMAT_WRONG.message("该场次已经结束，请重新进入"));

        List<Integer> ls = (List<Integer>) cache.get("interSpotMarketVOAvailableInstants" + stageId, () -> doInterSpotMarketVOAvailableInstants(stageId));
        return Result.success(ls);
    }

    public List<Integer> doInterSpotMarketVOAvailableInstants(String stageId) {
        Integer roundId = StageId.parse(stageId).getRoundId();
        Long compId = StageId.parse(stageId).getCompId();
        LambdaQueryWrapper<TieLinePowerDO> eqx
                = new LambdaQueryWrapper<TieLinePowerDO>().eq(TieLinePowerDO::getRoundId, roundId + 1);
        List<TieLinePowerDO> tieLinePowerDOs = tieLinePowerDOMapper.selectList(eqx)
                .stream().sorted(Comparator.comparing(TieLinePowerDO::getPrd)).collect(Collectors.toList());

        LambdaQueryWrapper<UnmetDemand> eq1 = new LambdaQueryWrapper<UnmetDemand>().eq(UnmetDemand::getRoundId, roundId + 1);
        List<UnmetDemand> collect = unmetDemandMapper.selectList(eq1).stream().sorted(Comparator.comparing(UnmetDemand::getPrd)).collect(Collectors.toList());
        List<Integer> instants = IntStream.range(0, 24).mapToObj(i -> {
            TieLinePowerDO tieLinePowerDO = tieLinePowerDOs.get(i);
            double already = tieLinePowerDO.getAnnualTielinePower() + tieLinePowerDO.getMonthlyTielinePower();
            UnmetDemand unmetDemand = collect.get(i);
            return unmetDemand.getDaReceivingMw() - already > 0 ? i : null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        return instants;
    }

    /**
     * 省间现货分时供需曲线
     * @param stageId 阶段id
     * @param instant 时刻
     */
    @SneakyThrows
    @GetMapping("getInterSpotMarketVO")
    public Result<InterSpotMarketVO> getInterSpotMarketVO(String stageId, Integer instant) {
        InterSpotMarketVO interSpotMarketVO = (InterSpotMarketVO) cache.get("getInterSpotMarketVO" + stageId + instant, () -> doGetInterSpotMarketVO(stageId, instant));
        return Result.success(interSpotMarketVO);
    }
    public InterSpotMarketVO doGetInterSpotMarketVO(String stageId, Integer instant) {
        Integer roundId = StageId.parse(stageId).getRoundId();
        Long compId = StageId.parse(stageId).getCompId();

        LambdaQueryWrapper<InterSpotUnitOfferDO> eq = new LambdaQueryWrapper<InterSpotUnitOfferDO>()
                .eq(InterSpotUnitOfferDO::getPrd, instant)
                .eq(InterSpotUnitOfferDO::getRoundId, roundId + 1);
        List<InterSpotUnitOfferDO> interSpotUnitOfferDOS = interSpotUnitOfferDOMapper.selectList(eq);
        List<Pair<Double, Double>> pairs = interSpotUnitOfferDOS.stream().flatMap(i -> Stream.of(
                Pair.of(i.getSpotOfferMw1(), i.getSpotOfferPrice1()),
                Pair.of(i.getSpotOfferMw2(), i.getSpotOfferPrice2()),
                Pair.of(i.getSpotOfferMw3(), i.getSpotOfferPrice3())
        )).filter(p -> !p.getLeft().equals(0D)).sorted(Comparator.comparing(Pair::getRight)).collect(Collectors.toList());
        double sellDeclaredTotal = pairs.stream().collect(Collectors.summarizingDouble(Pair::getLeft)).getSum();

        LambdaQueryWrapper<TieLinePowerDO> eqx
                = new LambdaQueryWrapper<TieLinePowerDO>().eq(TieLinePowerDO::getRoundId, roundId + 1).eq(TieLinePowerDO::getPrd, instant);
        TieLinePowerDO tieLinePowerDO = tieLinePowerDOMapper.selectOne(eqx);
        double already = tieLinePowerDO.getAnnualTielinePower() + tieLinePowerDO.getMonthlyTielinePower();
        LambdaQueryWrapper<UnmetDemand> eq1 = new LambdaQueryWrapper<UnmetDemand>().eq(UnmetDemand::getPrd, instant).eq(UnmetDemand::getRoundId, roundId + 1);
        Double receiverDeclaredTotal = unmetDemandMapper.selectOne(eq1).getDaReceivingMw() - already;

        LambdaQueryWrapper<InterSpotTransactionDO> last = new LambdaQueryWrapper<InterSpotTransactionDO>()
                .eq(InterSpotTransactionDO::getRoundId, roundId + 1)
                .eq(InterSpotTransactionDO::getPrd, instant);

        List<InterSpotTransactionDO> transactionDOs = interSpotTransactionDOMapper
                .selectList(last).stream().filter(i -> i.getClearedMw() > 0).collect(Collectors.toList());
        double dealTotal = transactionDOs.stream().collect(Collectors.summarizingDouble(InterSpotTransactionDO::getClearedMw)).getSum();


        Double dealAveragePrice = Collect.isEmpty(transactionDOs) ? null : transactionDOs.get(0).getClearedPrice();


        Double lx = 0D;
        List<SpotSection> supplySections = new ArrayList<>();
        for (Pair<Double, Double> pair : pairs) {
            SpotSection spotSection = new SpotSection(lx, lx + pair.getLeft(), pair.getRight());
            supplySections.add(spotSection);
            lx += pair.getLeft();
        }

        GridLimit generatorLimit = tunnel.priceLimit(UnitType.GENERATOR);
        GridLimit loadLimit = tunnel.priceLimit(UnitType.LOAD);
        Point<Double> supplyTerminus = Collect.isEmpty(supplySections) ? null : new Point<>(supplySections.get(supplySections.size() - 1).getRx(), generatorLimit.getHigh());

        List<SpotSection> requireSections = Collect.asList(new SpotSection(0D, receiverDeclaredTotal, loadLimit.getHigh()));
        Point<Double> requireTerminus = new Point<>(receiverDeclaredTotal, loadLimit.getLow());

        Point<Double> left = null;
        Point<Double> right = null;
        if (dealAveragePrice != null) {
            left = Point.<Double>builder().x(0D).y(dealAveragePrice).build();
            right = Point.<Double>builder().x(dealTotal).y(dealAveragePrice).build();
        }


        InterSpotMarketVO interSpotMarketVO = InterSpotMarketVO.builder()
                .sellDeclaredTotal(sellDeclaredTotal)
                .receiverDeclaredTotal(receiverDeclaredTotal)
                .dealTotal(dealTotal)
                .clearLineLeft(left)
                .clearLineRight(right)
                .dealAveragePrice(dealAveragePrice)
                .requireQuantity(receiverDeclaredTotal)
                .clearPrice(dealAveragePrice)
                .supplySections(supplySections)
                .supplyTerminus(supplyTerminus)
                .requireSections(requireSections)
                .requireTerminus(requireTerminus)
                .build();
         return interSpotMarketVO;
    }


    /**
     * 省间现货分设备成交量价
     * @param stageId 阶段id
     */

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @GetMapping("listInterSpotDeals")
    public Result<List<InterSpotUnitDealVO>> listInterSpotDeals(String stageId, Integer instant, @RequestHeader String token) {
        List<InterSpotUnitDealVO> data = (List<InterSpotUnitDealVO>) cache.
                get("doListInterSpotDeals" + stageId + instant + TokenUtils.getUserId(token), () -> doListInterSpotDeals(stageId, instant, token));
        return Result.success(data);
    }
    public List<InterSpotUnitDealVO> doListInterSpotDeals(String stageId, Integer instant, String token) {
        Integer roundId = StageId.parse(stageId).getRoundId();
        Long compId = StageId.parse(stageId).getCompId();
        String userId = TokenUtils.getUserId(token);
        boolean equals = tunnel.review();
        List<Unit> units = tunnel.listUnits(compId, roundId, equals ? null : userId).stream()
                .filter(u -> u.getMetaUnit().getUnitType().equals(UnitType.GENERATOR))
                .filter(u -> u.getMetaUnit().getProvince().equals(Province.TRANSFER))
                .collect(Collectors.toList());
        List<InterSpotUnitDealVO> interSpotUnitDealVOs = units.stream().map(unit -> {
            String name = unit.getMetaUnit().getName();
            Integer sourceId = unit.getMetaUnit().getSourceId();
            LambdaQueryWrapper<InterSpotTransactionDO> eq3 = new LambdaQueryWrapper<InterSpotTransactionDO>()
                    .eq(InterSpotTransactionDO::getRoundId, roundId + 1)
                    .eq(InterSpotTransactionDO::getSellerId, sourceId)
                    .eq(InterSpotTransactionDO::getPrd, instant);
            List<InterSpotUnitDealVO.Deal> deals = interSpotTransactionDOMapper.selectList(eq3).stream()
                    .map(i -> new InterSpotUnitDealVO.Deal(i.getClearedMw(), i.getClearedPrice())).collect(Collectors.toList());
            double sumMoney = deals.stream().collect(Collectors.summarizingDouble(d -> d.getPrice() * d.getQuantity())).getSum();
            double quantity = deals.stream().collect(Collectors.summarizingDouble(InterSpotUnitDealVO.Deal::getQuantity)).getSum();
            return new InterSpotUnitDealVO(name, deals, quantity == 0D ? null : sumMoney/quantity);
        }).collect(Collectors.toList());
        return interSpotUnitDealVOs;
    }

    final GameResultMapper gameResultMapper;

    /**
     * 本轮成绩排名
     * @param stageId 阶段id
     */
    @GetMapping("getRoundRankVO")
    Result<RoundRankVO> getRoundRankVO(String stageId, @RequestHeader String token) {
        Integer roundId = StageId.parse(stageId).getRoundId();
        String userId = TokenUtils.getUserId(token);
        LambdaQueryWrapper<GameResult> eq = new LambdaQueryWrapper<GameResult>().eq(GameResult::getTraderId, userId)
                .eq(GameResult::getRoundId, roundId + 1);
        GameResult gameResult = gameResultMapper.selectOne(eq);

        Comp comp = tunnel.runningComp();
        boolean equals = comp.getCompStage().equals(CompStage.RANKING);

        int size = tunnel.runningComp().getUserIds().size();
        long number = Math.round(Math.max(10D, size * 0.1));
        LambdaQueryWrapper<GameResult> last = new LambdaQueryWrapper<GameResult>().eq(GameResult::getRoundId, roundId + 1)
                .last(equals, String.format("limit %s", number));
        List<GameResult> gameResults = gameResultMapper.selectList(last).stream().sorted(Comparator.comparing(GameResult::getRanking)).collect(Collectors.toList());
        RoundRankVO roundRankVO = RoundRankVO.builder()
                .headLine(equals ? "提示：成绩排名表第一栏显示自己的排名，自第二栏起显示成绩排名前10%的交易员" : "提示：成绩排名表第一栏显示自己的排名，自第二栏起显示所有的交易员的成绩")
                .myRanking(Convertor.INST.toRound(gameResult))
                .rankings(Collect.transfer(gameResults, Convertor.INST::toRound))
                .build();

        return Result.success(roundRankVO);
    }


    final GameRankingMapper gameRankingMapper;
    /**
     * 总成绩排名
     * @param stageId 阶段id
     */
    @GetMapping("getFinalRankVO")
    public Result<FinalRankVO> getFinalRankVO(String stageId, @RequestHeader String token) {

        String userId = TokenUtils.getUserId(token);
        LambdaQueryWrapper<GameRanking> eq0 = new LambdaQueryWrapper<GameRanking>().eq(GameRanking::getTraderId, userId);
        GameRanking gameRanking = gameRankingMapper.selectOne(eq0);
        Integer roundTotal = tunnel.runningComp().getRoundTotal();

        List<FinalRankVO.Ranking> roundRankings = IntStream.range(0, roundTotal).mapToObj(i -> {
            LambdaQueryWrapper<GameResult> eq1 = new LambdaQueryWrapper<GameResult>().eq(GameResult::getTraderId, userId)
                    .eq(GameResult::getRoundId, i + 1);
            GameResult gameResult = gameResultMapper.selectOne(eq1);
            return Convertor.INST.toFinal(gameResult);
        }).collect(Collectors.toList());

        Set<String> userIds = new HashSet<>(tunnel.runningComp().getUserIds());

        List<GameRanking> gameRankings = gameRankingMapper.selectList(null).stream()
                .filter(g -> userIds.contains(g.getTraderId()))
                .sorted(Comparator.comparing(GameRanking::getTotalRanking))
                .collect(Collectors.toList());
        List<FinalRankVO.Ranking> finalRankVORankings = Collect.transfer(gameRankings, Convertor.INST::to).stream()
                .collect(Collectors.groupingBy(FinalRankVO.Ranking::getGroupId))
                .values().stream().map(rankings -> rankings.subList(0, (rankings.size() + 1) / 2))
                .flatMap(Collection::stream).collect(Collectors.toList());

        Map<String, List<GameResult>> gameResultMap = gameResultMapper.selectList(null).stream().collect(Collectors.groupingBy(GameResult::getTraderId));
        finalRankVORankings.forEach(r -> {
            List<Double> profits = gameResultMap.get(r.getUserId()).stream()
                    .sorted(Comparator.comparing(GameResult::getRoundId)).map(GameResult::getProfit).collect(Collectors.toList());
            r.setRoundProfits(profits);
        });

        FinalRankVO finalRankVO = FinalRankVO.builder()
                .myFinalRanking(Convertor.INST.to(gameRanking))
                .roundRankings(roundRankings)
                .finalRankings(finalRankVORankings)
                .build();
        return Result.success(finalRankVO);
    }

    final GeneratorResultMapper generatorResultMapper;
    /**
     * 结算明细--分机组
     * @param stageId 阶段id
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    @GetMapping("listGeneratorResults")
    public Result<List<GeneratorResult>> listGeneratorResults(String stageId, @RequestHeader String token) {
        List<GeneratorResult> ls = (List<GeneratorResult>) cache.get("listGeneratorResults" + stageId + token, ( ) -> doListGeneratorResults(stageId, token));
        return Result.success(ls);
    }

    public List<GeneratorResult> doListGeneratorResults(String stageId, @RequestHeader String token) {
        String userId = TokenUtils.getUserId(token);
        StageId parsed = StageId.parse(stageId);
        List<Unit> units = tunnel.listUnits(parsed.getCompId(), parsed.getRoundId(), tunnel.review() ? null : userId).stream()
                .filter(u -> u.getMetaUnit().getUnitType().equals(UnitType.GENERATOR)).collect(Collectors.toList());
        List<Integer> sourceIds = units.stream().map(u -> u.getMetaUnit().getSourceId()).collect(Collectors.toList());
        LambdaQueryWrapper<GeneratorResult> in = new LambdaQueryWrapper<GeneratorResult>()
                .eq(GeneratorResult::getRoundId, parsed.getRoundId() + 1)
                .in(GeneratorResult::getUnitId, sourceIds);
        List<GeneratorResult> generatorResults = Collect.isEmpty(sourceIds) ? Collections.EMPTY_LIST : generatorResultMapper.selectList(in);
        generatorResults.forEach(g -> {
            Unit unit = units.stream().filter(u -> u.getMetaUnit().getSourceId().equals(g.getUnitId())).findFirst().orElseThrow(SysEx::unreachable);
            g.setUnitName(unit.getMetaUnit().getName());
            g.setGeneratorType(unit.getMetaUnit().getGeneratorType());
            g.setRenewableType(unit.getMetaUnit().getRenewableType());
        });
        List<GeneratorResult> collect0 = generatorResults.stream().filter(g -> g.getTraderId().equals(userId)).collect(Collectors.toList());
        List<GeneratorResult> collect1 = generatorResults.stream().filter(g -> !g.getTraderId().equals(userId)).collect(Collectors.toList());
        generatorResults = Stream.of(collect0, collect1).flatMap(Collection::stream).collect(Collectors.toList());
        return generatorResults;
    }

    final LoadResultMapper loadResultMapper;
    /**
     * 结算明细--分负荷
     * @param stageId 阶段id
     */

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @GetMapping("listLoadsResults")
    Result<List<LoadResult>> listLoadsResults(String stageId, @RequestHeader String token) {
        List<LoadResult> ls = (List<LoadResult>) cache.get("listLoadsResults" + stageId + token, () -> doListLoadsResults(stageId, token));
        return Result.success(ls);
    }

    List<LoadResult> doListLoadsResults(String stageId, @RequestHeader String token) {
        String userId = TokenUtils.getUserId(token);
        StageId parsed = StageId.parse(stageId);
        List<Unit> units = tunnel.listUnits(parsed.getCompId(), parsed.getRoundId(), tunnel.review() ? null : userId).stream()
                .filter(u -> u.getMetaUnit().getUnitType().equals(UnitType.LOAD)).collect(Collectors.toList());
        List<Integer> sourceIds = units.stream().map(u -> u.getMetaUnit().getSourceId()).collect(Collectors.toList());
        LambdaQueryWrapper<LoadResult> in = new LambdaQueryWrapper<LoadResult>()
                .eq(LoadResult::getRoundId, parsed.getRoundId() + 1)
                .in(LoadResult::getLoadId, sourceIds);
        List<LoadResult> loadResults = Collect.isEmpty(sourceIds) ? Collections.EMPTY_LIST : loadResultMapper.selectList(in);

        loadResults.forEach(g -> {
            String name = units.stream().filter(u -> u.getMetaUnit().getSourceId().equals(g.getLoadId())).findFirst().orElseThrow(SysEx::unreachable).getMetaUnit().getName();
            g.setUnitName(name);
        });
        List<LoadResult> collect0 = loadResults.stream().filter(g -> g.getTraderId().equals(userId)).collect(Collectors.toList());
        List<LoadResult> collect1 = loadResults.stream().filter(g -> !g.getTraderId().equals(userId)).collect(Collectors.toList());
        loadResults = Stream.of(collect0, collect1).flatMap(Collection::stream).collect(Collectors.toList());
        return loadResults;
    }


    @Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public interface Convertor extends BasicConvertor {

        Convertor INST = Mappers.getMapper(Convertor.class);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        CompVO to(Comp comp);


        @BeanMapping(builder = @Builder(disableBuilder = true))
        default String toString(StageId stageId) {
            return stageId.toString();
        }

        @AfterMapping
        default void after(Comp comp, @MappingTarget CompVO compVO) {
            StageId stageId = StageId.builder().compId(compVO.getCompId())
                    .compStage(comp.getCompStage())
                    .roundId(comp.getRoundId())
                    .tradeStage(comp.getTradeStage())
                    .marketStatus(comp.getMarketStatus())
                    .build();
            compVO.setStageId(stageId.toString());
            DelayExecutor delayExecutor = BeanUtil.getBean(DelayExecutor.class);
        }

        @BeanMapping(builder = @Builder(disableBuilder = true))
        InterClearanceVO to(InterClearance interClearance);

        @AfterMapping
        default void after(InterClearance interClearance, @MappingTarget InterClearanceVO interClearanceVO) {
            interClearanceVO.setDealQuantity(interClearance.getMarketQuantity());
            if (interClearance.getDealPrice() != null) {
                interClearanceVO.setStart(Point.<Double>builder().x(0D).y(interClearance.getDealPrice()).build());
                interClearanceVO.setEnd(Point.<Double>builder().x(interClearance.getMarketQuantity()).y(interClearance.getDealPrice()).build());
            }
        }

        @BeanMapping(builder = @Builder(disableBuilder = true))
        @Mapping(source = "traderId", target = "userId")
        RoundRankVO.Ranking toRound(GameResult gameResult);


        @BeanMapping(builder = @Builder(disableBuilder = true))
        @Mapping(source = "traderId", target = "userId")
        @Mapping(source = "ranking", target = "number")
        FinalRankVO.Ranking toFinal(GameResult gameRanking);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        @Mapping(source = "traderId", target = "userId")
        @Mapping(source = "totalRanking", target = "number")
        FinalRankVO.Ranking to(GameRanking gameRanking);

        @AfterMapping
        default void afterMapping(GameRanking gameRanking, @MappingTarget FinalRankVO.Ranking ranking) {
            UserDO userDO = BeanUtil.getBean(UserDOMapper.class).selectById(gameRanking.getTraderId());
            ranking.setGroupId(userDO.getGroupId());
        }


    }







}
