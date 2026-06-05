package com.yuzhi.dts.copilot.ai.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.service.copilot.CaliberGuardrailSyncService;
import com.yuzhi.dts.copilot.ai.service.copilot.CaliberRuleRegistry;
import com.yuzhi.dts.copilot.ai.service.copilot.SemanticPackService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class CaliberGuardrailSyncHealthIndicatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldExposeCaliberSyncDriftInHealthDetails() {
        FakeExportProvider provider = new FakeExportProvider();
        provider.nextExport(exportWithFinanceRuleOverride(
                "CAL-MONTH-AMOUNT-TIER",
                "[CAL-MONTH-AMOUNT-TIER] 标签: month_amount_tier; 规则: drifted governance text"));
        CaliberGuardrailSyncHealthIndicator indicator = new CaliberGuardrailSyncHealthIndicator(service(provider));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("caliberSyncDrift", true)
                .containsEntry("stale", false)
                .containsEntry("fallbackMode", "NONE");
        assertThat(health.getDetails().get("exportHash")).asString().startsWith("sha256:");
        assertThat(health.getDetails().get("drifts").toString())
                .contains("finance", "CAL-MONTH-AMOUNT-TIER", "GUARDRAIL_TEXT_MISMATCH");
    }

    @Test
    void shouldExposeStaleFallbackWhenGovernanceExportIsUnavailable() {
        FakeExportProvider provider = new FakeExportProvider();
        provider.failWith(new IllegalStateException("HTTP 503"));
        CaliberGuardrailSyncHealthIndicator indicator = new CaliberGuardrailSyncHealthIndicator(service(provider));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("caliberSyncDrift", false)
                .containsEntry("stale", true)
                .containsEntry("fallbackMode", "STATIC_PACK")
                .containsEntry("lastStatus", "FAILED");
        assertThat(health.getDetails().get("error")).asString().contains("HTTP 503");
    }

    private CaliberGuardrailSyncService service(FakeExportProvider provider) {
        SemanticPackService semanticPackService = new SemanticPackService(objectMapper);
        semanticPackService.init();
        return new CaliberGuardrailSyncService(provider, semanticPackService, objectMapper);
    }

    private CaliberGuardrailSyncService.GovernanceCaliberExport exportWithFinanceRuleOverride(
            String ruleId,
            String replacement) {
        CaliberRuleRegistry registry = new CaliberRuleRegistry(objectMapper);
        registry.init();
        List<String> financeGuardrails = new ArrayList<>();
        for (String guardrail : registry.guardrailsForDomain("finance")) {
            financeGuardrails.add(guardrail.startsWith("[" + ruleId + "]") ? replacement : guardrail);
        }
        Map<String, List<String>> byDomain = new LinkedHashMap<>();
        byDomain.put("finance", financeGuardrails);
        byDomain.put("procurement", registry.guardrailsForDomain("procurement"));
        return new CaliberGuardrailSyncService.GovernanceCaliberExport("2026-06-drift", byDomain);
    }

    private static final class FakeExportProvider implements CaliberGuardrailSyncService.GovernanceCaliberExportProvider {

        private CaliberGuardrailSyncService.GovernanceCaliberExport export;
        private RuntimeException failure;

        @Override
        public CaliberGuardrailSyncService.GovernanceCaliberExport fetch() {
            if (failure != null) {
                throw failure;
            }
            return export;
        }

        private void nextExport(CaliberGuardrailSyncService.GovernanceCaliberExport export) {
            this.export = export;
            this.failure = null;
        }

        private void failWith(RuntimeException failure) {
            this.failure = failure;
        }
    }
}
