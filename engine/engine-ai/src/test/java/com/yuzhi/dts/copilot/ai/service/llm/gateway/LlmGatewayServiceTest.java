package com.yuzhi.dts.copilot.ai.service.llm.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.domain.AiProviderConfig;
import com.yuzhi.dts.copilot.ai.service.config.AiConfigService;
import com.yuzhi.dts.copilot.ai.service.llm.LlmProviderClient;
import com.yuzhi.dts.copilot.ai.service.llm.LlmProviderClientFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LlmGatewayServiceTest {

    @Mock
    private AiConfigService configService;

    @Mock
    private LlmProviderClientFactory clientFactory;

    @Mock
    private LlmProviderClient client;

    @Test
    void chatCompletionUsesFactoryResolvedClientForMiniMaxProvider() throws Exception {
        AiProviderConfig provider = new AiProviderConfig();
        provider.setId(5L);
        provider.setName("MiniMax");
        provider.setProviderType("MINIMAX");
        provider.setBaseUrl("https://api.minimaxi.com/anthropic");
        provider.setApiKey("test-key");
        provider.setModel("MiniMax-M2.7");
        provider.setTemperature(0.3);
        provider.setMaxTokens(4096);
        provider.setTimeoutSeconds(60);
        provider.setEnabled(true);

        when(configService.getEnabledProviders()).thenReturn(List.of(provider));
        when(configService.getProvider(5L)).thenReturn(java.util.Optional.of(provider));
        when(clientFactory.create(provider)).thenReturn(client);
        when(client.chatCompletion("MiniMax-M2.7", List.of(Map.of("role", "user", "content", "hi")), 0.3, 4096, null))
                .thenReturn(new ObjectMapper().readTree("""
                        {"choices":[{"message":{"content":"ok"}}]}
                        """));

        LlmGatewayService gateway = new LlmGatewayService(configService, clientFactory);

        assertThat(gateway.chatCompletion(List.of(Map.of("role", "user", "content", "hi")), null, null, null)
                .at("/choices/0/message/content").asText()).isEqualTo("ok");
        verify(clientFactory).create(provider);
    }
}
