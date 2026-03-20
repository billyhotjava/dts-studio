package com.yuzhi.dts.copilot.ai.service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.domain.AiProviderConfig;
import com.yuzhi.dts.copilot.ai.repository.AiProviderConfigRepository;
import com.yuzhi.dts.copilot.ai.service.llm.LlmProviderClient;
import com.yuzhi.dts.copilot.ai.service.llm.LlmProviderClientFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiConfigServiceTest {

    @Mock
    private AiProviderConfigRepository repository;

    @Mock
    private LlmProviderClientFactory clientFactory;

    @Mock
    private LlmProviderClient llmProviderClient;

    @Test
    void updateProviderKeepsExistingApiKeyWhenRequestApiKeyIsBlank() {
        assertThat(AiConfigService.mergeApiKey("sk-existing-secret", "   "))
                .isEqualTo("sk-existing-secret");
    }

    @Test
    void testProviderConfigUsesResolvedProviderProtocolClient() throws Exception {
        AiProviderConfig config = new AiProviderConfig();
        config.setName("MiniMax");
        config.setProviderType("MINIMAX");
        config.setBaseUrl("https://api.minimaxi.com/anthropic");
        config.setApiKey("test-key");
        config.setModel("MiniMax-M2.7");
        config.setTimeoutSeconds(60);

        when(clientFactory.create(config)).thenReturn(llmProviderClient);
        when(llmProviderClient.listModels()).thenReturn(new ObjectMapper().readTree("""
                {"data":[{"id":"MiniMax-M2.7"}]}
                """));

        AiConfigService service = new AiConfigService(repository, clientFactory);

        Map<String, Object> result = service.testProviderConfig(config);

        assertThat(result).containsEntry("success", true);
        assertThat(result).containsEntry("modelAvailable", true);
        verify(clientFactory).create(config);
        verify(llmProviderClient).listModels();
    }
}
