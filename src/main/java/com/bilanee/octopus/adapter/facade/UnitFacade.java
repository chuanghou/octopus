package com.bilanee.octopus.adapter.facade;


import com.bilanee.octopus.adapter.facade.po.BidPO;
import com.bilanee.octopus.adapter.facade.po.ClBidsPO;
import com.bilanee.octopus.adapter.facade.po.RealtimeBidPO;
import com.bilanee.octopus.adapter.tunnel.BidQuery;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.*;
import com.bilanee.octopus.basic.enums.TimeFrame;
import com.bilanee.octopus.domain.Unit;
import com.bilanee.octopus.domain.UnitCmd;
import com.bilanee.octopus.infrastructure.mapper.UnitDOMapper;
import com.google.common.collect.ListMultimap;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.domain.support.base.DomainTunnel;
import com.stellariver.milky.domain.support.command.CommandBus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequestMapping("/api/unit")
public class UnitFacade {

    final UnitDOMapper unitDOMapper;
    final CompFacade compFacade;
    final Tunnel tunnel;
    final DomainTunnel domainTunnel;

    @GetMapping("listClUnitVOs")
    public Result<List<ClUnitVO>> listClUnitVOs(String stageId, @RequestHeader String token) {

        String userId = TokenUtils.getUserId(token);
        StageId parsedStageId = StageId.parse(stageId);

        BidQuery bidQuery = BidQuery.builder().compId(parsedStageId.getCompId())
                .userId(userId)
                .roundId(parsedStageId.getRoundId())
                .tradeStage(parsedStageId.getTradeStage())
                .build();

        ListMultimap<Long, Bid> groupedByUnitId = tunnel.listBids(bidQuery).stream().collect(Collect.listMultiMap(Bid::getUnitId));

        List<ClUnitVO> clUnitVOs = groupedByUnitId.asMap().entrySet().stream().map(e -> {
            Long uId = e.getKey();
            Collection<Bid> bs = e.getValue();
            Unit unit = domainTunnel.getByAggregateId(Unit.class, uId);
            ListMultimap<TimeFrame, Bid> groupedByTimeFrame = bs.stream().collect(Collect.listMultiMap(Bid::getTimeFrame));
            List<ClBidVO> clBidVOs = groupedByTimeFrame.asMap().entrySet().stream().map(ee -> {
                TimeFrame timeFrame = ee.getKey();
                Collection<Bid> bids = ee.getValue();
                Double capacity = unit.getMetaUnit().getCapacity().get(timeFrame).get(unit.getMetaUnit().getUnitType().generalDirection());
                List<BalanceVO> balanceVOs = unit.getBalance().get(timeFrame).entrySet().stream()
                        .map(eee -> new BalanceVO(eee.getKey(), eee.getValue())).collect(Collectors.toList());
                return ClBidVO.builder().timeFrame(timeFrame)
                        .capacity(capacity)
                        .bidVOs(Collect.transfer(bids, Convertor.INST::to))
                        .balanceVOs(balanceVOs)
                        .build();
            }).collect(Collectors.toList());
            return ClUnitVO.builder().unitId(unit.getUnitId()).unitName(unit.getMetaUnit().getName()).clBidVOs(clBidVOs).build();

        }).collect(Collectors.toList());

        return Result.success(clUnitVOs);
    }


    @PostMapping("submitClBidsPO")
    public Result<Void> submitClBidsPO(ClBidsPO clBidsPO) {

        StageId pStageId = StageId.parse(clBidsPO.getStageId());
        StageId cStageId = tunnel.runningComp().getStageId();
        BizEx.falseThrow(pStageId.equals(cStageId), ErrorEnums.PARAM_FORMAT_WRONG.message("已经进入下一阶段不可报单"));

        List<BidPO> bidPOs = clBidsPO.getBidPOs();
        UnitCmd.CentralizedBids command = UnitCmd.CentralizedBids.builder().stageId(pStageId)
                .bids(Collect.transfer(bidPOs, Convertor.INST::to))
                .build();
        CommandBus.accept(command, new HashMap<>());
        return Result.success();
    }

    @PostMapping("submitRealtimeBidPO")
    public Result<Void> submitRealtimeBidPO(RealtimeBidPO realtimeBidPO) {

        return null;
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
