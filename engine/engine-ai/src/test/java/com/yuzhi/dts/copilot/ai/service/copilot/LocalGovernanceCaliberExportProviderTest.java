package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LocalGovernanceCaliberExportProviderTest {

    private LocalGovernanceCaliberExportProvider provider;

    @BeforeEach
    void setUp() {
        CaliberRuleRegistry registry = new CaliberRuleRegistry(new ObjectMapper());
        registry.init();
        provider = new LocalGovernanceCaliberExportProvider(registry);
    }

    @Test
    void shouldExportVersionedGuardrailsWithStableContentHash() {
        CaliberGuardrailSyncService.GovernanceCaliberExport first = provider.fetch();
        CaliberGuardrailSyncService.GovernanceCaliberExport second = provider.fetch();

        assertThat(first.version()).isEqualTo("local-governance/caliber-rules.v1.json");
        assertThat(first.contentHash()).startsWith("sha256:");
        assertThat(second.contentHash()).isEqualTo(first.contentHash());
        assertThat(first.guardrailsByDomain()).containsOnlyKeys("finance", "procurement");
        assertThat(first.guardrailsForDomain("finance"))
                .anySatisfy(rule -> assertThat(rule).contains("[CAL-SETTLEMENT-CHAIN]"));
        assertThat(first.guardrailsForDomain("procurement"))
                .anySatisfy(rule -> assertThat(rule).contains("[CAL-INVENTORY-COST]"));
    }

    @Test
    void shouldExportGuardrailsForRequestedDomainsOnly() {
        CaliberGuardrailSyncService.GovernanceCaliberExport finance = provider.fetch(List.of("finance"));
        CaliberGuardrailSyncService.GovernanceCaliberExport procurement = provider.fetch(List.of("procurement"));

        assertThat(finance.guardrailsByDomain()).containsOnlyKeys("finance");
        assertThat(procurement.guardrailsByDomain()).containsOnlyKeys("procurement");
        assertThat(finance.contentHash()).startsWith("sha256:");
        assertThat(procurement.contentHash()).startsWith("sha256:");
        assertThat(finance.contentHash()).isNotEqualTo(procurement.contentHash());
    }
}
