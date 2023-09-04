package com.bilanee.octopus.adapter.facade;

import com.bilanee.octopus.adapter.facade.vo.CompVO;
import com.bilanee.octopus.adapter.facade.vo.InterClearVO;
import com.bilanee.octopus.adapter.tunnel.BidQuery;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.*;
import com.bilanee.octopus.basic.enums.Direction;
import com.bilanee.octopus.basic.enums.TimeFrame;
import com.bilanee.octopus.demo.Section;
import com.bilanee.octopus.domain.Comp;
import com.bilanee.octopus.domain.Unit;
import com.bilanee.octopus.infrastructure.mapper.CompDOMapper;
import com.bilanee.octopus.infrastructure.mapper.UnitDOMapper;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.domain.support.base.DomainTunnel;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequestMapping("/api/comp")
public class CompFacade {

    final Tunnel tunnel;
    final CompDOMapper compDOMapper;
    final UnitDOMapper unitDOMapper;
    final DomainTunnel domainTunnel;


    /**
     * 当前运行竞赛查看
     * @return 当前运行竞赛概况
     */
    @GetMapping("/runningCompVO")
    public Result<CompVO> runningCompVO() {
        Comp comp = tunnel.runningComp();
        return Result.success(Convertor.INST.to(comp));
    }


    /**
     * 省间出清结果
     * @return 省间出清结果
     */
    @GetMapping("/interClearVO")
    public Result<List<InterClearVO>> interClearVO(String stageId) {
        InterClearVO interClearVO = InterClearVO.builder().build();
        StageId parsedStageId = StageId.parse(stageId);

        BidQuery bidQuery = BidQuery.builder().roundId(parsedStageId.getRoundId()).tradeStage(parsedStageId.getTradeStage()).build();
        List<Bid> bids = tunnel.listBids(bidQuery);
        List<Long> unitIds = bids.stream().map(Bid::getUnitId).collect(Collectors.toList());
        List<UnitVO> unitVOs = unitIds.stream().map(unitId -> domainTunnel.getByAggregateId(Unit.class, unitId))
                .map(unit -> new UnitVO(unit.getUnitId(), unit.getMetaUnit().getName())).collect(Collectors.toList());
        tunnel.listBids(bidQuery).stream().collect(Collect.listMultiMap(Bid::getTimeFrame)).asMap().entrySet().stream().map(e -> {
            TimeFrame timeFrame = e.getKey();
            Collection<Bid> bs = e.getValue();
            List<Bid> sellBids = bs.stream().filter(bid -> bid.getDirection() == Direction.SELL).collect(Collectors.toList());
            List<Bid> buyBids = bs.stream().filter(bid -> bid.getDirection() == Direction.BUY).collect(Collectors.toList());
            Double sellDeclaredQuantity = sellBids.stream().map(Bid::getQuantity).reduce(0D, Double::sum);
            Double buyDeclaredQuantity = buyBids.stream().map(Bid::getQuantity).reduce(0D, Double::sum);
            List<Deal> buyDeals = sellBids.stream().flatMap(bid -> bid.getDeals().stream()).collect(Collectors.toList());
            Double dealQuantity = buyDeals.stream().map(Deal::getQuantity).reduce(0D, Double::sum);
            Double dealPrice = buyDeals.get(0).getPrice();
            GridLimit transLimit = tunnel.transLimit(parsedStageId, timeFrame);
            List<Section> buildSections = buildSections(buyBids, Comparator.comparing(Bid::getPrice).reversed());
            List<Section> sellSections = buildSections(sellBids, Comparator.comparing(Bid::getPrice));

        })
        return Result.success(null);
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

        @AfterMapping
        default void after(Comp comp, @MappingTarget CompVO compVO) {
            StageId stageId = StageId.builder().compId(compVO.getCompId())
                    .compStage(comp.getCompStage())
                    .roundId(comp.getRoundId())
                    .tradeStage(comp.getTradeStage())
                    .marketStatus(comp.getMarketStatus())
                    .build();
            compVO.setStageId(stageId);
        }

    }





}
