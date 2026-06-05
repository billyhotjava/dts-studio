package com.yuzhi.dts.copilot.ai.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.service.copilot.FinanceReconciliationScorecardRegistry;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class FinanceReconciliationHealthIndicatorTest {

    @Test
    void exposesFinanceReconciliationScorecardPolicyWithoutClaimingLivePass() {
        FinanceReconciliationScorecardRegistry registry =
                new FinanceReconciliationScorecardRegistry(new ObjectMapper());
        registry.init();
        FinanceReconciliationHealthIndicator indicator =
                new FinanceReconciliationHealthIndicator(registry);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("scorecardId", "sprint33-finance-daily-scorecard")
                .containsEntry("runMode", "manual-or-scheduled")
                .containsEntry("healthStatus", "PENDING_LIVE_EVIDENCE")
                .containsEntry("itScript", "worklog/v1.0.0/sprint-33-202607/it/test_sprint33_reconciliation_scorecard.sh");
        assertThat(health.getDetails().get("requiredCategories").toString())
                .contains("f1-detail", "f2-summary-voucher", "f3-invariants", "f4-differential-grid");
    }

    @Test
    void exposesHealthComponentsSoActuatorCanShowFinanceReconciliationField() throws Exception {
        String applicationYaml = new String(
                getClass().getClassLoader().getResourceAsStream("application.yml").readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(applicationYaml)
                .contains("show-components: always");
    }
}
