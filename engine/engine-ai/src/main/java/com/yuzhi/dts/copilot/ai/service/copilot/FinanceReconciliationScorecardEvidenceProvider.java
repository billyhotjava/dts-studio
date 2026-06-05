package com.yuzhi.dts.copilot.ai.service.copilot;

import java.util.List;

public interface FinanceReconciliationScorecardEvidenceProvider {

    String oracleBindingId();

    String scorecardId();

    List<FinanceReconciliationScorecardService.CheckRun> currentRuns();

    List<FinanceReconciliationScorecardService.ReconciliationFailure> baselineFailures();
}
