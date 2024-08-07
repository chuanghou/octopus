package com.bilanee.octopus.adapter.facade;

import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.ErrorEnums;
import com.stellariver.milky.common.base.BizEx;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Aspect
@CustomLog
@Component
@RequiredArgsConstructor
@Order(2)
public class BidAspect {


    final Tunnel tunnel;
    final AtomicInteger biddingCounter = new AtomicInteger(0);
    final BlockingQueue<Object> queue = new ArrayBlockingQueue<>(1);

    @Pointcut("@annotation(com.bilanee.octopus.adapter.facade.ToBid)")
    public void pc() { }

    @Around("pc()")
    public Object resultResponseHandler(ProceedingJoinPoint pjp) throws Throwable {
        try {
            int counter = biddingCounter.addAndGet(2);
            log.info("biddingCounter.addAndGet(2) then get {}", counter);
            if (counter%2 != 0) {
                log.info("counter%2 != 0");
                throw new BizEx(ErrorEnums.PARAM_FORMAT_WRONG.message("开始出清，不允许报单"));
            }
            return pjp.proceed();
        } finally {
            int counter = biddingCounter.addAndGet(-2);
            log.info("biddingCounter.addAndGet(-2) then get {}", counter);
            if (counter < 0) {
                if (queue.isEmpty()) {
                    synchronized (queue) {
                        if (queue.isEmpty()) {
                            queue.add(new Object());
                        }
                    }
                }
            }
        }
    }


    @SneakyThrows
    public boolean stopBidCompletely(int length, TimeUnit timeUnit) {
        int counter = biddingCounter.decrementAndGet();
        log.info("biddingCounter.decrementAndGet() then get {}", counter);
        if (counter < 0) {
            return true;
        }
        Object poll = queue.poll(length, timeUnit);
        log.info("queue.poll(length, timeUnit) then get {}", poll);
        return poll != null;
    }

    @SneakyThrows
    public void recover() {
        int counter = biddingCounter.addAndGet(1);
        log.info("biddingCounter.addAndGet(1) then get {}", counter);
        if (counter != 0) {
            log.error("recover failure");
        }
    }

}
