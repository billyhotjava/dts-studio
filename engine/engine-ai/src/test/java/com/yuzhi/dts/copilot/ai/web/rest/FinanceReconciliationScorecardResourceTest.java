package com.yuzhi.dts.copilot.ai.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yuzhi.dts.copilot.ai.service.copilot.FinanceReconciliationScorecardScheduledPublisherService;
import com.yuzhi.dts.copilot.ai.web.rest.dto.ApiResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class FinanceReconciliationScorecardResourceTest {

    @Test
    void shouldExposeManualScorecardPublishEndpoint() throws Exception {
        RequestMapping mapping = FinanceReconciliationScorecardResource.class.getAnnotation(RequestMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/api/ai/finance/reconciliation-scorecards");
        assertThat(FinanceReconciliationScorecardResource.class.getDeclaredMethod("publishScheduled")
                .getAnnotation(PostMapping.class).value()).containsExactly("/publish-scheduled");
    }

    @Test
    void shouldDelegateManualPublishToScheduledPublisherService() {
        FinanceReconciliationScorecardScheduledPublisherService publisher =
                mock(FinanceReconciliationScorecardScheduledPublisherService.class);
        FinanceReconciliationScorecardScheduledPublisherService.ScheduledPublishResult scheduledResult =
                new FinanceReconciliationScorecardScheduledPublisherService.ScheduledPublishResult(
                        "COMPLETED",
                        1,
                        0,
                        List.of(),
                        "");
        when(publisher.publishScheduledScorecards()).thenReturn(scheduledResult);
        FinanceReconciliationScorecardResource resource = new FinanceReconciliationScorecardResource(publisher);

        ResponseEntity<ApiResponse<FinanceReconciliationScorecardScheduledPublisherService.ScheduledPublishResult>>
                response = resource.publishScheduled();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(scheduledResult);
        verify(publisher).publishScheduledScorecards();
    }
}
