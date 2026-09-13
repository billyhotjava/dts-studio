package com.yuzhi.dts.copilot.ai.web.rest;

import com.yuzhi.dts.copilot.ai.service.copilot.FinanceReconciliationScorecardScheduledPublisherService;
import com.yuzhi.dts.copilot.ai.web.rest.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/finance/reconciliation-scorecards")
public class FinanceReconciliationScorecardResource {

    private final FinanceReconciliationScorecardScheduledPublisherService scheduledPublisherService;

    public FinanceReconciliationScorecardResource(
            FinanceReconciliationScorecardScheduledPublisherService scheduledPublisherService) {
        this.scheduledPublisherService = scheduledPublisherService;
    }

    @PostMapping("/publish-scheduled")
    public ResponseEntity<ApiResponse<FinanceReconciliationScorecardScheduledPublisherService.ScheduledPublishResult>>
            publishScheduled() {
        return ResponseEntity.ok(ApiResponse.ok(scheduledPublisherService.publishScheduledScorecards()));
    }
}
