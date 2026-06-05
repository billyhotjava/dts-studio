package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class FinanceSignoffBaselineServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void buildsEngineeringReadyBaselineAndKeepsBusinessSignaturePending() {
        FinanceSignoffBaselineRegistry registry = baselineRegistry();
        FinanceSignoffBaselineService service = new FinanceSignoffBaselineService();
        FinanceSignoffBaselineRegistry.SignoffBaselinePolicy policy = registry.policy("sprint33-finance-signoff-baseline")
                .orElseThrow();

        FinanceSignoffBaselineService.SignoffBaselineReport report = service.buildBaseline(
                policy,
                passingScorecard(),
                evidenceRecords(policy.requiredEvidence()),
                List.of());

        assertThat(report.engineeringReady()).isTrue();
        assertThat(report.accepted()).isFalse();
        assertThat(report.signoffStatus()).isEqualTo("PENDING_SIGNATURE");
        assertThat(report.failureMessage()).contains("missing required signature");
        assertThat(report.accountPeriod()).isEqualTo("2026-06");
        assertThat(report.requiredEvidence())
                .extracting(FinanceSignoffBaselineService.EvidenceStatus::evidenceId)
                .containsExactly(
                        "f1-detail-reconciliation",
                        "f2-summary-dual-reconciliation",
                        "f2-voucher-subject-tieout",
                        "f3-invariant-guardrails",
                        "f4-differential-scorecard",
                        "f5-answer-audit-trail");
        assertThat(report.baselineMarkdown()).contains(
                "Sprint-33 财务签字基线",
                "IT 证据",
                "漂移基线采信",
                "未采信",
                "mvn -q -pl dts-copilot-ai -Dtest=FinanceDetailReconciliationServiceTest test",
                "bash worklog/v1.0.0/sprint-33-202607/it/test_f5_finance_answer_audit_trail.sh",
                "PENDING_SIGNATURE");
        assertThat(report.baselineMarkdown()).doesNotContain("TODO");
    }

    @Test
    void rejectsBaselineWhenRequiredEvidenceIsMissing() {
        FinanceSignoffBaselineRegistry registry = baselineRegistry();
        FinanceSignoffBaselineService service = new FinanceSignoffBaselineService();
        FinanceSignoffBaselineRegistry.SignoffBaselinePolicy policy = registry.policy("sprint33-finance-signoff-baseline")
                .orElseThrow();

        FinanceSignoffBaselineService.SignoffBaselineReport report = service.buildBaseline(
                policy,
                passingScorecard(),
                evidenceRecords(policy.requiredEvidence()).stream()
                        .filter(record -> !"f4-differential-scorecard".equals(record.evidenceId()))
                        .toList(),
                List.of(financeOwnerSignature(), auditSignature()));

        assertThat(report.engineeringReady()).isFalse();
        assertThat(report.accepted()).isFalse();
        assertThat(report.signoffStatus()).isEqualTo("EVIDENCE_INCOMPLETE");
        assertThat(report.failureMessage()).contains("missing evidence")
                .contains("f4-differential-scorecard");
    }

    @Test
    void acceptsBaselineOnlyAfterAllRequiredRolesSign() {
        FinanceSignoffBaselineRegistry registry = baselineRegistry();
        FinanceSignoffBaselineService service = new FinanceSignoffBaselineService();
        FinanceSignoffBaselineRegistry.SignoffBaselinePolicy policy = registry.policy("sprint33-finance-signoff-baseline")
                .orElseThrow();

        FinanceSignoffBaselineService.SignoffBaselineReport report = service.buildBaseline(
                policy,
                passingScorecard(),
                evidenceRecords(policy.requiredEvidence()),
                List.of(financeOwnerSignature(), auditSignature()));

        assertThat(report.engineeringReady()).isTrue();
        assertThat(report.accepted()).isTrue();
        assertThat(report.signoffStatus()).isEqualTo("SIGNED");
        assertThat(report.failureMessage()).isEmpty();
        assertThat(report.baselineMarkdown()).contains("SIGNED", "财务负责人", "审计复核");
    }

    @Test
    void pendingSignatureBaselineCannotSuppressScorecardDrift() {
        FinanceSignoffBaselineRegistry registry = baselineRegistry();
        FinanceSignoffBaselineService service = new FinanceSignoffBaselineService();
        FinanceSignoffBaselineRegistry.SignoffBaselinePolicy policy = registry.policy("sprint33-finance-signoff-baseline")
                .orElseThrow();
        FinanceSignoffBaselineService.SignoffBaselineReport pendingReport = service.buildBaseline(
                policy,
                passingScorecard(),
                evidenceRecords(policy.requiredEvidence()),
                List.of());
        FinanceReconciliationScorecardService.ReconciliationFailure knownFailure =
                failure("f4-differential-grid", "representative-grid", "sale-cross-month/projectId=1001", "amount mismatch", "0.01");

        List<FinanceReconciliationScorecardService.ReconciliationFailure> acceptedBaselineFailures =
                service.acceptedBaselineFailures(pendingReport, List.of(knownFailure));
        FinanceReconciliationScorecardService.ScorecardReport scorecardReport =
                new FinanceReconciliationScorecardService().score(scorecardSpec(), currentRunsWith(knownFailure), acceptedBaselineFailures);

        assertThat(pendingReport.signoffStatus()).isEqualTo("PENDING_SIGNATURE");
        assertThat(acceptedBaselineFailures).isEmpty();
        assertThat(scorecardReport.drifted()).isTrue();
        assertThat(scorecardReport.healthStatus()).isEqualTo("DRIFT");
        assertThat(scorecardReport.newDrifts()).containsExactly(knownFailure);
    }

    @Test
    void signedBaselineFailuresCanSuppressRepeatedDriftAlert() {
        FinanceSignoffBaselineRegistry registry = baselineRegistry();
        FinanceSignoffBaselineService service = new FinanceSignoffBaselineService();
        FinanceSignoffBaselineRegistry.SignoffBaselinePolicy policy = registry.policy("sprint33-finance-signoff-baseline")
                .orElseThrow();
        FinanceSignoffBaselineService.SignoffBaselineReport signedReport = service.buildBaseline(
                policy,
                passingScorecard(),
                evidenceRecords(policy.requiredEvidence()),
                List.of(financeOwnerSignature(), auditSignature()));
        FinanceReconciliationScorecardService.ReconciliationFailure knownFailure =
                failure("f4-differential-grid", "representative-grid", "sale-cross-month/projectId=1001", "amount mismatch", "0.01");

        List<FinanceReconciliationScorecardService.ReconciliationFailure> acceptedBaselineFailures =
                service.acceptedBaselineFailures(signedReport, List.of(knownFailure));
        FinanceReconciliationScorecardService.ScorecardReport scorecardReport =
                new FinanceReconciliationScorecardService().score(scorecardSpec(), currentRunsWith(knownFailure), acceptedBaselineFailures);

        assertThat(signedReport.signoffStatus()).isEqualTo("SIGNED");
        assertThat(acceptedBaselineFailures).containsExactly(knownFailure);
        assertThat(scorecardReport.drifted()).isFalse();
        assertThat(scorecardReport.healthStatus()).isEqualTo("FAIL");
        assertThat(scorecardReport.newDrifts()).isEmpty();
    }

    @Test
    void rejectsBaselineWhenScorecardDoesNotPass() {
        FinanceSignoffBaselineRegistry registry = baselineRegistry();
        FinanceSignoffBaselineService service = new FinanceSignoffBaselineService();
        FinanceSignoffBaselineRegistry.SignoffBaselinePolicy policy = registry.policy("sprint33-finance-signoff-baseline")
                .orElseThrow();

        FinanceSignoffBaselineService.SignoffBaselineReport report = service.buildBaseline(
                policy,
                new FinanceReconciliationScorecardService.ScorecardReport(
                        false,
                        true,
                        "DRIFT",
                        4,
                        3,
                        new BigDecimal("75.00"),
                        new BigDecimal("0.01"),
                        List.of(),
                        List.of(),
                        List.of(new FinanceReconciliationScorecardService.ReconciliationFailure(
                                "f4-differential-grid",
                                "month-settlement-grid",
                                "period=2026-06",
                                "amount mismatch",
                                new BigDecimal("0.01"),
                                "drift")),
                        "Reconciliation scorecard new drift"),
                evidenceRecords(policy.requiredEvidence()),
                List.of(financeOwnerSignature(), auditSignature()));

        assertThat(report.engineeringReady()).isFalse();
        assertThat(report.accepted()).isFalse();
        assertThat(report.signoffStatus()).isEqualTo("SCORECARD_FAILED");
        assertThat(report.failureMessage()).contains("Reconciliation scorecard new drift");
    }

    private FinanceSignoffBaselineRegistry baselineRegistry() {
        FinanceSignoffBaselineRegistry registry = new FinanceSignoffBaselineRegistry(objectMapper);
        registry.init();
        return registry;
    }

    private static FinanceReconciliationScorecardService.ScorecardReport passingScorecard() {
        return new FinanceReconciliationScorecardService.ScorecardReport(
                true,
                false,
                "PASS",
                4,
                4,
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                List.of(),
                List.of(),
                List.of(),
                "");
    }

    private static List<FinanceSignoffBaselineService.EvidenceRecord> evidenceRecords(
            List<FinanceSignoffBaselineRegistry.RequiredEvidence> requiredEvidence) {
        return requiredEvidence.stream()
                .map(evidence -> new FinanceSignoffBaselineService.EvidenceRecord(
                        evidence.id(),
                        evidence.feature(),
                        evidence.command(),
                        evidence.evidencePath(),
                        "PASS"))
                .toList();
    }

    private static FinanceSignoffBaselineService.SignatureRecord financeOwnerSignature() {
        return new FinanceSignoffBaselineService.SignatureRecord(
                "FINANCE_OWNER",
                "财务负责人",
                "2026-06-05",
                "同意作为后续漂移基线");
    }

    private static FinanceSignoffBaselineService.SignatureRecord auditSignature() {
        return new FinanceSignoffBaselineService.SignatureRecord(
                "AUDITOR",
                "审计复核",
                "2026-06-05",
                "证据包可复跑");
    }

    private static FinanceReconciliationScorecardService.ScorecardSpec scorecardSpec() {
        return new FinanceReconciliationScorecardService.ScorecardSpec(
                "sprint33-finance-daily-scorecard",
                BigDecimal.ZERO,
                true,
                List.of("f1-detail", "f2-summary-voucher", "f3-invariants", "f4-differential-grid"));
    }

    private static List<FinanceReconciliationScorecardService.CheckRun> currentRunsWith(
            FinanceReconciliationScorecardService.ReconciliationFailure failure) {
        return List.of(
                check("f1-detail", "detail-harness", true, 2, 0, "0.00", List.of()),
                check("f2-summary-voucher", "summary-dual", true, 3, 0, "0.00", List.of()),
                check("f3-invariants", "invariant-regression", true, 8, 0, "0.00", List.of()),
                check("f4-differential-grid", "representative-grid", false, 7, 1, "0.01", List.of(failure)));
    }

    private static FinanceReconciliationScorecardService.CheckRun check(
            String category,
            String checkId,
            boolean passed,
            int totalCells,
            int failedCells,
            String maxDifference,
            List<FinanceReconciliationScorecardService.ReconciliationFailure> failures) {
        return new FinanceReconciliationScorecardService.CheckRun(
                category,
                checkId,
                checkId,
                passed,
                totalCells,
                failedCells,
                new BigDecimal(maxDifference),
                failures);
    }

    private static FinanceReconciliationScorecardService.ReconciliationFailure failure(
            String category,
            String checkId,
            String cellKey,
            String status,
            String difference) {
        return new FinanceReconciliationScorecardService.ReconciliationFailure(
                category,
                checkId,
                cellKey,
                status,
                new BigDecimal(difference),
                status + " at " + cellKey);
    }
}
