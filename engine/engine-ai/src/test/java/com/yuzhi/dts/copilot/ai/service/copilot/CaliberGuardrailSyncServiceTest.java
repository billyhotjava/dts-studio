package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CaliberGuardrailSyncServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDetectGeneratedPackDriftAgainstGovernanceExport() {
        FakeExportProvider provider = new FakeExportProvider();
        provider.nextExport(exportWithFinanceRuleOverride(
                "CAL-MONTH-AMOUNT-TIER",
                "[CAL-MONTH-AMOUNT-TIER] 标签: month_amount_tier; 规则: drifted governance text"));
        CaliberGuardrailSyncService service = service(provider);

        CaliberGuardrailSyncService.SyncReport report = service.refresh();

        assertThat(report.status()).isEqualTo(CaliberGuardrailSyncService.SyncStatus.SUCCESS);
        assertThat(report.stale()).isFalse();
        assertThat(report.drifted()).isTrue();
        assertThat(report.drifts())
                .anySatisfy(drift -> assertThat(drift)
                        .extracting(
                                CaliberGuardrailSyncService.SyncDrift::domain,
                                CaliberGuardrailSyncService.SyncDrift::ruleId,
                                CaliberGuardrailSyncService.SyncDrift::type)
                        .containsExactly("finance", "CAL-MONTH-AMOUNT-TIER", "GUARDRAIL_TEXT_MISMATCH"));
    }

    @Test
    void shouldKeepLastSuccessfulGovernanceExportWhenProviderFails() {
        FakeExportProvider provider = new FakeExportProvider();
        provider.nextExport(exportFromRegistry("2026-06-ok"));
        CaliberGuardrailSyncService service = service(provider);
        CaliberGuardrailSyncService.SyncReport success = service.refresh();

        provider.failWith(new IllegalStateException("HTTP 503"));
        CaliberGuardrailSyncService.SyncReport stale = service.refresh();

        assertThat(success.status()).isEqualTo(CaliberGuardrailSyncService.SyncStatus.SUCCESS);
        assertThat(stale.status()).isEqualTo(CaliberGuardrailSyncService.SyncStatus.FAILED);
        assertThat(stale.stale()).isTrue();
        assertThat(stale.fallbackMode()).isEqualTo(CaliberGuardrailSyncService.FallbackMode.STALE_CACHE);
        assertThat(stale.error()).contains("HTTP 503");
        assertThat(stale.exportVersion()).isEqualTo("2026-06-ok");
        assertThat(stale.guardrailsForDomain("finance")).isNotEmpty();
        assertThat(stale.guardrailsForDomain(" Finance ")).isNotEmpty();
    }

    @Test
    void shouldFallbackToStaticPackGuardrailsWhenProviderFailsBeforeAnyCache() {
        FakeExportProvider provider = new FakeExportProvider();
        provider.failWith(new IllegalStateException("connect timeout"));
        CaliberGuardrailSyncService service = service(provider);

        CaliberGuardrailSyncService.SyncReport report = service.refresh();

        assertThat(report.status()).isEqualTo(CaliberGuardrailSyncService.SyncStatus.FAILED);
        assertThat(report.stale()).isTrue();
        assertThat(report.fallbackMode()).isEqualTo(CaliberGuardrailSyncService.FallbackMode.STATIC_PACK);
        assertThat(report.guardrailsForDomain("finance"))
                .anySatisfy(guardrail -> assertThat(guardrail).contains("[CAL-SETTLEMENT-CHAIN]"));
    }

    private CaliberGuardrailSyncService service(FakeExportProvider provider) {
        CaliberRuleRegistry registry = new CaliberRuleRegistry(objectMapper);
        registry.init();
        SemanticPackService semanticPackService = new SemanticPackService(objectMapper);
        semanticPackService.init();
        return new CaliberGuardrailSyncService(provider, semanticPackService, objectMapper);
    }

    private CaliberGuardrailSyncService.GovernanceCaliberExport exportFromRegistry(String version) {
        CaliberRuleRegistry registry = new CaliberRuleRegistry(objectMapper);
        registry.init();
        return new CaliberGuardrailSyncService.GovernanceCaliberExport(
                version,
                Map.of(
                        "finance", registry.guardrailsForDomain("finance"),
                        "procurement", registry.guardrailsForDomain("procurement")));
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
