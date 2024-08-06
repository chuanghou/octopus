package com.bilanee.octopus.infrastructure;

import com.stellariver.milky.common.tool.executor.ThreadLocalPasser;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class TraceIdPasser implements ThreadLocalPasser<String> {
    @Override
    public String prepareThreadLocal() {
        return MDC.get("traceId");
    }

    @Override
    public void pass(Object t) {
        if (t != null) {
            MDC.put("traceId", t.toString());
        }
    }

    @Override
    public void clearThreadLocal() {
        MDC.remove("traceId");
    }
}
