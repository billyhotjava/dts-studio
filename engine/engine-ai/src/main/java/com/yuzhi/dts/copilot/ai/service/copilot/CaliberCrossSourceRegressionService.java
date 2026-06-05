package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CaliberCrossSourceRegressionService {

    private static final String DEFAULT_SPEC_RESOURCE = "governance/caliber-cross-source-regression.v1.json";

    private final CaliberRuleRegistry ruleRegistry;
    private final SemanticPackService semanticPackService;
    private final ObjectMapper objectMapper;

    public CaliberCrossSourceRegressionService(
            CaliberRuleRegistry ruleRegistry,
            SemanticPackService semanticPackService,
            ObjectMapper objectMapper) {
        this.ruleRegistry = ruleRegistry;
        this.semanticPackService = semanticPackService;
        this.objectMapper = objectMapper;
    }

    public RegressionReport runDefault() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(DEFAULT_SPEC_RESOURCE)) {
            if (is == null) {
                throw new IllegalStateException("Caliber cross-source regression resource not found: "
                        + DEFAULT_SPEC_RESOURCE);
            }
            return run(objectMapper.readValue(is, RegressionSpec.class));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to run caliber cross-source regression", e);
        }
    }

    public RegressionReport run(RegressionSpec spec) {
        List<DomainReport> domainReports = new ArrayList<>();
        List<RegressionDrift> allDrifts = new ArrayList<>();
        for (DomainSpec domainSpec : spec.domains()) {
            DomainReport domainReport = evaluateDomain(domainSpec);
            domainReports.add(domainReport);
            allDrifts.addAll(domainReport.drifts());
        }
        return new RegressionReport(spec.version(), allDrifts.isEmpty(), domainReports, allDrifts);
    }

    private DomainReport evaluateDomain(DomainSpec domainSpec) {
        String domain = normalize(domainSpec.domain());
        List<String> ruleIds = requiredRuleIds(domainSpec);
        Set<String> registryRuleIds = registryRuleIds(domain);
        Map<String, List<SourceEvidence>> dbtEvidence = groupEvidence(domainSpec.dbtEvidence());
        Map<String, List<SourceEvidence>> glossaryEvidence = groupEvidence(domainSpec.glossaryEvidence());

        List<RuleReport> ruleReports = new ArrayList<>();
        List<RegressionDrift> domainDrifts = new ArrayList<>();
        for (String ruleId : ruleIds) {
            List<RegressionDrift> ruleDrifts = new ArrayList<>();
            if (!registryRuleIds.contains(ruleId)) {
                ruleDrifts.add(new RegressionDrift(domain, ruleId, "SOT",
                        "rule is not declared for this domain in governance/caliber-rules.v1.json"));
            }

            List<String> packEvidence = packEvidence(domain, ruleId);
            if (packEvidence.isEmpty()) {
                ruleDrifts.add(new RegressionDrift(domain, ruleId, "PACK",
                        "semantic pack has no generated/manual guardrail for rule"));
            }

            List<String> dbtRefs = evidenceRefs(dbtEvidence.get(ruleId));
            if (dbtRefs.isEmpty()) {
                ruleDrifts.add(new RegressionDrift(domain, ruleId, "DBT",
                        "dbt mart evidence is missing from cross-source regression spec"));
            }

            List<String> glossaryRefs = evidenceRefs(glossaryEvidence.get(ruleId));
            if (glossaryRefs.isEmpty()) {
                ruleDrifts.add(new RegressionDrift(domain, ruleId, "GLOSSARY",
                        "glossary/OpenMetadata evidence is missing from cross-source regression spec"));
            }

            domainDrifts.addAll(ruleDrifts);
            ruleReports.add(new RuleReport(
                    ruleId,
                    ruleDrifts.isEmpty(),
                    packEvidence,
                    dbtRefs,
                    glossaryRefs,
                    ruleDrifts));
        }
        return new DomainReport(domain, domainDrifts.isEmpty(), ruleReports, domainDrifts);
    }

    private List<String> requiredRuleIds(DomainSpec domainSpec) {
        if (!domainSpec.ruleIds().isEmpty()) {
            return domainSpec.ruleIds();
        }
        return registryRuleIds(normalize(domainSpec.domain())).stream().toList();
    }

    private Set<String> registryRuleIds(String domain) {
        return ruleRegistry.rules().stream()
                .filter(rule -> rule.domains().isEmpty()
                        || rule.domains().stream().map(CaliberCrossSourceRegressionService::normalize)
                                .anyMatch(domain::equals))
                .map(CaliberRuleRegistry.CaliberRule::id)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private List<String> packEvidence(String domain, String ruleId) {
        String expectedGuardrail = expectedGuardrail(domain, ruleId);
        if (expectedGuardrail.isBlank()) {
            return List.of();
        }
        return semanticPackService.getPack(domain)
                .map(pack -> pack.guardrails().stream()
                        .filter(expectedGuardrail::equals)
                        .map(ignored -> "semantic-pack:" + domain + ":generatedGuardrails")
                        .distinct()
                        .toList())
                .orElse(List.of());
    }

    private String expectedGuardrail(String domain, String ruleId) {
        String prefix = "[" + ruleId + "]";
        return ruleRegistry.guardrailsForDomain(domain).stream()
                .filter(guardrail -> guardrail.startsWith(prefix))
                .findFirst()
                .orElse("");
    }

    private static Map<String, List<SourceEvidence>> groupEvidence(List<SourceEvidence> evidence) {
        Map<String, List<SourceEvidence>> grouped = new LinkedHashMap<>();
        for (SourceEvidence item : evidence) {
            if (item.ruleId().isBlank()) {
                continue;
            }
            grouped.computeIfAbsent(item.ruleId(), ignored -> new ArrayList<>()).add(item);
        }
        return grouped;
    }

    private static List<String> evidenceRefs(List<SourceEvidence> evidence) {
        if (evidence == null || evidence.isEmpty()) {
            return List.of();
        }
        List<String> refs = new ArrayList<>();
        for (SourceEvidence item : evidence) {
            if (item.ref().isBlank()) {
                continue;
            }
            if (item.attributes().isEmpty()) {
                refs.add(item.ref());
            } else {
                refs.add(item.ref() + " [" + String.join(", ", item.attributes()) + "]");
            }
        }
        return List.copyOf(refs);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(values));
    }

    public record RegressionSpec(String version, List<DomainSpec> domains) {
        public RegressionSpec {
            version = textOrEmpty(version);
            domains = copyOrEmpty(domains);
        }
    }

    public record DomainSpec(
            String domain,
            List<String> ruleIds,
            List<SourceEvidence> dbtEvidence,
            List<SourceEvidence> glossaryEvidence) {
        public DomainSpec {
            domain = textOrEmpty(domain);
            ruleIds = copyOrEmpty(ruleIds);
            dbtEvidence = copyOrEmpty(dbtEvidence);
            glossaryEvidence = copyOrEmpty(glossaryEvidence);
        }
    }

    public record SourceEvidence(String ruleId, String ref, List<String> attributes) {
        public SourceEvidence {
            ruleId = textOrEmpty(ruleId);
            ref = textOrEmpty(ref);
            attributes = copyOrEmpty(attributes);
        }
    }

    public record RegressionReport(
            String version,
            boolean passed,
            List<DomainReport> domainReports,
            List<RegressionDrift> drifts) {
        public RegressionReport {
            version = textOrEmpty(version);
            domainReports = copyOrEmpty(domainReports);
            drifts = copyOrEmpty(drifts);
        }
    }

    public record DomainReport(
            String domain,
            boolean passed,
            List<RuleReport> ruleReports,
            List<RegressionDrift> drifts) {
        public DomainReport {
            domain = textOrEmpty(domain);
            ruleReports = copyOrEmpty(ruleReports);
            drifts = copyOrEmpty(drifts);
        }
    }

    public record RuleReport(
            String ruleId,
            boolean passed,
            List<String> packEvidence,
            List<String> dbtEvidence,
            List<String> glossaryEvidence,
            List<RegressionDrift> drifts) {
        public RuleReport {
            ruleId = textOrEmpty(ruleId);
            packEvidence = copyOrEmpty(packEvidence);
            dbtEvidence = copyOrEmpty(dbtEvidence);
            glossaryEvidence = copyOrEmpty(glossaryEvidence);
            drifts = copyOrEmpty(drifts);
        }
    }

    public record RegressionDrift(String domain, String ruleId, String source, String message) {
        public RegressionDrift {
            domain = textOrEmpty(domain);
            ruleId = textOrEmpty(ruleId);
            source = textOrEmpty(source);
            message = textOrEmpty(message);
        }
    }
}
