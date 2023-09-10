package com.bilanee.octopus.domain;

import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.Bid;
import com.bilanee.octopus.basic.Deal;
import com.bilanee.octopus.basic.ErrorEnums;
import com.bilanee.octopus.basic.StageId;
import com.bilanee.octopus.basic.enums.BidStatus;
import com.bilanee.octopus.basic.enums.Direction;
import com.bilanee.octopus.infrastructure.entity.Ask;
import com.bilanee.octopus.infrastructure.entity.IntraMarketHistoryDO;
import com.bilanee.octopus.infrastructure.entity.IntraMarketRealtimeDO;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.stellariver.milky.common.base.SysEx;
import com.stellariver.milky.common.tool.common.Clock;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.domain.support.command.CommandBus;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.stream.Collectors;

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
            rtBidContainer.setClose(false);
            rtBidContainer.setCancelBidId(null);
            rtBidContainer.setDeclareBid(bid);
        });
    }

    public void cancel(Long cancelBidId) {
        disruptor.publishEvent((rtBidContainer, sequence) -> {
            rtBidContainer.setClose(false);
            rtBidContainer.setCancelBidId(cancelBidId);
            rtBidContainer.setDeclareBid(null);
        });
    }

    public void cancelAll() {
        disruptor.publishEvent((rtBidContainer, sequence) -> {
            rtBidContainer.setClose(true);
            rtBidContainer.setDeclareBid(null);
            rtBidContainer.setCancelBidId(null);
        });
    }


    @Override
    public void onEvent(IntraBidContainer event, long sequence, boolean endOfBatch) {
        if (event.getDeclareBid() != null){
            doProcessNewBid(event.getDeclareBid());
        } else if (event.getCancelBidId() != null) {
            doProcessCancel(event.getCancelBidId());
        } else if (event.getClose()) {
            doClose();
        } else {
            throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
        }

    }

    private void doClose() {

        buyPriorityQueue.forEach(bid -> bid.setBidStatus(BidStatus.CANCELLED));
        tunnel.updateBids(new ArrayList<>(buyPriorityQueue));
        buyPriorityQueue.forEach(bid -> {
            UnitCmd.IntraCancelled command = UnitCmd.IntraCancelled.builder().unitId(bid.getUnitId()).cancelBidId(bid.getBidId()).build();
            CommandBus.accept(command, new HashMap<>());
        });
        buyPriorityQueue.clear();

        sellPriorityQueue.forEach(bid -> bid.setBidStatus(BidStatus.CANCELLED));
        tunnel.updateBids(new ArrayList<>(sellPriorityQueue));
        sellPriorityQueue.forEach(bid -> {
            UnitCmd.IntraCancelled command = UnitCmd.IntraCancelled.builder().unitId(bid.getUnitId()).cancelBidId(bid.getBidId()).build();
            CommandBus.accept(command, new HashMap<>());
        });
        sellPriorityQueue.clear();
    }

    private void doProcessCancel(Long cancelBidId) {
        boolean b = buyPriorityQueue.removeIf(bid -> {
            if (bid.getBidId().equals(cancelBidId)) {
                bid.setBidStatus(BidStatus.CANCELLED);
                tunnel.updateBids(Collect.asList(bid));
                UnitCmd.IntraCancelled command = UnitCmd.IntraCancelled.builder().unitId(bid.getUnitId()).cancelBidId(bid.getBidId()).build();
                CommandBus.accept(command, new HashMap<>());
                return true;
            }
            return false;
        });
        if ( !b ) {
            buyPriorityQueue.removeIf(bid -> {
                if (bid.getBidId().equals(cancelBidId)) {
                    bid.setBidStatus(BidStatus.CANCELLED);
                    tunnel.updateBids(Collect.asList(bid));
                    UnitCmd.IntraCancelled command = UnitCmd.IntraCancelled.builder().unitId(bid.getUnitId()).cancelBidId(bid.getBidId()).build();
                    CommandBus.accept(command, new HashMap<>());
                    return true;
                }
                return false;
            });
        }
    }

    public void doProcessNewBid(Bid declareBid) {
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
            double dealQuantity = Math.min(buyBid.getQuantity(), sellBid.getQuantity());
            Deal deal = Deal.builder().quantity(dealQuantity).price(dealPrice).timeStamp(Clock.currentTimeMillis()).build();
            buyBid.getDeals().add(deal);
            sellBid.getDeals().add(deal);
            double buyBalance = buyBid.getQuantity() - dealQuantity;
            if (buyBalance == 0L) {
                buyBid.setBidStatus(BidStatus.COMPLETE_DEAL);
                buyPriorityQueue.remove();
            } else {
                buyBid.setBidStatus(BidStatus.PART_DEAL);
            }

            double sellBalance = sellBid.getQuantity() - dealQuantity;
            if (sellBalance == 0L) {
                sellBid.setBidStatus(BidStatus.COMPLETE_DEAL);
                sellPriorityQueue.remove();
            } else {
                sellBid.setBidStatus(BidStatus.PART_DEAL);
            }

            tunnel.updateBids(Collect.asList(buyBid, sellBid));

            latestPrice = dealPrice;
        }

        StageId stageId = tunnel.runningComp().getStageId();

        // history
        Double buyTotalQuantity = buyPriorityQueue.stream().map(Bid::getBalance).reduce(0D, Double::sum);
        Double sellTotalQuantity = buyPriorityQueue.stream().map(Bid::getBalance).reduce(0D, Double::sum);
        Triple<Double, Double, Double> market = Triple.of(buyTotalQuantity, sellTotalQuantity, latestPrice);
        IntraMarketHistoryDO intraMarketHistoryDO = IntraMarketHistoryDO.builder()
                .stageId(stageId.toString()).province(intraSymbol.getProvince()).timeFrame(intraSymbol.getTimeFrame())
                .buyQuantity(buyTotalQuantity).sellQuantity(sellTotalQuantity).latestPrice(latestPrice)
                .build();

        // 实时
        List<Ask> buyAsks = extractAsks(buyPriorityQueue);
        List<Ask> sellAsks = extractAsks(sellPriorityQueue);

        List<Double> buySections = extractSections(buyPriorityQueue);
        List<Double> sellSections = extractSections(sellPriorityQueue);

        IntraMarketRealtimeDO intraMarketRealtimeDO = IntraMarketRealtimeDO.builder()
                .stageId(stageId.toString()).province(intraSymbol.getProvince()).timeFrame(intraSymbol.getTimeFrame())
                .buyAsks(buyAsks).sellAsks(sellAsks).buySections(buySections).sellSections(sellSections)
                .build();
        tunnel.record(intraMarketHistoryDO, intraMarketRealtimeDO);

    }

    private List<Double> extractSections(Collection<Bid> bids) {
        return bids.stream().collect(Collect.select(
                bid -> bid.getPrice() > 0D && bid.getBalance() <= 400D,
                bid -> bid.getPrice() > 400D && bid.getBalance() <= 800D,
                bid -> bid.getPrice() > 800D && bid.getBalance() <= 1200D,
                bid -> bid.getPrice() > 1200D && bid.getBalance() <= 1600D,
                bid -> bid.getPrice() > 1600D && bid.getBalance() <= 2000D
        )).stream().map(bs -> bs.stream().map(Bid::getBalance).reduce(0D, Double::sum)).collect(Collectors.toList());
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
