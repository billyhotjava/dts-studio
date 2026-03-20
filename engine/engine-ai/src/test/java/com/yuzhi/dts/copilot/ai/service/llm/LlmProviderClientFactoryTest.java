package com.yuzhi.dts.copilot.ai.service.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.copilot.ai.domain.AiProviderConfig;
import org.junit.jupiter.api.Test;

class LlmProviderClientFactoryTest {

    private final LlmProviderClientFactory factory = new LlmProviderClientFactory();

    @Test
    void resolvesMiniMaxProviderTypeToAnthropicCompatibleClient() {
        AiProviderConfig config = provider("MINIMAX", "https://api.minimaxi.com/anthropic");

        assertThat(factory.create(config)).isInstanceOf(AnthropicCompatibleClient.class);
    }

    @Test
    void resolvesAnthropicProviderTypeToAnthropicCompatibleClient() {
        AiProviderConfig config = provider("ANTHROPIC", "https://api.anthropic.com/v1");

        assertThat(factory.create(config)).isInstanceOf(AnthropicCompatibleClient.class);
    }

    @Test
    void resolvesCustomAnthropicBaseUrlToAnthropicCompatibleClient() {
        AiProviderConfig config = provider("CUSTOM", "https://api.minimaxi.com/anthropic");

        assertThat(factory.create(config)).isInstanceOf(AnthropicCompatibleClient.class);
    }

    @Test
    void resolvesOpenAiCompatibleProviderToOpenAiClient() {
        AiProviderConfig config = provider("QWEN", "https://dashscope.aliyuncs.com/compatible-mode/v1");

        assertThat(factory.create(config)).isInstanceOf(OpenAiCompatibleClient.class);
    }

    private AiProviderConfig provider(String providerType, String baseUrl) {
        AiProviderConfig config = new AiProviderConfig();
        config.setName(providerType.toLowerCase());
        config.setProviderType(providerType);
        config.setBaseUrl(baseUrl);
        config.setApiKey("test-key");
        config.setTimeoutSeconds(60);
        config.setModel("test-model");
        return config;
    }
}
