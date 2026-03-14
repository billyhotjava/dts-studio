package com.yuzhi.dts.copilot.ai.service.llm.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.yuzhi.dts.copilot.ai.domain.AiProviderConfig;
import com.yuzhi.dts.copilot.ai.service.config.AiConfigService;
import com.yuzhi.dts.copilot.ai.service.llm.OpenAiCompatibleClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Multi-provider LLM gateway with circuit breaker and fallback.
 * Routes to default provider first, falls back to others on failure.
 */
@Service
public class LlmGatewayService {

    private static final Logger log = LoggerFactory.getLogger(LlmGatewayService.class);

    private final AiConfigService configService;
    private final Map<Long, ProviderState> providerStates = new ConcurrentHashMap<>();

    public LlmGatewayService(AiConfigService configService) {
        this.configService = configService;
    }

    /**
     * Synchronous chat completion with fallback across providers.
     */
    public JsonNode chatCompletion(List<Map<String, Object>> messages,
                                   Double temperature, Integer maxTokens,
                                   List<Map<String, Object>> tools) throws IOException {
        List<ProviderState> providers = getOrderedProviders();
        if (providers.isEmpty()) {
            throw new IOException("No LLM providers configured or available");
        }

        IOException lastException = null;
        for (ProviderState state : providers) {
            if (!state.isAvailable()) {
                log.debug("Skipping provider {} (circuit open)", state.getProviderName());
                continue;
            }
            try {
                AiProviderConfig config = configService.getProvider(state.getProviderId()).orElse(null);
                if (config == null) continue;

                Double temp = temperature != null ? temperature : config.getTemperature();
                Integer tokens = maxTokens != null ? maxTokens : config.getMaxTokens();

                JsonNode result = state.getClient().chatCompletion(
                        config.getModel(), messages, temp, tokens, tools);
                state.recordSuccess();
                return result;
            } catch (IOException | InterruptedException e) {
                log.warn("Provider {} failed: {}", state.getProviderName(), e.getMessage());
                state.recordFailure();
                lastException = (e instanceof IOException) ? (IOException) e : new IOException(e);
            }
        }
        throw lastException != null ? lastException : new IOException("All providers failed");
    }

    /**
     * Streaming chat completion with fallback across providers.
     */
    public void chatCompletionStream(List<Map<String, Object>> messages,
                                     Double temperature, Integer maxTokens,
                                     OutputStream output) throws IOException {
        List<ProviderState> providers = getOrderedProviders();
        if (providers.isEmpty()) {
            throw new IOException("No LLM providers configured or available");
        }

        IOException lastException = null;
        for (ProviderState state : providers) {
            if (!state.isAvailable()) {
                log.debug("Skipping provider {} (circuit open)", state.getProviderName());
                continue;
            }
            try {
                AiProviderConfig config = configService.getProvider(state.getProviderId()).orElse(null);
                if (config == null) continue;

                Double temp = temperature != null ? temperature : config.getTemperature();
                Integer tokens = maxTokens != null ? maxTokens : config.getMaxTokens();

                state.getClient().chatCompletionStream(
                        config.getModel(), messages, temp, tokens, output);
                state.recordSuccess();
                return;
            } catch (IOException | InterruptedException e) {
                log.warn("Provider {} streaming failed: {}", state.getProviderName(), e.getMessage());
                state.recordFailure();
                lastException = (e instanceof IOException) ? (IOException) e : new IOException(e);
            }
        }
        throw lastException != null ? lastException : new IOException("All providers failed");
    }

    /**
     * List currently available (healthy) providers.
     */
    public List<String> getAvailableProviders() {
        refreshProviderStates();
        return providerStates.values().stream()
                .filter(ProviderState::isAvailable)
                .map(ProviderState::getProviderName)
                .collect(Collectors.toList());
    }

    /**
     * Manually reset the circuit breaker for a provider.
     */
    public void resetCircuitBreaker(Long providerId) {
        ProviderState state = providerStates.get(providerId);
        if (state != null) {
            state.resetCircuitBreaker();
            log.info("Circuit breaker reset for provider {}", state.getProviderName());
        }
    }

    /**
     * Returns ordered list of provider states: default first, then by priority.
     * 如果数据库中无任何配置，通过 resolveEffectiveProvider 降级到本地文件或内置默认值。
     */
    private List<ProviderState> getOrderedProviders() {
        refreshProviderStates();
        List<AiProviderConfig> configs = configService.getEnabledProviders();
        List<ProviderState> states = configs.stream()
                .map(c -> providerStates.get(c.getId()))
                .filter(s -> s != null)
                .collect(Collectors.toList());

        // 降级：无数据库配置时，使用 resolveEffectiveProvider 的结果
        if (states.isEmpty()) {
            AiProviderConfig fallback = configService.resolveEffectiveProvider();
            Long fallbackId = fallback.getId() != null ? fallback.getId() : -1L;
            ProviderState fallbackState = providerStates.computeIfAbsent(fallbackId, id -> {
                OpenAiCompatibleClient client = new OpenAiCompatibleClient(
                        fallback.getBaseUrl(),
                        fallback.getApiKey(),
                        fallback.getTimeoutSeconds() != null ? fallback.getTimeoutSeconds() : 60
                );
                return new ProviderState(fallbackId, fallback.getName(), client);
            });
            states = List.of(fallbackState);
        }

        return states;
    }

    /**
     * Refresh provider states from the database, creating clients as needed.
     */
    private void refreshProviderStates() {
        List<AiProviderConfig> configs = configService.getEnabledProviders();
        for (AiProviderConfig config : configs) {
            providerStates.computeIfAbsent(config.getId(), id -> {
                OpenAiCompatibleClient client = new OpenAiCompatibleClient(
                        config.getBaseUrl(),
                        config.getApiKey(),
                        config.getTimeoutSeconds() != null ? config.getTimeoutSeconds() : 60
                );
                return new ProviderState(config.getId(), config.getName(), client);
            });
        }
        // Remove states for providers that are no longer configured (keep fallback id=-1)
        List<Long> activeIds = configs.stream().map(AiProviderConfig::getId).collect(Collectors.toList());
        activeIds.add(-1L);
        providerStates.keySet().removeIf(id -> !activeIds.contains(id));
    }
}
