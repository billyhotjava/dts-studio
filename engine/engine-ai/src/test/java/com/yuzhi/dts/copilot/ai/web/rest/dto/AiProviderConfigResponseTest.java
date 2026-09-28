package com.yuzhi.dts.copilot.ai.web.rest.dto;

import com.yuzhi.dts.copilot.ai.domain.AiProviderConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiProviderConfigResponseTest {

    @Test
    void fromMasksApiKeyAndPreservesMetadata() {
        AiProviderConfig provider = new AiProviderConfig();
        provider.setId(9L);
        provider.setName("DeepSeek");
        provider.setBaseUrl("https://api.deepseek.com/v1");
        provider.setApiKey("sk-1234567890abcd");
        provider.setModel("deepseek-chat");
        provider.setEnabled(true);

        AiProviderConfigResponse response = AiProviderConfigResponse.from(provider);

        assertThat(response.id()).isEqualTo(9L);
        assertThat(response.name()).isEqualTo("DeepSeek");
        assertThat(response.hasApiKey()).isTrue();
        assertThat(response.apiKeyMasked()).isNotBlank();
        assertThat(response.apiKeyMasked()).doesNotContain("1234567890");
        assertThat(response.apiKeyMasked()).endsWith("abcd");
    }
}
