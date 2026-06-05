package com.yuzhi.dts.copilot.ai.service.copilot;

import java.util.ArrayList;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class FinanceReconciliationScorecardScheduledPublisherService {

    private final List<FinanceReconciliationScorecardEvidenceProvider> evidenceProviders;
    private final FinanceReconciliationScorecardPublisherService publisherService;

    public FinanceReconciliationScorecardScheduledPublisherService(
            List<FinanceReconciliationScorecardEvidenceProvider> evidenceProviders,
            FinanceReconciliationScorecardPublisherService publisherService) {
        this.evidenceProviders = evidenceProviders == null ? List.of() : List.copyOf(evidenceProviders);
        this.publisherService = publisherService;
    }

    @Scheduled(
            fixedDelayString = "${copilot.finance.reconciliation.scorecard.schedule.interval-ms:86400000}",
            initialDelayString = "${copilot.finance.reconciliation.scorecard.schedule.initial-delay-ms:60000}")
    public ScheduledPublishResult publishScheduledScorecards() {
        if (evidenceProviders.isEmpty()) {
            return new ScheduledPublishResult(
                    "SKIPPED",
                    0,
                    0,
                    List.of(),
                    "Finance reconciliation scorecard schedule skipped: no finance scorecard evidence provider registered");
        }

        List<FinanceReconciliationScorecardPublisherService.PublishResult> results = new ArrayList<>();
        for (FinanceReconciliationScorecardEvidenceProvider provider : evidenceProviders) {
            results.add(publisherService.publishLatest(
                    provider.oracleBindingId(),
                    provider.scorecardId(),
                    provider.currentRuns(),
                    provider.baselineFailures()));
        }
        long publishedCount = results.stream()
                .filter(FinanceReconciliationScorecardPublisherService.PublishResult::published)
                .count();
        return new ScheduledPublishResult(
                "COMPLETED",
                evidenceProviders.size(),
                Math.toIntExact(publishedCount),
                results,
                "");
    }

    public record ScheduledPublishResult(
            String status,
            int totalProviders,
            int publishedCount,
            List<FinanceReconciliationScorecardPublisherService.PublishResult> results,
            String failureMessage) {
        public ScheduledPublishResult {
            status = status == null ? "" : status;
            totalProviders = Math.max(totalProviders, 0);
            publishedCount = Math.max(publishedCount, 0);
            results = results == null ? List.of() : List.copyOf(results);
            failureMessage = failureMessage == null ? "" : failureMessage;
        }
    }
}
