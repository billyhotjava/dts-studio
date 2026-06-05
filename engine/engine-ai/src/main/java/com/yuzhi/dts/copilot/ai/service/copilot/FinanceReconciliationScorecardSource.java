package com.yuzhi.dts.copilot.ai.service.copilot;

import java.util.Optional;

@FunctionalInterface
public interface FinanceReconciliationScorecardSource {

    Optional<FinanceReconciliationScorecardService.ScorecardReport> latestScorecard(String oracleBindingId);

    static FinanceReconciliationScorecardSource empty() {
        return ignored -> Optional.empty();
    }
}
