package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FinanceWeakPathReconciliationCandidateRegistry {

    private static final Logger log = LoggerFactory.getLogger(FinanceWeakPathReconciliationCandidateRegistry.class);
    private static final String REGISTRY_RESOURCE = "governance/finance-weak-path-reconciliation-candidates.v1.json";

    private final ObjectMapper objectMapper;
    private Map<String, WeakPathCandidatePolicy> policies = Map.of();

    public FinanceWeakPathReconciliationCandidateRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(REGISTRY_RESOURCE)) {
            if (is == null) {
                log.warn("Finance weak path reconciliation candidate resource not found: {}", REGISTRY_RESOURCE);
                this.policies = Map.of();
                return;
            }
            WeakPathCandidateDocument document = objectMapper.readValue(is, WeakPathCandidateDocument.class);
            Map<String, WeakPathCandidatePolicy> loaded = new LinkedHashMap<>();
            for (WeakPathCandidatePolicy policy : document.policies()) {
                loaded.put(policy.id(), policy);
            }
            this.policies = java.util.Collections.unmodifiableMap(loaded);
            log.info("Loaded {} finance weak path reconciliation candidate policy(s) from {}", policies.size(), REGISTRY_RESOURCE);
        } catch (Exception e) {
            log.warn("Failed to load finance weak path reconciliation candidate policies from {}: {}", REGISTRY_RESOURCE, e.getMessage());
            this.policies = Map.of();
        }
    }

    public List<WeakPathCandidatePolicy> policies() {
        return new ArrayList<>(policies.values());
    }

    public Optional<WeakPathCandidatePolicy> policy(String id) {
        return Optional.ofNullable(policies.get(id));
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private record WeakPathCandidateDocument(String version, List<WeakPathCandidatePolicy> policies) {
        private WeakPathCandidateDocument {
            policies = copyOrEmpty(policies);
        }
    }

    public record WeakPathCandidatePolicy(
            String id,
            List<String> financeDomains,
            List<String> weakTiers,
            List<String> financeKeywords,
            Integer minCount,
            Integer semanticDraftThreshold,
            List<String> reconciliationSets,
            String itScript,
            String notes) {
        public WeakPathCandidatePolicy {
            id = textOrEmpty(id);
            financeDomains = copyOrEmpty(financeDomains);
            weakTiers = copyOrEmpty(weakTiers);
            financeKeywords = copyOrEmpty(financeKeywords);
            minCount = minCount == null ? 1 : Math.max(minCount, 1);
            semanticDraftThreshold = semanticDraftThreshold == null ? minCount : Math.max(semanticDraftThreshold, minCount);
            reconciliationSets = copyOrEmpty(reconciliationSets);
            itScript = textOrEmpty(itScript);
            notes = textOrEmpty(notes);
        }

        public FinanceWeakPathReconciliationCandidateService.WeakPathCandidateSpec candidateSpec() {
            return new FinanceWeakPathReconciliationCandidateService.WeakPathCandidateSpec(
                    id,
                    financeDomains,
                    weakTiers,
                    financeKeywords,
                    minCount,
                    semanticDraftThreshold,
                    reconciliationSets);
        }
    }
}
