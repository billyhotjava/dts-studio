package com.yuzhi.dts.copilot.ai.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.service.copilot.AiCopilotService;
import com.yuzhi.dts.copilot.ai.service.copilot.Nl2SqlService;
import com.yuzhi.dts.copilot.ai.service.copilot.OntologyService;
import com.yuzhi.dts.copilot.ai.service.copilot.ScreenGenerationService;
import com.yuzhi.dts.copilot.ai.service.copilot.SemanticPackService;
import com.yuzhi.dts.copilot.ai.service.llm.gateway.LlmGatewayService;
import com.yuzhi.dts.copilot.ai.web.rest.dto.ApiResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class AiCopilotResourceTest {

    @Test
    void signalsReturnsOntologySignalSummariesForDomain() {
        AiCopilotResource resource = resourceWithSemanticPacks();

        ResponseEntity<ApiResponse<List<AiCopilotResource.SignalSummary>>> response =
                resource.signals("flowerbiz");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data())
                .extracting(AiCopilotResource.SignalSummary::title)
                .contains("坏账风险", "欠费预警");
        assertThat(response.getBody().data())
                .filteredOn(signal -> signal.title().equals("坏账风险"))
                .singleElement()
                .satisfies(signal -> {
                    assertThat(signal.id()).isEqualTo("flowerbiz:坏账风险");
                    assertThat(signal.severity()).isEqualTo("high");
                    assertThat(signal.description()).contains("坏账");
                    assertThat(signal.source()).isEqualTo("ontology.flowerbiz.signals");
                    assertThat(signal.objectName()).isEqualTo("坏账汇总");
                    assertThat(signal.linkedActions()).containsExactly("创建坏账处理单");
                });
    }

    @Test
    void signalsReturnsEmptyListForUnknownDomain() {
        AiCopilotResource resource = resourceWithSemanticPacks();

        ResponseEntity<ApiResponse<List<AiCopilotResource.SignalSummary>>> response =
                resource.signals("missing-domain");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEmpty();
    }

    private AiCopilotResource resourceWithSemanticPacks() {
        SemanticPackService semanticPackService = new SemanticPackService(new ObjectMapper());
        semanticPackService.init();
        return new AiCopilotResource(
                mock(AiCopilotService.class),
                mock(Nl2SqlService.class),
                mock(LlmGatewayService.class),
                mock(ScreenGenerationService.class),
                new OntologyService(semanticPackService));
    }
}
