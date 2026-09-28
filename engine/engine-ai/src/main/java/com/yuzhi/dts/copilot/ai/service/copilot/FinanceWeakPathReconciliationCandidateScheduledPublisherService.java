package com.yuzhi.dts.copilot.ai.service.copilot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class FinanceWeakPathReconciliationCandidateScheduledPublisherService {

    private final FinanceWeakPathReconciliationCandidatePublisherService publisherService;
    private final String policyId;
    private final int days;
    private final int candidateLimit;

    public FinanceWeakPathReconciliationCandidateScheduledPublisherService(
            FinanceWeakPathReconciliationCandidatePublisherService publisherService,
            @Value("${copilot.finance.reconciliation.weak-path.policy-id:sprint33-finance-weak-path-reconciliation}")
            String policyId,
            @Value("${copilot.finance.reconciliation.weak-path.days:7}") int days,
            @Value("${copilot.finance.reconciliation.weak-path.candidate-limit:10}") int candidateLimit) {
        this.publisherService = publisherService;
        this.policyId = policyId == null ? "" : policyId.trim();
        this.days = days <= 0 ? 7 : days;
        this.candidateLimit = candidateLimit <= 0 ? 10 : candidateLimit;
    }

    @Scheduled(
            fixedDelayString = "${copilot.finance.reconciliation.weak-path.schedule.interval-ms:86400000}",
            initialDelayString = "${copilot.finance.reconciliation.weak-path.schedule.initial-delay-ms:120000}")
    public ScheduledPublishResult publishScheduledCandidates() {
        FinanceWeakPathReconciliationCandidatePublisherService.PublishResult result =
                publisherService.publishFromTelemetry(policyId, days, candidateLimit);
        return new ScheduledPublishResult(
                result.status(),
                result.policyId(),
                result.snapshots().size(),
                result.semanticDrafts().size(),
                result);
    }

    public record ScheduledPublishResult(
            String status,
            String policyId,
            int snapshotCount,
            int semanticDraftCount,
            FinanceWeakPathReconciliationCandidatePublisherService.PublishResult publishResult) {
        public ScheduledPublishResult {
            status = status == null ? "" : status;
            policyId = policyId == null ? "" : policyId;
            snapshotCount = Math.max(snapshotCount, 0);
            semanticDraftCount = Math.max(semanticDraftCount, 0);
        }
    }
}
