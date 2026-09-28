package com.yuzhi.dts.copilot.ai.service.copilot;

import java.util.ArrayList;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class FinanceReconciliationScorecardScheduledPublisherService {

    private static final String NO_EVIDENCE_PROVIDER_REGISTERED = "NO_EVIDENCE_PROVIDER_REGISTERED";
    private static final String PENDING_LIVE_EVIDENCE = "PENDING_LIVE_EVIDENCE";
    private static final String ENABLE_LIVE_PROVIDER_NEXT_ACTION =
            "enable a live FinanceReconciliationScorecardEvidenceProvider and satisfy F1/F2/F3/F4 required lanes";

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
                    "Finance reconciliation scorecard schedule skipped: no finance scorecard evidence provider registered",
                    NO_EVIDENCE_PROVIDER_REGISTERED,
                    ENABLE_LIVE_PROVIDER_NEXT_ACTION);
        }

        List<FinanceReconciliationScorecardPublisherService.PublishResult> results = new ArrayList<>();
        for (FinanceReconciliationScorecardEvidenceProvider provider : evidenceProviders) {
            results.add(publisherService.publishLatest(
                    provider.authorityBindingId(),
                    provider.scorecardId(),
                    provider.currentRuns(),
                    provider.baselineFailures()));
        }
        long publishedCount = results.stream()
                .filter(FinanceReconciliationScorecardPublisherService.PublishResult::published)
                .count();
        if (publishedCount == 0) {
            FinanceReconciliationScorecardPublisherService.PublishResult pendingResult = results.stream()
                    .filter(result -> PENDING_LIVE_EVIDENCE.equals(result.healthStatus()))
                    .findFirst()
                    .orElse(null);
            if (pendingResult != null) {
                return new ScheduledPublishResult(
                        PENDING_LIVE_EVIDENCE,
                        evidenceProviders.size(),
                        0,
                        results,
                        pendingResult.failureMessage(),
                        PENDING_LIVE_EVIDENCE,
                        ENABLE_LIVE_PROVIDER_NEXT_ACTION);
            }
        }
        return new ScheduledPublishResult(
                "COMPLETED",
                evidenceProviders.size(),
                Math.toIntExact(publishedCount),
                results,
                "",
                "",
                "");
    }

    public record ScheduledPublishResult(
            String status,
            int totalProviders,
            int publishedCount,
            List<FinanceReconciliationScorecardPublisherService.PublishResult> results,
            String failureMessage,
            String skippedReasonCode,
            String nextAction) {
        public ScheduledPublishResult(
                String status,
                int totalProviders,
                int publishedCount,
                List<FinanceReconciliationScorecardPublisherService.PublishResult> results,
                String failureMessage) {
            this(status, totalProviders, publishedCount, results, failureMessage, "", "");
        }

        public ScheduledPublishResult {
            status = status == null ? "" : status;
            totalProviders = Math.max(totalProviders, 0);
            publishedCount = Math.max(publishedCount, 0);
            results = results == null ? List.of() : List.copyOf(results);
            failureMessage = failureMessage == null ? "" : failureMessage;
            skippedReasonCode = skippedReasonCode == null ? "" : skippedReasonCode;
            nextAction = nextAction == null ? "" : nextAction;
        }
    }
}
