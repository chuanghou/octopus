package com.bilanee.octopus;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilanee.octopus.adapter.facade.CompFacade;
import com.bilanee.octopus.adapter.facade.ManageFacade;
import com.bilanee.octopus.adapter.facade.UnitFacade;
import com.bilanee.octopus.adapter.facade.po.*;
import com.bilanee.octopus.adapter.facade.vo.*;
import com.bilanee.octopus.adapter.repository.UnitAdapter;
import com.bilanee.octopus.adapter.tunnel.BidQuery;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.Bid;
import com.bilanee.octopus.basic.Deal;
import com.bilanee.octopus.basic.StageId;
import com.bilanee.octopus.basic.TokenUtils;
import com.bilanee.octopus.basic.enums.*;
import com.bilanee.octopus.domain.Comp;
import com.bilanee.octopus.domain.Unit;
import com.bilanee.octopus.infrastructure.entity.UnitDO;
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

    @Autowired
    DomainTunnel domainTunnel;

    @Autowired
    Comp.DelayExecutor delayExecutor;

    @AfterEach
    public void clear() {
        delayExecutor.removeStepCommand();
    }
    
    
    @Test
    public void testStep() throws InterruptedException {
        Map<TradeStage, Integer> marketStageBidLengths = new HashMap<>();
        Map<TradeStage, Integer> marketStageClearLengths = new HashMap<>();
        for (TradeStage marketStage : TradeStage.marketStages()) {
            marketStageBidLengths.put(marketStage, 2);
            marketStageClearLengths.put(marketStage, 2);
        }

        CompCreatePO compCreatePO = CompCreatePO.builder()
                .startTimeStamp(Clock.currentTimeMillis() + 2_000)
                .quitCompeteLength(2)
                .quitResultLength(2)
                .marketStageBidLengths(marketStageBidLengths)
                .marketStageClearLengths(marketStageClearLengths)
                .tradeResultLength(2)
                .userIds(Arrays.asList("0", "1"))
                .enableQuiz(true)
                .build();

        manageFacade.createComp(compCreatePO);
        Result<CompVO> compVOResult = compFacade.runningCompVO(TokenUtils.sign("0"));
        Assertions.assertTrue(compVOResult.getSuccess());
        Comp comp = tunnel.runningComp();
        StageId stageId = comp.getStageId();
        Thread.sleep(2100);
        StageId stageId1 = tunnel.runningComp().getStageId();
        boolean equals = stageId1.equals(stageId.next(comp));
        Assertions.assertTrue(equals);
        Assertions.assertEquals(stageId1.getCompStage(), CompStage.QUIT_COMPETE);

        Thread.sleep(2100);
        StageId stageId2 = tunnel.runningComp().getStageId();
        equals = stageId2.equals(stageId1.next(comp));
        Assertions.assertTrue(equals);
        Assertions.assertEquals(stageId2.getCompStage(), CompStage.QUIT_RESULT);

        Thread.sleep(2100);
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
        Thread.sleep(2050);
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
        Thread.sleep(2050);
        Comp comp1 = tunnel.runningComp();
        Assertions.assertNull(comp1.getEndingTimeStamp());
        stageId = tunnel.runningComp().getStageId();
        Assertions.assertEquals(stageId.getCompStage(), CompStage.RANKING);
        Assertions.assertNull(stageId.getRoundId());
        Assertions.assertNull(stageId.getTradeStage());
        Assertions.assertNull(stageId.getMarketStatus());
        Thread.sleep(3000);
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
                .quitCompeteLength(1000)
                .quitResultLength(1000)
                .marketStageBidLengths(marketStageBidLengths)
                .marketStageClearLengths(marketStageClearLengths)
                .tradeResultLength(1000)
                .userIds(Arrays.asList("0", "1"))
                .enableQuiz(false)
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
            marketStageBidLengths.put(marketStage, 1000);
            marketStageClearLengths.put(marketStage, 1000);
        }

        // 比赛参数
        CompCreatePO compCreatePO = CompCreatePO.builder()
                .startTimeStamp(Clock.currentTimeMillis() + 1000)
                .quitCompeteLength(1000)
                .quitResultLength(1000)
                .marketStageBidLengths(marketStageBidLengths)
                .marketStageClearLengths(marketStageClearLengths)
                .tradeResultLength(1000)
                .userIds(Collect.transfer(userVOs, UserVO::getUserId))
                .enableQuiz(false)
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

        Thread.sleep(100);
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

        Thread.sleep(100);
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

}
