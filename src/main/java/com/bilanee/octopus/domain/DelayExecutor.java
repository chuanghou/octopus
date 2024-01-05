package com.bilanee.octopus.domain;

import com.stellariver.milky.common.base.SysEx;
import com.stellariver.milky.domain.support.command.CommandBus;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
@CustomLog
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DelayExecutor implements Runnable, ApplicationRunner {

    @Getter
    final DelayQueue<DelayCommandWrapper> delayQueue = new DelayQueue<>();
    final ExecutorService executorService = Executors.newFixedThreadPool(1);

    @Override
    @SuppressWarnings("all")
    public void run() {

        while (true) {
            DelayCommandWrapper delayCommandWrapper;
            try {
                delayCommandWrapper = delayQueue.poll(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new SysEx(e);
            }
            if (delayCommandWrapper != null) {
                CommandBus.accept(delayCommandWrapper.getCommand(), new HashMap<>());
            }
        }

    }

    public void removeStepCommand() {
        delayQueue.clear();
    }

    @Override
    public void run(ApplicationArguments args) {
        executorService.execute(this);
    }
}
