package com.bilanee.octopus.domain;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilanee.octopus.adapter.facade.CompFacade;
import com.bilanee.octopus.adapter.facade.ManageFacade;
import com.bilanee.octopus.adapter.facade.QuizFacade;
import com.bilanee.octopus.adapter.facade.UnitFacade;
import com.bilanee.octopus.adapter.tunnel.BidQuery;
import com.bilanee.octopus.adapter.tunnel.Ssh;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.adapter.ws.WebSocket;
import com.bilanee.octopus.adapter.ws.WsMessage;
import com.bilanee.octopus.adapter.ws.WsTopic;
import com.bilanee.octopus.basic.*;
import com.bilanee.octopus.basic.enums.*;
import com.bilanee.octopus.infrastructure.entity.*;
import com.bilanee.octopus.infrastructure.mapper.*;
import com.google.common.collect.ListMultimap;
import com.stellariver.milky.common.base.SysEx;
import com.stellariver.milky.common.tool.common.Kit;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.domain.support.base.DomainTunnel;
import com.stellariver.milky.domain.support.command.CommandBus;
import com.stellariver.milky.domain.support.context.Context;
import com.stellariver.milky.domain.support.dependency.UniqueIdGetter;
import com.stellariver.milky.domain.support.event.EventRouter;
import com.stellariver.milky.domain.support.event.EventRouters;
import com.stellariver.milky.domain.support.event.FinalEventRouter;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@CustomLog
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Routers implements EventRouters {

    final UniqueIdGetter uniqueIdGetter;
    final Tunnel tunnel;
    final ProcessorManager processorManager;
    final DomainTunnel domainTunnel;
    final TransactionDOMapper transactionDOMapper;
    final LoadResultMapper loadResultMapper;
    final GeneratorResultMapper generatorResultMapper;
    final ForwardLoadBidMapper forwardLoadBidMapper;
    final ForwardUnitOfferMapper forwardUnitOfferMapper;

    final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);


    public void fillAnnualBid(Unit unit) {
        if (unit.getMetaUnit().getProvince().interDirection() != unit.getMetaUnit().getUnitType().generalDirection()) {
            return;
        }
        MetaUnit metaUnit = unit.getMetaUnit();
        Integer roundId = unit.getRoundId();
        Integer sourceId = unit.getMetaUnit().getSourceId();

        Comp comp = tunnel.runningComp();

        StageId stageIdAn = StageId.builder().compStage(CompStage.TRADE).roundId(roundId)
                .compId(comp.getCompId()).tradeStage(TradeStage.AN_INTER).marketStatus(MarketStatus.BID).build();

        StageId stageIdMo = StageId.builder().compStage(CompStage.TRADE).roundId(roundId)
                .compId(comp.getCompId()).tradeStage(TradeStage.MO_INTER).marketStatus(MarketStatus.BID).build();

        if (metaUnit.getUnitType() == UnitType.GENERATOR) {
            LambdaQueryWrapper<ForwardUnitOffer> eq = new LambdaQueryWrapper<ForwardUnitOffer>()
                    .eq(ForwardUnitOffer::getUnitId, sourceId).eq(ForwardUnitOffer::getRoundId, roundId + 1);
            List<ForwardUnitOffer> unitOffers = forwardUnitOfferMapper.selectList(eq);

            if (Collect.isEmpty(unitOffers)) {
                throw new RuntimeException(sourceId + " " + roundId);
            }

            List<Bid> bidsAn = new ArrayList<>();

            unitOffers.forEach(unitOffer -> {

                Bid bid1An = Bid.builder().unitId(unit.getUnitId()).direction(metaUnit.getUnitType().generalDirection()).build();
                bid1An.setTimeFrame(Kit.enumOfMightEx(TimeFrame::getDbCode, unitOffer.getPfvPrd()));
                bid1An.setQuantity(unitOffer.getAnnualOfferMw1());
                bid1An.setPrice(unitOffer.getAnnualOfferPrice1());

                Bid bid2An = Bid.builder().unitId(unit.getUnitId()).direction(metaUnit.getUnitType().generalDirection()).build();
                bid2An.setTimeFrame(Kit.enumOfMightEx(TimeFrame::getDbCode, unitOffer.getPfvPrd()));
                bid2An.setQuantity(unitOffer.getAnnualOfferMw2());
                bid2An.setPrice(unitOffer.getAnnualOfferPrice2());

                Bid bid3An = Bid.builder().unitId(unit.getUnitId()).direction(metaUnit.getUnitType().generalDirection()).build();
                bid3An.setTimeFrame(Kit.enumOfMightEx(TimeFrame::getDbCode, unitOffer.getPfvPrd()));
                bid3An.setQuantity(unitOffer.getAnnualOfferMw3());
                bid3An.setPrice(unitOffer.getAnnualOfferPrice3());

                bidsAn.addAll(Collect.asList(bid1An, bid2An, bid3An));
            });


            UnitCmd.InterBids commandAn = UnitCmd.InterBids.builder().stageId(stageIdAn).bids(bidsAn).build();
            CommandBus.driveByEvent(commandAn, stepped);

        } else if (metaUnit.getUnitType() == UnitType.LOAD) {
            LambdaQueryWrapper<ForwardLoadBid> eq = new LambdaQueryWrapper<ForwardLoadBid>()
                    .eq(ForwardLoadBid::getLoadId, sourceId).eq(ForwardLoadBid::getRoundId, roundId + 1);
            List<ForwardLoadBid> loadBids = forwardLoadBidMapper.selectList(eq);

            if (Collect.isEmpty(loadBids)) {
                throw new RuntimeException(sourceId + " " + roundId);
            }

            List<Bid> bidsAn = new ArrayList<>();

            loadBids.forEach(loadBid -> {

                Bid bid1An = Bid.builder().unitId(unit.getUnitId()).direction(metaUnit.getUnitType().generalDirection()).build();
                bid1An.setTimeFrame(Kit.enumOfMightEx(TimeFrame::getDbCode, loadBid.getPfvPrd()));
                bid1An.setQuantity(loadBid.getAnnualBidMw1());
                bid1An.setPrice(loadBid.getAnnualBidPrice1());

                Bid bid2An = Bid.builder().unitId(unit.getUnitId()).direction(metaUnit.getUnitType().generalDirection()).build();
                bid2An.setTimeFrame(Kit.enumOfMightEx(TimeFrame::getDbCode, loadBid.getPfvPrd()));
                bid2An.setQuantity(loadBid.getAnnualBidMw2());
                bid2An.setPrice(loadBid.getAnnualBidPrice2());

                Bid bid3An = Bid.builder().unitId(unit.getUnitId()).direction(metaUnit.getUnitType().generalDirection()).build();
                bid3An.setTimeFrame(Kit.enumOfMightEx(TimeFrame::getDbCode, loadBid.getPfvPrd()));
                bid3An.setQuantity(loadBid.getAnnualBidMw3());
                bid3An.setPrice(loadBid.getAnnualBidPrice3());

                bidsAn.addAll(Collect.asList(bid1An, bid2An, bid3An));
            });

            UnitCmd.InterBids commandAn = UnitCmd.InterBids.builder().stageId(stageIdAn).bids(bidsAn).build();
            CommandBus.driveByEvent(commandAn, stepped);
        } else {
            throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
        }
    }

    public void monthlyDefaultBid(CompEvent.Stepped stepped, Context context) {

        Comp comp = tunnel.runningComp();
        Integer roundId = comp.getRoundId();

        StageId stageIdMo = StageId.builder().compStage(CompStage.TRADE).roundId(roundId)
                .compId(comp.getCompId()).tradeStage(TradeStage.MO_INTER).marketStatus(MarketStatus.BID).build();

        List<Unit> units = tunnel.listUnits(comp.getCompId(), roundId, null).stream()
                .filter(u -> u.getMetaUnit().getUnitType().generalDirection() == u.getMetaUnit().getProvince().interDirection())
                .collect(Collectors.toList());

        units.forEach(unit -> {
            MetaUnit metaUnit = unit.getMetaUnit();
            Integer sourceId = metaUnit.getSourceId();

            if (metaUnit.getUnitType() == UnitType.GENERATOR) {
                LambdaQueryWrapper<ForwardUnitOffer> eq = new LambdaQueryWrapper<ForwardUnitOffer>()
                        .eq(ForwardUnitOffer::getUnitId, sourceId).eq(ForwardUnitOffer::getRoundId, roundId + 1);
                List<ForwardUnitOffer> unitOffers = forwardUnitOfferMapper.selectList(eq);

                if (Collect.isEmpty(unitOffers)) {
                    throw new RuntimeException(sourceId + " " + roundId);
                }

                List<Bid> bidsMo = new ArrayList<>();

                unitOffers.forEach(unitOffer -> {

                    Bid bid1Mo = Bid.builder().unitId(unit.getUnitId()).direction(metaUnit.getUnitType().generalDirection()).build();
                    bid1Mo.setTimeFrame(Kit.enumOfMightEx(TimeFrame::getDbCode, unitOffer.getPfvPrd()));
                    bid1Mo.setQuantity(unitOffer.getMonthlyOfferMw1());
                    bid1Mo.setPrice(unitOffer.getMonthlyOfferPrice1());

                    Bid bid2Mo = Bid.builder().unitId(unit.getUnitId()).direction(metaUnit.getUnitType().generalDirection()).build();
                    bid2Mo.setTimeFrame(Kit.enumOfMightEx(TimeFrame::getDbCode, unitOffer.getPfvPrd()));
                    bid2Mo.setQuantity(unitOffer.getMonthlyOfferMw2());
                    bid2Mo.setPrice(unitOffer.getMonthlyOfferPrice2());

                    Bid bid3Mo = Bid.builder().unitId(unit.getUnitId()).direction(metaUnit.getUnitType().generalDirection()).build();
                    bid3Mo.setTimeFrame(Kit.enumOfMightEx(TimeFrame::getDbCode, unitOffer.getPfvPrd()));
                    bid3Mo.setQuantity(unitOffer.getMonthlyOfferMw3());
                    bid3Mo.setPrice(unitOffer.getMonthlyOfferPrice3());

                    bidsMo.addAll(Collect.asList(bid1Mo, bid2Mo, bid3Mo));
                });

                UnitCmd.InterBids commandMo = UnitCmd.InterBids.builder().stageId(stageIdMo).bids(bidsMo).build();
                CommandBus.driveByEvent(commandMo, stepped);

            } else if (metaUnit.getUnitType() == UnitType.LOAD) {
                LambdaQueryWrapper<ForwardLoadBid> eq = new LambdaQueryWrapper<ForwardLoadBid>()
                        .eq(ForwardLoadBid::getLoadId, sourceId).eq(ForwardLoadBid::getRoundId, roundId + 1);
                List<ForwardLoadBid> loadBids = forwardLoadBidMapper.selectList(eq);

                if (Collect.isEmpty(loadBids)) {
                    throw new RuntimeException(sourceId + " " + roundId);
                }

                List<Bid> bidsMo = new ArrayList<>();

                loadBids.forEach(loadBid -> {

                    Bid bid1Mo = Bid.builder().unitId(unit.getUnitId()).direction(metaUnit.getUnitType().generalDirection()).build();
                    bid1Mo.setTimeFrame(Kit.enumOfMightEx(TimeFrame::getDbCode, loadBid.getPfvPrd()));
                    bid1Mo.setQuantity(loadBid.getMonthlyBidMw1());
                    bid1Mo.setPrice(loadBid.getMonthlyBidPrice1());

                    Bid bid2Mo = Bid.builder().unitId(unit.getUnitId()).direction(metaUnit.getUnitType().generalDirection()).build();
                    bid2Mo.setTimeFrame(Kit.enumOfMightEx(TimeFrame::getDbCode, loadBid.getPfvPrd()));
                    bid2Mo.setQuantity(loadBid.getMonthlyBidMw2());
                    bid2Mo.setPrice(loadBid.getMonthlyBidPrice2());

                    Bid bid3Mo = Bid.builder().unitId(unit.getUnitId()).direction(metaUnit.getUnitType().generalDirection()).build();
                    bid3Mo.setTimeFrame(Kit.enumOfMightEx(TimeFrame::getDbCode, loadBid.getPfvPrd()));
                    bid3Mo.setQuantity(loadBid.getMonthlyBidMw3());
                    bid3Mo.setPrice(loadBid.getMonthlyBidPrice3());

                    bidsMo.addAll(Collect.asList(bid1Mo, bid2Mo, bid3Mo));

                });

                UnitCmd.InterBids commandMo = UnitCmd.InterBids.builder().stageId(stageIdMo).bids(bidsMo).build();
                CommandBus.driveByEvent(commandMo, stepped);
            } else {
                throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
            }
        });
    }

    @EventRouter(order = 1L)
    public void route(CompEvent.Created created, Context context) {
        Comp comp = created.getComp();
        List<Map<String, Collection<MetaUnit>>> roundMetaUnits = created.getRoundMetaUnits();
        for (int roundId = 0; roundId < roundMetaUnits.size(); roundId++) {
            Map<String, Collection<MetaUnit>> groupMetaUnits = roundMetaUnits.get(roundId);
            for (String userId : groupMetaUnits.keySet()) {
                Collection<MetaUnit> metaUnits = groupMetaUnits.get(userId);
                for (MetaUnit metaUnit : metaUnits) {
                    UnitCmd.Create command = UnitCmd.Create.builder().unitId(uniqueIdGetter.get())
                            .compId(comp.getCompId())
                            .roundId(roundId)
                            .balance(metaUnit.getCapacity())
                            .metaUnit(metaUnit)
                            .userId(userId)
                            .build();
                    CommandBus.driveByEvent(command, created);
                }
            }
        }
    }

    final InterDealDOMapper interDealDOMapper;
    final IntraDealDOMapper intraDealDOMapper;
    final RollDealDOMapper rollDealDOMapper;


    @EventRouter(order = 1L)
    public void routeForMultiClear(CompEvent.Stepped stepped, Context context) {
        StageId now = stepped.getNow();
        boolean b0 = now.getTradeStage() == TradeStage.MULTI_ANNUAL && now.getMarketStatus() == MarketStatus.CLEAR;
        if (!(b0)) {
            return;
        }
        CompCmd.ClearMulti command = CompCmd.ClearMulti.builder().compId(now.getCompId()).build();
        CommandBus.driveByEvent(command, stepped);
        BidQuery bidQuery = BidQuery.builder()
                .roundId(now.getRoundId()).tradeStage(now.getTradeStage()).compId(stepped.getCompId()).build();
        String dt = tunnel.runningComp().getDt();
        List<Bid> bids = tunnel.listBids(bidQuery).stream()
                .filter(bid -> bid.getDirection() == Direction.SELL)
                .filter(bid -> Collect.isNotEmpty(bid.getDeals())).collect(Collectors.toList());

        List<Unit> units = tunnel.listUnits(now.getCompId(), now.getRoundId(), null);
        Map<Long, Unit> unitMap = Collect.toMap(units, Unit::getUnitId);

        bids.stream().collect(Collect.listMultiMap(Bid::getUnitId)).asMap().forEach((uId, bs) -> {
            List<Deal> deals = bs.stream().flatMap(bid -> bid.getDeals().stream()).collect(Collectors.toList());
            double sum = deals.stream().collect(Collectors.summarizingDouble(Deal::getQuantity)).getSum();
            Unit unit = unitMap.get(uId);
            Integer sourceId = unit.getMetaUnit().getSourceId();
            MultiYearPoolTransactionDO multiYearPoolTransactionDO = MultiYearPoolTransactionDO.builder()
                    .roundId(now.getRoundId() + 1)
                    .dt(dt)
                    .clearedMwh(deals.stream().collect(Collectors.summarizingDouble(Deal::getQuantity)).getSum())
                    .clearedPrice(deals.get(0).getPrice())
                    .unitId(sourceId)
                    .build();
            multiYearPoolTransactionDOMapper.insert(multiYearPoolTransactionDO);

            LambdaQueryWrapper<GeneratorForecastValueDO> eq = new LambdaQueryWrapper<GeneratorForecastValueDO>()
                    .eq(GeneratorForecastValueDO::getRoundId, now.getRoundId() + 1)
                    .eq(GeneratorForecastValueDO::getUnitId, sourceId);
            List<GeneratorForecastValueDO> generatorForecastValueDOS = generatorForecastValueMapper.selectList(eq);
            double sumRtp = generatorForecastValueDOS.stream().collect(Collectors.summarizingDouble(GeneratorForecastValueDO::getRtP)).getSum();
            Map<Integer, GeneratorForecastValueDO> map = Collect.toMap(generatorForecastValueDOS, GeneratorForecastValueDO::getPrd);
            IntStream.range(0, 24).forEach(i -> {
                LambdaQueryWrapper<TransactionDO> eq1 = new LambdaQueryWrapper<TransactionDO>()
                        .eq(TransactionDO::getRoundId, now.getRoundId() + 1)
                        .eq(TransactionDO::getPrd, i)
                        .eq(TransactionDO::getResourceId, sourceId)
                        .eq(TransactionDO::getResourceType, unit.getMetaUnit().getUnitType().getDbCode())
                        .eq(TransactionDO::getMarketType, now.getTradeStage().getMarketType());
                TransactionDO transactionDO1 = transactionDOMapper.selectOne(eq1);
                transactionDO1.setClearedMw((map.get(i).getRtP() / sumRtp) * sum);
                transactionDOMapper.updateById(transactionDO1);
            });

        });



    }

    final MultiYearPoolTransactionDOMapper multiYearPoolTransactionDOMapper;
    final GeneratorForecastValueMapper generatorForecastValueMapper;

    /**
     * 年度省间出清和月度省间出清，主要是为了清算集中竞价结果，算成交价格，确定各个量价最后的成交数量
     */
    @EventRouter(order = 1L)
    public void routeForInterClear(CompEvent.Stepped stepped, Context context) {
        StageId now = stepped.getNow();
        boolean b0 = now.getTradeStage() == TradeStage.AN_INTER && now.getMarketStatus() == MarketStatus.CLEAR;
        boolean b1 = now.getTradeStage() == TradeStage.MO_INTER && now.getMarketStatus() == MarketStatus.CLEAR;
        if (!(b0 || b1)) {
            return;
        }
        CompCmd.Clear command = CompCmd.Clear.builder().compId(now.getCompId()).build();
        CommandBus.driveByEvent(command, stepped);
        List<Unit> units = tunnel.listUnits(now.getCompId(), now.getRoundId(), null);
        units.forEach(unit -> {
            UnitCmd.InterDeduct interDeduct = UnitCmd.InterDeduct.builder().unitId(unit.getUnitId()).build();
            CommandBus.driveByEvent(interDeduct, stepped);
        } );
        BidQuery bidQuery = BidQuery.builder()
                .roundId(now.getRoundId()).tradeStage(now.getTradeStage()).compId(stepped.getCompId()).build();
        String dt = tunnel.runningComp().getDt();
        Map<Long, Collection<Bid>> bidMap = tunnel.listBids(bidQuery).stream().collect(Collect.listMultiMap(Bid::getUnitId)).asMap();
        bidMap.forEach((unitId, bids) -> {
            Unit unit = domainTunnel.getByAggregateId(Unit.class, unitId);
            bids.stream().collect(Collect.listMultiMap(Bid::getTimeFrame)).asMap().forEach(((tf, bs) -> {
                List<Deal> deals = bs.stream().flatMap(b -> b.getDeals().stream()).collect(Collectors.toList());
                if (Collect.isEmpty(deals)) {
                    return;
                }
                InterDealDO interDealDO = InterDealDO.builder().roundId(now.getRoundId() + 1)
                        .resourceId(unit.getMetaUnit().getSourceId())
                        .resourceType(unit.getMetaUnit().getUnitType().getDbCode())
                        .dt(dt)
                        .pfvPrd(tf.getDbCode())
                        .marketType(now.getTradeStage().getMarketType2())
                        .clearedMw(deals.stream().collect(Collectors.summarizingDouble(Deal::getQuantity)).getSum())
                        .clearedPrice(deals.get(0).getPrice())
                        .build();
                interDealDOMapper.insert(interDealDO);
            }));
        });
        storeDb(now);

    }

    @EventRouter(order = 1L)
    public void routeForIntraBid(CompEvent.Stepped stepped, Context context) {
        StageId now = stepped.getNow();
        boolean b0 = now.getTradeStage() == TradeStage.AN_INTRA && now.getMarketStatus() == MarketStatus.BID;
        boolean b1 = now.getTradeStage() == TradeStage.MO_INTRA && now.getMarketStatus() == MarketStatus.BID;
        boolean b2 = now.getTradeStage() == TradeStage.ROLL && now.getMarketStatus() == MarketStatus.BID;
        if (!(b0 || b1 || b2)) {
            return;
        }
        scheduleWsCast(180_000L);

        scheduleWsCast(210_000L);

        scheduleWsCast(240_000L);

        scheduleWsCast(300_000L);

        scheduleWsCast(360_000L);
    }

    private void scheduleWsCast(long delay) {
        scheduledExecutorService.schedule(() -> {
            TradeStage tradeStage = tunnel.runningComp().getTradeStage();
            WsTopic wsTopic;
            if (tradeStage == TradeStage.AN_INTRA) {
                wsTopic = WsTopic.AN_INTRA_BID;
                WebSocket.cast(WsMessage.builder().wsTopic(wsTopic).body(delay).build());
            } else if (tradeStage == TradeStage.MO_INTRA) {
                wsTopic = WsTopic.MO_INTRA_BID;
                WebSocket.cast(WsMessage.builder().wsTopic(wsTopic).body(delay).build());
            } else if (tradeStage == TradeStage.ROLL) {
                wsTopic = WsTopic.ROLL_BID;
                log.info("cast {}", wsTopic);
                WebSocket.cast(WsMessage.builder().wsTopic(wsTopic).body(delay).build());
            }
        }, delay, TimeUnit.MILLISECONDS);
    }


    /**
     * 年度省内和月度省内出清，其实本质是为了，关闭所有挂单，执行的其实是撤单策略
     */
    @EventRouter(order = 1L)
    public void routeForIntraClear(CompEvent.Stepped stepped, Context context) {
        StageId now = stepped.getNow();
        boolean b0 = now.getTradeStage() == TradeStage.AN_INTRA && now.getMarketStatus() == MarketStatus.CLEAR;
        boolean b1 = now.getTradeStage() == TradeStage.MO_INTRA && now.getMarketStatus() == MarketStatus.CLEAR;
        if (!(b0 || b1)) {
            return;
        }

        BidQuery bidQuery = BidQuery.builder()
                .roundId(now.getRoundId()).tradeStage(now.getTradeStage()).compId(stepped.getCompId()).build();
        List<Bid> bids = tunnel.listBids(bidQuery);
        String dt = tunnel.runningComp().getDt();
        bids.stream().flatMap(b -> b.getDeals().stream()).distinct().forEach(deal -> {
            Unit buyUnit = domainTunnel.getByAggregateId(Unit.class, deal.getBuyUnitId());
            Unit sellUnit = domainTunnel.getByAggregateId(Unit.class, deal.getSellUnitId());
            IntraDealDO intraDealDO = IntraDealDO.builder()
                    .roundId(now.getRoundId() + 1)
                    .buyerId(buyUnit.getMetaUnit().getSourceId())
                    .buyerType(buyUnit.getMetaUnit().getUnitType().getDbCode())
                    .sellerId(sellUnit.getMetaUnit().getSourceId())
                    .sellerType(sellUnit.getMetaUnit().getUnitType().getDbCode())
                    .dt(dt)
                    .pfvPrd(deal.getTimeFrame().getDbCode())
                    .marketType(now.getTradeStage().getMarketType2())
                    .transTime(new Date(deal.getTimeStamp()))
                    .clearedMw(deal.getQuantity())
                    .clearedPrice(deal.getPrice())
                    .build();
            intraDealDOMapper.insert(intraDealDO);
        });

        ListMultimap<Pair<Province, TimeFrame>, Bid> groupBids = bids.stream().collect(Collect.listMultiMap(b -> Pair.of(b.getProvince(), b.getTimeFrame())));
        groupBids.asMap().forEach((p, subBids) -> {
            double totalMoney = subBids.stream().flatMap(bid -> bid.getDeals().stream()).collect(Collectors.summarizingDouble(deal -> deal.getPrice() * deal.getQuantity())).getSum();
            double totalQuantity = subBids.stream().flatMap(bid -> bid.getDeals().stream()).collect(Collectors.summarizingDouble(Deal::getQuantity)).getSum();
            if (totalQuantity != 0) {
                double averagePrice = totalMoney / totalQuantity;
                Integer prov = p.getKey().getDbCode();
                Integer pfvPrd = p.getValue().getDbCode();
                LambdaQueryWrapper<ForwardMarketTransactionStatusDO> eq = new LambdaQueryWrapper<ForwardMarketTransactionStatusDO>().eq(ForwardMarketTransactionStatusDO::getRoundId, now.getRoundId() + 1)
                        .eq(ForwardMarketTransactionStatusDO::getMarketType, now.getTradeStage().getMarketType2())
                        .eq(ForwardMarketTransactionStatusDO::getProv, prov)
                        .eq(ForwardMarketTransactionStatusDO::getPfvPrd, pfvPrd);
                ForwardMarketTransactionStatusDO forwardMarketTransactionStatusDO = forwardMarketTransactionStatusDOMapper.selectOne(eq);
                forwardMarketTransactionStatusDO.setAvgClearedPrice(averagePrice);
                forwardMarketTransactionStatusDOMapper.updateById(forwardMarketTransactionStatusDO);
            }

        });
        storeDb(now);
        processorManager.close();



        if (b0) {
            Ssh.exec("python manage.py monthly_default_bid");
            monthlyDefaultBid(stepped, context);
        }
    }

    final ForwardMarketTransactionStatusDOMapper forwardMarketTransactionStatusDOMapper;

    /**
     * 年度省内和月度省内出清，其实本质是为了，关闭所有挂单，执行的其实是撤单策略
     */
    @EventRouter(order = 1L)
    public void routeForRollClear(CompEvent.Stepped stepped, Context context) {
        log.info("stepped {}", stepped);
        StageId now = stepped.getNow();
        if (!(now.getTradeStage() == TradeStage.ROLL && now.getMarketStatus() == MarketStatus.CLEAR)) {
            return;
        }

        BidQuery bidQuery = BidQuery.builder()
                .roundId(now.getRoundId()).tradeStage(now.getTradeStage()).compId(stepped.getCompId()).build();
        List<Bid> bids = tunnel.listBids(bidQuery);
        String dt = tunnel.runningComp().getDt();
        bids.stream().flatMap(b -> b.getDeals().stream()).distinct().forEach(deal -> {
            Unit buyUnit = domainTunnel.getByAggregateId(Unit.class, deal.getBuyUnitId());
            Unit sellUnit = domainTunnel.getByAggregateId(Unit.class, deal.getSellUnitId());
            RollDealDO rollDealDO = RollDealDO.builder()
                    .roundId(now.getRoundId() + 1)
                    .buyerId(buyUnit.getMetaUnit().getSourceId())
                    .buyerType(buyUnit.getMetaUnit().getUnitType().getDbCode())
                    .sellerId(sellUnit.getMetaUnit().getSourceId())
                    .sellerType(sellUnit.getMetaUnit().getUnitType().getDbCode())
                    .dt(dt)
                    .prd(deal.getInstant())
                    .transTime(new Date(deal.getTimeStamp()))
                    .clearedMw(deal.getQuantity())
                    .clearedPrice(deal.getPrice())
                    .build();
            f.insert(rollDealDO);
        });


        log.info("now {}", now);
        storeDbOfRoll(now);
        processorManager.close();
    }

    private void storeDb(StageId now) {
        BidQuery bidQuery = BidQuery.builder().compId(now.getCompId()).roundId(now.getRoundId()).tradeStage(now.getTradeStage()).build();
        List<Bid> bids = tunnel.listBids(bidQuery);
        bids.stream().collect(Collect.listMultiMap(Bid::getUnitId)).asMap().forEach((unitId, unitBids) -> {
            Unit unit = domainTunnel.getByAggregateId(Unit.class, unitId);
            Direction gDirection = unit.getMetaUnit().getUnitType().generalDirection();

            Arrays.stream(TimeFrame.values()).forEach(tf -> {
                List<Bid> tBids = unitBids.stream().filter(bid -> bid.getTimeFrame() == tf).collect(Collectors.toList());
                LambdaQueryWrapper<TransactionDO> eq = new LambdaQueryWrapper<TransactionDO>()
                        .in(TransactionDO::getPrd, tf.getPrds())
                        .eq(TransactionDO::getRoundId, now.getRoundId() + 1)
                        .eq(TransactionDO::getResourceId, unit.getMetaUnit().getSourceId())
                        .eq(TransactionDO::getResourceType, unit.getMetaUnit().getUnitType().getDbCode())
                        .eq(TransactionDO::getMarketType, now.getTradeStage().getMarketType());

                double quantity = tBids.stream()
                        .flatMap(uBid -> uBid.getDeals().stream()).map(Deal::getQuantity).reduce(0D, Double::sum);
                if (quantity > 0) {
                    List<Direction> directions = tBids.stream().map(Bid::getDirection).distinct().collect(Collectors.toList());
                    if (directions.size() > 1) {
                        throw new RuntimeException();
                    }
                    Direction direction1 = directions.get(0);
                    int ratio = direction1 == gDirection ? 1 : -1;
                    List<TransactionDO> transactionDOS = transactionDOMapper.selectList(eq);
                    transactionDOS.forEach(t -> t.setClearedMw(quantity * ratio));
                    transactionDOS.forEach(transactionDOMapper::updateById);
                }
            });
        });
    }


    private void storeDbOfRoll(StageId now) {
        BidQuery bidQuery = BidQuery.builder().compId(now.getCompId()).roundId(now.getRoundId()).tradeStage(now.getTradeStage()).build();
        List<Bid> bids = tunnel.listBids(bidQuery);
        log.info("bids {}", bids);
        bids.stream().collect(Collect.listMultiMap(Bid::getUnitId)).asMap().forEach((unitId, unitBids) -> {
            Unit unit = domainTunnel.getByAggregateId(Unit.class, unitId);
            Direction gDirection = unit.getMetaUnit().getUnitType().generalDirection();

            IntStream.range(0, 24).forEach(prd -> {
                List<Bid> tBids = unitBids.stream().filter(bid -> bid.getInstant() == prd).collect(Collectors.toList());
                LambdaQueryWrapper<TransactionDO> eq = new LambdaQueryWrapper<TransactionDO>()
                        .eq(TransactionDO::getPrd, prd)
                        .eq(TransactionDO::getRoundId, now.getRoundId() + 1)
                        .eq(TransactionDO::getResourceId, unit.getMetaUnit().getSourceId())
                        .eq(TransactionDO::getResourceType, unit.getMetaUnit().getUnitType().getDbCode())
                        .eq(TransactionDO::getMarketType, now.getTradeStage().getMarketType());

                double quantity = tBids.stream()
                        .flatMap(uBid -> uBid.getDeals().stream()).map(Deal::getQuantity).reduce(0D, Double::sum);
                if (quantity > 0) {
                    List<Direction> directions = tBids.stream().map(Bid::getDirection).distinct().collect(Collectors.toList());
                    if (directions.size() > 1) {
                        throw new RuntimeException();
                    }
                    Direction direction1 = directions.get(0);
                    int ratio = direction1 == gDirection ? 1 : -1;
                    List<TransactionDO> transactionDOS = transactionDOMapper.selectList(eq);
                    transactionDOS.forEach(t -> t.setClearedMw(quantity * ratio));
                    transactionDOS.forEach(transactionDOMapper::updateById);
                }
            });
        });
    }

    @EventRouter(order = 1L)
    public void routeForRoundIdUpdate(CompEvent.Stepped stepped, Context context) {
        StageId now = stepped.getNow();
        if (now.getTradeStage() == TradeStage.AN_INTER && now.getMarketStatus() == MarketStatus.BID) {
            tunnel.writeBackDbRoundId(stepped.getNow().getRoundId());
        }
    }

    @EventRouter(order = 1L)
    public void fillReverseBalance(CompEvent.Stepped stepped, Context context) {
        StageId now = stepped.getNow();

        boolean b0 = now.getTradeStage() == TradeStage.MO_INTRA && now.getMarketStatus() == MarketStatus.BID;
        if (b0) {
            List<Unit> units = tunnel.listUnits(now.getCompId(), now.getRoundId(), null);
            units.forEach(unit -> {
                UnitCmd.FillBalance command = UnitCmd.FillBalance.builder().unitId(unit.getUnitId()).tradeStage(TradeStage.MO_INTRA).build();
                CommandBus.driveByEvent(command, stepped);
            });
        }

        boolean b1 = now.getTradeStage() == TradeStage.ROLL && now.getMarketStatus() == MarketStatus.BID;
        if (b1) {
            List<Unit> units = tunnel.listUnits(now.getCompId(), now.getRoundId(), null);
            units.forEach(unit -> {
                UnitCmd.FillBalance command = UnitCmd.FillBalance.builder().unitId(unit.getUnitId()).tradeStage(TradeStage.ROLL).build();
                CommandBus.driveByEvent(command, stepped);
            });
        }

    }

    final CompFacade compFacade;

    @FinalEventRouter
    public void routeStageIdChanged(List<CompEvent.Stepped> stepped, Context context) {
        StageId now = stepped.get(0).getNow();
        WebSocket.cast(WsMessage.builder().wsTopic(WsTopic.STAGE_ID).build());
        boolean b0 = now.getTradeStage() == TradeStage.DA_INTER;
        boolean b1 = now.getMarketStatus() == MarketStatus.BID;
        if (b0 && b1) {
            CompletableFuture.runAsync(() -> {
                LambdaQueryWrapper<StackDiagramDO> eq = new LambdaQueryWrapper<StackDiagramDO>()
                        .eq(StackDiagramDO::getRoundId, now.getRoundId() + 1);
                Boolean required = stackDiagramDOMapper.selectList(eq).stream()
                        .map(s -> s.getIntraprovincialMonthlyTielinePower() < s.getDaReceivingTarget())
                        .reduce(false, (a, b) -> a || b);
                if (!required) {
                    manageFacade.step();
                }
            });
        }

    }

    final InterSpotUnitOfferDOMapper interSpotUnitOfferDOMapper;
    final ManageFacade manageFacade;
    final GeneratorDaSegmentMapper generatorDaSegmentMapper;
    final UnitFacade unitFacade;

    /**
     * 省内现货之后预出清
     */
    @EventRouter
    public void routeBeforeAfterIntraSpotBid(CompEvent.Stepped stepped, Context context) {
        StageId now = stepped.getNow();
        boolean b0 = now.getTradeStage() == TradeStage.DA_INTER;
        boolean b1 = now.getMarketStatus() == MarketStatus.BID;
        if (b0 && b1) {
            Ssh.exec("python manage.py adjust_offer");
            Ssh.exec("python manage.py intra_pre_clearing 1");
            LambdaQueryWrapper<StackDiagramDO> eq = new LambdaQueryWrapper<StackDiagramDO>()
                    .eq(StackDiagramDO::getRoundId, stepped.getNow().getRoundId() + 1);
            Boolean required = stackDiagramDOMapper.selectList(eq).stream()
                    .map(s -> s.getIntraprovincialMonthlyTielinePower() < s.getDaReceivingTarget())
                    .reduce(false, (a, b) -> a || b);
            if (required) {
                Ssh.exec("python manage.py inter_spot_default_bid");
            }
        }
    }

    final StackDiagramDOMapper stackDiagramDOMapper;
    final UnmetDemandMapper unmetDemandMapper;
    final TieLinePowerDOMapper tieLinePowerDOMapper;
    final InterSpotTransactionDOMapper interSpotTransactionDOMapper;
    final MarketSettingMapper marketSettingMapper;

    /**
     * 填充省间现货报价
     */
    @EventRouter
    public void routerAfterInterSpotBid(CompEvent.Stepped stepped, Context context) {
        StageId now = stepped.getNow();
        boolean b0 = now.getTradeStage() == TradeStage.DA_INTER;
        boolean b1 = now.getMarketStatus() == MarketStatus.CLEAR;
        if (!(b0 && b1)) {
            return;
        }
        Integer roundId = now.getRoundId();
        String dt = marketSettingMapper.selectById(1).getDt();

        LambdaQueryWrapper<TieLinePowerDO> eq
                = new LambdaQueryWrapper<TieLinePowerDO>().eq(TieLinePowerDO::getRoundId, roundId + 1);
        Map<Integer, Double> already = tieLinePowerDOMapper.selectList(eq).stream()
                .collect(Collectors.toMap(TieLinePowerDO::getPrd, t -> t.getAnnualTielinePower() + t.getMonthlyTielinePower()));
        Map<Integer, Double> demands = unmetDemandMapper.selectList(new LambdaQueryWrapper<UnmetDemand>().eq(UnmetDemand::getRoundId, roundId + 1)).stream()
                .collect(Collectors.toMap(UnmetDemand::getPrd, u -> u.getDaReceivingMw() - already.get(u.getPrd())));
        demands.entrySet().stream().filter(e -> e.getValue() > 0).forEach(e -> {
            Integer instant = e.getKey();
            double require = e.getValue();
            LambdaQueryWrapper<InterSpotUnitOfferDO> eq1 = new LambdaQueryWrapper<InterSpotUnitOfferDO>()
                    .eq(InterSpotUnitOfferDO::getRoundId, roundId + 1).eq(InterSpotUnitOfferDO::getPrd, instant);
            List<Section> sections = interSpotUnitOfferDOMapper.selectList(eq1).stream()
                    .map(u -> Arrays.asList(
                            new Section(u.getUnitId(), u.getSpotOfferMw1(), u.getSpotOfferPrice1(), 0D),
                            new Section(u.getUnitId(), u.getSpotOfferMw2(), u.getSpotOfferPrice2(), 0D),
                            new Section(u.getUnitId(), u.getSpotOfferMw3(), u.getSpotOfferPrice3(), 0D)
                    )).flatMap(Collection::stream)
                    .filter(s -> s.getPrice() != null)
                    .filter(s -> s.getQuantity() != null && s.getQuantity() > 0)
                    .sorted(Comparator.comparing(Section::getPrice)).collect(Collectors.toList());
            Double price = null;
            double marketQuantity = 0D;
            double nonMarketQuantity = 0D;
            double accumulate = 0D;

            List<List<Section>> groupSections = sections.stream().collect(Collectors.groupingBy(Section::getPrice))
                    .entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).collect(Collectors.toList());
            for (List<Section> gSecs : groupSections) {
                double sum = gSecs.stream().collect(Collectors.summarizingDouble(Section::getQuantity)).getSum();
                double v = accumulate + sum;
                if (v >= require) {
                    price = gSecs.get(0).price;
                    marketQuantity = require;
                    double ratio = (require - accumulate) / sum;
                    gSecs.forEach(gSec -> gSec.setDealQuantity(ratio * gSec.getQuantity()));
                    break;
                }
                accumulate += sum;
                gSecs.forEach(s -> s.setDealQuantity(s.getQuantity()));
            }

            if (price == null) {
                if (!sections.isEmpty()) {
                    price = sections.get(sections.size() - 1).getPrice();
                }
                marketQuantity = accumulate;
                nonMarketQuantity = require - marketQuantity;
            }

            LambdaQueryWrapper<TieLinePowerDO> eq2 = new LambdaQueryWrapper<TieLinePowerDO>()
                    .eq(TieLinePowerDO::getRoundId, roundId + 1).eq(TieLinePowerDO::getPrd, instant);
            TieLinePowerDO tieLinePowerDO = tieLinePowerDOMapper.selectOne(eq2);
            tieLinePowerDO.setDaNonmarketTielinePower(nonMarketQuantity);
            tieLinePowerDO.setDaMarketTielinePower(marketQuantity);
            tieLinePowerDO.setDaTielinePower(nonMarketQuantity + marketQuantity);

            tieLinePowerDOMapper.updateById(tieLinePowerDO);

            Map<Integer, Collection<Section>> map = sections.stream().collect(Collect.listMultiMap(Section::getSourceId)).asMap();
            for (Map.Entry<Integer, Collection<Section>> entry : map.entrySet()) {
                Integer sourceId = entry.getKey();
                double dealTotal = entry.getValue().stream().collect(Collectors.summarizingDouble(Section::getDealQuantity)).getSum();
                LambdaQueryWrapper<InterSpotTransactionDO> eq3 = new LambdaQueryWrapper<InterSpotTransactionDO>()
                        .eq(InterSpotTransactionDO::getRoundId, roundId + 1)
                        .eq(InterSpotTransactionDO::getPrd, instant)
                        .eq(InterSpotTransactionDO::getSellerId, sourceId);
                InterSpotTransactionDO interSpotTransactionDO = interSpotTransactionDOMapper.selectOne(eq3);
                if (interSpotTransactionDO == null) {
                    InterSpotTransactionDO spotTransactionDO = InterSpotTransactionDO.builder().prd(instant)
                            .roundId(roundId + 1)
                            .sellerId(sourceId)
                            .clearedMw(dealTotal)
                            .clearedPrice(price)
                            .id(uniqueIdGetter.get())
                            .dt(dt)
                            .build();
                    interSpotTransactionDOMapper.insert(spotTransactionDO);
                } else {
                    interSpotTransactionDOMapper.updateById(interSpotTransactionDO);
                }
            }
        });
    }


    /**
     * 执行正式出清
     */
    @EventRouter
    public void routerAfterIntraSpotBid(CompEvent.Stepped stepped, Context context){
        StageId now = stepped.getNow();
        boolean b0 = now.getTradeStage() == TradeStage.DA_INTER;
        boolean b1 = now.getMarketStatus() == MarketStatus.CLEAR;
        if (b0 && b1) {
            log.info("开始执行正式出清");
//            CompletableFuture<Void> future0 = CompletableFuture.runAsync(() -> {
                Ssh.exec("python manage.py intra_da_market_clearing 2 1");
                Ssh.exec("python manage.py intra_da_ruc 1");
                Ssh.exec("python manage.py intra_rt_ed 1");
//            });

//            CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
                Ssh.exec("python manage.py intra_da_market_clearing 2 2");
                Ssh.exec("python manage.py intra_da_ruc 2");
                Ssh.exec("python manage.py intra_rt_ed 2");
//            });
//            Stream.of(future0, future1).forEach(CompletableFuture::join);
            log.info("结束执行正式出清");
        }


    }

    /**
     * 执行清算
     */
    @EventRouter(order = 2)
    public void routerDefaultAnnualBid(CompEvent.Stepped stepped, Context context){
        StageId now = stepped.getNow();
        boolean b0 = now.getMarketStatus() == MarketStatus.BID;
        boolean b1 = now.getTradeStage() == TradeStage.MULTI_ANNUAL;
        boolean b2 = now.getRoundId() == 0;
        if (!(b0 && b1 && b2)) {
            return;
        }
        Ssh.exec("python manage.py annual_default_bid");
        Comp comp = tunnel.runningComp();
        List<Unit> units = tunnel.listUnits(comp.getCompId(), null, null);
        units.forEach(this::fillAnnualBid);

    }


    /**
     * 执行清算
     */
    @EventRouter
    public void routerAfterIntraSpotClear(CompEvent.Stepped stepped, Context context){
        StageId now = stepped.getNow();
        boolean b0 = now.getTradeStage() == TradeStage.END;
        if (!b0) {
            return;
        }
        log.position("routerAfterIntraSpotClear").info("routerAfterIntraSpotClear");
        Ssh.exec("python manage.py settle");
    }

    @EventRouter
    public void routerForFinalClear(CompEvent.Stepped stepped, Context context){
        StageId now = stepped.getNow();
        boolean b0 = now.getCompStage() == CompStage.RANKING;
        if (!b0) {
            return;
        }
        Ssh.exec("python manage.py game_ranking");
    }

    final QuizResultDOMapper quizResultDOMapper;
    final QuizFacade quizFacade;


    @EventRouter(order = 1)
    public void writeScore(CompEvent.Stepped stepped, Context context) {

        StageId now = stepped.getNow();
        boolean b = now.getCompStage() == CompStage.QUIT_RESULT;
        if (!b) {
            return;
        }
        List<String> userIds = tunnel.runningComp().getUserIds();
        quizResultDOMapper.selectList(null).forEach(quizResultDOMapper::deleteById);
        StageId stageId = tunnel.runningComp().getStageId();
        userIds.forEach(userId -> {
            Integer score = quizFacade.getScore(stageId.toString(), TokenUtils.sign(userId)).getData();
            QuizResultDO quizResultDO = QuizResultDO.builder().traderId(userId).score(score).build();
            quizResultDOMapper.insert(quizResultDO);
        });
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static private class Section {

        Integer sourceId;
        Double quantity;
        Double price;
        Double dealQuantity;

    }





}
