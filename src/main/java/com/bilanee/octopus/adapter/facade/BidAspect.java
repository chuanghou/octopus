package com.bilanee.octopus.adapter.facade;

import com.bilanee.octopus.adapter.tunnel.Tunnel;
import com.bilanee.octopus.basic.ErrorEnums;
import com.stellariver.milky.common.base.BizEx;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Aspect
@Component
@RequiredArgsConstructor
public class BidAspect {

    final Tunnel tunnel;
    final AtomicInteger biddingCounter;
    @Around()
    public Object resultResponseHandler(ProceedingJoinPoint pjp) {
        boolean equals = Boolean.TRUE.equals(tunnel.runningComp().getForbid());
        BizEx.trueThrow(equals, ErrorEnums.PARAM_FORMAT_WRONG.message("出清过程中不允许报单！"));
        biddingCounter.compareAndSet()
    }
}
