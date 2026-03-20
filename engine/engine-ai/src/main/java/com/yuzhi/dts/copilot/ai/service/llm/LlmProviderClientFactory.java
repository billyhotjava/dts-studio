package com.yuzhi.dts.copilot.ai.service.llm;

import com.yuzhi.dts.copilot.ai.domain.AiProviderConfig;
import org.springframework.stereotype.Component;

@Component
public class LlmProviderClientFactory {

    public LlmProviderClient create(AiProviderConfig config) {
        if (shouldUseAnthropicCompatibleProtocol(config)) {
            return new AnthropicCompatibleClient(
                    config.getBaseUrl(),
                    config.getApiKey(),
                    config.getTimeoutSeconds() != null ? config.getTimeoutSeconds() : 60);
        }
        return new OpenAiCompatibleClient(
                config.getBaseUrl(),
                config.getApiKey(),
                config.getTimeoutSeconds() != null ? config.getTimeoutSeconds() : 60);
    }

    boolean shouldUseAnthropicCompatibleProtocol(AiProviderConfig config) {
        String providerType = config.getProviderType();
        if (providerType != null) {
            String normalized = providerType.trim().toUpperCase();
            if ("MINIMAX".equals(normalized) || "ANTHROPIC".equals(normalized)) {
                return true;
            }
        }
        String baseUrl = config.getBaseUrl();
        return baseUrl != null && baseUrl.toLowerCase().contains("/anthropic");
    }
}
