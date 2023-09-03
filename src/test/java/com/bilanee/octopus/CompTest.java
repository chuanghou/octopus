package com.bilanee.octopus;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilanee.octopus.adapter.facade.CompFacade;
import com.bilanee.octopus.adapter.facade.ManageFacade;
import com.bilanee.octopus.adapter.facade.UnitFacade;
import com.bilanee.octopus.adapter.facade.po.BidPO;
import com.bilanee.octopus.adapter.facade.po.ClBidsPO;
import com.bilanee.octopus.adapter.facade.po.CompCreatePO;
import com.bilanee.octopus.adapter.facade.vo.CompVO;
import com.bilanee.octopus.adapter.repository.UnitAdapter;
import com.bilanee.octopus.adapter.tunnel.BidQuery;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.*;
import com.bilanee.octopus.basic.enums.TimeFrame;
import com.bilanee.octopus.basic.enums.CompStage;
import com.bilanee.octopus.basic.enums.MarketStatus;
import com.bilanee.octopus.basic.enums.TradeStage;
import com.bilanee.octopus.domain.CompCmd;
import com.bilanee.octopus.domain.Unit;
import com.bilanee.octopus.infrastructure.entity.UnitDO;
import com.bilanee.octopus.infrastructure.mapper.MetaUnitDOMapper;
import com.bilanee.octopus.infrastructure.mapper.UnitDOMapper;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.tool.common.Clock;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.domain.support.command.CommandBus;
import lombok.CustomLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CustomLog
@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CompTest {

    @Autowired
    ManageFacade manageFacade;

    @Autowired
    CompFacade compFacade;

    @Autowired
    MetaUnitDOMapper metaUnitDOMapper;

    @Autowired
    UnitDOMapper unitDOMapper;

    @Autowired
    UnitFacade unitFacade;

    @Autowired
    Tunnel tunnel;

    @Test
    public void testDelay() throws InterruptedException {
        Map<TradeStage, Integer> marketStageBidLengths = new HashMap<>();
        Map<TradeStage, Integer> marketStageClearLengths = new HashMap<>();
        for (TradeStage marketStage : TradeStage.marketStages()) {
            marketStageBidLengths.put(marketStage, 1);
            marketStageClearLengths.put(marketStage, 1);
        }

        CompCreatePO compCreatePO = CompCreatePO.builder()
                .compInitLength(1000)
                .quitCompeteLength(1)
                .quitResultLength(1)
                .marketStageBidLengths(marketStageBidLengths)
                .marketStageClearLengths(marketStageClearLengths)
                .tradeResultLength(1)
                .userIds(Arrays.asList("0", "1"))
                .build();

        manageFacade.createComp(compCreatePO);
        Result<CompVO> compVOResult = compFacade.runningComp();
        Assertions.assertTrue(compVOResult.getSuccess());
        Long compId = compVOResult.getData().getCompId();
        LambdaQueryWrapper<UnitDO> queryWrapper = new LambdaQueryWrapper<UnitDO>().eq(UnitDO::getCompId, compId);
        List<UnitDO> unitDOS = unitDOMapper.selectList(queryWrapper);
        Assertions.assertEquals(unitDOS.size(), 2 * 3 * 4);

        CompCmd.Step command = CompCmd.Step.builder()
                .compId(compId)
                .compStage(CompStage.TRADE)
                .roundId(0)
                .tradeStage(TradeStage.AN_INTER)
                .marketStatus(MarketStatus.BID)
                .endingTimeStamp(Clock.currentTimeMillis() + 1000_000)
                .build();
        CommandBus.accept(command, new HashMap<>());
        StageId stageId = tunnel.runningComp().getStageId();
        Unit unit = UnitAdapter.Convertor.INST.to(unitDOS.get(0));

        BidPO bidPO0 = BidPO.builder().unitId(unit.getUnitId()).direction(unit.getMetaUnit().getUnitType().generalDirection())
                .timeFrame(TimeFrame.PEAK).price(100D).quantity(100D).build();
        List<BidPO> list0 = Collect.asList(bidPO0, bidPO0, bidPO0);
        BidPO bidPO1 = BidPO.builder().unitId(unit.getUnitId()).direction(unit.getMetaUnit().getUnitType().generalDirection())
                .timeFrame(TimeFrame.FLAT).price(100D).quantity(100D).build();
        List<BidPO> list1 = Collect.asList(bidPO1, bidPO1, bidPO1);
        BidPO bidPO2 = BidPO.builder().unitId(unit.getUnitId()).direction(unit.getMetaUnit().getUnitType().generalDirection())
                .timeFrame(TimeFrame.VALLEY).price(100D).quantity(100D).build();
        List<BidPO> list2 = Collect.asList(bidPO2, bidPO2, bidPO2);
        List<BidPO> bidPOs = Stream.of(list0, list1, list2).flatMap(Collection::stream).collect(Collectors.toList());
        ClBidsPO clBidsPO = ClBidsPO.builder().stageId(stageId.toString()).bidPOs(bidPOs).build();
        Result<Void> result = unitFacade.submitClBidsPO(clBidsPO);
        Assertions.assertTrue(result.getSuccess());
        BidQuery bidQuery = BidQuery.builder().unitId(unit.getUnitId()).build();
        List<Bid> bids = tunnel.listBids(bidQuery);
        Assertions.assertEquals(bids.size(), 9);
        long now = Clock.currentTimeMillis();
        bids.forEach(bid -> Assertions.assertTrue(bid.getDeclareTimeStamp() < now));
        result = unitFacade.submitClBidsPO(clBidsPO);
        Assertions.assertTrue(result.getSuccess());
        bids = tunnel.listBids(bidQuery);
        Assertions.assertEquals(bids.size(), 9);
        bids.forEach(bid -> Assertions.assertTrue(bid.getDeclareTimeStamp() > now));
        Result<List<ClUnitVO>> listResult = unitFacade.listClUnitVOs(stageId.toString(), TokenUtils.sign(unit.getUserId()));
        Assertions.assertTrue(listResult.getSuccess());
        List<ClUnitVO> data = listResult.getData();
        Assertions.assertEquals(data.size(), 1);
        ClUnitVO clUnitVO = data.get(0);
        Assertions.assertEquals(clUnitVO.getUnitId(), unit.getUnitId());
        Assertions.assertEquals(clUnitVO.getUnitName(), unit.getMetaUnit().getName());
        Assertions.assertEquals(clUnitVO.getClBidVOs().size(), 3);

    }

}
