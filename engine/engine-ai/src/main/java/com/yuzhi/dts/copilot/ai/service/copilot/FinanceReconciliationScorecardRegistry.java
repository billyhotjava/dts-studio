package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FinanceReconciliationScorecardRegistry {

    private static final Logger log = LoggerFactory.getLogger(FinanceReconciliationScorecardRegistry.class);
    private static final String REGISTRY_RESOURCE = "governance/finance-reconciliation-scorecard.v1.json";

    private final ObjectMapper objectMapper;
    private Map<String, ScorecardPolicy> policies = Map.of();

    public FinanceReconciliationScorecardRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(REGISTRY_RESOURCE)) {
            if (is == null) {
                log.warn("Finance reconciliation scorecard resource not found: {}", REGISTRY_RESOURCE);
                this.policies = Map.of();
                return;
            }
            ScorecardDocument document = objectMapper.readValue(is, ScorecardDocument.class);
            Map<String, ScorecardPolicy> loaded = new LinkedHashMap<>();
            for (ScorecardPolicy policy : document.policies()) {
                loaded.put(policy.id(), policy);
            }
            this.policies = java.util.Collections.unmodifiableMap(loaded);
            log.info("Loaded {} finance reconciliation scorecard policy(s) from {}", policies.size(), REGISTRY_RESOURCE);
        } catch (Exception e) {
            log.warn("Failed to load finance reconciliation scorecard policies from {}: {}", REGISTRY_RESOURCE, e.getMessage());
            this.policies = Map.of();
        }
    }

    public List<ScorecardPolicy> policies() {
        return new ArrayList<>(policies.values());
    }

    public Optional<ScorecardPolicy> policy(String id) {
        return Optional.ofNullable(policies.get(id));
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static BigDecimal decimalOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record ScorecardDocument(String version, List<ScorecardPolicy> policies) {
        private ScorecardDocument {
            policies = copyOrEmpty(policies);
        }
    }

    public record ScorecardPolicy(
            String id,
            String runMode,
            BigDecimal maxAllowedDifference,
            boolean failOnNewDrift,
            List<CategoryPolicy> categories,
            String itScript,
            List<String> evidenceSources,
            String notes) {
        public ScorecardPolicy {
            id = textOrEmpty(id);
            runMode = textOrEmpty(runMode);
            maxAllowedDifference = decimalOrZero(maxAllowedDifference);
            categories = copyOrEmpty(categories);
            itScript = textOrEmpty(itScript);
            evidenceSources = copyOrEmpty(evidenceSources);
            notes = textOrEmpty(notes);
        }

        public FinanceReconciliationScorecardService.ScorecardSpec scorecardSpec() {
            return new FinanceReconciliationScorecardService.ScorecardSpec(
                    id,
                    maxAllowedDifference,
                    failOnNewDrift,
                    categories.stream()
                            .filter(CategoryPolicy::required)
                            .map(CategoryPolicy::id)
                            .toList());
        }
    }

    public record CategoryPolicy(String id, String name, String sourceFeature, boolean required) {
        public CategoryPolicy {
            id = textOrEmpty(id);
            name = textOrEmpty(name);
            sourceFeature = textOrEmpty(sourceFeature);
        }
    }
}
