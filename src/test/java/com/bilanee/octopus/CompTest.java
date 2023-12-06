package com.bilanee.octopus;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilanee.octopus.adapter.facade.CompFacade;
import com.bilanee.octopus.adapter.facade.ManageFacade;
import com.bilanee.octopus.adapter.facade.Segment;
import com.bilanee.octopus.adapter.facade.UnitFacade;
import com.bilanee.octopus.adapter.facade.po.*;
import com.bilanee.octopus.adapter.facade.vo.*;
import com.bilanee.octopus.adapter.repository.UnitAdapter;
import com.bilanee.octopus.adapter.tunnel.BidQuery;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.*;
import com.bilanee.octopus.basic.enums.*;
import com.bilanee.octopus.domain.Comp;
import com.bilanee.octopus.domain.Unit;
import com.bilanee.octopus.infrastructure.entity.UnitDO;
import com.bilanee.octopus.infrastructure.mapper.MarketSettingMapper;
import com.bilanee.octopus.infrastructure.mapper.MetaUnitDOMapper;
import com.bilanee.octopus.infrastructure.mapper.UnitDOMapper;
import com.stellariver.milky.common.base.Result;
import com.stellariver.milky.common.base.SysEx;
import com.stellariver.milky.common.tool.common.Clock;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.domain.support.base.DomainTunnel;
import lombok.CustomLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

    @Autowired
    DomainTunnel domainTunnel;

    @Autowired
    Comp.DelayExecutor delayExecutor;


    @Test
    public void clearTest() {
        unitFacade.calculateDaCost(75097L, 440D, 545D);
    }

    @AfterEach
    public void clear() {
        delayExecutor.removeStepCommand();
    }
    
    
    @Test
    public void testStep() throws InterruptedException {
        Map<TradeStage, Integer> marketStageBidLengths = new HashMap<>();
        Map<TradeStage, Integer> marketStageClearLengths = new HashMap<>();
        for (TradeStage marketStage : TradeStage.marketStages()) {
            marketStageBidLengths.put(marketStage, 5);
            marketStageClearLengths.put(marketStage, 5);
        }

        CompCreatePO compCreatePO = CompCreatePO.builder()
                .startTimeStamp(Clock.currentTimeMillis() + 2)
                .build();

        manageFacade.createComp(compCreatePO);
        for (int i = 0; i < 12; i++) {
            manageFacade.step();
        }


        Result<CompVO> compVOResult = compFacade.runningCompVO(TokenUtils.sign("1000"));
        Assertions.assertTrue(compVOResult.getSuccess());
        Comp comp = tunnel.runningComp();
        StageId stageId = comp.getStageId();
        Thread.sleep(3_000);
        StageId stageId1 = tunnel.runningComp().getStageId();
        boolean equals = stageId1.equals(stageId.next(comp));
        Assertions.assertTrue(equals);
        Assertions.assertEquals(stageId1.getCompStage(), CompStage.QUIT_COMPETE);

        Thread.sleep(6_000);
        StageId stageId2 = tunnel.runningComp().getStageId();
        equals = stageId2.equals(stageId1.next(comp));
        Assertions.assertTrue(equals);
        Assertions.assertEquals(stageId2.getCompStage(), CompStage.QUIT_RESULT);

        Thread.sleep(6000);
        StageId stageId3 = tunnel.runningComp().getStageId();
        equals = stageId3.equals(stageId2.next(comp));
        Assertions.assertTrue(equals);
        Assertions.assertEquals(stageId3.getCompStage(), CompStage.TRADE);
        Assertions.assertEquals(stageId3.getRoundId(), 0);
        Assertions.assertEquals(stageId3.getTradeStage(), TradeStage.AN_INTER);
        Assertions.assertEquals(stageId3.getMarketStatus(), MarketStatus.BID);
        for (int i = 0; i < 11; i++) {
            manageFacade.step();
            System.out.println(tunnel.runningComp().getStageId());
        }
        stageId = tunnel.runningComp().getStageId();
        Assertions.assertEquals(stageId.getCompStage(), CompStage.TRADE);
        Assertions.assertEquals(stageId.getRoundId(), 0);
        Assertions.assertEquals(stageId.getTradeStage(), TradeStage.END);
        Assertions.assertEquals(stageId.getMarketStatus(), MarketStatus.BID);
        Thread.sleep(6000);
        stageId = tunnel.runningComp().getStageId();
        Assertions.assertEquals(stageId.getCompStage(), CompStage.TRADE);
        Assertions.assertEquals(stageId.getRoundId(), 1);
        Assertions.assertEquals(stageId.getTradeStage(), TradeStage.AN_INTER);
        Assertions.assertEquals(stageId.getMarketStatus(), MarketStatus.BID);
        for (int i = 0; i < 12; i++) {
            manageFacade.step();
        }
        stageId = tunnel.runningComp().getStageId();
        Assertions.assertEquals(stageId.getCompStage(), CompStage.TRADE);
        Assertions.assertEquals(stageId.getRoundId(), 2);
        Assertions.assertEquals(stageId.getTradeStage(), TradeStage.AN_INTER);
        Assertions.assertEquals(stageId.getMarketStatus(), MarketStatus.BID);

        for (int i = 0; i < 11; i++) {
            manageFacade.step();
        }
        stageId = tunnel.runningComp().getStageId();
        Assertions.assertEquals(stageId.getCompStage(), CompStage.TRADE);
        Assertions.assertEquals(stageId.getRoundId(), 2);
        Assertions.assertEquals(stageId.getTradeStage(), TradeStage.END);
        Assertions.assertEquals(stageId.getMarketStatus(), MarketStatus.BID);
        Thread.sleep(6000);
        Comp comp1 = tunnel.runningComp();
        Assertions.assertNull(comp1.getEndingTimeStamp());
        stageId = tunnel.runningComp().getStageId();
        Assertions.assertEquals(stageId.getCompStage(), CompStage.RANKING);
        Assertions.assertNull(stageId.getRoundId());
        Assertions.assertNull(stageId.getTradeStage());
        Assertions.assertNull(stageId.getMarketStatus());
        Thread.sleep(6000);
        stageId = tunnel.runningComp().getStageId();
        Assertions.assertEquals(stageId.getCompStage(), CompStage.RANKING);
        Assertions.assertNull(stageId.getRoundId());
        Assertions.assertNull(stageId.getTradeStage());
        Assertions.assertNull(stageId.getMarketStatus());
        Throwable throwableBackUp = null;
        Result<Void> step = manageFacade.step();
        Assertions.assertFalse(step.getSuccess());
        Assertions.assertEquals(step.getMessage(), "已经到了最后阶段");

    }

    @Test
    public void testDelay() throws InterruptedException {
        Map<TradeStage, Integer> marketStageBidLengths = new HashMap<>();
        Map<TradeStage, Integer> marketStageClearLengths = new HashMap<>();
        for (TradeStage marketStage : TradeStage.marketStages()) {
            marketStageBidLengths.put(marketStage, 1);
            marketStageClearLengths.put(marketStage, 1);
        }

        CompCreatePO compCreatePO = CompCreatePO.builder()
                .startTimeStamp(Clock.currentTimeMillis() + 1000)
                .build();

        manageFacade.createComp(compCreatePO);
        Result<CompVO> compVOResult = compFacade.runningCompVO(TokenUtils.sign("0"));
        Assertions.assertTrue(compVOResult.getSuccess());
        Long compId = compVOResult.getData().getCompId();
        LambdaQueryWrapper<UnitDO> queryWrapper = new LambdaQueryWrapper<UnitDO>().eq(UnitDO::getCompId, compId);
        List<UnitDO> unitDOS = unitDOMapper.selectList(queryWrapper);
        Assertions.assertEquals(unitDOS.size(), 2 * 3 * 4);
        manageFacade.step();
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
        InterBidsPO interBidsPO = InterBidsPO.builder().stageId(stageId.toString()).bidPOs(bidPOs).build();
        Result<Void> result = unitFacade.submitInterBidsPO(interBidsPO);
        Assertions.assertTrue(result.getSuccess());
        BidQuery bidQuery = BidQuery.builder().unitIds(Collect.asSet(unit.getUnitId())).build();
        List<Bid> bids = tunnel.listBids(bidQuery);
        Assertions.assertEquals(bids.size(), 9);
        long now = Clock.currentTimeMillis();
        bids.forEach(bid -> Assertions.assertTrue(bid.getDeclareTimeStamp() < now));
        Thread.sleep(10);
        result = unitFacade.submitInterBidsPO(interBidsPO);
        Assertions.assertTrue(result.getSuccess());
        bids = tunnel.listBids(bidQuery);
        Assertions.assertEquals(bids.size(), 9);
        bids.forEach(bid -> Assertions.assertTrue(bid.getDeclareTimeStamp() > now));
        Result<List<UnitInterBidVO>> listResult = unitFacade.listInterBidsVOs(stageId.toString(), TokenUtils.sign(unit.getUserId()));
        Assertions.assertTrue(listResult.getSuccess());
        List<UnitInterBidVO> data = listResult.getData();
        Assertions.assertEquals(data.size(), 2);
        UnitInterBidVO interBidsVO = data.stream().filter(i -> i.getUnitId().equals(unit.getUnitId())).findFirst().orElseThrow(SysEx::unreachable);
        Assertions.assertEquals(interBidsVO.getUnitId(), unit.getUnitId());
        Assertions.assertEquals(interBidsVO.getUnitName(), unit.getMetaUnit().getName());
        Assertions.assertEquals(interBidsVO.getInterBidVOS().size(), 3);

    }



    @Test
    public void testClearProcedure() throws InterruptedException {

        // 创建比赛
        List<UserVO> userVOs = manageFacade.listUserVOs();
        Map<TradeStage, Integer> marketStageBidLengths = new HashMap<>();
        Map<TradeStage, Integer> marketStageClearLengths = new HashMap<>();
        for (TradeStage marketStage : TradeStage.marketStages()) {
            marketStageBidLengths.put(marketStage, 100000000);
            marketStageClearLengths.put(marketStage, 10000000);
        }

        // 比赛参数
        CompCreatePO compCreatePO = CompCreatePO.builder()
                .startTimeStamp(Clock.currentTimeMillis() + 1000)
                .build();
        Result<Void> result = manageFacade.createComp(compCreatePO);
        Assertions.assertTrue(result.getSuccess());

        Comp comp = tunnel.runningComp();
        manageFacade.step();
        StageId rawStageId = tunnel.runningComp().getStageId();
        StageId stageId = StageId.builder().compId(comp.getCompId())
                .compStage(CompStage.TRADE)
                .roundId(0)
                .tradeStage(TradeStage.AN_INTER)
                .marketStatus(MarketStatus.BID)
                .build();
        Assertions.assertEquals(rawStageId, stageId);

        Result<List<UnitInterBidVO>> resultBidsVO0 = unitFacade.listInterBidsVOs(stageId.toString(), TokenUtils.sign(userVOs.get(0).getUserId()));
        Assertions.assertTrue(resultBidsVO0.getSuccess() && resultBidsVO0.getData().size() == 2);
        Result<List<UnitInterBidVO>> resultBidsVO1 = unitFacade.listInterBidsVOs(stageId.toString(), TokenUtils.sign(userVOs.get(1).getUserId()));
        Assertions.assertTrue(resultBidsVO1.getSuccess() && resultBidsVO1.getData().size() == 2);

        UnitInterBidVO interBidsVO0 = resultBidsVO0.getData().stream()
                .filter(u -> u.getProvince().equals(Province.TRANSFER) && u.getUnitType().equals(UnitType.GENERATOR))
                .findFirst().orElseThrow((SysEx::unreachable));
        Long unitId0 = interBidsVO0.getUnitId();

        UnitInterBidVO interBidsVO1 = resultBidsVO0.getData().stream()
                .filter(u -> u.getProvince().equals(Province.RECEIVER) && u.getUnitType().equals(UnitType.LOAD))
                .findFirst().orElseThrow((SysEx::unreachable));
        Long unitId1 = interBidsVO1.getUnitId();

        BidPO bidPO0 = BidPO.builder().unitId(unitId0).direction(Direction.SELL)
                .timeFrame(TimeFrame.PEAK).price(100D).quantity(100D).build();
        List<BidPO> list0 = Collect.asList(bidPO0, bidPO0, bidPO0);
        BidPO bidPO1 = BidPO.builder().unitId(unitId0).direction(Direction.SELL)
                .timeFrame(TimeFrame.FLAT).price(100D).quantity(100D).build();
        List<BidPO> list1 = Collect.asList(bidPO1, bidPO1, bidPO1);
        BidPO bidPO2 = BidPO.builder().unitId(unitId0).direction(Direction.SELL)
                .timeFrame(TimeFrame.VALLEY).price(100D).quantity(100D).build();
        List<BidPO> list2 = Collect.asList(bidPO2, bidPO2, bidPO2);
        List<BidPO> bidPO0s = Stream.of(list0, list1, list2).flatMap(Collection::stream).collect(Collectors.toList());
        InterBidsPO interBidsPO = InterBidsPO.builder().stageId(stageId.toString()).bidPOs(bidPO0s).build();
        Result<Void> result0 = unitFacade.submitInterBidsPO(interBidsPO);
        Assertions.assertTrue(result0.getSuccess());


        BidPO bidPO3 = BidPO.builder().unitId(unitId1).direction(Direction.BUY)
                .timeFrame(TimeFrame.PEAK).price(100D).quantity(100D).build();
        List<BidPO> list3 = Collect.asList(bidPO3, bidPO3, bidPO3);
        BidPO bidPO4 = BidPO.builder().unitId(unitId1).direction(Direction.BUY)
                .timeFrame(TimeFrame.FLAT).price(100D).quantity(100D).build();
        List<BidPO> list4 = Collect.asList(bidPO4, bidPO4, bidPO4);
        BidPO bidPO5 = BidPO.builder().unitId(unitId1).direction(Direction.BUY)
                .timeFrame(TimeFrame.VALLEY).price(100D).quantity(100D).build();
        List<BidPO> list5 = Collect.asList(bidPO5, bidPO5, bidPO5);
        List<BidPO> bidPO1s = Stream.of(list3, list4, list5).flatMap(Collection::stream).collect(Collectors.toList());
        InterBidsPO interBidsPO0 = InterBidsPO.builder().stageId(stageId.toString()).bidPOs(bidPO1s).build();

        Result<Void> result1 = unitFacade.submitInterBidsPO(interBidsPO0);
        Assertions.assertTrue(result1.getSuccess());

        Result<List<UnitInterBidVO>> listResult0 = unitFacade.listInterBidsVOs(stageId.toString(), TokenUtils.sign(userVOs.get(0).getUserId()));
        Assertions.assertTrue(listResult0.getSuccess());
        interBidsVO0 = listResult0.getData().stream().filter(i -> i.getUnitId().equals(unitId0)).findFirst().orElseThrow(SysEx::unreachable);
        Assertions.assertEquals(3, interBidsVO0.getInterBidVOS().size());
        interBidsVO0.getInterBidVOS().forEach(interBidVO -> Assertions.assertEquals(3, interBidVO.getBidVOs().size()));
        Result<List<UnitInterBidVO>> listResult1 = unitFacade.listInterBidsVOs(stageId.toString(), TokenUtils.sign(userVOs.get(0).getUserId()));
        Assertions.assertTrue(listResult1.getSuccess());
        Assertions.assertEquals(3, interBidsVO1.getInterBidVOS().size());
        interBidsVO1 = listResult1.getData().stream().filter(i -> i.getUnitId().equals(unitId1)).findFirst().orElseThrow(SysEx::unreachable);
        interBidsVO1.getInterBidVOS().forEach(interBidVO -> Assertions.assertEquals(3, interBidVO.getBidVOs().size()));

        manageFacade.step();

        stageId = StageId.builder().compId(comp.getCompId())
                .compStage(CompStage.TRADE)
                .roundId(0)
                .tradeStage(TradeStage.AN_INTER)
                .marketStatus(MarketStatus.CLEAR)
                .build();

        Result<List<InterClearanceVO>> clearance0 = compFacade.interClearanceVO(stageId.toString(), TokenUtils.sign(userVOs.get(0).getUserId()));
        Result<List<InterClearanceVO>> clearance1 = compFacade.interClearanceVO(stageId.toString(), TokenUtils.sign(userVOs.get(1).getUserId()));

        Assertions.assertTrue(clearance0.getSuccess());
        Assertions.assertTrue(clearance1.getSuccess());

        Unit unit0 = domainTunnel.getByAggregateId(Unit.class, unitId0);
        Unit unit1 = domainTunnel.getByAggregateId(Unit.class, unitId1);

        unit0.getBalance().forEach(((timeFrame, balance) -> {
            Double unitBalance = balance.get(Direction.SELL);
            Double capacity = unit0.getMetaUnit().getCapacity().get(timeFrame).get(Direction.SELL);
            Assertions.assertEquals(capacity - unitBalance, 300D);
        }));

        unit1.getBalance().forEach(((timeFrame, balance) -> {
            Double unitBalance = balance.get(Direction.BUY);
            Double capacity = unit1.getMetaUnit().getCapacity().get(timeFrame).get(Direction.BUY);
            Assertions.assertEquals(capacity - unitBalance, 300D);
        }));

        manageFacade.step();

        comp = tunnel.runningComp();
        Assertions.assertEquals(comp.getTradeStage(), TradeStage.AN_INTRA);

        Long generatorUnitId = unitId0;
        BidPO bidPO = BidPO.builder().unitId(generatorUnitId)
                .timeFrame(TimeFrame.PEAK).price(300D).direction(Direction.SELL).quantity(2000D).build();
        IntraBidPO intraBidPO = IntraBidPO.builder().stageId(comp.getStageId().toString()).bidPO(bidPO).build();
        Result<Void> bidResult = unitFacade.submitIntraBidPO(intraBidPO);
        Assertions.assertFalse(bidResult.getSuccess());
        Assertions.assertEquals(bidResult.getMessage(), "报单超过持仓量");

        bidPO = BidPO.builder().unitId(generatorUnitId)
                .timeFrame(TimeFrame.PEAK).price(300D).direction(Direction.BUY).quantity(100D).build();
        intraBidPO = IntraBidPO.builder().stageId(comp.getStageId().toString()).bidPO(bidPO).build();
        bidResult = unitFacade.submitIntraBidPO(intraBidPO);
        Assertions.assertFalse(bidResult.getSuccess());
        Assertions.assertEquals(bidResult.getMessage(), "省内年度报单方向错误");


        Unit generatorUnit = domainTunnel.getByAggregateId(Unit.class, generatorUnitId);
        Double unitBalance0 = generatorUnit.getBalance().get(TimeFrame.PEAK).get(Direction.SELL);
        bidPO = BidPO.builder().unitId(generatorUnitId)
                .timeFrame(TimeFrame.PEAK).price(300D).direction(Direction.SELL).quantity(100D).build();
        intraBidPO = IntraBidPO.builder().stageId(comp.getStageId().toString()).bidPO(bidPO).build();
        bidResult = unitFacade.submitIntraBidPO(intraBidPO);
        Assertions.assertTrue(bidResult.getSuccess());
        generatorUnit = domainTunnel.getByAggregateId(Unit.class, generatorUnitId);
        Assertions.assertEquals(unitBalance0 - generatorUnit.getBalance().get(TimeFrame.PEAK).get(Direction.SELL), 100D);


        List<Unit> units = tunnel.listUnits(comp.getCompId(), 0, generatorUnit.getUserId()).stream().filter(u -> {
            boolean b0 = u.getMetaUnit().getUnitType() == UnitType.LOAD;
            boolean b1 = u.getMetaUnit().getProvince() == Province.TRANSFER;
            return b0 && b1;
        }).collect(Collectors.toList());
        Assertions.assertEquals(units.size(), 1);
        Unit loadUnit = units.get(0);
        Long loadUnitId = loadUnit.getUnitId();
        bidPO = BidPO.builder().unitId(loadUnitId)
                .timeFrame(TimeFrame.PEAK).price(300D).direction(Direction.BUY).quantity(50D).build();
        intraBidPO = IntraBidPO.builder().stageId(comp.getStageId().toString()).bidPO(bidPO).build();
        bidResult = unitFacade.submitIntraBidPO(intraBidPO);
        Assertions.assertTrue(bidResult.getSuccess());

        Thread.sleep(1000);
        BidQuery bidQuery = BidQuery.builder().unitIds(Collect.asSet(generatorUnitId)).tradeStage(TradeStage.AN_INTRA).build();
        List<Bid> bids = tunnel.listBids(bidQuery);
        Assertions.assertEquals(bids.size(), 1);
        Bid bid = bids.get(0);
        Assertions.assertEquals(bid.getDeals().size(), 1);
        Deal deal = bid.getDeals().get(0);
        Assertions.assertEquals(deal.getQuantity(), 50D);
        Assertions.assertEquals(bid.getBidStatus(), BidStatus.PART_DEAL);
        Long partDealBidId = bid.getBidId();

        bidQuery = BidQuery.builder().unitIds(Collect.asSet(loadUnitId)).tradeStage(TradeStage.AN_INTRA).build();
        bids = tunnel.listBids(bidQuery);
        Assertions.assertEquals(bids.size(), 1);
        bid = bids.get(0);
        Assertions.assertEquals(bid.getDeals().size(), 1);
        deal = bid.getDeals().get(0);
        Assertions.assertEquals(deal.getQuantity(), 50D);
        Assertions.assertEquals(bid.getBidStatus(), BidStatus.COMPLETE_DEAL);

        generatorUnit = domainTunnel.getByAggregateId(Unit.class, generatorUnitId);
        unitBalance0 = generatorUnit.getBalance().get(TimeFrame.PEAK).get(Direction.SELL);
        IntraCancelPO intraCancelPO = IntraCancelPO.builder().stageId(comp.getStageId().toString()).bidId(partDealBidId).build();
        bidResult = unitFacade.submitIntraCancelPO(intraCancelPO);
        Assertions.assertTrue(bidResult.getSuccess());
        Thread.sleep(2000);
        generatorUnit = domainTunnel.getByAggregateId(Unit.class, generatorUnitId);
        Assertions.assertEquals(unitBalance0 - generatorUnit.getBalance().get(TimeFrame.PEAK).get(Direction.SELL), -50D);
        bidQuery = BidQuery.builder().unitIds(Collect.asSet(generatorUnitId)).tradeStage(TradeStage.AN_INTRA).build();
        bids = tunnel.listBids(bidQuery);
        Assertions.assertEquals(bids.size(), 1);
        bid = bids.get(0);
        Assertions.assertEquals(bid.getBidStatus(), BidStatus.CANCELLED);

        generatorUnit = domainTunnel.getByAggregateId(Unit.class, generatorUnitId);
        unitBalance0 = generatorUnit.getBalance().get(TimeFrame.PEAK).get(Direction.SELL);
        bidPO = BidPO.builder().unitId(generatorUnitId)
                .timeFrame(TimeFrame.PEAK).price(300D).direction(Direction.SELL).quantity(100D).build();
        intraBidPO = IntraBidPO.builder().stageId(comp.getStageId().toString()).bidPO(bidPO).build();
        bidResult = unitFacade.submitIntraBidPO(intraBidPO);
        Assertions.assertTrue(bidResult.getSuccess());
        generatorUnit = domainTunnel.getByAggregateId(Unit.class, generatorUnitId);
        Assertions.assertEquals(unitBalance0 - generatorUnit.getBalance().get(TimeFrame.PEAK).get(Direction.SELL), 100D);

        Result<List<IntraSymbolBidVO>> intraSymbolBidVOsResult = unitFacade
                .listIntraSymbolBidVOs(comp.getStageId().toString(), TokenUtils.sign(unit0.getUserId()));
        Assertions.assertTrue(intraSymbolBidVOsResult.getSuccess());

        manageFacade.step();

        Thread.sleep(3000);
        generatorUnit = domainTunnel.getByAggregateId(Unit.class, generatorUnitId);
        Assertions.assertEquals(unitBalance0, generatorUnit.getBalance().get(TimeFrame.PEAK).get(Direction.SELL));

        manageFacade.step();
        manageFacade.step();
        manageFacade.step();

        generatorUnit = domainTunnel.getByAggregateId(Unit.class, generatorUnitId);
        Double sellBalance = generatorUnit.getBalance().get(TimeFrame.PEAK).get(Direction.SELL);
        Double buyBalance = generatorUnit.getBalance().get(TimeFrame.PEAK).get(Direction.BUY);
        Assertions.assertEquals(sellBalance + buyBalance, generatorUnit.getMetaUnit().getCapacity().get(TimeFrame.PEAK).get(Direction.SELL));

        comp = tunnel.runningComp();
        bidPO = BidPO.builder().unitId(generatorUnitId)
                .timeFrame(TimeFrame.PEAK).price(300D).direction(Direction.BUY).quantity(100D).build();
        intraBidPO = IntraBidPO.builder().stageId(comp.getStageId().toString()).bidPO(bidPO).build();
        bidResult = unitFacade.submitIntraBidPO(intraBidPO);
        Assertions.assertTrue(bidResult.getSuccess());
        bidPO = BidPO.builder().unitId(generatorUnitId)
                .timeFrame(TimeFrame.PEAK).price(300D).direction(Direction.SELL).quantity(100D).build();
        intraBidPO = IntraBidPO.builder().stageId(comp.getStageId().toString()).bidPO(bidPO).build();
        bidResult = unitFacade.submitIntraBidPO(intraBidPO);
        Assertions.assertFalse(bidResult.getSuccess());
        Assertions.assertEquals(bidResult.getMessage(), "省内月度报单必须保持同一个方向");

        manageFacade.step();
        manageFacade.step();
        comp = tunnel.runningComp();
        String userId = userVOs.get(0).getUserId();
        String token = TokenUtils.sign(userId);
        Result<List<IntraDaBidVO>> listResult = unitFacade.listDaBidVOs(comp.getStageId().toString(), token);
        Assertions.assertTrue(listResult.getSuccess());



    }


    @Test
    public void testSpot() {
        // 创建比赛
        List<UserVO> userVOs = manageFacade.listUserVOs();
        Map<TradeStage, Integer> marketStageBidLengths = new HashMap<>();
        Map<TradeStage, Integer> marketStageClearLengths = new HashMap<>();
        for (TradeStage marketStage : TradeStage.marketStages()) {
            marketStageBidLengths.put(marketStage, 1000);
            marketStageClearLengths.put(marketStage, 1000);
        }

        // 比赛参数
        CompCreatePO compCreatePO = CompCreatePO.builder()
                .startTimeStamp(Clock.currentTimeMillis() + 1000)

                .build();
        Result<Void> result = manageFacade.createComp(compCreatePO);
        Assertions.assertTrue(result.getSuccess());
        Long compId = tunnel.runningComp().getCompId();
        StageId stageId = StageId.builder().compId(compId).compStage(CompStage.TRADE).tradeStage(TradeStage.DA_INTER).roundId(0).marketStatus(MarketStatus.CLEAR).build();
        Result<SpotMarketVO> spotMarketVO0 = compFacade.listSpotMarketVOs(stageId.toString(), Province.TRANSFER.name(), TokenUtils.sign("1000"));
        Assertions.assertTrue(spotMarketVO0.getSuccess());
        Result<SpotMarketVO> spotMarketVO1 = compFacade.listSpotMarketVOs(stageId.toString(), Province.RECEIVER.name(), TokenUtils.sign("1000"));
        Assertions.assertTrue(spotMarketVO1.getSuccess());

        Result<SpotMarketVO> spotMarketVO2 = compFacade.listSpotMarketVOs(stageId.toString(), Province.TRANSFER.name(), TokenUtils.sign("1001"));
        Assertions.assertTrue(spotMarketVO2.getSuccess());
        Result<SpotMarketVO> spotMarketVO3 = compFacade.listSpotMarketVOs(stageId.toString(), Province.RECEIVER.name(), TokenUtils.sign("1001"));
        Assertions.assertTrue(spotMarketVO3.getSuccess());

    }


    @Test
    public void testSpotBid() {
        Map<TradeStage, Integer> marketStageBidLengths = new HashMap<>();
        Map<TradeStage, Integer> marketStageClearLengths = new HashMap<>();
        for (TradeStage marketStage : TradeStage.marketStages()) {
            marketStageBidLengths.put(marketStage, 100);
            marketStageClearLengths.put(marketStage, 100);
        }

        CompCreatePO compCreatePO = CompCreatePO.builder()
                .startTimeStamp(Clock.currentTimeMillis() + 2000000)
                .build();

        manageFacade.createComp(compCreatePO);
        manageFacade.step();
        Result<CompVO> compVOResult = compFacade.runningCompVO(TokenUtils.sign("1000"));
        Assertions.assertTrue(compVOResult.getSuccess());
        Comp comp = tunnel.runningComp();
        StageId stageId = comp.getStageId();
        StageId stageId1 = StageId.builder().compStage(CompStage.TRADE).compId(comp.getCompId())
                .roundId(0).tradeStage(TradeStage.AN_INTER).marketStatus(MarketStatus.BID).build();
        Assertions.assertEquals(stageId1, stageId);


        IntStream.range(0, 8).forEach(i -> manageFacade.step());
        comp = tunnel.runningComp();
        stageId = comp.getStageId();
        stageId1 = StageId.builder().compStage(CompStage.TRADE).compId(comp.getCompId())
                .roundId(0).tradeStage(TradeStage.DA_INTRA).marketStatus(MarketStatus.BID).build();
        Assertions.assertEquals(stageId1, stageId);

        Result<List<UnitVO>> listResult0 = unitFacade.listAssignUnitVOs(stageId.toString(), TokenUtils.sign("1000"));
        Assertions.assertTrue(listResult0.getSuccess());
        List<UnitVO> unitVOs = listResult0.getData();
        Assertions.assertEquals(unitVOs.size(), 4);
        UnitVO classicGenerator = unitVOs.get(0);
        UnitVO renewableGenerator = unitVOs.get(1);
        UnitVO load0 = unitVOs.get(2);
        UnitVO load1 = unitVOs.get(3);

        Result<List<IntraDaBidVO>> listResult1 = unitFacade.listDaBidVOs(stageId.toString(), TokenUtils.sign("1000"));
        Assertions.assertTrue(listResult1.getSuccess());
        List<IntraDaBidVO> intraDaBidVOs = listResult1.getData();


        IntraDaBidVO classicDaBidVO = intraDaBidVOs.stream()
                .filter(i -> i.getUnitId().equals(classicGenerator.getUnitId())).findFirst().orElseThrow(SysEx::unreachable);
        Segment minSegment = classicDaBidVO.getMinSegment();
        Assertions.assertNotNull(classicDaBidVO.getMinSegment());
        Assertions.assertEquals(minSegment.getPrice(), classicGenerator.getMetaUnit().getMinOutputPrice());
        Assertions.assertEquals(minSegment.getStart(), 0D);
        Assertions.assertEquals(minSegment.getEnd(), classicGenerator.getMetaUnit().getMinCapacity());
        Assertions.assertEquals(classicDaBidVO.getSegments().size(), 5);
        Assertions.assertEquals(classicDaBidVO.getSegments().get(0).getStart(), classicGenerator.getMetaUnit().getMinCapacity());
        Assertions.assertEquals(classicDaBidVO.getSegments().get(4).getEnd(), classicGenerator.getMetaUnit().getMaxCapacity());
        classicDaBidVO.getSegments().forEach(s -> Assertions.assertTrue(s.getEnd() >= s.getStart()));
        List<Segment> segments = classicDaBidVO.getSegments();
        IntStream.range(0, 4).forEach(i -> Assertions.assertEquals(segments.get(i).getEnd(), segments.get(i + 1).getStart()));


        IntraDaBidVO renewableDaBidVO = intraDaBidVOs.stream()
                .filter(i -> i.getUnitId().equals(renewableGenerator.getUnitId())).findFirst().orElseThrow(SysEx::unreachable);
        Assertions.assertNull(renewableDaBidVO.getMinSegment());
        List<Segment> segments1 = renewableDaBidVO.getSegments();
        Assertions.assertNotNull(segments1);
        Assertions.assertEquals(segments1.size(), 5);
        Assertions.assertEquals(segments1.get(0).getStart(), 0D);
        Assertions.assertEquals(segments1.get(4).getEnd(), renewableGenerator.getMetaUnit().getMaxCapacity());
        segments1.forEach(s -> Assertions.assertTrue(s.getEnd() >= s.getStart()));
        IntStream.range(0, 4).forEach(i -> Assertions.assertEquals(segments1.get(i).getEnd(), segments1.get(i + 1).getStart()));
        Assertions.assertNotNull(renewableDaBidVO.getForecasts());
        Assertions.assertEquals(renewableDaBidVO.getForecasts().size(), 24);
        Assertions.assertNotNull(renewableDaBidVO.getDeclares());
        Assertions.assertEquals(renewableDaBidVO.getDeclares().size(), 24);

        IntraDaBidVO loadDaBidVO0 = intraDaBidVOs.stream()
                .filter(i -> i.getUnitId().equals(load0.getUnitId())).findFirst().orElseThrow(SysEx::unreachable);
        Assertions.assertNotNull(loadDaBidVO0.getForecasts());
        Assertions.assertEquals(loadDaBidVO0.getForecasts().size(), 24);
        Assertions.assertNotNull(loadDaBidVO0.getDeclares());
        Assertions.assertEquals(loadDaBidVO0.getDeclares().size(), 24);
        List<Segment> segments2 = classicDaBidVO.getSegments();
        int second = LocalTime.now().getHour() / 4 + 1;
        IntStream.range(0, 4).forEach(i -> {
            segments2.get(i).setEnd(segments2.get(0).getStart() + (i + 1) * second);
            segments2.get(i + 1).setStart(segments2.get(0).getStart() + (i + 1) * second);
        });
        IntStream.range(0, 5).forEach( i ->  segments2.get(i).setPrice((double) ((i + 1) * second)));
        IntraDaBidPO intraDaBidPO = IntraDaBidPO.builder().unitId(classicDaBidVO.getUnitId()).segments(segments2).build();
        Result<Void> voidResult = unitFacade.submitDaBidVO(stageId.toString(), intraDaBidPO, TokenUtils.sign("1000"));
        Assertions.assertTrue(voidResult.getSuccess());
        listResult1 = unitFacade.listDaBidVOs(stageId.toString(), TokenUtils.sign("1000"));
        intraDaBidVOs = listResult1.getData();


        classicDaBidVO = intraDaBidVOs.stream()
                .filter(i -> i.getUnitId().equals(classicGenerator.getUnitId())).findFirst().orElseThrow(SysEx::unreachable);
        Assertions.assertEquals(classicDaBidVO.getSegments().size(), 5);
        Assertions.assertEquals(classicDaBidVO.getSegments().get(0).getStart(), classicGenerator.getMetaUnit().getMinCapacity());
        Assertions.assertEquals(classicDaBidVO.getSegments().get(4).getEnd(), classicGenerator.getMetaUnit().getMaxCapacity());
        List<Segment> segments3 = classicDaBidVO.getSegments();
        IntStream.range(0, 4).forEach(i -> {
            Segment segment = segments3.get(i);
            double v = segment.getEnd() - segment.getStart();
            Assertions.assertEquals(v, second);
        });
        IntStream.range(0, 4).forEach(i -> {
            Assertions.assertEquals(segments3.get(i).getEnd(), segments3.get(i + 1).getStart());
            Assertions.assertEquals(segments3.get(i).getPrice(), (double) (i + 1) * second);
        });

        List<Segment> segments4 = renewableDaBidVO.getSegments();
        IntStream.range(0, 4).forEach(i -> {
            segments4.get(i).setEnd(segments4.get(0).getStart() + (i + 1) * second);
            segments4.get(i + 1).setStart(segments4.get(0).getStart() + (i + 1) * second);
        });
        IntStream.range(0, 5).forEach( i ->  segments4.get(i).setPrice((double) ((i + 1) * second)));
        intraDaBidPO = IntraDaBidPO.builder().unitId(renewableDaBidVO.getUnitId())
                .segments(segments4)
                .declares(IntStream.range(0, 24).mapToObj(i -> (double) (i + 1) * second).collect(Collectors.toList()))
                .build();
        voidResult = unitFacade.submitDaBidVO(stageId.toString(), intraDaBidPO, TokenUtils.sign("1000"));
        Assertions.assertTrue(voidResult.getSuccess());

        listResult1 = unitFacade.listDaBidVOs(stageId.toString(), TokenUtils.sign("1000"));
        IntraDaBidVO renewableDaBidVO1 = listResult1.getData().stream()
                .filter(i -> i.getUnitId().equals(renewableGenerator.getUnitId())).findFirst().orElseThrow(SysEx::unreachable);
        List<Segment> segments5 = renewableDaBidVO1.getSegments();

        IntStream.range(0, 4).forEach(i -> {
            Segment segment = segments5.get(i);
            double v = segment.getEnd() - segment.getStart();
            Assertions.assertEquals(v, second);
        });
        IntStream.range(0, 4).forEach(i -> {
            Assertions.assertEquals(segments5.get(i).getEnd(), segments5.get(i + 1).getStart());
            Assertions.assertEquals(segments5.get(i).getPrice(), (double) (i + 1) * second);
        });

        IntStream.range(0, 24).forEach(i -> {
            List<Double> declares = renewableDaBidVO1.getDeclares();
            Assertions.assertEquals(declares.get(i), (i + 1) * second);
        });

        IntraDaBidPO intraDaBidPOx = IntraDaBidPO.builder().unitId(loadDaBidVO0.getUnitId())
                .declares(IntStream.range(0, 24).mapToObj(i -> (double) (i + 1) * second).collect(Collectors.toList()))
                .build();
        Result<Void> voidResult1 = unitFacade.submitDaBidVO(stageId.toString(), intraDaBidPOx, TokenUtils.sign("10000"));
        Assertions.assertTrue(voidResult1.getSuccess());

        listResult1 = unitFacade.listDaBidVOs(stageId.toString(), TokenUtils.sign("1000"));
        IntraDaBidVO loadVO = listResult1.getData().stream()
                .filter(i -> i.getUnitId().equals(load0.getUnitId())).findFirst().orElseThrow(SysEx::unreachable);
        IntStream.range(0, 24).forEach(i -> {
            List<Double> declares = loadVO.getDeclares();
            Assertions.assertEquals(declares.get(i), (i + 1) * second);
        });

    }


    @Test
    public void calculateCost() {
        Map<TradeStage, Integer> marketStageBidLengths = new HashMap<>();
        Map<TradeStage, Integer> marketStageClearLengths = new HashMap<>();
        for (TradeStage marketStage : TradeStage.marketStages()) {
            marketStageBidLengths.put(marketStage, 100);
            marketStageClearLengths.put(marketStage, 100);
        }

        CompCreatePO compCreatePO = CompCreatePO.builder()
                .startTimeStamp(Clock.currentTimeMillis() + 2000000)
                .build();

        Result<Void> result = manageFacade.createComp(compCreatePO);
        Assertions.assertTrue(result.getSuccess());
        Result<Void> stepResult = manageFacade.step();
        Comp comp = tunnel.runningComp();
        StageId stageId = comp.getStageId();
        Result<List<UnitVO>> listResult = unitFacade.listAssignUnitVOs(stageId.toString(), TokenUtils.sign("1000"));
        Assertions.assertTrue(listResult.getSuccess());
        List<UnitVO> unitVOs = listResult.getData();

        UnitVO classicGenerator = unitVOs.stream().filter(unitVO -> GeneratorType.CLASSIC.equals(unitVO.getMetaUnit().getGeneratorType())).findFirst().orElseThrow(SysEx::unreachable);
        MetaUnit metaUnit = classicGenerator.getMetaUnit();
        Double minCapacity = metaUnit.getMinCapacity();
        Double maxCapacity = metaUnit.getMaxCapacity();
        double cost = unitFacade.calculateDaCost(classicGenerator.getUnitId(), minCapacity, minCapacity + 280).getData();
        compFacade.listSpotMarketVOs(stageId.toString(), Province.RECEIVER.name(), TokenUtils.sign("10000"));
        compFacade.listSpotMarketVOs(stageId.toString(), Province.TRANSFER.name(), TokenUtils.sign("10000"));


    }

    @Test
    public void testSpotInterBid() {
        Map<TradeStage, Integer> marketStageBidLengths = new HashMap<>();
        Map<TradeStage, Integer> marketStageClearLengths = new HashMap<>();
        for (TradeStage marketStage : TradeStage.marketStages()) {
            marketStageBidLengths.put(marketStage, 100);
            marketStageClearLengths.put(marketStage, 100);
        }

        CompCreatePO compCreatePO = CompCreatePO.builder()
                .startTimeStamp(Clock.currentTimeMillis() + 2000000)
                .build();

        manageFacade.createComp(compCreatePO);
        IntStream.range(0, 10).forEach(i -> manageFacade.step());
        Comp comp = tunnel.runningComp();
        StageId currentStageId = comp.getStageId();
        StageId targetStageId = StageId.builder().compId(comp.getCompId())
                .roundId(0)
                .compStage(CompStage.TRADE)
                .tradeStage(TradeStage.DA_INTER)
                .marketStatus(MarketStatus.BID)
                .build();
        Assertions.assertEquals(currentStageId, targetStageId);

        Result<List<SpotInterBidVO>> listResult = unitFacade.listSpotInterBidVO(currentStageId.toString(), TokenUtils.sign("1000"));
        Assertions.assertTrue(listResult.getSuccess());
        List<SpotInterBidVO> spotInterBidVOs = listResult.getData();
        SpotInterBidVO spotInterBidVO = spotInterBidVOs.get(0);

        SpotBidPO.SpotBidPOBuilder builder = SpotBidPO.builder().unitId(spotInterBidVO.getUnitId()).stageId(currentStageId.toString());

        double price = (LocalTime.now().getSecond() + 1) * 10;
        SpotInterBidVO finalSpotInterBidVO = spotInterBidVO;
        List<InstantSpotBidPO> instantSpotBidPOs = spotInterBidVO.getInstantSpotBidVOs().stream().map(vo -> {
            SpotInterBidPO spotInterBidPO = new SpotInterBidPO();
            spotInterBidPO.setUnitId(finalSpotInterBidVO.getUnitId());
            Integer instant = vo.getInstant();
            Double preCleared = vo.getPreCleared();
            Double maxCapacity = vo.getMaxCapacity();
            double q = (maxCapacity - preCleared) / 3;
            InterSpotBid interSpotBid = InterSpotBid.builder().price((double) price).quantity(q).build();
            List<InterSpotBid> interSpotBids = Collect.asList(interSpotBid, interSpotBid, interSpotBid);
            return InstantSpotBidPO.builder().instant(instant).interSpotBids(interSpotBids).build();
        }).collect(Collectors.toList());
        SpotBidPO spotBidPO = builder.instantSpotBidPOs(instantSpotBidPOs).build();
        Result<Void> submitResult = unitFacade.submitInterSpotBid(spotBidPO, comp.getStageId().toString());
        Assertions.assertTrue(submitResult.getSuccess());

        listResult = unitFacade.listSpotInterBidVO(currentStageId.toString(), TokenUtils.sign("1000"));
        Assertions.assertTrue(listResult.getSuccess());
        spotInterBidVO = listResult.getData().get(0);
        List<InstantSpotBidVO> instantSpotBidVOs = spotInterBidVO.getInstantSpotBidVOs();
        instantSpotBidVOs.forEach(i -> i.getInterSpotBids().forEach(b -> Assertions.assertEquals(b.getPrice(), price)));

        Result<Void> step = manageFacade.step();
        Assertions.assertTrue(step.getSuccess());

        Result<SpotInterClearanceVO> spotInterClearanceVO = compFacade.getSpotInterClearanceVO(currentStageId.toString(), TokenUtils.sign("1000"));
        Assertions.assertTrue(spotInterClearanceVO.getSuccess());
        SpotInterClearanceVO sVO = spotInterClearanceVO.getData();

        Comp comp1 = tunnel.runningComp();
        IntStream.range(0, 24).forEach(i -> {
            Result<InterSpotMarketVO> interSpotMarketVO = compFacade.getInterSpotMarketVO(comp1.getStageId().toString(), i, TokenUtils.sign("1000"));
            Assertions.assertTrue(interSpotMarketVO.getSuccess());
        });

        Result<List<InterSpotUnitDealVO>> listResult1 = compFacade.listInterSpotDeals(currentStageId.toString(), 1, TokenUtils.sign("1000"));
        Assertions.assertTrue(listResult1.getSuccess());
        List<InterSpotUnitDealVO> data = listResult1.getData();
        System.out.println("");
    }

    @Autowired
    MarketSettingMapper marketSettingMapper;
    @Test
    public void test() {
        marketSettingMapper.selectById(1);
     }




}
