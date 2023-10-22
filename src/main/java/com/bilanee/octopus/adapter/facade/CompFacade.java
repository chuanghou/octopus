package com.bilanee.octopus.adapter.facade;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilanee.octopus.adapter.facade.vo.*;
import com.bilanee.octopus.adapter.repository.UnitAdapter;
import com.bilanee.octopus.adapter.tunnel.BidQuery;
import com.bilanee.octopus.adapter.tunnel.InterClearance;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.*;
import com.bilanee.octopus.basic.enums.*;
import com.bilanee.octopus.domain.Comp;
import com.bilanee.octopus.domain.IntraSymbol;
import com.bilanee.octopus.domain.Unit;
import com.bilanee.octopus.infrastructure.entity.*;
import com.bilanee.octopus.infrastructure.mapper.*;
import com.google.common.collect.ListMultimap;
import com.stellariver.milky.common.base.ExceptionType;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.base.SysEx;
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
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 竞赛相关
 */
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequestMapping("/api/comp")
public class CompFacade {

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
    final MinOutputCostMapper minOutputCostMapper;

    /**
     * 当前运行竞赛查看
     * 返回result
     * @return 当前运行竞赛概况
     */
    @GetMapping("/runningCompVO")
    public Result<CompVO> runningCompVO(@RequestHeader String token) {
        String userId = TokenUtils.getUserId(token);
        Comp comp = tunnel.runningComp();
        if (comp == null || !comp.getUserIds().contains(userId)) {
            return Result.error(ErrorEnums.COMP_NOT_EXISTED, ExceptionType.BIZ);
        }
        return Result.success(Convertor.INST.to(comp));
    }


    /**
     * 省间出清结果
     * @param stageId 阶段id
     * @param token 访问者token
     * @return 省间出清结果
     */
    @GetMapping("/interClearanceVO")
    public Result<List<InterClearanceVO>> interClearanceVO(@NotBlank String stageId, @RequestHeader String token) {
        StageId parsedStageId = StageId.parse(stageId);

        // 清算值
        LambdaQueryWrapper<ClearanceDO> eq = new LambdaQueryWrapper<ClearanceDO>().eq(ClearanceDO::getStageId, stageId);
        List<ClearanceDO> clearanceDOs = clearanceDOMapper.selectList(eq);
        List<InterClearance> interClearances = Collect.transfer(clearanceDOs, clearanceDO -> Json.parse(clearanceDO.getClearance(), InterClearance.class));
        Map<TimeFrame, InterClearanceVO> interClearVOs = interClearances.stream().map(Convertor.INST::to).collect(Collectors.toMap(InterClearanceVO::getTimeFrame, i -> i));

        boolean ranking = tunnel.runningComp().getCompStage() == CompStage.RANKING;

        // 单元信息
        LambdaQueryWrapper<UnitDO> queryWrapper = new LambdaQueryWrapper<UnitDO>().eq(UnitDO::getCompId, parsedStageId.getCompId())
                .eq(UnitDO::getRoundId, parsedStageId.getRoundId())
                .eq(!ranking, UnitDO::getUserId, TokenUtils.getUserId(token));
        List<UnitDO> unitDOs = unitDOMapper.selectList(queryWrapper);
        List<Unit> units = Collect.transfer(unitDOs, UnitAdapter.Convertor.INST::to).stream()
                .filter(unit -> unit.getMetaUnit().getProvince().interDirection() == unit.getMetaUnit().getUnitType().generalDirection()).collect(Collectors.toList());
        List<UnitVO> unitVOs = Collect.transfer(units, u -> new UnitVO(u.getUnitId(), u.getMetaUnit().getName(), u.getMetaUnit()));
        interClearVOs.values().forEach(interClearanceVO -> interClearanceVO.setUnitVOs(unitVOs));

        // 委托及成交信息
        BidQuery bidQuery = BidQuery.builder()
                .compId(parsedStageId.getCompId())
                .roundId(parsedStageId.getRoundId())
                .tradeStage(parsedStageId.getTradeStage())
                .userId(ranking ? null : TokenUtils.getUserId(token))
                .build();

        tunnel.listBids(bidQuery).stream().collect(Collect.listMultiMap(Bid::getTimeFrame)).asMap().forEach((timeFrame, bids) -> {
            List<UnitDealVO> unitDealVOS = bids.stream().collect(Collect.listMultiMap(Bid::getUnitId)).asMap().entrySet().stream().map(e -> {
                Long unitId = e.getKey();
                Collection<Bid> unitBids = e.getValue();
                List<Deal> deals = unitBids.stream().flatMap(bid -> bid.getDeals().stream()).collect(Collectors.toList());
                Double totalQuantity = deals.stream().map(Deal::getQuantity).reduce(0D, Double::sum);
                Double totalVolume = deals.stream().map(deal -> deal.getQuantity() * deal.getPrice()).reduce(0D, Double::sum);
                return UnitDealVO.builder()
                        .unitId(unitId)
                        .averagePrice(totalVolume / totalQuantity)
                        .totalQuantity(totalQuantity)
                        .deals(deals)
                        .build();
            }).collect(Collectors.toList());
            Optional.ofNullable(interClearVOs.get(timeFrame)).ifPresent(interClearanceVO -> interClearanceVO.setUnitDealVOS(unitDealVOS));
        });

        return Result.success(new ArrayList<>(interClearVOs.values()));
    }

    /**
     * 省内结算结果
     * @param stageId 阶段id
     * @param token 访问者token
     * @return 省间出清结果
     */
    @GetMapping("/intraClearanceVO")
    public Result<List<IntraClearanceVO>> intraClearanceVO(@NotBlank String stageId, @RequestHeader String token) {

        Comp comp = tunnel.runningComp();
        StageId parsed = StageId.parse(stageId);
        BidQuery bidQuery = BidQuery.builder().compId(parsed.getCompId())
                .roundId(parsed.getRoundId()).tradeStage(parsed.getTradeStage())
                .build();

        boolean equals = comp.getCompStage().equals(CompStage.RANKING);
        LambdaQueryWrapper<UnitDO> queryWrapper = new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getCompId, parsed.getCompId())
                .eq(UnitDO::getRoundId, parsed.getRoundId())
                .eq(!equals, UnitDO::getUserId, TokenUtils.getUserId(token));
        List<UnitDO> unitDOs = unitDOMapper.selectList(queryWrapper);
        List<Unit> units = Collect.transfer(unitDOs, UnitAdapter.Convertor.INST::to).stream()
                .filter(unit -> unit.getMetaUnit().getProvince().interDirection() == unit.getMetaUnit().getUnitType().generalDirection()).collect(Collectors.toList());
        List<UnitVO> unitVOs = Collect.transfer(units, u -> new UnitVO(u.getUnitId(), u.getMetaUnit().getName(), u.getMetaUnit()));

        ListMultimap<IntraSymbol, Bid> groupedBids = tunnel.listBids(bidQuery).stream().collect(Collect.listMultiMap(i -> new IntraSymbol(i.getProvince(), i.getTimeFrame())));
        List<IntraClearanceVO> intraClearanceVOs = groupedBids.asMap().entrySet().stream().map(e -> {
            IntraSymbol intraSymbol = e.getKey();
            Collection<Bid> bids = e.getValue();
            List<Deal> deals = bids.stream().flatMap(b -> b.getDeals().stream()).collect(Collectors.toList());
            Double maxPrice = deals.stream().max(Comparator.comparing(Deal::getPrice)).map(Deal::getPrice).orElse(null);
            Double minPrice = deals.stream().min(Comparator.comparing(Deal::getPrice)).map(Deal::getPrice).orElse(null);
            Double totalVolume = deals.stream().map(d -> d.getPrice() * d.getQuantity()).reduce(0D, Double::sum);
            Double totalQuantity = deals.stream().map(Deal::getQuantity).reduce(0D, Double::sum);
            Double averagePrice = totalQuantity.equals(0D) ? null : (totalVolume / totalQuantity);
            Double buyTransit = bids.stream().filter(bid -> bid.getDirection() == Direction.BUY).map(Bid::getTransit).reduce(0D, Double::sum);
            Double sellTransit = bids.stream().filter(bid -> bid.getDirection() == Direction.SELL).map(Bid::getTransit).reduce(0D, Double::sum);

            List<Predicate<Deal>> selectors = IntStream.range(0, 10)
                    .mapToObj(i -> (Predicate<Deal>) d -> d.getPrice() > i * 200D && d.getPrice() <= (i + 1) * 200).collect(Collectors.toList());

            List<Double> dealHistogram = deals.stream().collect(Collect.select(selectors))
                    .stream().map(ds -> ds.stream().collect(Collectors.summarizingDouble(Deal::getQuantity)).getSum()).collect(Collectors.toList());

            Map<Long, Collection<Bid>> bidMap = bids.stream().collect(Collect.listMultiMap(Bid::getUnitId)).asMap();
            List<UnitDealVO> unitDealVOs = bidMap.entrySet().stream().map(ee -> {
                Long unitId = ee.getKey();
                Collection<Bid> unitBids = ee.getValue();
                List<Deal> unitDeals = unitBids.stream().flatMap(bid -> bid.getDeals().stream()).collect(Collectors.toList());
                Double unitTotalVolume = unitDeals.stream().map(deal -> deal.getQuantity() * deal.getPrice()).reduce(0D, Double::sum);
                Double unitTotalQuantity = unitDeals.stream().map(Deal::getQuantity).reduce(0D, Double::sum);
                return UnitDealVO.builder()
                        .unitId(unitId)
                        .averagePrice(unitTotalVolume / unitTotalQuantity)
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
                    .totalDealQuantity(totalQuantity)
                    .unitVOs(unitVOs)
                    .unitDealVOS(unitDealVOs)
                    .dealHistogram(dealHistogram)
                    .build();
        }).collect(Collectors.toList());
        return Result.success(intraClearanceVOs);
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

    final SubregionPriceMapper subregionPriceMapper;


    /**
     * 省内现货市场：市场成交概况，市场供需曲线
     * @param stageId 当前页面所处stageId
     * @param province 查看省份
     * @return 现货供需曲线
     */
    @GetMapping("listSpotMarketVOs")
    public Result<SpotMarketVO> listSpotMarketVOs(String stageId, String province, @RequestHeader String token) {
        StageId parsed = StageId.parse(stageId);
        Province parsedProvince = Kit.enumOfMightEx(Province::name, province);
        Comp comp = tunnel.runningComp();

        SpotMarketVO.SpotMarketVOBuilder builder = SpotMarketVO.builder();

        //  表头的4个值
        LambdaQueryWrapper<SubregionPrice> eq = new LambdaQueryWrapper<SubregionPrice>()
                .eq(SubregionPrice::getRoundId, parsed.getRoundId() + 1).eq(SubregionPrice::getSubregionId, parsedProvince.getDbCode());
        List<SubregionPrice> subregionPrices = subregionPriceMapper.selectList(eq);
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
        List<Double> daInstantLoadBids = loadDaForecastBidMapper.selectList(in).stream().collect(Collectors.groupingBy(LoadDaForecastBidDO::getPrd)).values()
                .stream().map(bids -> bids.stream().collect(Collectors.summarizingDouble(LoadDaForecastBidDO::getBidMw)).getSum()).collect(Collectors.toList());

        LambdaQueryWrapper<LoadForecastValueDO> in1 = new LambdaQueryWrapper<LoadForecastValueDO>().in(LoadForecastValueDO::getLoadId, loadSourceIds);
        List<Double> rtInstantLoadBids = loadForecastValueMapper.selectList(in1).stream().collect(Collectors.groupingBy(LoadForecastValueDO::getPrd)).values()
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

        // 搜索的单元列表
        boolean equals = comp.getCompStage().equals(CompStage.RANKING);
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


        builder.unitVOs(unitVOs);

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
            List<Section> supplySections = toSections(supplyQps, Comparator.comparing(Qp::getPrice));
            Point<Double> supplyTerminus = new Point<>(supplySections.get(supplySections.size() - 1).getRx(), offerPriceCap);
            List<Qp> demandQps = demandDa.get(i);
            List<Section> demandSections = toSections(demandQps, Comparator.comparing(Qp::getPrice).reversed());
            Point<Double> demandTerminus = new Point<>(demandSections.get(demandSections.size() - 1).getRx(), 0D);
            return SpotMarketEntityVO.builder()
                    .supplySections(supplySections)
                    .supplyTerminus(supplyTerminus)
                    .demandSections(demandSections)
                    .demandTerminus(demandTerminus)
                    .build();
        }).collect(Collectors.toList());

        builder.daEntityVOs(daEntityVOs);


        List<SpotMarketEntityVO> rtEntityVOs = IntStream.range(0, 24).mapToObj(i -> {
            List<Qp> supplyQps = supplyRt.get(i);
            List<Section> supplySections = toSections(supplyQps, Comparator.comparing(Qp::getPrice));
            Point<Double> supplyTerminus = new Point<>(supplySections.get(supplySections.size() - 1).getRx(), offerPriceCap);
            List<Qp> demandQps = demandRt.get(i);
            List<Section> demandSections = toSections(demandQps, Comparator.comparing(Qp::getPrice).reversed());
            Point<Double> demandTerminus = new Point<>(demandSections.get(demandSections.size() - 1).getRx(), 0D);
            return SpotMarketEntityVO.builder()
                    .supplySections(supplySections)
                    .supplyTerminus(supplyTerminus)
                    .demandSections(demandSections)
                    .demandTerminus(demandTerminus)
                    .build();
        }).collect(Collectors.toList());

        SpotMarketVO spotMarketVO = builder.rtEntityVOs(rtEntityVOs).build();
        return Result.success(spotMarketVO);
    }

    List<Section> toSections(List<Qp> qps, Comparator<Qp> comparator) {
        qps = qps.stream().sorted(comparator).collect(Collectors.toList());
        Double accumulate = 0D;
        List<Section> sections = new ArrayList<>();
        for (Qp qp : qps) {
            Section section = Section.builder()
                    .unitId(qp.getUnitId()).lx(accumulate).rx(accumulate + qp.getQuantity()).y(qp.getPrice()).build();
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

        // 1. 火电机组的最小输出量
        List<Qp> minOutputQps = classicUnitDOs.stream()
                .map(unitDO -> new Qp(null, unitDO.getUnitId(), unitDO.getMetaUnit().getMinCapacity(), unitDO.getMetaUnit().getMinOutputPrice()))
                .collect(Collectors.toList());

        // 2. 火电机组5段量价
        Map<Integer, Long> classicUnitIds = classicUnitDOs.stream().collect(Collectors.toMap(unitDO -> unitDO.getMetaUnit().getSourceId(), UnitDO::getUnitId));
        LambdaQueryWrapper<GeneratorDaSegmentBidDO> in0 = new LambdaQueryWrapper<GeneratorDaSegmentBidDO>()
                .eq(GeneratorDaSegmentBidDO::getRoundId, stageId.getRoundId() + 1)
                .in(GeneratorDaSegmentBidDO::getUnitId, classicUnitIds.keySet());

        List<GeneratorDaSegmentBidDO> generatorDaSegmentBidDOs = Collect.isEmpty(classicUnitIds) ? Collections.EMPTY_LIST : generatorDaSegmentMapper.selectList(in0);

        List<Qp> classicQps = generatorDaSegmentBidDOs.stream()
                .map(gDO -> new Qp(null, classicUnitIds.get(gDO.getUnitId()), gDO.getOfferMw(), gDO.getOfferPrice())).collect(Collectors.toList());

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
                .in(GeneratorForecastValueDO::getUnitId, renewableUnitIds.keySet());
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
                return new Qp(t.getPrd(), null, tielinePower, marketSettingDO.getOfferPriceFloor());
            }).collect(Collectors.toList());
        }

        List<Qp> nonInstantQps = Stream.of(minOutputQps, classicQps).flatMap(Collection::stream).collect(Collectors.toList());

        ListMultimap<Integer, Qp> groupDaQps = Stream.of(daRenewableQps, tielineQps)
                .flatMap(Collection::stream).collect(Collect.listMultiMap(Qp::getInstant));
        List<List<Qp>> daInstantQps = IntStream.range(0, 24).mapToObj(i -> new ArrayList<>(groupDaQps.get(i))).collect(Collectors.toList());
        daInstantQps.forEach(qps -> qps.addAll(nonInstantQps));

        ListMultimap<Integer, Qp> groupRtQps = Stream.of(rtRenewableQps, tielineQps).flatMap(Collection::stream).collect(Collect.listMultiMap(Qp::getInstant));
        List<List<Qp>> rtInstantQps = IntStream.range(0, 24).mapToObj(i -> new ArrayList<>(groupRtQps.get(i))).collect(Collectors.toList());
        rtInstantQps.forEach(qps -> qps.addAll(nonInstantQps));
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
        List<LoadDaForecastBidDO> loadDaForecastBidDOS = loadDaForecastBidMapper.selectList(in);
        List<Qp> daInstantQps = loadDaForecastBidDOS.stream()
                .collect(Collectors.groupingBy(LoadDaForecastBidDO::getPrd))
                .entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> e.getValue().stream().map(b -> new Qp(e.getKey(), loadIds.get(b.getLoadId()), b.getBidMw(), b.getBidPrice())).collect(Collectors.toList()))
                .flatMap(Collection::stream).collect(Collectors.toList());

        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(1);
        LambdaQueryWrapper<SprDO> eq0 = new LambdaQueryWrapper<SprDO>().eq(SprDO::getProv, province.getDbCode());
        List<Qp> rtInstantQps = sprDOMapper.selectList(eq0).stream()
                .sorted(Comparator.comparing(SprDO::getPrd))
                .map(sprDO -> new Qp(sprDO.getPrd(), null, sprDO.getRtLoad(), marketSettingDO.getBidPriceCap())).collect(Collectors.toList());

        List<Qp> tielineQps = new ArrayList<>();
        if (province == Province.TRANSFER) {
            LambdaQueryWrapper<TieLinePowerDO> eq1 = new LambdaQueryWrapper<TieLinePowerDO>().eq(TieLinePowerDO::getRoundId, stageId.getRoundId());
            tielineQps = tieLinePowerDOMapper.selectList(eq1).stream().sorted(Comparator.comparing(TieLinePowerDO::getPrd)).map(t -> {
                double tielinePower = t.getAnnualTielinePower() + t.getMonthlyTielinePower() + t.getDaTielinePower();
                return new Qp(t.getPrd(), null, tielinePower, marketSettingDO.getBidPriceFloor());
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
                    qps.add(new Qp(i, unitId, lastQuantity, segmentBidDO.getOfferPrice()));
                    break;
                } else {
                    qps.add(new Qp(i, unitId, segmentBidDO.getOfferMw(), segmentBidDO.getOfferPrice()));
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
    }




    /**
     *  省内现货市场：中标电源结构
     * @param stageId 界面阶段id
     * @param province 查看省份
     *
     */
    @GetMapping ("listSpotBiddenEntityVOs")
    public Result<SpotBiddenVO> listSpotBiddenEntityVOs(@NotBlank String stageId, @NotBlank String province, @RequestHeader String token)  {
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
        SpotBiddenVO spotBiddenVO = SpotBiddenVO.builder().daSpotBiddenEntityVO(daSpotBiddenEntityVO).rtSpotBiddenEntityVO(rtSpotBiddenEntityVO).build();
        return Result.success(spotBiddenVO);
    }

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
            LambdaQueryWrapper<LoadDaForecastBidDO> wrapper0 = new LambdaQueryWrapper<LoadDaForecastBidDO>()
                    .eq(LoadDaForecastBidDO::getRoundId, roundId).in(LoadDaForecastBidDO::getLoadId, unitIds.keySet());
            List<LoadDaForecastBidDO> loadDaForecastBidDOs = loadDaForecastBidMapper.selectList(wrapper0);
                intraLoads = loadDaForecastBidDOs.stream().collect(Collectors.groupingBy(LoadDaForecastBidDO::getPrd))
                    .entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue)
                    .map(bs -> bs.stream().collect(Collectors.summarizingDouble(LoadDaForecastBidDO::getBidMw)).getSum())
                    .collect(Collectors.toList());
        } else {
            LambdaQueryWrapper<SprDO> eq = new LambdaQueryWrapper<SprDO>().eq(SprDO::getProv, parsedProvince.getDbCode());
            intraLoads = sprDOMapper.selectList(eq).stream().sorted(Comparator.comparing(SprDO::getPrd)).map(SprDO::getRtLoad).collect(Collectors.toList());
        }


        LambdaQueryWrapper<TieLinePowerDO> eq = new LambdaQueryWrapper<TieLinePowerDO>().eq(TieLinePowerDO::getRoundId, roundId - 1);
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

        LambdaQueryWrapper<UnitBasic> in0 = new LambdaQueryWrapper<UnitBasic>().in(UnitBasic::getUnitId, unitIds.keySet());

        List<UnitBasic> unitBasics = unitBasicMapper.selectList(in0);

        double classicTotal = unitBasics.stream().filter(unitBasic -> classicUnitIds.containsKey(unitBasic.getUnitId()))
                .collect(Collectors.summarizingDouble(UnitBasic::getMaxP)).getSum();

        List<Double> renewableTotals;
        if (da) {
            LambdaQueryWrapper<GeneratorDaForecastBidDO> in = new LambdaQueryWrapper<GeneratorDaForecastBidDO>().eq(GeneratorDaForecastBidDO::getRoundId, roundId + 1)
                    .in(GeneratorDaForecastBidDO::getUnitId, unitIds.keySet());
            renewableTotals = generatorDaForecastBidMapper.selectList(in).stream()
                    .collect(Collectors.groupingBy(GeneratorDaForecastBidDO::getPrd))
                    .entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue)
                    .map(ls -> ls.stream().collect(Collectors.summarizingDouble(GeneratorDaForecastBidDO::getForecastMw)).getSum()).collect(Collectors.toList());
        } else {
            LambdaQueryWrapper<SprDO> eqx = new LambdaQueryWrapper<SprDO>().eq(SprDO::getProv, parsedProvince.getDbCode());
            renewableTotals = sprDOMapper.selectList(eqx).stream().sorted(Comparator.comparing(SprDO::getPrd)).map(SprDO::getRtRenewable).collect(Collectors.toList());
        }

        LambdaQueryWrapper<SpotUnitCleared> in1 = new LambdaQueryWrapper<SpotUnitCleared>()
                .eq(SpotUnitCleared::getRoundId, parsedStageId.getRoundId() + 1)
                .in(SpotUnitCleared::getUnitId, unitIds.keySet());
        List<SpotUnitCleared> spotUnitCleareds = spotUnitClearedMapper.selectList(in1);

        List<Double> classicBidden = spotUnitCleareds.stream().filter(c -> classicUnitIds.containsKey(c.getUnitId()))
                .collect(Collectors.groupingBy(SpotUnitCleared::getPrd)).entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue)
                .map(cs -> cs.stream().collect(Collectors.summarizingDouble(SpotUnitCleared::getDaClearedMw)).getSum())
                .collect(Collectors.toList());
        builder.classicBidden(classicBidden);
        builder.classicNotBidden(classicBidden.stream().map(c -> classicTotal - c).collect(Collectors.toList()));

        List<Double> renewableBidden = spotUnitCleareds.stream().filter(c -> renewableUnitIds.containsKey(c.getUnitId()))
                .collect(Collectors.groupingBy(SpotUnitCleared::getPrd)).entrySet().stream()
                .sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue)
                .map(cs -> cs.stream().collect(Collectors.summarizingDouble(SpotUnitCleared::getDaClearedMw)).getSum())
                .collect(Collectors.toList());
        builder.renewableBidden(renewableBidden);
        builder.renewableNotBidden(IntStream.range(0, 24).mapToObj(i -> renewableTotals.get(i) - renewableBidden.get(i)).collect(Collectors.toList()));


        // 分报价区间
        LambdaQueryWrapper<GeneratorDaSegmentBidDO> in = new LambdaQueryWrapper<GeneratorDaSegmentBidDO>()
                .eq(GeneratorDaSegmentBidDO::getRoundId, parsedStageId.getRoundId() + 1)
                .in(GeneratorDaSegmentBidDO::getUnitId, unitIds.keySet());
        List<GeneratorDaSegmentBidDO> generatorDaSegmentBidDOs = generatorDaSegmentMapper.selectList(in);

        List<List<GeneratorDaSegmentBidDO>> segmentBids = generatorDaSegmentBidDOs.stream().collect(Collectors.groupingBy(GeneratorDaSegmentBidDO::getPrd))
                .entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).collect(Collectors.toList());

        List<List<SpotUnitCleared>> indexedSpotUnitCleared = spotUnitCleareds.stream().collect(Collectors.groupingBy(SpotUnitCleared::getPrd))
                .entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).collect(Collectors.toList());

        List<List<Double>> priceStatistics = IntStream.range(0, 24)
                .mapToObj(i -> process(segmentBids.get(i), indexedSpotUnitCleared.get(i), unitDOs, da)).collect(Collectors.toList());
        builder.priceStatistics(priceStatistics);

        return builder.build();
    }

    private List<Double> process(List<GeneratorDaSegmentBidDO> segments, List<SpotUnitCleared> clears, List<UnitDO> unitDOs, boolean da) {
        Map<Integer, List<GeneratorDaSegmentBidDO>> groupedByUnitIds = segments.stream().collect(Collectors.groupingBy(GeneratorDaSegmentBidDO::getUnitId));
        Map<Integer, SpotUnitCleared> unitClearedMap = Collect.toMapMightEx(clears, SpotUnitCleared::getUnitId);
        List<Pair<Double, Double>> collectQps = unitDOs.stream().map(unitDO -> {
            Integer sourceId = unitDO.getMetaUnit().getSourceId();
            List<GeneratorDaSegmentBidDO> generatorDaSegmentBidDOs = groupedByUnitIds.get(sourceId).stream()
                    .sorted(Comparator.comparing(GeneratorDaSegmentBidDO::getOfferId)).collect(Collectors.toList());
            SpotUnitCleared spotUnitCleared = unitClearedMap.get(sourceId);
            List<Pair<Double, Double>> qps = new ArrayList<>();
            if (GeneratorType.CLASSIC.equals(unitDO.getMetaUnit().getGeneratorType())) {
                qps.add(Pair.of(unitDO.getMetaUnit().getMinCapacity(), unitDO.getMetaUnit().getMinOutputPrice()));
            }
            generatorDaSegmentBidDOs.forEach(gDO -> qps.add(Pair.of(gDO.getOfferMw(), gDO.getOfferPrice())));
            List<Pair<Double, Double>> clearedQps = new ArrayList<>();
            if (spotUnitCleared.getDaClearedMw().equals(0D)) {
                return clearedQps;
            }
            double accumulate = 0D;
            for (GeneratorDaSegmentBidDO generatorDaSegmentBidDO : generatorDaSegmentBidDOs) {
                Double q = generatorDaSegmentBidDO.getOfferMw();
                Double p = generatorDaSegmentBidDO.getOfferPrice();
                Double clear = da ? spotUnitCleared.getDaClearedMw() : spotUnitCleared.getRtClearedMw();
                if (accumulate + q >= clear) {
                    double v = accumulate + q - clear;
                    Pair<Double, Double> qp = Pair.of(v, p);
                    qps.add(qp);
                    return qps;
                }
                qps.add(Pair.of(p, q));
            }
            return qps;
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
    @GetMapping("getSpotInterClearanceVO")
    @SuppressWarnings("unchecked")
    public Result<SpotInterClearanceVO> getSpotInterClearanceVO(String stageId, @RequestHeader String token) {
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
        return Result.success(spotInterClearanceVO);
    }

    final InterSpotUnitOfferDOMapper interSpotUnitOfferDOMapper;
    final UnmetDemandMapper unmetDemandMapper;

    /**
     * 省间现货分时供需曲线
     * @param stageId 阶段id
     * @param instant 时刻
     */
    @GetMapping("getInterSpotMarketVO")
    public Result<InterSpotMarketVO> getInterSpotMarketVO(String stageId, Integer instant, @RequestHeader String token) {
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
        LambdaQueryWrapper<UnmetDemand> eq1 = new LambdaQueryWrapper<UnmetDemand>().eq(UnmetDemand::getPrd, instant);
        Double receiverDeclaredTotal = unmetDemandMapper.selectOne(eq1).getDaReceivingMw() - already;



        LambdaQueryWrapper<InterSpotTransactionDO> last = new LambdaQueryWrapper<InterSpotTransactionDO>()
                .eq(InterSpotTransactionDO::getRoundId, roundId + 1);

        List<InterSpotTransactionDO> transactionDOs = interSpotTransactionDOMapper
                .selectList(last).stream().filter(i -> i.getClearedMw() > 0).collect(Collectors.toList());
        double dealTotal = transactionDOs.stream().collect(Collectors.summarizingDouble(InterSpotTransactionDO::getClearedMw)).getSum();
        Double dealAveragePrice = Collect.isEmpty(transactionDOs) ? null : transactionDOs.get(0).getClearedPrice();


        Double lx = 0D;
        List<SpotSection> spotSections = new ArrayList<>();
        for (Pair<Double, Double> pair : pairs) {
            SpotSection spotSection = new SpotSection(lx, lx + pair.getLeft(), pair.getRight());
            spotSections.add(spotSection);
            lx += pair.getLeft();
        }
        InterSpotMarketVO interSpotMarketVO = InterSpotMarketVO.builder()
                .sellDeclaredTotal(sellDeclaredTotal)
                .receiverDeclaredTotal(receiverDeclaredTotal)
                .dealTotal(dealTotal)
                .dealAveragePrice(dealAveragePrice)
                .requireQuantity(receiverDeclaredTotal)
                .clearPrice(dealAveragePrice)
                .spotSections(spotSections)
                .build();
         return Result.success(interSpotMarketVO);
    }


    /**
     * 省间现货分设备成交量价
     * @param stageId 阶段id
     */
    @GetMapping("listInterSpotDeals")
    public Result<List<InterSpotUnitDealVO>> listInterSpotDeals(String stageId, Integer instant, @RequestHeader String token) {
        Integer roundId = StageId.parse(stageId).getRoundId();
        Long compId = StageId.parse(stageId).getCompId();
        String userId = TokenUtils.getUserId(token);
        boolean equals = tunnel.runningComp().getTradeStage().equals(TradeStage.END);
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
            return new InterSpotUnitDealVO(name, deals);
        }).collect(Collectors.toList());
        return Result.success(interSpotUnitDealVOs);
    }


    /**
     * 本轮成绩排名
     */
    @GetMapping("getRanking")
    public Result<RankingVO> getRanking(String stageId, @RequestHeader String token) {

        return null;
    }


    /**
     * 结算概况
     */
    @GetMapping("getSettlementProfile")
    public Result<SettlementProfile> getSettlementProfile(String stageId, @RequestHeader String token) {

        return null;
    }

    /**
     * 结算明细
     */
    @GetMapping("getSettlementDetail")
    public Result<SettlementDetail> getSettlementDetail(String stageId, @RequestHeader String token) {

        return null;
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
        }

        @BeanMapping(builder = @Builder(disableBuilder = true))
        InterClearanceVO to(InterClearance interClearance);

    }







}
