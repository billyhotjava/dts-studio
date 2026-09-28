package com.yuzhi.dts.copilot.ai.service.copilot;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FinanceApplicationMysqlAuthorityScorecardCheckService {

    private static final String CATEGORY = "f2-summary-voucher";
    private static final String CHECK_ID = "application-mysql-authority-proof";
    private static final String CHECK_NAME = "ADS vs 应用 MySQL 权威基准 proof";
    private static final String PENDING_LIVE_EVIDENCE = "PENDING_LIVE_EVIDENCE";
    private static final String AUTHORITY_PROOF_LABEL = "Finance application MySQL authority proof";

    public FinanceReconciliationScorecardService.CheckRun toCheckRun(
            FinanceApplicationMysqlAuthorityProofRunner.RunResult result) {
        FinanceApplicationMysqlAuthorityProofRunner.RunResult safeResult = result == null
                ? new FinanceApplicationMysqlAuthorityProofRunner.RunResult(
                        "",
                        FinanceApplicationMysqlAuthorityProofRunner.RunStatus.DISABLED,
                        AUTHORITY_PROOF_LABEL + " result is missing",
                        List.of())
                : result;
        if (safeResult.status() == FinanceApplicationMysqlAuthorityProofRunner.RunStatus.DISABLED
                || safeResult.status() == FinanceApplicationMysqlAuthorityProofRunner.RunStatus.NOT_FOUND
                || safeResult.reports().isEmpty()) {
            return singleFailureCheck(
                    PENDING_LIVE_EVIDENCE,
                    StringUtils.hasText(safeResult.message())
                            ? safeResult.message()
                            : AUTHORITY_PROOF_LABEL + " evidence is not available");
        }

        List<FinanceReconciliationScorecardService.ReconciliationFailure> failures = safeResult.reports().stream()
                .flatMap(report -> report.reconciliation().diffs().stream()
                        .filter(diff -> !"matched".equals(diff.status()))
                        .map(diff -> toFailure(report, diff)))
                .toList();
        BigDecimal maxDifference = safeResult.reports().stream()
                .flatMap(report -> report.reconciliation().diffs().stream())
                .map(FinanceSummaryDualReconciliationService.SummaryDiff::difference)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        int totalCells = safeResult.reports().stream()
                .mapToInt(report -> report.reconciliation().diffs().size())
                .sum();
        boolean passed = safeResult.status() == FinanceApplicationMysqlAuthorityProofRunner.RunStatus.PASSED
                && failures.isEmpty();
        if (!passed && failures.isEmpty()) {
            return singleFailureCheck(
                    "APPLICATION_MYSQL_AUTHORITY_PROOF_FAILED",
                    StringUtils.hasText(safeResult.message())
                            ? safeResult.message()
                            : AUTHORITY_PROOF_LABEL + " failed");
        }
        return new FinanceReconciliationScorecardService.CheckRun(
                CATEGORY,
                CHECK_ID,
                CHECK_NAME,
                passed,
                Math.max(totalCells, 1),
                failures.size(),
                maxDifference,
                failures);
    }

    private static FinanceReconciliationScorecardService.ReconciliationFailure toFailure(
            FinanceApplicationMysqlAuthorityProofService.ProofReport report,
            FinanceSummaryDualReconciliationService.SummaryDiff diff) {
        return new FinanceReconciliationScorecardService.ReconciliationFailure(
                CATEGORY,
                CHECK_ID,
                report.caseId() + "/" + summaryCellKey(diff.key()),
                diff.status(),
                diff.difference(),
                report.failureMessage());
    }

    private static String summaryCellKey(FinanceSummaryDualReconciliationService.SummaryKey key) {
        String dimensionsText = key.dimensions().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        return dimensionsText.isEmpty() ? key.metricId() : key.metricId() + "/" + dimensionsText;
    }

    private static FinanceReconciliationScorecardService.CheckRun singleFailureCheck(
            String status,
            String message) {
        FinanceReconciliationScorecardService.ReconciliationFailure failure =
                new FinanceReconciliationScorecardService.ReconciliationFailure(
                        CATEGORY,
                        CHECK_ID,
                        CHECK_ID,
                        status,
                        BigDecimal.ZERO,
                        message);
        return new FinanceReconciliationScorecardService.CheckRun(
                CATEGORY,
                CHECK_ID,
                CHECK_NAME,
                false,
                1,
                1,
                BigDecimal.ZERO,
                List.of(failure));
    }
}
