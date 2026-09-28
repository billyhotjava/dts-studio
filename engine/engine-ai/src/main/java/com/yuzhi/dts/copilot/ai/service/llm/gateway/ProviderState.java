package com.yuzhi.dts.copilot.ai.service.llm.gateway;

import com.yuzhi.dts.copilot.ai.service.llm.LlmProviderClient;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks health state and circuit breaker for a single LLM provider.
 */
public class ProviderState {

    private static final int FAILURE_THRESHOLD = 3;
    private static final long CIRCUIT_OPEN_DURATION_SECONDS = 60;

    private final Long providerId;
    private final String providerName;
    private final LlmProviderClient client;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicReference<Instant> lastFailureTime = new AtomicReference<>(null);
    private volatile boolean circuitOpen = false;

    public ProviderState(Long providerId, String providerName, LlmProviderClient client) {
        this.providerId = providerId;
        this.providerName = providerName;
        this.client = client;
    }

    public Long getProviderId() {
        return providerId;
    }

    public String getProviderName() {
        return providerName;
    }

    public LlmProviderClient getClient() {
        return client;
    }

    public boolean isAvailable() {
        if (!circuitOpen) {
            return true;
        }
        // Check if the circuit open duration has elapsed
        Instant lastFail = lastFailureTime.get();
        if (lastFail != null && Instant.now().isAfter(lastFail.plusSeconds(CIRCUIT_OPEN_DURATION_SECONDS))) {
            // Half-open: allow a retry
            return true;
        }
        return false;
    }

    public void recordSuccess() {
        consecutiveFailures.set(0);
        circuitOpen = false;
    }

    public void recordFailure() {
        lastFailureTime.set(Instant.now());
        if (consecutiveFailures.incrementAndGet() >= FAILURE_THRESHOLD) {
            circuitOpen = true;
        }
    }

    public void resetCircuitBreaker() {
        consecutiveFailures.set(0);
        circuitOpen = false;
        lastFailureTime.set(null);
    }

    public boolean isCircuitOpen() {
        return circuitOpen;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }
}
