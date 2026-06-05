package com.yuzhi.dts.copilot.ai.service.copilot;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FinanceReconciliationScorecardPublisherService {

    private static final String PENDING_LIVE_EVIDENCE = "PENDING_LIVE_EVIDENCE";
    private static final String POLICY_MISSING = "POLICY_MISSING";
    private static final String INVALID_REQUEST = "INVALID_REQUEST";

    private final FinanceReconciliationScorecardRegistry registry;
    private final FinanceReconciliationScorecardService scorecardService;
    private final FinanceReconciliationScorecardSnapshotService snapshotService;

    public FinanceReconciliationScorecardPublisherService(
            FinanceReconciliationScorecardRegistry registry,
            FinanceReconciliationScorecardService scorecardService,
            FinanceReconciliationScorecardSnapshotService snapshotService) {
        this.registry = registry;
        this.scorecardService = scorecardService;
        this.snapshotService = snapshotService;
    }

    public PublishResult publishLatest(
            String oracleBindingId,
            String scorecardId,
            List<FinanceReconciliationScorecardService.CheckRun> currentRuns,
            List<FinanceReconciliationScorecardService.ReconciliationFailure> baselineFailures) {
        String safeOracleBindingId = textOrEmpty(oracleBindingId).trim();
        String safeScorecardId = textOrEmpty(scorecardId).trim();
        List<FinanceReconciliationScorecardService.CheckRun> safeRuns =
                currentRuns == null ? List.of() : List.copyOf(currentRuns);
        List<FinanceReconciliationScorecardService.ReconciliationFailure> safeBaseline =
                baselineFailures == null ? List.of() : List.copyOf(baselineFailures);

        if (!StringUtils.hasText(safeOracleBindingId) || !StringUtils.hasText(safeScorecardId)) {
            return PublishResult.notPublished(
                    safeOracleBindingId,
                    safeScorecardId,
                    INVALID_REQUEST,
                    List.of(),
                    "Finance reconciliation scorecard publish failed: oracleBindingId and scorecardId are required");
        }

        return registry.policy(safeScorecardId)
                .map(policy -> publishPolicy(safeOracleBindingId, policy, safeRuns, safeBaseline))
                .orElseGet(() -> PublishResult.notPublished(
                        safeOracleBindingId,
                        safeScorecardId,
                        POLICY_MISSING,
                        List.of(),
                        "Finance reconciliation scorecard policy missing: scorecardId=" + safeScorecardId));
    }

    private PublishResult publishPolicy(
            String oracleBindingId,
            FinanceReconciliationScorecardRegistry.ScorecardPolicy policy,
            List<FinanceReconciliationScorecardService.CheckRun> currentRuns,
            List<FinanceReconciliationScorecardService.ReconciliationFailure> baselineFailures) {
        List<String> missingCategories = missingRequiredCategories(policy, currentRuns);
        if (!missingCategories.isEmpty()) {
            return PublishResult.notPublished(
                    oracleBindingId,
                    policy.id(),
                    PENDING_LIVE_EVIDENCE,
                    missingCategories,
                    "Finance reconciliation scorecard missing required live evidence: scorecardId="
                            + policy.id()
                            + ", categories="
                            + String.join(",", missingCategories));
        }

        FinanceReconciliationScorecardService.ScorecardReport report =
                scorecardService.score(policy.scorecardSpec(), currentRuns, baselineFailures);
        snapshotService.publish(oracleBindingId, policy.id(), report);
        return new PublishResult(
                true,
                oracleBindingId,
                policy.id(),
                report.healthStatus(),
                List.of(),
                report,
                report.failureMessage());
    }

    private static List<String> missingRequiredCategories(
            FinanceReconciliationScorecardRegistry.ScorecardPolicy policy,
            List<FinanceReconciliationScorecardService.CheckRun> currentRuns) {
        Set<String> seenCategories = new LinkedHashSet<>();
        for (FinanceReconciliationScorecardService.CheckRun run : currentRuns) {
            seenCategories.add(run.category());
        }
        return policy.categories().stream()
                .filter(FinanceReconciliationScorecardRegistry.CategoryPolicy::required)
                .map(FinanceReconciliationScorecardRegistry.CategoryPolicy::id)
                .filter(category -> !seenCategories.contains(category))
                .toList();
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    public record PublishResult(
            boolean published,
            String oracleBindingId,
            String scorecardId,
            String healthStatus,
            List<String> missingCategories,
            FinanceReconciliationScorecardService.ScorecardReport report,
            String failureMessage) {
        public PublishResult {
            oracleBindingId = textOrEmpty(oracleBindingId);
            scorecardId = textOrEmpty(scorecardId);
            healthStatus = textOrEmpty(healthStatus);
            missingCategories = missingCategories == null ? List.of() : List.copyOf(missingCategories);
            failureMessage = textOrEmpty(failureMessage);
        }

        private static PublishResult notPublished(
                String oracleBindingId,
                String scorecardId,
                String healthStatus,
                List<String> missingCategories,
                String failureMessage) {
            return new PublishResult(
                    false,
                    oracleBindingId,
                    scorecardId,
                    healthStatus,
                    missingCategories,
                    null,
                    failureMessage);
        }
    }
}
