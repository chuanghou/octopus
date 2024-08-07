package com.bilanee.octopus.domain;

import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.adapter.ws.WebSocket;
import com.bilanee.octopus.adapter.ws.WsMessage;
import com.bilanee.octopus.adapter.ws.WsTopic;
import com.bilanee.octopus.basic.*;
import com.bilanee.octopus.basic.enums.*;
import com.bilanee.octopus.infrastructure.entity.Ask;
import com.bilanee.octopus.infrastructure.entity.IntraInstantDO;
import com.bilanee.octopus.infrastructure.entity.IntraQuotationDO;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.stellariver.milky.common.base.SysEx;
import com.stellariver.milky.common.tool.common.Clock;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.domain.support.command.CommandBus;
import com.stellariver.milky.domain.support.dependency.UniqueIdGetter;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.MDC;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@CustomLog
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Processor implements EventHandler<IntraBidContainer> {

    final Tunnel tunnel;
    final Object symbol;
    final Disruptor<IntraBidContainer> disruptor = new Disruptor<>(IntraBidContainer::new, 1024, DaemonThreadFactory.INSTANCE);
    final PriorityQueue<Bid> buyPriorityQueue = new PriorityQueue<>(buyComparator);
    final PriorityQueue<Bid> sellPriorityQueue = new PriorityQueue<>(sellComparator);
    final UniqueIdGetter uniqueIdGetter;

    private Double latestPrice = 0D;

    private final BlockingQueue<Object> blockingQueue = new ArrayBlockingQueue<>(1);

    public Processor(Tunnel tunnel, Object symbol, UniqueIdGetter uniqueIdGetter) {
        this.tunnel = tunnel;
        this.symbol = symbol;
        this.uniqueIdGetter = uniqueIdGetter;
        disruptor.handleEventsWith(this);
        disruptor.setDefaultExceptionHandler(new ExceptionHandler<IntraBidContainer>() {
            @Override
            public void handleEventException(Throwable ex, long sequence, IntraBidContainer event) {
                log.error("event is {}", event, ex);
            }

            @Override
            public void handleOnStartException(Throwable ex) {
                log.error("handleOnStartException", ex);
            }

            @Override
            public void handleOnShutdownException(Throwable ex) {
                log.error("handleOnShutdownException", ex);
            }
        });
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

    public boolean empty() {
        return disruptor.getRingBuffer().remainingCapacity() == disruptor.getBufferSize();
    }


    public void declare(Bid bid) {
        disruptor.publishEvent((rtBidContainer, sequence) -> {
            rtBidContainer.setTraceId(MDC.get("traceId"));
            rtBidContainer.setOperation(Operation.DECLARE);
            rtBidContainer.setDeclareBid(bid);
            long blockingNumber = disruptor.getBufferSize() - disruptor.getRingBuffer().remainingCapacity();
            log.info("disruptor.publishNewBidEvent {}, blocking number {}", rtBidContainer, blockingNumber);
        });
    }

    public void cancel(Long cancelBidId, Direction cancelBidDirection) {
        disruptor.publishEvent((rtBidContainer, sequence) -> {
            rtBidContainer.setTraceId(MDC.get("traceId"));
            rtBidContainer.setOperation(Operation.CANCEL);
            rtBidContainer.setCancelBidId(cancelBidId);
            rtBidContainer.setCancelBidDirection(cancelBidDirection);
            rtBidContainer.setEnqueue(new Date());
            long blockingNumber = disruptor.getBufferSize() - disruptor.getRingBuffer().remainingCapacity();
            log.info("disruptor.publishCancelBidEvent {}, blocking number {}", rtBidContainer, blockingNumber);
        });
    }

    @SneakyThrows
    public void close() {
        blockingQueue.clear();
        disruptor.publishEvent((rtBidContainer, sequence) -> {
            rtBidContainer.setTraceId(MDC.get("traceId"));
            rtBidContainer.setOperation(Operation.CLOSE);
            rtBidContainer.setEnqueue(new Date());
            long blockingNumber = disruptor.getBufferSize() - disruptor.getRingBuffer().remainingCapacity();
            log.info("disruptor.publishCancelBidEvent {}, blocking number {}", rtBidContainer, blockingNumber);
        });
        blockingQueue.poll(5, TimeUnit.MINUTES);
    }

    public void clear() {
        buyPriorityQueue.clear();
        sellPriorityQueue.clear();
    }

    @Override
    public void onEvent(IntraBidContainer event, long sequence, boolean endOfBatch) {
        try {
            if (event.getTraceId() != null) {
                MDC.put("traceId", event.getTraceId());
            }
            log.info("onEvent(IntraBidContainer event {}", event);
            doOnEvent(event, sequence, endOfBatch);
        } catch (Throwable throwable) {
            if (event.getOperation() == Operation.CLOSE) {
                blockingQueue.add(new Object());
            }
            log.error("Processor process failure", throwable);
        } finally {
            MDC.remove("traceId");
            event.setEnqueue(null);
            log.info("event enqueue {}, finish {}", event.getEnqueue(), new Date());
        }
    }

    public void doOnEvent(IntraBidContainer event, long sequence, boolean endOfBatch) {

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
            WebSocket.cast(WsMessage.builder().wsTopic(wsTopic).build());
        } else if (tradeStage == TradeStage.MO_INTRA) {
            wsTopic = WsTopic.MO_INTRA_BID;
            WebSocket.cast(WsMessage.builder().wsTopic(wsTopic).build());
        } else if (tradeStage == TradeStage.ROLL) {
            wsTopic = WsTopic.ROLL_BID;
            WebSocket.cast(WsMessage.builder().wsTopic(wsTopic).build());
        }
        if (event.getOperation() == Operation.CLOSE) {
            blockingQueue.add(new Object());
        }
    }

    private void doClose() {

        buyPriorityQueue.forEach(bid -> {
            bid.setCloseBalance(bid.getTransit());
            bid.setBidStatus(BidStatus.SYSTEM_CANCELLED);
        });
        tunnel.updateBids(new ArrayList<>(buyPriorityQueue));
        buyPriorityQueue.forEach(bid -> {
            if (bid.getTimeFrame() != null) {
                UnitCmd.IntraBidCancelled command = UnitCmd.IntraBidCancelled
                        .builder().unitId(bid.getUnitId()).cancelBidId(bid.getBidId()).build();
                CommandBus.accept(command, new HashMap<>());
            } else {
                UnitCmd.RollBidCancelled command = UnitCmd.RollBidCancelled
                        .builder().unitId(bid.getUnitId()).cancelBidId(bid.getBidId()).build();
                CommandBus.accept(command, new HashMap<>());
            }
        });
        buyPriorityQueue.clear();

        sellPriorityQueue.forEach(bid -> {
            bid.setCloseBalance(bid.getTransit());
            bid.setBidStatus(BidStatus.SYSTEM_CANCELLED);
        });
        tunnel.updateBids(new ArrayList<>(sellPriorityQueue));
        sellPriorityQueue.forEach(bid -> {
            if (bid.getTimeFrame() != null) {
                UnitCmd.IntraBidCancelled command = UnitCmd.IntraBidCancelled
                        .builder().unitId(bid.getUnitId()).cancelBidId(bid.getBidId()).build();
                CommandBus.accept(command, new HashMap<>());
            } else {
                UnitCmd.RollBidCancelled command = UnitCmd.RollBidCancelled
                        .builder().unitId(bid.getUnitId()).cancelBidId(bid.getBidId()).build();
                CommandBus.accept(command, new HashMap<>());
            }
        });
        sellPriorityQueue.clear();
        latestPrice = null;
    }

    private void doCancel(Long cancelBidId, Direction cancelBidDirection) {
        PriorityQueue<Bid> bids = cancelBidDirection == Direction.BUY ? buyPriorityQueue : sellPriorityQueue;
        boolean b = bids.removeIf(bid -> {
            if (bid.getBidId().equals(cancelBidId)) {
                bid.setCancelledTimeStamp(Clock.currentTimeMillis());
                bid.setBidStatus(BidStatus.MANUAL_CANCELLED);
                tunnel.updateBids(Collect.asList(bid));
                if (bid.getTimeFrame() != null) {
                    UnitCmd.IntraBidCancelled command = UnitCmd.IntraBidCancelled
                            .builder().unitId(bid.getUnitId()).cancelBidId(bid.getBidId()).build();
                    CommandBus.accept(command, new HashMap<>());
                } else {
                    UnitCmd.RollBidCancelled command = UnitCmd.RollBidCancelled
                            .builder().unitId(bid.getUnitId()).cancelBidId(bid.getBidId()).build();
                    CommandBus.accept(command, new HashMap<>());
                }
                return true;
            }
            return false;
        });

        if (!b) {
            return;
        }

        // 实时
        List<Bid> sortedBuyBids = buyPriorityQueue.stream()
                .sorted(Comparator.comparing(Bid::getPrice).reversed()).collect(Collectors.toList());
        List<Ask> buyAsks = extractAsks(sortedBuyBids);

        List<Bid> sortedSellBids = sellPriorityQueue.stream()
                .sorted(Comparator.comparing(Bid::getPrice)).collect(Collectors.toList());
        List<Ask> sellAsks = extractAsks(sortedSellBids);

        List<Volume> buyVolumes = extractVolumes(buyPriorityQueue, false);
        List<Volume> sellVolumes = extractVolumes(sellPriorityQueue, true);

        StageId stageId = tunnel.runningComp().getStageId();
        IntraInstantDO.IntraInstantDOBuilder builder = IntraInstantDO.builder().price(latestPrice)
                .stageId(stageId.toString())
                .buyAsks(buyAsks).sellAsks(sellAsks).buyVolumes(buyVolumes).sellVolumes(sellVolumes);
        if (symbol instanceof IntraSymbol) {
            Province province = ((IntraSymbol) symbol).getProvince();
            TimeFrame timeFrame = ((IntraSymbol) symbol).getTimeFrame();
            builder.province(province).timeFrame(timeFrame);
        } else if (symbol instanceof RollSymbol){
            Province province = ((RollSymbol) symbol).getProvince();
            Integer instant = ((RollSymbol) symbol).getInstant();
            builder.province(province).instant(instant);
        }
        tunnel.record(null, builder.build());
    }

    public void doNewBid(Bid declareBid) {
        declareBid.setDeclareTimeStamp(Clock.currentTimeMillis());
        declareBid.setBidStatus(BidStatus.NEW_DECELERATED);
        tunnel.insertBid(declareBid);
        log.info("tunnel.insertBid(declareBid) {}", declareBid);
        if (declareBid.getDirection() == Direction.BUY) {
            buyPriorityQueue.add(declareBid);
        } else if (declareBid.getDirection() == Direction.SELL) {
            sellPriorityQueue.add(declareBid);
        } else {
            throw new RuntimeException();
        }

        StageId stageId = tunnel.runningComp().getStageId();

        while (true) {

            Deal deal;
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

            Deal.DealBuilder<?, ?> builder = Deal.builder().id(uniqueIdGetter.get()).buyUnitId(buyBid.getUnitId()).sellUnitId(sellBid.getUnitId())
                    .quantity(dealQuantity).price(dealPrice).timeStamp(Clock.currentTimeMillis());
            if (symbol instanceof IntraSymbol) {
                builder.timeFrame(((IntraSymbol) symbol).getTimeFrame());
            } else if (symbol instanceof RollSymbol) {
                builder.instant(((RollSymbol) symbol).getInstant());
            } else {
                throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
            }
            deal = builder.build();
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

            log.info("tunnel.updateBids(Collect.asList(buyBid, sellBid)) {} {}", buyBid, sellBid);
            tunnel.updateBids(Collect.asList(buyBid, sellBid));

            latestPrice = dealPrice;

            if (deal != null) {
                IntraQuotationDO.IntraQuotationDOBuilder intraQuotationDOBuilder = IntraQuotationDO.builder()
                        .stageId(stageId.toString())
                        .latestPrice(latestPrice)
                        .timeStamp(Clock.currentTimeMillis());
                if (symbol instanceof IntraSymbol) {
                    intraQuotationDOBuilder.province(((IntraSymbol) symbol).getProvince())
                            .timeFrame(((IntraSymbol) symbol).getTimeFrame());
                } else {
                    intraQuotationDOBuilder.province(((RollSymbol) symbol).getProvince())
                            .instant(((RollSymbol) symbol).getInstant());
                }
                if (declareBid.getDirection() == Direction.BUY) {
                    intraQuotationDOBuilder.sellQuantity(0D).buyQuantity(deal.getQuantity());
                } else {
                    intraQuotationDOBuilder.sellQuantity(deal.getQuantity()).buyQuantity(0D);
                }
                tunnel.recordIntraQuotationDO(intraQuotationDOBuilder.build());
            }
        }

        // 实时
        List<Bid> sortedBuyBids = buyPriorityQueue.stream()
                .sorted(Comparator.comparing(Bid::getPrice).reversed()).collect(Collectors.toList());
        List<Ask> buyAsks = extractAsks(sortedBuyBids);

        List<Bid> sortedSellBids = sellPriorityQueue.stream()
                .sorted(Comparator.comparing(Bid::getPrice)).collect(Collectors.toList());
        List<Ask> sellAsks = extractAsks(sortedSellBids);

        List<Volume> buyVolumes = extractVolumes(buyPriorityQueue, false);
        List<Volume> sellVolumes = extractVolumes(sellPriorityQueue, true);

        IntraInstantDO.IntraInstantDOBuilder builder = IntraInstantDO.builder().price(latestPrice).stageId(stageId.toString())
                .buyAsks(buyAsks).sellAsks(sellAsks).buyVolumes(buyVolumes).sellVolumes(sellVolumes);
        if (symbol instanceof IntraSymbol) {
            builder.province(((IntraSymbol) symbol).getProvince()).timeFrame(((IntraSymbol) symbol).getTimeFrame());
        } else if (symbol instanceof RollSymbol) {
            builder.province(((RollSymbol) symbol).getProvince()).instant(((RollSymbol) symbol).getInstant());
        } else {
            throw new SysEx(ErrorEnums.UNREACHABLE_CODE);
        }

        tunnel.recordIntraInstantDO(builder.build());

    }

    private List<Volume> extractVolumes(Collection<Bid> bids, boolean natural) {
        long count = bids.stream().map(Bid::getPrice).distinct().count();
        if (count <= 5) {
            Comparator<Map.Entry<Double, List<Bid>>> comparator = natural ? Map.Entry.comparingByKey() : Map.Entry.<Double, List<Bid>>comparingByKey().reversed();
            return bids.stream().collect(Collectors.groupingBy(Bid::getPrice)).entrySet().stream().sorted(comparator)
                    .map(e -> new Volume(e.getKey().toString(), e.getValue().stream().collect(Collectors.summarizingDouble(Bid::getTransit)).getSum())).collect(Collectors.toList());
        } else {
            double maxPrice = bids.stream().max(Comparator.comparing(Bid::getPrice)).map(Bid::getPrice).orElseThrow(SysEx::unreachable) + 1;
            double minPrice = bids.stream().min(Comparator.comparing(Bid::getPrice)).map(Bid::getPrice).orElseThrow(SysEx::unreachable) - 1;
            double v = (maxPrice - minPrice) / 5;
            List<Predicate<Bid>> predicates = IntStream.range(0, 5).mapToObj(i -> Pair.of(i * v + minPrice, (i + 1) * v + minPrice))
                    .map(p -> (Predicate<Bid>) bid -> bid.getPrice() >= p.getLeft() && bid.getPrice() < p.getRight())
                    .collect(Collectors.toList());
            List<Double> volumeQuantity = bids.stream().collect(Collect.select(predicates))
                    .stream().map(bs -> bs.stream().map(Bid::getTransit).reduce(0D, Double::sum))
                    .collect(Collectors.toList());
            List<Volume> volumes = IntStream.range(0, 5)
                    .mapToObj(i -> new Volume(String.format("%.2f-%.2f", i * v + minPrice, (i + 1) * v + minPrice), volumeQuantity.get(i)))
                    .collect(Collectors.toList());
            if (!natural) {
                Collections.reverse(volumes);
            }
            return volumes;

        }

    }


    private List<Ask> extractAsks(Iterable<Bid> bids) {
        List<Ask> asks = new ArrayList<>();
        for (Bid bid : bids) {
            if (asks.isEmpty() || !asks.get(asks.size() - 1).getPrice().equals(bid.getPrice())) {
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
