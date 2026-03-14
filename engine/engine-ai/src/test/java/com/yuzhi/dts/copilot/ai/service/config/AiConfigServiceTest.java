package com.yuzhi.dts.copilot.ai.service.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiConfigServiceTest {

    @Test
    void updateProviderKeepsExistingApiKeyWhenRequestApiKeyIsBlank() {
        assertThat(AiConfigService.mergeApiKey("sk-existing-secret", "   "))
                .isEqualTo("sk-existing-secret");
    }
}
