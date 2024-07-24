package com.bilanee.octopus.domain;

import com.bilanee.octopus.adapter.facade.BidAspect;
import com.stellariver.milky.domain.support.command.Command;
import com.stellariver.milky.domain.support.command.CommandBus;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
@CustomLog
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DelayExecutor {
    final BidAspect bidAspect;
    final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
    ScheduledFuture<Object> scheduledFuture;

    @SuppressWarnings("unchecked")
    public void schedule(Command command, long length, TimeUnit timeUnit) {
        scheduledFuture = (ScheduledFuture<Object>) scheduledExecutorService.schedule(() -> {
            bidAspect.stopBidCompletely(30, TimeUnit.SECONDS);
            try {
                CommandBus.acceptMemoryTransactional(command, new HashMap<>());
            } finally {
                bidAspect.recover();
            }
        }, length, timeUnit);
    }

    public void removeStepCommand() {
        if (scheduledFuture == null || scheduledFuture.isCancelled()) {
            return;
        }
        scheduledFuture.cancel(false);
    }

}
