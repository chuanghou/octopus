package com.bilanee.octopus.adapter.tunnel;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilanee.octopus.adapter.repository.UnitAdapter;
import com.bilanee.octopus.basic.*;
import com.bilanee.octopus.basic.enums.*;
import com.bilanee.octopus.config.OctopusProperties;
import com.bilanee.octopus.domain.Comp;
import com.bilanee.octopus.domain.Unit;
import com.bilanee.octopus.infrastructure.entity.*;
import com.bilanee.octopus.infrastructure.mapper.*;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.SysEx;
import com.stellariver.milky.common.tool.common.Kit;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.common.tool.util.Json;
import com.stellariver.milky.domain.support.base.DomainTunnel;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Builder;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class Tunnel {

    final MetaUnitDOMapper metaUnitDOMapper;
    final BidDOMapper bidDOMapper;
    final DomainTunnel domainTunnel;
    final CompDOMapper compDOMapper;
    final MarketSettingMapper marketSettingMapper;
    final TransLimitDOMapper transLimitDOMapper;
    final ClearanceDOMapper clearanceDOMapper;
    final IntraQuotationDOMapper intraQuotationDOMapper;
    final IntraInstantDOMapper intraInstantDOMapper;
    final UnitDOMapper unitDOMapper;
    final TieLinePowerDOMapper tieLinePowerDOMapper;
    final StackDiagramDOMapper stackDiagramDOMapper;
    final GeneratorResultMapper generatorResultMapper;
    final LoadResultMapper loadResultMapper;
    final RestTemplate restTemplate;
    final OctopusProperties octopusProperties;



    private final Cache<String, Object> cache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();
    @SneakyThrows
    public boolean singleLoginLimit() {
        return (boolean) cache.get("singleLoginLimit", () -> {
            MarketSettingDO marketSettingDO = marketSettingMapper.selectById(1);
            Boolean singleLoginLimit = marketSettingDO.getSingleLoginLimit();
            return Boolean.TRUE.equals(singleLoginLimit);
        });
    }

    public boolean review() {
        return marketSettingMapper.selectById(1).getIsEnteringReviewStage() && runningComp() != null
                && (CompStage.RANKING.equals(runningComp().getCompStage()) || CompStage.REVIEW.equals(runningComp().getCompStage()));
    }

    @Data
    @lombok.Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static public class Message {
        Integer code;
        String message;
    }

    @SneakyThrows
    public Map<String, Collection<MetaUnit>> assignMetaUnits(Integer roundId, List<String> traderIds, List<String> robotIds) {

        String queryString = "trader_id_list=[" + String.join(",", traderIds) + "]&" + "robot_id_list=[" + String.join(",", robotIds) + "]";

        URI uri = new URI("http", null, octopusProperties.getIp(), octopusProperties.getDjangoPort(), "/automatic_assigned/", queryString, null);
        ResponseEntity<String> responseEntity = restTemplate.exchange(uri, HttpMethod.GET, null, String.class);
        Message parse = Json.parse(responseEntity.getBody(), Message.class);
        BizEx.falseThrow(Integer.valueOf(0).equals(parse.getCode()), ErrorEnums.SYS_EX.message(queryString + Json.toJson(parse)));

        List<String> userIds = Stream.of(traderIds, robotIds).flatMap(Collection::stream).collect(Collectors.toList());
        ListMultimap<String, MetaUnit> metaUnitMap = ArrayListMultimap.create();

        LambdaQueryWrapper<GeneratorResult> eq0 = new LambdaQueryWrapper<GeneratorResult>()
                .eq(GeneratorResult::getRoundId, roundId + 1)
                .in(GeneratorResult::getTraderId, userIds);

        List<GeneratorResult> generatorResults = Collect.isEmpty(userIds) ? Collections.emptyList() : generatorResultMapper.selectList(eq0);
        generatorResults.forEach(gR -> {
            LambdaQueryWrapper<MetaUnitDO> metaUnitDOEq = new LambdaQueryWrapper<MetaUnitDO>()
                    .eq(MetaUnitDO::getSourceId, gR.getUnitId()).eq(MetaUnitDO::getUnitType, UnitType.GENERATOR);
            MetaUnitDO metaUnitDO = metaUnitDOMapper.selectOne(metaUnitDOEq);
            MetaUnit metaUnit = Convertor.INST.to(metaUnitDO);
            metaUnitMap.put(gR.getTraderId(), metaUnit);
        });

        LambdaQueryWrapper<LoadResult> eq1 = new LambdaQueryWrapper<LoadResult>()
                .eq(LoadResult::getRoundId, roundId + 1)
                .in(LoadResult::getTraderId, userIds);
        List<LoadResult> loadResults = Collect.isEmpty(userIds) ? Collections.EMPTY_LIST : loadResultMapper.selectList(eq1);
        loadResults.forEach(lR -> {
            LambdaQueryWrapper<MetaUnitDO> metaUnitDOEq = new LambdaQueryWrapper<MetaUnitDO>()
                    .eq(MetaUnitDO::getSourceId, lR.getLoadId()).eq(MetaUnitDO::getUnitType, UnitType.LOAD);
            MetaUnitDO metaUnitDO = metaUnitDOMapper.selectOne(metaUnitDOEq);
            MetaUnit metaUnit = Convertor.INST.to(metaUnitDO);
            metaUnitMap.put(lR.getTraderId(), metaUnit);
        });


        return metaUnitMap.asMap();
    }

    public MetaUnit getMetaUnitById(Long id) {
        MetaUnitDO metaUnitDO = metaUnitDOMapper.selectById(id);
        return Convertor.INST.to(metaUnitDO);
    }


    public List<Bid> listBids(BidQuery q) {
        LambdaQueryWrapper<BidDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(q.getCompId() != null, BidDO::getCompId, q.getCompId());
        queryWrapper.eq(!Kit.isBlank(q.getUserId()), BidDO::getUserId, q.getUserId());
        if (q.getUnitIds() != null) {
            if (Collect.isEmpty(q.getUnitIds())) {
                return new ArrayList<>();
            }
            queryWrapper.in(BidDO::getUnitId, q.getUnitIds());

        }
        queryWrapper.eq(q.getRoundId() != null, BidDO::getRoundId, q.getRoundId());
        queryWrapper.eq(q.getProvince() != null, BidDO::getProvince, Kit.op(q.getProvince()).orElse(null));
        queryWrapper.eq(q.getDirection() != null, BidDO::getDirection, Kit.op(q.getDirection()).orElse(null));
        queryWrapper.eq(q.getTradeStage() != null, BidDO::getTradeStage, Kit.op(q.getTradeStage()).orElse(null));
        queryWrapper.eq(q.getBidStatus() != null, BidDO::getBidStatus, Kit.op(q.getBidStatus()).orElse(null));
        queryWrapper.eq(q.getTimeFrame() != null, BidDO::getTimeFrame, Kit.op(q.getTimeFrame()).orElse(null));
        List<BidDO> bidDOs = bidDOMapper.selectList(queryWrapper);
        return Collect.transfer(bidDOs, Convertor.INST::to);
    }

    public Bid getByBidId(Long bidId) {
        BidDO bidDO = bidDOMapper.selectById(bidId);
        return Convertor.INST.to(bidDO);
    }


    public List<Unit> listUnits(Long compId, Integer roundId, String userId) {
        LambdaQueryWrapper<UnitDO> queryWrapper = new LambdaQueryWrapper<UnitDO>()
                .eq(compId != null, UnitDO::getCompId, compId)
                .eq(roundId != null, UnitDO::getRoundId, roundId)
                .eq(StringUtils.isNotBlank(userId), UnitDO::getUserId, userId);
        List<UnitDO> unitDOs = unitDOMapper.selectList(queryWrapper);
        return Collect.transfer(unitDOs, UnitAdapter.Convertor.INST::to);
    }

    public void coverBids(List<Bid> bids) {
        List<BidDO> bidDOs = Collect.transfer(bids, Convertor.INST::to);
        BidDO bidDO = bidDOs.get(0);
        LambdaQueryWrapper<BidDO> queryWrapper = new LambdaQueryWrapper<BidDO>().eq(BidDO::getCompId, bidDO.getCompId())
                .eq(BidDO::getRoundId, bidDO.getRoundId())
                .eq(BidDO::getTradeStage, bidDO.getTradeStage())
                .eq(BidDO::getUnitId, bidDO.getUnitId());
        bidDOMapper.selectList(queryWrapper).forEach(bidDOMapper::deleteById);
        bidDOs.forEach(bidDOMapper::insert);
    }

    public void insertBid(Bid bid) {
        BidDO bidDO = Convertor.INST.to(bid);
        bidDOMapper.insert(bidDO);
    }

    public void updateBids(List<Bid> bids) {
        List<BidDO> bidDOs = Collect.transfer(bids, Convertor.INST::to);
        bidDOs.forEach(bidDOMapper::updateById);
    }

    public void record(IntraQuotationDO intraQuotationDO, IntraInstantDO intraInstantDO) {
        if (intraQuotationDO != null) {
            intraQuotationDOMapper.insert(intraQuotationDO);
        }
        LambdaQueryWrapper<IntraInstantDO> eq = new LambdaQueryWrapper<IntraInstantDO>()
                .eq(IntraInstantDO::getStageId, intraInstantDO.getStageId())
                .eq(IntraInstantDO::getProvince, intraInstantDO.getProvince())
                .eq(IntraInstantDO::getTimeFrame, intraInstantDO.getTimeFrame());
        if (intraInstantDOMapper.selectOne(eq) == null) {
            intraInstantDOMapper.insert(intraInstantDO);
        } else {
            intraInstantDOMapper.update(intraInstantDO, eq);
        }
    }



    public void recordIntraQuotationDO(IntraQuotationDO intraQuotationDO) {
        intraQuotationDOMapper.insert(intraQuotationDO);
    }

    final IntraCostMapper intraCostMapper;

    public Double cost(Long unitId, Double quantity, Integer roundId) {
        Unit unit = domainTunnel.getByAggregateId(Unit.class, unitId);
        if (unit.getMetaUnit().getGeneratorType() == GeneratorType.CLASSIC) {
            LambdaQueryWrapper<IntraCost> eq = new LambdaQueryWrapper<IntraCost>()
                    .eq(IntraCost::getUnitId, unit.getMetaUnit().getSourceId()).eq(IntraCost::getRoundId, roundId + 1);
            IntraCost intraCost = intraCostMapper.selectOne(eq);
            return 2 * intraCost.getCostQuadraticCoe() * quantity + intraCost.getCostPrimaryCoe();
        } else if (unit.getMetaUnit().getGeneratorType() == GeneratorType.RENEWABLE) {
            LambdaQueryWrapper<SystemReleaseParametersDO> eq = new LambdaQueryWrapper<SystemReleaseParametersDO>()
                    .eq(SystemReleaseParametersDO::getProv, unit.getMetaUnit().getProvince().getDbCode())
                    .eq(SystemReleaseParametersDO::getPrd, 1)
                    .eq(SystemReleaseParametersDO::getRoundId, roundId + 1);
            return systemReleaseParametersDOMapper.selectOne(eq).getRenewableGovernmentSubsidy();
        } else {
            throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
        }
    }

    final SystemReleaseParametersDOMapper systemReleaseParametersDOMapper;

    public Double cost(Long unitId, Double startQuantity, Double endQuantity, Integer roundId) {
        Unit unit = domainTunnel.getByAggregateId(Unit.class, unitId);
        if (unit.getMetaUnit().getGeneratorType() == GeneratorType.CLASSIC) {
            return (cost(unitId, startQuantity, roundId) + cost(unitId, endQuantity, roundId)) / 2;
        } else if (unit.getMetaUnit().getGeneratorType() == GeneratorType.RENEWABLE) {
            LambdaQueryWrapper<SystemReleaseParametersDO> eq = new LambdaQueryWrapper<SystemReleaseParametersDO>()
                    .eq(SystemReleaseParametersDO::getProv, unit.getMetaUnit().getProvince().getDbCode())
                    .eq(SystemReleaseParametersDO::getPrd, 1)
                    .eq(SystemReleaseParametersDO::getRoundId, roundId + 1);
            return systemReleaseParametersDOMapper.selectOne(eq).getRenewableGovernmentSubsidy();
        } else {
            throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
        }
    }

    public void recordIntraInstantDO(IntraInstantDO intraInstantDO) {
        LambdaQueryWrapper<IntraInstantDO> eq = new LambdaQueryWrapper<IntraInstantDO>()
                .eq(IntraInstantDO::getStageId, intraInstantDO.getStageId())
                .eq(IntraInstantDO::getProvince, intraInstantDO.getProvince())
                .eq(IntraInstantDO::getTimeFrame, intraInstantDO.getTimeFrame());
        if (intraInstantDOMapper.selectOne(eq) == null) {
            intraInstantDOMapper.insert(intraInstantDO);
        } else {
            intraInstantDOMapper.update(intraInstantDO, eq);
        }
    }


    public Comp runningComp() {
        LambdaQueryWrapper<CompDO> queryWrapper = new LambdaQueryWrapper<CompDO>().orderByDesc(CompDO::getGmtCreate).last("LIMIT 1");
        CompDO compDO = compDOMapper.selectOne(queryWrapper);
        if (compDO == null) {
            return null;
        }
        return domainTunnel.getByAggregateIdOptional(Comp.class, compDO.getCompId()).orElse(null);
    }

    public GridLimit priceLimit(UnitType unitType) {
        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(1);
        if (unitType == UnitType.LOAD) {
            return GridLimit.builder().low(marketSettingDO.getBidPriceFloor()).high(marketSettingDO.getBidPriceCap()).build();
        } else if (unitType == UnitType.GENERATOR) {
            return GridLimit.builder().low(marketSettingDO.getOfferPriceFloor()).high(marketSettingDO.getOfferPriceCap()).build();
        } else {
            throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
        }
    }

    public Map<UnitType, GridLimit> priceLimits() {
        MarketSettingDO marketSettingDO = marketSettingMapper.selectById(1);
        GridLimit loadPriceLimit = GridLimit.builder().low(marketSettingDO.getBidPriceFloor()).high(marketSettingDO.getBidPriceCap()).build();
        GridLimit generatorPriceLimit = GridLimit.builder().low(marketSettingDO.getOfferPriceFloor()).high(marketSettingDO.getOfferPriceCap()).build();
        return Collect.asMap(UnitType.LOAD, loadPriceLimit, UnitType.GENERATOR, generatorPriceLimit);
    }

    public GridLimit transLimit(StageId stageId, TimeFrame timeFrame) {
        Map<TradeStage, Map<TimeFrame, GridLimit>> prepare = prepare(stageId.getRoundId());
        GridLimit originalTransLimit = prepare.get(stageId.getTradeStage()).get(timeFrame);
        if (stageId.getTradeStage() == TradeStage.AN_INTER) {
            return originalTransLimit;
        } else if (stageId.getTradeStage() == TradeStage.MO_INTER) {
            stageId = StageId.parse(stageId.toString());
            stageId.setTradeStage(TradeStage.AN_INTER);
            LambdaQueryWrapper<ClearanceDO> eq = new LambdaQueryWrapper<ClearanceDO>()
                    .eq(ClearanceDO::getStageId, stageId.toString())
                    .eq(ClearanceDO::getTimeFrame, timeFrame)
                    ;
            ClearanceDO clearanceDO = clearanceDOMapper.selectOne(eq);
            InterClearance interClearance = Json.parse(clearanceDO.getClearance(), InterClearance.class);
            double dealQuantity = interClearance.getMarketQuantity() + interClearance.getNonMarketQuantity();
            return GridLimit.builder()
                    .low(originalTransLimit.getLow() - dealQuantity)
                    .high(originalTransLimit.getHigh() - dealQuantity)
                    .build();
        } else {
            throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
        }
    }

    public Map<TradeStage, Map<TimeFrame, GridLimit>> prepare(Integer roundId) {
        LambdaQueryWrapper<TransLimitDO> eq = new LambdaQueryWrapper<TransLimitDO>().eq(TransLimitDO::getRoundId, roundId + 1);
        List<TransLimitDO> transLimitDOs = transLimitDOMapper.selectList(eq);
        Map<TradeStage, Map<TimeFrame, GridLimit>> marketTypeTransLimit = new HashMap<>();

        Map<TimeFrame, GridLimit> transLimit = new HashMap<>();
        for (TransLimitDO limitDO : transLimitDOs) {
            TimeFrame timeFrame = Kit.enumOfMightEx(TimeFrame::getDbCode, limitDO.getPfvPrd());
            GridLimit gridLimit = GridLimit.builder()
                    .low(limitDO.getMinAnnualReceivingMw())
                    .high(limitDO.getMaxAnnualReceivingMw())
                    .build();
            transLimit.put(timeFrame, gridLimit);
        }
        marketTypeTransLimit.put(TradeStage.AN_INTER, transLimit);

        transLimit = new HashMap<>();
        for (TransLimitDO limitDO : transLimitDOs) {
            TimeFrame timeFrame = Kit.enumOfMightEx(TimeFrame::getDbCode, limitDO.getPfvPrd());
            GridLimit gridLimit = GridLimit.builder()
                    .low(limitDO.getMinMonthlyReceivingMw())
                    .high(limitDO.getMaxMonthlyReceivingMw())
                    .build();
            transLimit.put(timeFrame, gridLimit);
        }
        marketTypeTransLimit.put(TradeStage.MO_INTER, transLimit);

        return marketTypeTransLimit;
    }


    public void persistInterClearance(InterClearance interClearance) {
        StageId stageId = interClearance.getStageId();
        ClearanceDO clearanceDO = ClearanceDO.builder().stageId(stageId.toString())
                .timeFrame(interClearance.getTimeFrame())
                .clearance(Json.toJson(interClearance))
                .build();
        clearanceDOMapper.insert(clearanceDO);


        // 写回数据库给现货那边使用,
        List<Integer> prds = interClearance.getTimeFrame().getPrds();
        prds.forEach(prd -> {

            // 回写tie_line_power表
            LambdaQueryWrapper<TieLinePowerDO> eq0 = new LambdaQueryWrapper<TieLinePowerDO>()
                    .eq(TieLinePowerDO::getRoundId, stageId.getRoundId() + 1)
                    .eq(TieLinePowerDO::getPrd, prd);
            TieLinePowerDO tieLinePowerDO = tieLinePowerDOMapper.selectOne(eq0);
            if (stageId.getTradeStage() == TradeStage.AN_INTER) {
                tieLinePowerDO.setAnnualMarketTielinePower(interClearance.getMarketQuantity());
                tieLinePowerDO.setAnnualNonmarketTielinePower(interClearance.getNonMarketQuantity());
                tieLinePowerDO.setAnnualTielinePower(interClearance.getMarketQuantity() + interClearance.getNonMarketQuantity());
            } else if (stageId.getTradeStage() == TradeStage.MO_INTER){
                tieLinePowerDO.setMonthlyMarketTielinePower(interClearance.getMarketQuantity());
                tieLinePowerDO.setMonthlyNonmarketTielinePower(interClearance.getNonMarketQuantity());
                tieLinePowerDO.setMonthlyTielinePower(interClearance.getMarketQuantity() + interClearance.getNonMarketQuantity());
            } else {
                throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
            }
            tieLinePowerDOMapper.updateById(tieLinePowerDO);

            LambdaQueryWrapper<StackDiagramDO> eq1 = new LambdaQueryWrapper<StackDiagramDO>()
                    .eq(StackDiagramDO::getRoundId, stageId.getRoundId() + 1)
                    .eq(StackDiagramDO::getPrd, prd);
            List<StackDiagramDO> stackDiagramDOS = stackDiagramDOMapper.selectList(eq1);
            if (stageId.getTradeStage() == TradeStage.AN_INTER) {
                stackDiagramDOS.forEach(stackDiagramDO -> {
                   stackDiagramDO.setIntraprovincialAnnualTielinePower(interClearance.getMarketQuantity() + interClearance.getNonMarketQuantity());
                    Double maxMonthlyReceivingMw = stackDiagramDO.getMaxMonthlyReceivingMw();
                    double max = Math.max(maxMonthlyReceivingMw, interClearance.getMarketQuantity() + interClearance.getNonMarketQuantity());
                    stackDiagramDO.setMonthlyReceivingForecastMw(max);
                });
            } else if (stageId.getTradeStage() == TradeStage.MO_INTER){
                for (StackDiagramDO stackDiagramDO : stackDiagramDOS) {
                    double v = interClearance.getMarketQuantity() + interClearance.getNonMarketQuantity() + stackDiagramDO.getIntraprovincialAnnualTielinePower();
                    stackDiagramDO.setIntraprovincialMonthlyTielinePower(v);
                    Double daReceivingTarget = stackDiagramDO.getDaReceivingTarget();
                    double max = Math.max(daReceivingTarget, v);
                    stackDiagramDO.setDaReceivingForecastMw(max);
                }
            } else {
                throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
            }
            stackDiagramDOS.forEach(stackDiagramDOMapper::updateById);
        });




    }

    public void writeBackDbRoundId(Integer roundId) {
        MarketSettingDO marketSettingDO = new MarketSettingDO();
        marketSettingDO.setMarketSettingId(1);
        marketSettingDO.setRoundId(roundId + 1);
        marketSettingMapper.updateById(marketSettingDO);
    }

    @Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    public interface Convertor extends BasicConvertor {

        Convertor INST = Mappers.getMapper(Convertor.class);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        MetaUnit to(MetaUnitDO metaUnitDO);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        Bid to(BidDO bidDO);

        @BeanMapping(builder = @Builder(disableBuilder = true))
        BidDO to(Bid bid);

    }

}
