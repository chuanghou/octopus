package com.bilanee.octopus.domain;

import com.stellariver.milky.domain.support.command.Command;
import com.stellariver.milky.domain.support.command.CommandBus;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
@CustomLog
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DelayExecutor {

    final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
    ScheduledFuture<Object> scheduledFuture;

    public void schedule(Command command, long length, TimeUnit timeUnit) {
        scheduledFuture = scheduledExecutorService.schedule(() -> CommandBus.accept(command, new HashMap<>()), length, timeUnit);
    }

    public void removeStepCommand() {
        if (scheduledFuture == null || scheduledFuture.isCancelled()) {
            return;
        }
        scheduledFuture.cancel(false);
    }

}
