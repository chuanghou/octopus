package com.bilanee.octopus.adapter.facade;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilanee.octopus.adapter.facade.vo.*;
import com.bilanee.octopus.adapter.repository.UnitAdapter;
import com.bilanee.octopus.adapter.tunnel.BidQuery;
import com.bilanee.octopus.adapter.tunnel.InterClearance;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.*;
import com.bilanee.octopus.basic.enums.CompStage;
import com.bilanee.octopus.basic.enums.Direction;
import com.bilanee.octopus.basic.enums.TimeFrame;
import com.bilanee.octopus.domain.Comp;
import com.bilanee.octopus.domain.IntraSymbol;
import com.bilanee.octopus.domain.Unit;
import com.bilanee.octopus.infrastructure.entity.ClearanceDO;
import com.bilanee.octopus.infrastructure.entity.UnitDO;
import com.bilanee.octopus.infrastructure.mapper.ClearanceDOMapper;
import com.bilanee.octopus.infrastructure.mapper.CompDOMapper;
import com.bilanee.octopus.infrastructure.mapper.UnitDOMapper;
import com.google.common.collect.ListMultimap;
import com.stellariver.milky.common.base.ExceptionType;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.common.tool.util.Json;
import com.stellariver.milky.domain.support.base.DomainTunnel;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import java.util.*;
import java.util.stream.Collectors;

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
            interClearVOs.get(timeFrame).setUnitDealVOS(unitDealVOS);
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

            Map<Long, Collection<Bid>> bidMap = bids.stream().collect(Collect.listMultiMap(Bid::getUnitId)).asMap();
            List<UnitDealVO> unitDealVOs = bidMap.entrySet().stream().map(ee -> {
                Long unitId = ee.getKey();
                Collection<Bid> unitBids = ee.getValue();
                Double unitTotalVolume = deals.stream().map(deal -> deal.getQuantity() * deal.getPrice()).reduce(0D, Double::sum);
                Double unitTotalQuantity = deals.stream().map(Deal::getQuantity).reduce(0D, Double::sum);
                return UnitDealVO.builder()
                        .unitId(unitId)
                        .averagePrice(totalVolume / unitTotalQuantity)
                        .totalQuantity(unitTotalQuantity)
                        .deals(deals)
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
