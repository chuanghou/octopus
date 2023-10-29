package com.bilanee.octopus.domain;

import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.adapter.ws.WsHandler;
import com.bilanee.octopus.adapter.ws.WsMessage;
import com.bilanee.octopus.adapter.ws.WsTopic;
import com.bilanee.octopus.basic.*;
import com.bilanee.octopus.basic.enums.BidStatus;
import com.bilanee.octopus.basic.enums.Direction;
import com.bilanee.octopus.basic.enums.Operation;
import com.bilanee.octopus.basic.enums.TradeStage;
import com.bilanee.octopus.infrastructure.entity.Ask;
import com.bilanee.octopus.infrastructure.entity.IntraInstantDO;
import com.bilanee.octopus.infrastructure.entity.IntraQuotationDO;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.stellariver.milky.common.base.BizEx;
import com.stellariver.milky.common.tool.common.Clock;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.domain.support.command.CommandBus;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntraProcessor implements EventHandler<IntraBidContainer> {

    final Tunnel tunnel;
    final IntraSymbol intraSymbol;
    final Disruptor<IntraBidContainer> disruptor = new Disruptor<>(IntraBidContainer::new, 1024, DaemonThreadFactory.INSTANCE);
    final PriorityQueue<Bid> buyPriorityQueue = new PriorityQueue<>(buyComparator);
    final PriorityQueue<Bid> sellPriorityQueue = new PriorityQueue<>(sellComparator);

    private Double latestPrice = 0D;

    public IntraProcessor(Tunnel tunnel, IntraSymbol intraSymbol) {
        this.tunnel = tunnel;
        this.intraSymbol = intraSymbol;
        disruptor.handleEventsWith(this);
        disruptor.start();
    }

    static private final Comparator<Bid> buyComparator = (o1, o2) -> {
        if (o1.getPrice() > o2.getPrice()) {
            return -1;
        } else if (o1.getPrice() < o2.getPrice()) {
            return 1;
        } else {
            return o1.getDeclareTimeStamp().compareTo(o2.getDeclareTimeStamp());
        }
    };

    static private final Comparator<Bid> sellComparator = (o1, o2) -> {
        if (o1.getPrice() < o2.getPrice()) {
            return -1;
        } else if (o1.getPrice() > o2.getPrice()) {
            return 1;
        } else {
            return o1.getDeclareTimeStamp().compareTo(o2.getDeclareTimeStamp());
        }
    };


    public void declare(Bid bid) {
        disruptor.publishEvent((rtBidContainer, sequence) -> {
            rtBidContainer.setOperation(Operation.DECLARE);
            rtBidContainer.setDeclareBid(bid);
        });
    }

    public void cancel(Long cancelBidId, Direction cancelBidDirection) {
        disruptor.publishEvent((rtBidContainer, sequence) -> {
            rtBidContainer.setOperation(Operation.CANCEL);
            rtBidContainer.setCancelBidId(cancelBidId);
            rtBidContainer.setCancelBidDirection(cancelBidDirection);
        });
    }

    public void close() {
        disruptor.publishEvent((rtBidContainer, sequence) -> rtBidContainer.setOperation(Operation.CLOSE));
    }


    @Override
    public void onEvent(IntraBidContainer event, long sequence, boolean endOfBatch) {
        if (event.getOperation() == Operation.DECLARE) {
            doNewBid(event.getDeclareBid());
        } else if (event.getOperation() == Operation.CANCEL) {
            doCancel(event.getCancelBidId(), event.getCancelBidDirection());
        } else if (event.getOperation() == Operation.CLOSE) {
            doClose();
        }

        // 推送消息
        WsTopic wsTopic;
        TradeStage tradeStage = tunnel.runningComp().getTradeStage();
        if (tradeStage == TradeStage.AN_INTRA) {
            wsTopic = WsTopic.AN_INTRA_BID;
            WsHandler.cast(WsMessage.builder().wsTopic(wsTopic).build());
        } else if (tradeStage == TradeStage.MO_INTRA) {
            wsTopic = WsTopic.MO_INTRA_BID;
            WsHandler.cast(WsMessage.builder().wsTopic(wsTopic).build());
        }
    }

    private void doClose() {

        buyPriorityQueue.forEach(bid -> bid.setBidStatus(BidStatus.CANCELLED));
        tunnel.updateBids(new ArrayList<>(buyPriorityQueue));
        buyPriorityQueue.forEach(bid -> {
            UnitCmd.IntraBidCancelled command = UnitCmd.IntraBidCancelled.builder().unitId(bid.getUnitId()).cancelBidId(bid.getBidId()).build();
            CommandBus.accept(command, new HashMap<>());
        });
        buyPriorityQueue.clear();

        sellPriorityQueue.forEach(bid -> bid.setBidStatus(BidStatus.CANCELLED));
        tunnel.updateBids(new ArrayList<>(sellPriorityQueue));
        sellPriorityQueue.forEach(bid -> {
            UnitCmd.IntraBidCancelled command = UnitCmd.IntraBidCancelled.builder().unitId(bid.getUnitId()).cancelBidId(bid.getBidId()).build();
            CommandBus.accept(command, new HashMap<>());
        });
        sellPriorityQueue.clear();
    }

    private void doCancel(Long cancelBidId, Direction cancelBidDirection) {
        PriorityQueue<Bid> bids = cancelBidDirection == Direction.BUY ? buyPriorityQueue : sellPriorityQueue;
        boolean b = bids.removeIf(bid -> {
            if (bid.getBidId().equals(cancelBidId)) {
                bid.setBidStatus(BidStatus.CANCELLED);
                tunnel.updateBids(Collect.asList(bid));
                UnitCmd.IntraBidCancelled command = UnitCmd.IntraBidCancelled
                        .builder().unitId(bid.getUnitId()).cancelBidId(bid.getBidId()).build();
                CommandBus.accept(command, new HashMap<>());
                return true;
            }
            return false;
        });
        BizEx.falseThrow(b, ErrorEnums.SYS_EX.message("无可撤报单" + cancelBidId));

        // 实时
        List<Bid> sortedBuyBids = buyPriorityQueue.stream()
                .sorted(Comparator.comparing(Bid::getPrice).reversed()).collect(Collectors.toList());
        List<Ask> buyAsks = extractAsks(sortedBuyBids);

        List<Bid> sortedSellBids = sellPriorityQueue.stream()
                .sorted(Comparator.comparing(Bid::getPrice)).collect(Collectors.toList());
        List<Ask> sellAsks = extractAsks(sortedSellBids);

        List<Volume> buyVolumes = extractVolumes(buyPriorityQueue);
        List<Volume> sellVolumes = extractVolumes(sellPriorityQueue);

        StageId stageId = tunnel.runningComp().getStageId();

        IntraInstantDO intraInstantDO = IntraInstantDO.builder().price(latestPrice)
                .stageId(stageId.toString()).province(intraSymbol.getProvince()).timeFrame(intraSymbol.getTimeFrame())
                .buyAsks(buyAsks).sellAsks(sellAsks).buyVolumes(buyVolumes).sellVolumes(sellVolumes)
                .build();
        tunnel.record(null, intraInstantDO);
    }

    public void doNewBid(Bid declareBid) {
        Direction direction = declareBid.getDirection();
        declareBid.setDeclareTimeStamp(Clock.currentTimeMillis());
        declareBid.setBidStatus(BidStatus.NEW_DECELERATED);
        tunnel.insertBid(declareBid);
        if (declareBid.getDirection() == Direction.BUY) {
            buyPriorityQueue.add(declareBid);
        } else if (declareBid.getDirection() == Direction.SELL) {
            sellPriorityQueue.add(declareBid);
        } else {
            throw new RuntimeException();
        }
        Deal deal = null;
        while (true) {
            Bid buyBid = buyPriorityQueue.peek();
            Bid sellBid = sellPriorityQueue.peek();

            if (buyBid == null || sellBid == null) {
                break;
            }

            if (buyBid.getPrice() < sellBid.getPrice()) {
                break;
            }
            /*
              the deal price rule is not same with stock market,
              the stock market will use the higher price
             */
            Double dealPrice = buyBid.getDeclareTimeStamp() > sellBid.getDeclareTimeStamp() ? sellBid.getPrice() : buyBid.getPrice();
            double dealQuantity = Math.min(buyBid.getTransit(), sellBid.getTransit());
            deal = Deal.builder().quantity(dealQuantity).price(dealPrice).timeStamp(Clock.currentTimeMillis()).build();
            buyBid.getDeals().add(deal);
            sellBid.getDeals().add(deal);
            double buyBalance = buyBid.getTransit();
            if (Double.valueOf(0D).equals(buyBalance)) {
                buyBid.setBidStatus(BidStatus.COMPLETE_DEAL);
                buyPriorityQueue.remove();
            } else {
                buyBid.setBidStatus(BidStatus.PART_DEAL);
            }

            double sellBalance = sellBid.getTransit();
            if (Double.valueOf(0D).equals(sellBalance)) {
                sellBid.setBidStatus(BidStatus.COMPLETE_DEAL);
                sellPriorityQueue.remove();
            } else {
                sellBid.setBidStatus(BidStatus.PART_DEAL);
            }

            tunnel.updateBids(Collect.asList(buyBid, sellBid));

            latestPrice = dealPrice;
        }

        StageId stageId = tunnel.runningComp().getStageId();

        IntraQuotationDO intraQuotationDO = null;
        if (deal != null) {
            IntraQuotationDO.IntraQuotationDOBuilder builder = IntraQuotationDO.builder()
                    .stageId(stageId.toString()).province(intraSymbol.getProvince()).timeFrame(intraSymbol.getTimeFrame())
                    .latestPrice(latestPrice)
                    .timeStamp(Clock.currentTimeMillis());
            if (declareBid.getDirection() == Direction.BUY) {
                builder.sellQuantity(deal.getQuantity()).buyQuantity(0D);
            } else {
                builder.buyQuantity(deal.getQuantity()).sellQuantity(0D);
            }
            intraQuotationDO = builder.build();
        }

        // 实时
        List<Ask> buyAsks = extractAsks(buyPriorityQueue);
        List<Ask> sellAsks = extractAsks(sellPriorityQueue);

        List<Volume> buyVolumes = extractVolumes(buyPriorityQueue);
        List<Volume> sellVolumes = extractVolumes(sellPriorityQueue);

        IntraInstantDO intraInstantDO = IntraInstantDO.builder().price(latestPrice)
                .stageId(stageId.toString()).province(intraSymbol.getProvince()).timeFrame(intraSymbol.getTimeFrame())
                .buyAsks(buyAsks).sellAsks(sellAsks).buyVolumes(buyVolumes).sellVolumes(sellVolumes)
                .build();
        tunnel.record(intraQuotationDO, intraInstantDO);

    }

    private List<Volume> extractVolumes(Collection<Bid> bids) {
        List<Predicate<Bid>> predicates = IntStream.range(0, 10).mapToObj(i -> Pair.of(i * 200, (i + 1) * 200))
                .map(p -> (Predicate<Bid>) bid -> bid.getPrice() > p.getLeft() && bid.getPrice() <= p.getRight())
                .collect(Collectors.toList());
        List<Double> volumeQuantity = bids.stream().collect(Collect.select(predicates))
                .stream().map(bs -> bs.stream().map(Bid::getTransit).reduce(0D, Double::sum))
                .collect(Collectors.toList());
        return IntStream.range(0, 10)
                .mapToObj(i -> new Volume(String.format("%s-%s", i * 200, (i + 1) * 200), volumeQuantity.get(i)))
                .collect(Collectors.toList());
    }


    private List<Ask> extractAsks(Iterable<Bid> bids) {
        List<Ask> asks = new ArrayList<>();
        for (Bid bid : bids) {
            if (asks.size() == 0 || !asks.get(asks.size() - 1).getPrice().equals(bid.getPrice())) {
                if (asks.size() >= 5) {
                    break;
                }
                double balance = bid.getQuantity() - bid.getDeals().stream().map(Deal::getQuantity).reduce(0D, Double::sum);
                Ask ask = new Ask(balance, bid.getPrice());
                asks.add(ask);
            } else {
                double balance = bid.getQuantity() - bid.getDeals().stream().map(Deal::getQuantity).reduce(0D, Double::sum);
                Ask ask = asks.get(asks.size() - 1);
                ask.setQuantity(ask.getQuantity() + balance);
            }
        }
        return asks;
    }


}
