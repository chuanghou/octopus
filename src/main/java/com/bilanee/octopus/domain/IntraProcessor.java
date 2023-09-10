package com.bilanee.octopus.domain;

import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.Bid;
import com.bilanee.octopus.basic.Deal;
import com.bilanee.octopus.basic.ErrorEnums;
import com.bilanee.octopus.basic.enums.BidStatus;
import com.bilanee.octopus.basic.enums.Direction;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.stellariver.milky.common.base.SysEx;
import com.stellariver.milky.common.tool.common.Clock;
import com.stellariver.milky.common.tool.util.Collect;
import lombok.AccessLevel;

import lombok.experimental.FieldDefaults;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntraProcessor implements EventHandler<IntraBidContainer> {

    final Tunnel tunnel;

    final Disruptor<IntraBidContainer> disruptor = new Disruptor<>(IntraBidContainer::new, 1024, DaemonThreadFactory.INSTANCE);

    final PriorityQueue<Bid> buyPriorityQueue = new PriorityQueue<>(buyComparator);

    final PriorityQueue<Bid> sellPriorityQueue = new PriorityQueue<>(sellComparator);


    public IntraProcessor(Tunnel tunnel) {
        this.tunnel = tunnel;
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

        buyPriorityQueue.forEach(bid -> {
            bid.setBidStatus(BidStatus.CANCELLED);
        });
        tunnel.updateBids(new ArrayList<>(buyPriorityQueue));

        sellPriorityQueue.forEach(bid -> {
            bid.setBidStatus(BidStatus.CANCELLED);
        });
        tunnel.updateBids(new ArrayList<>(sellPriorityQueue));

    }

    private void doProcessCancel(Long cancelBidId) {
        boolean b = buyPriorityQueue.removeIf(bid -> {
            if (bid.getBidId().equals(cancelBidId)) {
                bid.setBidStatus(BidStatus.CANCELLED);
                tunnel.updateBids(Collect.asList(bid));
                return true;
            }
            return false;
        });
        if ( !b ) {
            buyPriorityQueue.removeIf(bid -> {
                if (bid.getBidId().equals(cancelBidId)) {
                    bid.setBidStatus(BidStatus.CANCELLED);
                    tunnel.updateBids(Collect.asList(bid));
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
        Bid buyBid = buyPriorityQueue.peek();
        Bid sellBid = sellPriorityQueue.peek();

        if (buyBid == null || sellBid == null) {
            return;
        }

        if (buyBid.getPrice() < sellBid.getPrice()) {
            return;
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
    }


}
