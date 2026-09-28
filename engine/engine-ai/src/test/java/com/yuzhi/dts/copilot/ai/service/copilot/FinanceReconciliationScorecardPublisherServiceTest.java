package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.domain.FinanceReconciliationScorecardSnapshot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FinanceReconciliationScorecardPublisherServiceTest {

    private static final String SCORECARD_ID = "sprint33-finance-daily-scorecard";

    private final FinanceReconciliationScorecardRegistry registry = initializedRegistry();
    private final FinanceReconciliationScorecardService scorecardService =
            new FinanceReconciliationScorecardService();
    private final FinanceReconciliationScorecardSnapshotService snapshotService =
            mock(FinanceReconciliationScorecardSnapshotService.class);

    @Test
    void publishesCompleteEvidenceBundleAsLatestScorecardSnapshot() {
        FinanceReconciliationScorecardPublisherService service =
                new FinanceReconciliationScorecardPublisherService(registry, scorecardService, snapshotService);
        when(snapshotService.publish(
                eq("month-settlement"),
                eq(SCORECARD_ID),
                argThat(report -> report.passed() && "PASS".equals(report.healthStatus()))))
                .thenReturn(snapshot("month-settlement", "PASS"));

        FinanceReconciliationScorecardPublisherService.PublishResult result =
                service.publishLatest("month-settlement", SCORECARD_ID, passingRuns(), List.of());

        assertThat(result.published()).isTrue();
        assertThat(result.healthStatus()).isEqualTo("PASS");
        assertThat(result.missingCategories()).isEmpty();
        assertThat(result.report()).isNotNull();
        assertThat(result.report().passed()).isTrue();
        verify(snapshotService).publish(
                eq("month-settlement"),
                eq(SCORECARD_ID),
                argThat(report -> report.passed() && "PASS".equals(report.healthStatus())));
    }

    @Test
    void serializesAuthorityBindingIdWithLegacyOracleBindingAlias() {
        FinanceReconciliationScorecardPublisherService.PublishResult result =
                new FinanceReconciliationScorecardPublisherService.PublishResult(
                        true,
                        "month-settlement",
                        SCORECARD_ID,
                        "PASS",
                        List.of(),
                        null,
                        "");

        Map<String, Object> serialized = new ObjectMapper().convertValue(result, Map.class);

        assertThat(serialized)
                .containsEntry("authorityBindingId", "month-settlement")
                .containsEntry("oracleBindingId", "month-settlement");
    }

    @Test
    void refusesToPublishWhenRequiredEvidenceLaneIsMissing() {
        FinanceReconciliationScorecardPublisherService service =
                new FinanceReconciliationScorecardPublisherService(registry, scorecardService, snapshotService);

        FinanceReconciliationScorecardPublisherService.PublishResult result =
                service.publishLatest(
                        "month-settlement",
                        SCORECARD_ID,
                        List.of(
                                check("f1-detail"),
                                check("f2-summary-voucher"),
                                check("f3-invariants")),
                        List.of());

        assertThat(result.published()).isFalse();
        assertThat(result.healthStatus()).isEqualTo("PENDING_LIVE_EVIDENCE");
        assertThat(result.missingCategories()).containsExactly("f4-differential-grid");
        assertThat(result.failureMessage())
                .contains("missing required live evidence", "f4-differential-grid");
        verify(snapshotService, never()).publish(eq("month-settlement"), eq(SCORECARD_ID), argThat(report -> true));
    }

    @Test
    void refusesToPublishWhenRequiredLaneOnlyHasPendingLiveEvidence() {
        FinanceReconciliationScorecardPublisherService service =
                new FinanceReconciliationScorecardPublisherService(registry, scorecardService, snapshotService);

        FinanceReconciliationScorecardPublisherService.PublishResult result =
                service.publishLatest(
                        "month-settlement",
                        SCORECARD_ID,
                        List.of(
                                pendingLiveCheck("f1-detail"),
                                check("f2-summary-voucher"),
                                check("f3-invariants"),
                                check("f4-differential-grid")),
                        List.of());

        assertThat(result.published()).isFalse();
        assertThat(result.healthStatus()).isEqualTo("PENDING_LIVE_EVIDENCE");
        assertThat(result.missingCategories()).containsExactly("f1-detail");
        assertThat(result.failureMessage())
                .contains("pending live evidence", "f1-detail", "f1-detail-check");
        verify(snapshotService, never()).publish(eq("month-settlement"), eq(SCORECARD_ID), argThat(report -> true));
    }

    @Test
    void reportsTheRequiredPendingLaneWhenOptionalLaneAlsoHasPendingLiveEvidence() {
        FinanceReconciliationScorecardRegistry customRegistry = mock(FinanceReconciliationScorecardRegistry.class);
        when(customRegistry.policy(SCORECARD_ID)).thenReturn(Optional.of(scorecardPolicyWithOptionalLane()));
        FinanceReconciliationScorecardPublisherService service =
                new FinanceReconciliationScorecardPublisherService(customRegistry, scorecardService, snapshotService);

        FinanceReconciliationScorecardPublisherService.PublishResult result =
                service.publishLatest(
                        "month-settlement",
                        SCORECARD_ID,
                        List.of(
                                pendingLiveCheck("optional-observer"),
                                pendingLiveCheck("f1-detail"),
                                check("f2-summary-voucher"),
                                check("f3-invariants"),
                                check("f4-differential-grid")),
                        List.of());

        assertThat(result.published()).isFalse();
        assertThat(result.missingCategories()).containsExactly("f1-detail");
        assertThat(result.failureMessage())
                .contains("category=f1-detail", "checkId=f1-detail-check")
                .doesNotContain("optional-observer");
        verify(snapshotService, never()).publish(eq("month-settlement"), eq(SCORECARD_ID), argThat(report -> true));
    }

    @Test
    void refusesUnknownScorecardPolicyWithoutWritingSnapshot() {
        FinanceReconciliationScorecardPublisherService service =
                new FinanceReconciliationScorecardPublisherService(registry, scorecardService, snapshotService);

        FinanceReconciliationScorecardPublisherService.PublishResult result =
                service.publishLatest("month-settlement", "missing-policy", passingRuns(), List.of());

        assertThat(result.published()).isFalse();
        assertThat(result.healthStatus()).isEqualTo("POLICY_MISSING");
        assertThat(result.failureMessage()).contains("scorecard policy missing", "missing-policy");
        verify(snapshotService, never()).publish(eq("month-settlement"), eq("missing-policy"), argThat(report -> true));
    }

    @Test
    void reportsAuthorityBindingIdForInvalidPublishRequest() {
        FinanceReconciliationScorecardPublisherService service =
                new FinanceReconciliationScorecardPublisherService(registry, scorecardService, snapshotService);

        FinanceReconciliationScorecardPublisherService.PublishResult result =
                service.publishLatest("", SCORECARD_ID, passingRuns(), List.of());

        assertThat(result.published()).isFalse();
        assertThat(result.healthStatus()).isEqualTo("INVALID_REQUEST");
        assertThat(result.failureMessage())
                .contains("authorityBindingId and scorecardId are required")
                .doesNotContain("oracleBindingId");
        verify(snapshotService, never()).publish(eq(""), eq(SCORECARD_ID), argThat(report -> true));
    }

    private static FinanceReconciliationScorecardRegistry initializedRegistry() {
        FinanceReconciliationScorecardRegistry registry =
                new FinanceReconciliationScorecardRegistry(new ObjectMapper());
        registry.init();
        return registry;
    }

    private static List<FinanceReconciliationScorecardService.CheckRun> passingRuns() {
        return List.of(
                check("f1-detail"),
                check("f2-summary-voucher"),
                check("f3-invariants"),
                check("f4-differential-grid"));
    }

    private static FinanceReconciliationScorecardService.CheckRun check(String category) {
        return new FinanceReconciliationScorecardService.CheckRun(
                category,
                category + "-check",
                category + " local/live evidence",
                true,
                1,
                0,
                BigDecimal.ZERO,
                List.of());
    }

    private static FinanceReconciliationScorecardService.CheckRun pendingLiveCheck(String category) {
        return new FinanceReconciliationScorecardService.CheckRun(
                category,
                category + "-check",
                category + " pending live evidence",
                false,
                1,
                1,
                BigDecimal.ZERO,
                List.of(new FinanceReconciliationScorecardService.ReconciliationFailure(
                        category,
                        category + "-check",
                        "live-evidence",
                        "PENDING_LIVE_EVIDENCE",
                        BigDecimal.ZERO,
                        "live evidence is not configured")));
    }

    private static FinanceReconciliationScorecardRegistry.ScorecardPolicy scorecardPolicyWithOptionalLane() {
        return new FinanceReconciliationScorecardRegistry.ScorecardPolicy(
                SCORECARD_ID,
                "daily",
                BigDecimal.ZERO,
                true,
                List.of(
                        new FinanceReconciliationScorecardRegistry.CategoryPolicy(
                                "optional-observer",
                                "Optional observer",
                                "F4",
                                false),
                        new FinanceReconciliationScorecardRegistry.CategoryPolicy(
                                "f1-detail",
                                "F1 detail",
                                "F1",
                                true),
                        new FinanceReconciliationScorecardRegistry.CategoryPolicy(
                                "f2-summary-voucher",
                                "F2 summary voucher",
                                "F2",
                                true),
                        new FinanceReconciliationScorecardRegistry.CategoryPolicy(
                                "f3-invariants",
                                "F3 invariants",
                                "F3",
                                true),
                        new FinanceReconciliationScorecardRegistry.CategoryPolicy(
                                "f4-differential-grid",
                                "F4 differential grid",
                                "F4",
                                true)),
                "it/test_sprint33_reconciliation_scorecard.sh",
                List.of(),
                "");
    }

    private static FinanceReconciliationScorecardSnapshot snapshot(String oracleBindingId, String status) {
        FinanceReconciliationScorecardSnapshot snapshot = new FinanceReconciliationScorecardSnapshot();
        snapshot.setOracleBindingId(oracleBindingId);
        snapshot.setScorecardId(SCORECARD_ID);
        snapshot.setHealthStatus(status);
        return snapshot;
    }
}
