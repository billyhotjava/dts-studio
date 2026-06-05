package com.yuzhi.dts.copilot.ai.service.copilot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LocalGovernanceCaliberExportProvider
        implements CaliberGuardrailSyncService.GovernanceCaliberExportProvider {

    private static final String LOCAL_EXPORT_VERSION = "local-governance/caliber-rules.v1.json";
    private static final List<String> DEFAULT_DOMAINS = List.of("finance", "procurement");

    private final CaliberRuleRegistry ruleRegistry;

    public LocalGovernanceCaliberExportProvider(CaliberRuleRegistry ruleRegistry) {
        this.ruleRegistry = ruleRegistry;
    }

    @Override
    public CaliberGuardrailSyncService.GovernanceCaliberExport fetch() {
        Map<String, List<String>> guardrailsByDomain = new LinkedHashMap<>();
        for (String domain : DEFAULT_DOMAINS) {
            guardrailsByDomain.put(domain, ruleRegistry.guardrailsForDomain(domain));
        }
        return new CaliberGuardrailSyncService.GovernanceCaliberExport(LOCAL_EXPORT_VERSION, guardrailsByDomain);
    }
}
