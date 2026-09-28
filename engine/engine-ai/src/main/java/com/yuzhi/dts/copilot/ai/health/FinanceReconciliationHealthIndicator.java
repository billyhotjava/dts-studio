package com.yuzhi.dts.copilot.ai.health;

import com.yuzhi.dts.copilot.ai.service.copilot.FinanceReconciliationScorecardRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class FinanceReconciliationHealthIndicator implements HealthIndicator {

    private static final String SCORECARD_ID = "sprint33-finance-daily-scorecard";
    private static final String PENDING_LIVE_EVIDENCE = "PENDING_LIVE_EVIDENCE";
    private static final String POLICY_MISSING = "POLICY_MISSING";

    private final FinanceReconciliationScorecardRegistry registry;

    public FinanceReconciliationHealthIndicator(FinanceReconciliationScorecardRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Health health() {
        return registry.policy(SCORECARD_ID)
                .map(this::policyHealth)
                .orElseGet(this::missingPolicyHealth);
    }

    private Health policyHealth(FinanceReconciliationScorecardRegistry.ScorecardPolicy policy) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("scorecardId", policy.id());
        details.put("runMode", policy.runMode());
        details.put("healthStatus", PENDING_LIVE_EVIDENCE);
        details.put("maxAllowedDifference", policy.maxAllowedDifference());
        details.put("failOnNewDrift", policy.failOnNewDrift());
        details.put("requiredCategories", requiredCategories(policy));
        details.put("itScript", policy.itScript());
        details.put("evidenceSources", policy.evidenceSources());
        details.put("notes", policy.notes());
        return Health.up()
                .withDetails(details)
                .build();
    }

    private Health missingPolicyHealth() {
        return Health.down()
                .withDetail("scorecardId", SCORECARD_ID)
                .withDetail("healthStatus", POLICY_MISSING)
                .build();
    }

    private static List<String> requiredCategories(FinanceReconciliationScorecardRegistry.ScorecardPolicy policy) {
        return policy.categories().stream()
                .filter(FinanceReconciliationScorecardRegistry.CategoryPolicy::required)
                .map(FinanceReconciliationScorecardRegistry.CategoryPolicy::id)
                .toList();
    }
}
