package com.yuzhi.dts.copilot.ai.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yuzhi.dts.copilot.ai.domain.AiProviderConfig;
import com.yuzhi.dts.copilot.ai.service.config.AiConfigService;
import com.yuzhi.dts.copilot.ai.web.rest.dto.ApiResponse;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AiConfigResourceTest {

    @Mock
    private AiConfigService configService;

    @Test
    void testProviderDelegatesToConfigServiceTestResult() {
        when(configService.getProvider(7L)).thenReturn(Optional.of(new AiProviderConfig()));
        when(configService.testProvider(7L)).thenReturn(Map.of(
                "success", true,
                "provider", "MiniMax",
                "modelAvailable", true
        ));
        AiConfigResource resource = new AiConfigResource(configService, "secret");

        ResponseEntity<ApiResponse<Map<String, Object>>> response = resource.testProvider("secret", 7L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().data()).containsEntry("provider", "MiniMax");
        verify(configService).testProvider(7L);
    }
}
