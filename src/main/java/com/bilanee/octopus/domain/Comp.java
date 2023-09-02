package com.bilanee.octopus.domain;

import com.bilanee.octopus.adapter.BidQuery;
import com.bilanee.octopus.adapter.Tunnel;
import com.bilanee.octopus.basic.*;
import com.stellariver.milky.common.base.StaticWire;
import com.stellariver.milky.common.base.SysEx;
import com.stellariver.milky.common.tool.common.Clock;
import com.stellariver.milky.common.tool.common.Kit;
import com.stellariver.milky.common.tool.util.Collect;
import com.stellariver.milky.domain.support.ErrorEnums;
import com.stellariver.milky.domain.support.base.AggregateRoot;
import com.stellariver.milky.domain.support.command.Command;
import com.stellariver.milky.domain.support.command.CommandBus;
import com.stellariver.milky.domain.support.command.ConstructorHandler;
import com.stellariver.milky.domain.support.command.MethodHandler;
import com.stellariver.milky.domain.support.context.Context;
import com.stellariver.milky.domain.support.dependency.UniqueIdGetter;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Data
@CustomLog
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Comp extends AggregateRoot {

    Long compId;

    Integer roundTotal = 3;

    CompStage compStage;
    Integer roundId;
    TradeStage tradeStage;
    MarketStatus marketStatus;

    Long endingTimeStamp;


    @StaticWire
    static private DelayExecutor delayExecutor;
    @StaticWire
    static private OctopusProperties octopusProperties;
    @StaticWire
    static private Tunnel tunnel;
    @StaticWire
    UniqueIdGetter uniqueIdGetter;

    @Override
    public String getAggregateId() {
        return compId.toString();
    }


    @ConstructorHandler
    public static Comp create(CompCmd.Create command, Context context) {

        // init comp status
        Comp comp = new Comp();
        comp.setCompId(command.getCompId());
        comp.setCompStage(CompStage.INT);
        long endingTimeStamp = Clock.currentTimeMillis()
                + (long) command.getCompInitLength() * octopusProperties.getDelayUnits();
        comp.setEndingTimeStamp(endingTimeStamp);

        // assign metaUnit
        List<Map<String, List<MetaUnit>>> roundMetaUnitDOs = IntStream.range(0, comp.getRoundTotal())
                .mapToObj(roundId -> tunnel.assignMetaUnits(roundId, command.getUserIds())).collect(Collectors.toList());

        // fill stage step trigger
        fillDelayCommand(command, comp);
        delayExecutor.start();

        CompEvent.Created event = CompEvent.Created.builder().comp(comp).roundMetaUnits(roundMetaUnitDOs).build();
        context.publish(event);
        return comp;
    }



    private static void fillDelayCommand(CompCmd.Create command, Comp comp) {
        // quiz compete
        long executeTime = comp.endingTimeStamp;
        long endingTimeStamp = executeTime
                + (long) command.getQuitCompeteLength() * octopusProperties.getDelayUnits();
        CompCmd.Step stepCommand = CompCmd.Step.builder()
                .compId(comp.getCompId())
                .compStage(CompStage.QUIT_COMPETE)
                .endingTimeStamp(endingTimeStamp)
                .build();
        pushDelayCommand(stepCommand, executeTime);

        executeTime = endingTimeStamp;
        endingTimeStamp = executeTime
                + (long) command.getQuitResultLength() * octopusProperties.getDelayUnits();
        stepCommand = CompCmd.Step.builder()
                .compId(comp.getCompId())
                .compStage(CompStage.QUIT_RESULT)
                .endingTimeStamp(endingTimeStamp)
                .build();
        pushDelayCommand(stepCommand, executeTime);

        for (int i = 0; i < comp.roundTotal; i++) {
            for (TradeStage marketStage : TradeStage.marketStages()) {
                executeTime = endingTimeStamp;
                endingTimeStamp = executeTime
                        + (long) command.getMarketStageBidLengths().get(marketStage) * octopusProperties.getDelayUnits();
                stepCommand = CompCmd.Step.builder().compId(comp.getCompId()).compStage(CompStage.TRADE)
                        .roundId(i).tradeStage(marketStage).marketStatus(MarketStatus.BID).endingTimeStamp(endingTimeStamp).build();
                pushDelayCommand(stepCommand, executeTime);
                executeTime = endingTimeStamp;
                endingTimeStamp = executeTime
                        + (long) command.getMarketStageClearLengths().get(marketStage) * octopusProperties.getDelayUnits();
                stepCommand = CompCmd.Step.builder().compId(comp.getCompId()).compStage(CompStage.TRADE)
                        .roundId(i).tradeStage(marketStage).marketStatus(MarketStatus.CLEAR).endingTimeStamp(endingTimeStamp).build();
                pushDelayCommand(stepCommand, executeTime);
            }
            executeTime = endingTimeStamp;
            endingTimeStamp = executeTime
                    + (long) command.getTradeResultLength() * octopusProperties.getDelayUnits();
            stepCommand = CompCmd.Step.builder().compId(comp.getCompId()).compStage(CompStage.TRADE)
                    .roundId(i).tradeStage(TradeStage.END).marketStatus(null).endingTimeStamp(endingTimeStamp).build();
            pushDelayCommand(stepCommand, executeTime);
        }

        executeTime = endingTimeStamp;
        stepCommand = CompCmd.Step.builder()
                .compId(comp.getCompId())
                .compStage(CompStage.RANKING)
                .endingTimeStamp(null)
                .build();
        pushDelayCommand(stepCommand, executeTime);
    }

    @MethodHandler
    public void clear(CompCmd.Clear command, Context context) {
        SysEx.trueThrow((tradeStage != TradeStage.AN_INTER) && (tradeStage != TradeStage.MO_INTER), ErrorEnums.SYS_EX);
        SysEx.trueThrow(marketStatus != MarketStatus.CLEAR, ErrorEnums.SYS_EX);
        BidQuery bidQuery = BidQuery.builder().compId(compId).roundId(roundId).tradeStage(tradeStage).build();
        List<Bid> bids = tunnel.listBids(bidQuery);
        List<Object> collect = tunnel.listBids(bidQuery)
                .stream().collect(Collect.listMultiMap(Bid::getTimeFrame))
                .asMap().values().stream().map(this::doClear).collect(Collectors.toList());
//        tunnel.
    }

    private Object doClear(Collection<Bid> bids) {
        List<Bid> buyBids = bids.stream().filter(bid -> bid.getDirection() == Direction.BUY)
                .sorted(Comparator.comparing(Bid::getPrice).reversed()).collect(Collectors.toList());
        List<Bid> sellBids = bids.stream().filter(bid -> bid.getDirection() == Direction.SELL)
                .sorted(Comparator.comparing(Bid::getPrice)).collect(Collectors.toList());
        Point<Double> interPoint = ClearUtil.analyzeInterPoint(buyBids, sellBids);
        if (interPoint == null || Kit.eq(interPoint.x, 0D)) {
            return null;
        }
        ClearUtil.deal(buyBids, interPoint, uniqueIdGetter);
        ClearUtil.deal(sellBids, interPoint, uniqueIdGetter);

        return null;

    }


    @MethodHandler
    public void step(CompCmd.Step command, Context context) {
        this.compStage = command.getCompStage();
        this.roundId = command.getRoundId();
        this.tradeStage = command.getTradeStage();
        this.marketStatus = command.getMarketStatus();
        this.endingTimeStamp = command.getEndingTimeStamp();
        context.publishPlaceHolderEvent(getAggregateId());
    }


    static private void pushDelayCommand(CompCmd.Step command, long executeTime) {
        DelayCommandWrapper delayCommandWrapper = new DelayCommandWrapper(command, new Date(executeTime));
        delayExecutor.delayQueue.add(delayCommandWrapper);
    }


    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static class DelayCommandWrapper implements Delayed {

        private Command command;
        private Date executeDate;

        public DelayCommandWrapper(Command command, Date executeDate) {
            this.command = command;
            this.executeDate = executeDate;
        }

        @Override
        public long getDelay(TimeUnit timeUnit) {
            return timeUnit.convert(executeDate.getTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(@NonNull Delayed o) {
            return Long.compare(executeDate.getTime(), ((DelayCommandWrapper) o).getExecuteDate().getTime());
        }

    }


    @Component
    @FieldDefaults(level = AccessLevel.PRIVATE)
    static class DelayExecutor implements Runnable{

        final DelayQueue<DelayCommandWrapper> delayQueue = new DelayQueue<>();
        final ExecutorService executorService = Executors.newFixedThreadPool(1);
        final AtomicBoolean started = new AtomicBoolean(false);
        public void start() {
            boolean b = started.compareAndSet(false, true);
            if (b) {
                executorService.execute(this);
            }
        }

        @Override
        @SuppressWarnings("all")
        public void run() {

            while (true) {
                DelayCommandWrapper delayCommandWrapper;
                try {
                    delayCommandWrapper = delayQueue.take();
                } catch (InterruptedException e) {
                    throw new SysEx(e);
                }
                Command command = delayCommandWrapper.getCommand();
                CommandBus.accept(command, new HashMap<>());
            }

        }

    }


}
