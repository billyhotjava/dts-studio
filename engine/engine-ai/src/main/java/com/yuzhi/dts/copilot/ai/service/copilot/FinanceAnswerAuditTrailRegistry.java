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
public class FinanceAnswerAuditTrailRegistry {

    private static final Logger log = LoggerFactory.getLogger(FinanceAnswerAuditTrailRegistry.class);
    private static final String REGISTRY_RESOURCE = "governance/finance-answer-audit-trail.v1.json";

    private final ObjectMapper objectMapper;
    private Map<String, AuditTrailPolicy> policies = Map.of();
    private Map<String, AuditTrailBindingPolicy> bindingPolicies = Map.of();

    public FinanceAnswerAuditTrailRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(REGISTRY_RESOURCE)) {
            if (is == null) {
                log.warn("Finance answer audit trail resource not found: {}", REGISTRY_RESOURCE);
                this.policies = Map.of();
                this.bindingPolicies = Map.of();
                return;
            }
            AuditTrailDocument document = objectMapper.readValue(is, AuditTrailDocument.class);
            Map<String, AuditTrailPolicy> loadedPolicies = new LinkedHashMap<>();
            for (AuditTrailPolicy policy : document.policies()) {
                loadedPolicies.put(policy.id(), policy);
            }
            Map<String, AuditTrailBindingPolicy> loadedBindings = new LinkedHashMap<>();
            for (AuditTrailBindingPolicy bindingPolicy : document.bindingPolicies()) {
                loadedBindings.put(bindingPolicy.oracleBindingId(), bindingPolicy);
            }
            this.policies = java.util.Collections.unmodifiableMap(loadedPolicies);
            this.bindingPolicies = java.util.Collections.unmodifiableMap(loadedBindings);
            log.info("Loaded {} finance answer audit trail policy(s) and {} binding policy(s) from {}",
                    policies.size(), bindingPolicies.size(), REGISTRY_RESOURCE);
        } catch (Exception e) {
            log.warn("Failed to load finance answer audit trail registry from {}: {}", REGISTRY_RESOURCE, e.getMessage());
            this.policies = Map.of();
            this.bindingPolicies = Map.of();
        }
    }

    public Optional<AuditTrailPolicy> policy(String id) {
        return Optional.ofNullable(policies.get(id));
    }

    public List<AuditTrailPolicy> policies() {
        return new ArrayList<>(policies.values());
    }

    public Optional<AuditTrailBindingPolicy> bindingPolicy(String oracleBindingId) {
        return Optional.ofNullable(bindingPolicies.get(oracleBindingId));
    }

    public List<AuditTrailBindingPolicy> bindingPolicies() {
        return new ArrayList<>(bindingPolicies.values());
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private record AuditTrailDocument(
            String version,
            List<AuditTrailPolicy> policies,
            List<AuditTrailBindingPolicy> bindingPolicies) {
        private AuditTrailDocument {
            policies = copyOrEmpty(policies);
            bindingPolicies = copyOrEmpty(bindingPolicies);
        }
    }

    public record AuditTrailPolicy(
            String id,
            List<String> domains,
            List<String> requiredSections,
            String itScript) {
        public AuditTrailPolicy {
            id = textOrEmpty(id);
            domains = copyOrEmpty(domains);
            requiredSections = copyOrEmpty(requiredSections);
            itScript = textOrEmpty(itScript);
        }
    }

    public record AuditTrailBindingPolicy(
            String oracleBindingId,
            String reportCode,
            List<String> adsModels,
            List<String> caliberRuleIds,
            List<String> invariantIds,
            List<String> lineageRefs) {
        public AuditTrailBindingPolicy {
            oracleBindingId = textOrEmpty(oracleBindingId);
            reportCode = textOrEmpty(reportCode);
            adsModels = copyOrEmpty(adsModels);
            caliberRuleIds = copyOrEmpty(caliberRuleIds);
            invariantIds = copyOrEmpty(invariantIds);
            lineageRefs = copyOrEmpty(lineageRefs);
        }
    }
}
