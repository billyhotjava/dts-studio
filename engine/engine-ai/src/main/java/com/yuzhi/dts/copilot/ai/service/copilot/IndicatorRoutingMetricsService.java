package com.yuzhi.dts.copilot.ai.service.copilot;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class IndicatorRoutingMetricsService {

    private final MeterRegistry meterRegistry;

    public IndicatorRoutingMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    private IndicatorRoutingMetricsService() {
        this.meterRegistry = null;
    }

    static IndicatorRoutingMetricsService noop() {
        return new IndicatorRoutingMetricsService();
    }

    public void recordMatch(IndicatorMatcherService.Confidence tier, int candidateCount) {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder("copilot.indicator.routing.total")
                .description("Published indicator routing match outcomes")
                .tag("tier", tier == null ? "NONE" : tier.name())
                .tag("has_candidates", candidateCount > 0 ? "true" : "false")
                .register(meterRegistry)
                .increment();
    }
}
