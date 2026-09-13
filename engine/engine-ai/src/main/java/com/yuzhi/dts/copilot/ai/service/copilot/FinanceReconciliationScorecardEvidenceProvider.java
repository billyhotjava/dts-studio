package com.yuzhi.dts.copilot.ai.service.copilot;

import java.util.List;

public interface FinanceReconciliationScorecardEvidenceProvider {

    String authorityBindingId();

    default String oracleBindingId() {
        return authorityBindingId();
    }

    String scorecardId();

    List<FinanceReconciliationScorecardService.CheckRun> currentRuns();

    List<FinanceReconciliationScorecardService.ReconciliationFailure> baselineFailures();
}
