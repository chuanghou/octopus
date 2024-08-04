package com.bilanee.octopus.infrastructure;

import com.stellariver.milky.common.base.TraceIdGetter;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class OctopusTraceIdSetter implements TraceIdGetter {
    @Override
    public String getTraceId() {
        return MDC.get("traceId");
    }
}
