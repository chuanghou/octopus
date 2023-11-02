package com.bilanee.octopus.domain;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilanee.octopus.adapter.tunnel.BidQuery;
import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.adapter.ws.WsHandler;
import com.bilanee.octopus.adapter.ws.WsMessage;
import com.bilanee.octopus.adapter.ws.WsTopic;
import com.bilanee.octopus.basic.Bid;
import com.bilanee.octopus.basic.Deal;
import com.bilanee.octopus.basic.MetaUnit;
import com.bilanee.octopus.basic.StageId;
import com.bilanee.octopus.basic.enums.MarketStatus;
import com.bilanee.octopus.basic.enums.TimeFrame;
import com.bilanee.octopus.basic.enums.TradeStage;
import com.bilanee.octopus.infrastructure.entity.*;
import com.bilanee.octopus.infrastructure.mapper.*;
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


    @EventRouter
    public void routerForRecord(CompEvent.Stepped stepped, Context context) {

    }

    final InterSpotUnitOfferDOMapper interSpotUnitOfferDOMapper;

    @EventRouter
    public void routeClearInterSpotBid(CompEvent.Stepped stepped, Context context) {
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

    @EventRouter
    public void clearForInterSpotBid(CompEvent.Stepped stepped, Context context) {
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
