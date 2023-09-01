package com.bilanee.octopus.domain;

import com.bilanee.octopus.basic.CompStage;
import com.bilanee.octopus.basic.MarketStatus;
import com.bilanee.octopus.basic.OctopusProperties;
import com.bilanee.octopus.basic.TradeStage;
import com.stellariver.milky.common.base.StaticWire;
import com.stellariver.milky.common.base.SysEx;
import com.stellariver.milky.common.tool.common.Clock;
import com.stellariver.milky.domain.support.base.AggregateRoot;
import com.stellariver.milky.domain.support.command.Command;
import com.stellariver.milky.domain.support.command.CommandBus;
import com.stellariver.milky.domain.support.command.ConstructorHandler;
import com.stellariver.milky.domain.support.command.MethodHandler;
import com.stellariver.milky.domain.support.context.Context;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;


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

    @Override
    public String getAggregateId() {
        return compId.toString();
    }


    @ConstructorHandler
    public static Comp create(CompCommand.Create command, Context context) {

        // init
        Comp comp = new Comp();
        comp.setCompId(command.getCompId());
        comp.setCompStage(CompStage.INT);
        comp.setEndingTimeStamp(Clock.currentTimeMillis() + (long) command.getCompInitLength() * octopusProperties.getDelayUnits());

        fillDelayCommand(command, comp);
        delayExecutor.start();

        context.publishPlaceHolderEvent(comp.getAggregateId());
        return comp;
    }

    private static void fillDelayCommand(CompCommand.Create command, Comp comp) {
        // quiz compete
        long executeTime = comp.endingTimeStamp;
        long endingTimeStamp = executeTime + (long) command.getQuitCompeteLength() * octopusProperties.getDelayUnits();
        CompCommand.Step stepCommand = CompCommand.Step.builder()
                .compId(comp.getCompId())
                .compStage(CompStage.QUIT_COMPETE)
                .endingTimeStamp(endingTimeStamp)
                .build();
        pushDelayCommand(stepCommand, executeTime);

        executeTime = endingTimeStamp;
        endingTimeStamp = executeTime + (long) command.getQuitResultLength() * octopusProperties.getDelayUnits();
        stepCommand = CompCommand.Step.builder()
                .compId(comp.getCompId())
                .compStage(CompStage.QUIT_RESULT)
                .endingTimeStamp(endingTimeStamp)
                .build();
        pushDelayCommand(stepCommand, executeTime);

        for (int i = 0; i < comp.roundTotal; i++) {
            for (TradeStage marketStage : TradeStage.marketStages()) {
                executeTime = endingTimeStamp;
                endingTimeStamp = executeTime + (long) command.getMarketStageBidLengths().get(marketStage) * octopusProperties.getDelayUnits();
                stepCommand = CompCommand.Step.builder().compId(comp.getCompId()).compStage(CompStage.TRADE)
                        .roundId(i).tradeStage(marketStage).marketStatus(MarketStatus.BID).endingTimeStamp(endingTimeStamp).build();
                pushDelayCommand(stepCommand, executeTime);
                executeTime = endingTimeStamp;
                endingTimeStamp = executeTime + (long) command.getMarketStageClearLengths().get(marketStage) * octopusProperties.getDelayUnits();
                stepCommand = CompCommand.Step.builder().compId(comp.getCompId()).compStage(CompStage.TRADE)
                        .roundId(i).tradeStage(marketStage).marketStatus(MarketStatus.CLEAR).endingTimeStamp(endingTimeStamp).build();
                pushDelayCommand(stepCommand, executeTime);
            }
            executeTime = endingTimeStamp;
            endingTimeStamp = executeTime + (long) command.getTradeResultLength() * octopusProperties.getDelayUnits();
            stepCommand = CompCommand.Step.builder().compId(comp.getCompId()).compStage(CompStage.TRADE)
                    .roundId(i).tradeStage(TradeStage.END).marketStatus(null).endingTimeStamp(endingTimeStamp).build();
            pushDelayCommand(stepCommand, executeTime);
        }

        executeTime = endingTimeStamp;
        stepCommand = CompCommand.Step.builder()
                .compId(comp.getCompId())
                .compStage(CompStage.RANKING)
                .endingTimeStamp(null)
                .build();
        pushDelayCommand(stepCommand, executeTime);
    }

    @MethodHandler
    public void step(CompCommand.Step command, Context context) {
        this.compStage = command.getCompStage();
        this.roundId = command.getRoundId();
        this.tradeStage = command.getTradeStage();
        this.marketStatus = command.getMarketStatus();
        this.endingTimeStamp = command.getEndingTimeStamp();
        context.publishPlaceHolderEvent(getAggregateId());
    }

    static private void pushDelayCommand(CompCommand.Step command, long executeTime) {
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
