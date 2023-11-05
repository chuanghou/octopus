package com.bilanee.octopus.domain;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilanee.octopus.adapter.tunnel.BidQuery;
import com.bilanee.octopus.adapter.tunnel.Ssh;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.adapter.ws.WsHandler;
import com.bilanee.octopus.adapter.ws.WsMessage;
import com.bilanee.octopus.adapter.ws.WsTopic;
import com.bilanee.octopus.basic.*;
import com.bilanee.octopus.basic.enums.CompStage;
import com.bilanee.octopus.basic.enums.MarketStatus;
import com.bilanee.octopus.basic.enums.TimeFrame;
import com.bilanee.octopus.basic.enums.TradeStage;
import com.bilanee.octopus.infrastructure.entity.*;
import com.bilanee.octopus.infrastructure.mapper.*;
import com.stellariver.milky.common.base.SysEx;
import com.stellariver.milky.common.tool.common.Clock;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.domain.support.base.DomainTunnel;
import com.stellariver.milky.domain.support.command.CommandBus;
import com.stellariver.milky.domain.support.context.Context;
import com.stellariver.milky.domain.support.dependency.UniqueIdGetter;
import com.stellariver.milky.domain.support.event.EventRouter;
import com.stellariver.milky.domain.support.event.EventRouters;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.*;
import java.util.stream.Collectors;

@CustomLog
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Routers implements EventRouters {

    final UniqueIdGetter uniqueIdGetter;
    final Tunnel tunnel;
    final IntraManager intraManager;
    final DomainTunnel domainTunnel;
    final TransactionDOMapper transactionDOMapper;
    final LoadResultMapper loadResultMapper;
    final GeneratorResultMapper generatorResultMapper;

    @EventRouter
    public void route(CompEvent.Created created, Context context) {
        Comp comp = created.getComp();
        List<Map<String, List<MetaUnit>>> roundMetaUnits = created.getRoundMetaUnits();
        for (int roundId = 0; roundId < roundMetaUnits.size(); roundId++) {
            Map<String, List<MetaUnit>> groupMetaUnits = roundMetaUnits.get(roundId);
            for (String userId : groupMetaUnits.keySet()) {
                List<MetaUnit> metaUnits = groupMetaUnits.get(userId);
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


    /**
     * 年度省间出清和月度省间出清，主要是为了清算集中竞价结果，算成交价格，确定各个量价最后的成交数量
     */
    @EventRouter
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


        // 清空 上一次的
        LambdaQueryWrapper<InterDealDO> eq = new LambdaQueryWrapper<InterDealDO>().eq(InterDealDO::getRoundId, now.getRoundId() + 1)
                .eq(InterDealDO::getMarketType, now.getTradeStage().getMarketType());
        interDealDOMapper.delete(eq);

        BidQuery bidQuery = BidQuery.builder()
                .roundId(now.getRoundId()).tradeStage(now.getTradeStage()).compId(stepped.getCompId()).build();
        List<Bid> bids = tunnel.listBids(bidQuery);
        String dt = tunnel.runningComp().getDt();
        Map<Long, Collection<Bid>> bidMap = bids.stream().collect(Collect.listMultiMap(Bid::getUnitId)).asMap();
        bidMap.forEach((unitId, userBids) -> {
            Unit unit = domainTunnel.getByAggregateId(Unit.class, unitId);
            userBids.stream().collect(Collect.listMultiMap(Bid::getTimeFrame)).asMap().forEach(((timeFrame, bs) -> {
                Map<Double, Collection<Bid>> priceGrouped = bids.stream().collect(Collect.listMultiMap(Bid::getPrice)).asMap();
                if (priceGrouped.size() != 1) {
                    throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
                }
                priceGrouped.forEach((k, cbs) -> {
                    InterDealDO interDealDO = InterDealDO.builder().roundId(now.getRoundId() + 1)
                            .resourceId(unit.getMetaUnit().getSourceId())
                            .resourceType(unit.getMetaUnit().getUnitType().getDbCode())
                            .dt(dt)
                            .pfvPrd(timeFrame.getDbCode())
                            .marketType(now.getTradeStage().getMarketType())
                            .clearedMw(cbs.stream().flatMap(b -> b.getDeals().stream()).collect(Collectors.summarizingDouble(Deal::getQuantity)).getSum())
                            .clearedPrice(k)
                            .build();
                    interDealDOMapper.insert(interDealDO);
                });
            } ));
        });


        storeDb(now);


    }

    /**
     * 年度省内和月度省内出清，其实本质是为了，关闭所有挂单，执行的其实是撤单策略
     */
    @EventRouter
    public void routeForIntraClear(CompEvent.Stepped stepped, Context context) {
        StageId now = stepped.getNow();
        boolean b0 = now.getTradeStage() == TradeStage.AN_INTRA && now.getMarketStatus() == MarketStatus.CLEAR;
        boolean b1 = now.getTradeStage() == TradeStage.MO_INTRA && now.getMarketStatus() == MarketStatus.CLEAR;
        if (!(b0 || b1)) {
            return;
        }

        // 清空 上一次的
        LambdaQueryWrapper<IntraDealDO> eq = new LambdaQueryWrapper<IntraDealDO>().eq(IntraDealDO::getRoundId, now.getRoundId() + 1)
                .eq(IntraDealDO::getMarketType, now.getTradeStage().getMarketType());
        intraDealDOMapper.delete(eq);

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
                    .marketType(now.getTradeStage().getMarketType())
                    .transTime(new Date(deal.getTimeStamp()))
                    .clearedMw(deal.getQuantity())
                    .clearedPrice(deal.getPrice())
                    .build();
            intraDealDOMapper.insert(intraDealDO);
        });



        storeDb(now);
        intraManager.close();

    }

    private void storeDb(StageId now) {
        BidQuery bidQuery = BidQuery.builder().compId(now.getCompId()).roundId(now.getRoundId()).tradeStage(now.getTradeStage()).build();
        List<Bid> bids = tunnel.listBids(bidQuery);
        bids.stream().collect(Collect.listMultiMap(Bid::getUnitId)).asMap().forEach((unitId, unitBids) -> {
            Unit unit = domainTunnel.getByAggregateId(Unit.class, unitId);
            Arrays.stream(TimeFrame.values()).forEach(tf -> {
                List<Bid> tBids = unitBids.stream().filter(bid -> bid.getTimeFrame() == tf).collect(Collectors.toList());
                LambdaQueryWrapper<TransactionDO> eq = new LambdaQueryWrapper<TransactionDO>()
                        .in(TransactionDO::getPrd, tf.getPrds())
                        .eq(TransactionDO::getRoundId, now.getRoundId() + 1)
                        .eq(TransactionDO::getResourceId, unit.getMetaUnit().getSourceId())
                        .eq(TransactionDO::getResourceType, unit.getMetaUnit().getUnitType().getDbCode())
                        .eq(TransactionDO::getMarketType, now.getTradeStage().getDbCode());
                Double quantity = tBids.stream()
                        .flatMap(uBid -> uBid.getDeals().stream()).map(Deal::getQuantity).reduce(0D, Double::sum);
                List<TransactionDO> transactionDOS = transactionDOMapper.selectList(eq);
                transactionDOS.forEach(t -> t.setClearedMw(quantity));
                transactionDOS.forEach(transactionDOMapper::updateById);
            });
        });
    }

    @EventRouter
    public void routeForRoundIdUpdate(CompEvent.Stepped stepped, Context context) {
        StageId now = stepped.getNow();
        if (now.getTradeStage() == TradeStage.AN_INTER && now.getMarketStatus() == MarketStatus.BID) {
            tunnel.writeBackDbRoundId(stepped.getNow().getRoundId());
        }
    }

    @EventRouter
    public void fillReverseBalance(CompEvent.Stepped stepped, Context context) {
        StageId now = stepped.getNow();
        boolean b0 = now.getTradeStage() == TradeStage.MO_INTRA && now.getMarketStatus() == MarketStatus.BID;
        if (b0) {
            List<Unit> units = tunnel.listUnits(now.getCompId(), now.getRoundId(), null);
            units.forEach(unit -> {
                UnitCmd.FillBalance command = UnitCmd.FillBalance.builder().unitId(unit.getUnitId()).build();
                CommandBus.driveByEvent(command, stepped);
            });
        }
    }

    @EventRouter
    public void routeStageIdChanged(CompEvent.Stepped stepped, Context context) {
        WsHandler.cast(WsMessage.builder().wsTopic(WsTopic.STAGE_ID).build());
    }

    final InterSpotUnitOfferDOMapper interSpotUnitOfferDOMapper;

    /**
     * 省内现货之后预出清
     */
    @EventRouter
    public void routeBeforeAfterIntraSpotBid(CompEvent.Stepped stepped, Context context) {
        StageId now = stepped.getNow();
        boolean b0 = now.getTradeStage() == TradeStage.DA_INTER;
        boolean b1 = now.getMarketStatus() == MarketStatus.BID;
        if (!(b0 && b1)) {
            return;
        }
        Ssh.exec("python manage.py intra_pre_clearing 1");
        Ssh.exec("python manage.py intra_pre_clearing 2");
    }


    /**
     * 省间现货之前清空报价表
     */
    @EventRouter
    public void routeBeforeInterSpotBid(CompEvent.Stepped stepped, Context context) {
        StageId now = stepped.getNow();
        boolean b0 = now.getTradeStage() == TradeStage.DA_INTER;
        boolean b1 = now.getMarketStatus() == MarketStatus.BID;
        if (!(b0 && b1)) {
            return;
        }
        Integer roundId = now.getRoundId();
        LambdaQueryWrapper<InterSpotUnitOfferDO> eq = new LambdaQueryWrapper<InterSpotUnitOfferDO>().eq(InterSpotUnitOfferDO::getRoundId, roundId + 1);
        interSpotUnitOfferDOMapper.selectList(eq).forEach(interSpotUnitOfferDO -> {
            interSpotUnitOfferDO.setSpotOfferMw1(0D);
            interSpotUnitOfferDO.setSpotOfferMw2(0D);
            interSpotUnitOfferDO.setSpotOfferMw3(0D);
            interSpotUnitOfferDO.setSpotOfferPrice1(0D);
            interSpotUnitOfferDO.setSpotOfferPrice2(0D);
            interSpotUnitOfferDO.setSpotOfferPrice3(0D);
            interSpotUnitOfferDOMapper.updateById(interSpotUnitOfferDO);
        });
    }

    final UnmetDemandMapper unmetDemandMapper;
    final TieLinePowerDOMapper tieLinePowerDOMapper;
    final InterSpotTransactionDOMapper interSpotTransactionDOMapper;

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
        LambdaQueryWrapper<TieLinePowerDO> eq
                = new LambdaQueryWrapper<TieLinePowerDO>().eq(TieLinePowerDO::getRoundId, roundId + 1);
        Map<Integer, Double> already = tieLinePowerDOMapper.selectList(eq).stream()
                .collect(Collectors.toMap(TieLinePowerDO::getPrd, t -> t.getAnnualTielinePower() + t.getMonthlyTielinePower()));
        Map<Integer, Double> demand = unmetDemandMapper.selectList(null).stream()
                .collect(Collectors.toMap(UnmetDemand::getPrd, u -> u.getDaReceivingMw() - already.get(u.getPrd())));
        for (int instant = 0; instant < 24; instant++) {
            double require = demand.get(instant) - already.get(instant);
            LambdaQueryWrapper<InterSpotUnitOfferDO> eq1 = new LambdaQueryWrapper<InterSpotUnitOfferDO>()
                    .eq(InterSpotUnitOfferDO::getRoundId, roundId + 1).eq(InterSpotUnitOfferDO::getPrd, instant);
            List<Section> sections = interSpotUnitOfferDOMapper.selectList(eq1)
                    .stream().filter(i -> !i.getSpotOfferMw1().equals(0D) || !i.getSpotOfferMw2().equals(0D) || !i.getSpotOfferMw3().equals(0D))
                    .map(u -> Arrays.asList(
                            new Section(u.getUnitId(), u.getSpotOfferMw1(), u.getSpotOfferPrice1(), 0D),
                            new Section(u.getUnitId(), u.getSpotOfferMw2(), u.getSpotOfferPrice2(), 0D),
                            new Section(u.getUnitId(), u.getSpotOfferMw3(), u.getSpotOfferPrice3(), 0D)
                    )).flatMap(Collection::stream).sorted(Comparator.comparing(Section::getPrice)).collect(Collectors.toList());
            Double price = null;
            double marketQuantity = 0D;
            double nonMarketQuantity = 0D;
            double accumulate = 0D;

            for (Section section : sections) {
                double v = accumulate + section.getQuantity();
                if (v >= require) {
                    price = section.price;
                    marketQuantity = require;
                    section.setDealQuantity(require - accumulate);
                    break;
                }
                accumulate += section.getQuantity();
                section.setDealQuantity(section.quantity);
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
                            .dt(Clock.todayString())
                            .build();
                    interSpotTransactionDOMapper.insert(spotTransactionDO);
                } else {
                    interSpotTransactionDOMapper.updateById(interSpotTransactionDO);
                }
            }
        }
    }


    /**
     * 执行正式出清
     */
    @EventRouter
    public void routerAfterIntraSpotBid(CompEvent.Stepped stepped, Context context){
        StageId now = stepped.getNow();
        boolean b0 = now.getTradeStage() == TradeStage.DA_INTRA;
        boolean b1 = now.getMarketStatus() == MarketStatus.CLEAR;
        if (!(b0 && b1)) {
            return;
        }

        Ssh.exec("python manage.py intra_da_market_clearing 2 1");
        Ssh.exec("python manage.py intra_da_ruc 1");
        Ssh.exec("python manage.py intra_rt_ed 1");
        Ssh.exec("python manage.py intra_da_market_clearing 2 2");
        Ssh.exec("python manage.py intra_da_ruc 2");
        Ssh.exec("python manage.py intra_rt_ed 2");
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


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static private class Section {

        int sourceId;
        double quantity;
        double price;
        double dealQuantity;

    }





}
