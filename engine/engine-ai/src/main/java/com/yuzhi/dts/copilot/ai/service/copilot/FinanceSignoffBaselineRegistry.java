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
public class FinanceSignoffBaselineRegistry {

    private static final Logger log = LoggerFactory.getLogger(FinanceSignoffBaselineRegistry.class);
    private static final String REGISTRY_RESOURCE = "governance/finance-signoff-baseline.v1.json";

    private final ObjectMapper objectMapper;
    private Map<String, SignoffBaselinePolicy> policies = Map.of();

    public FinanceSignoffBaselineRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(REGISTRY_RESOURCE)) {
            if (is == null) {
                log.warn("Finance signoff baseline resource not found: {}", REGISTRY_RESOURCE);
                this.policies = Map.of();
                return;
            }
            SignoffBaselineDocument document = objectMapper.readValue(is, SignoffBaselineDocument.class);
            Map<String, SignoffBaselinePolicy> loaded = new LinkedHashMap<>();
            for (SignoffBaselinePolicy policy : document.policies()) {
                loaded.put(policy.id(), policy);
            }
            this.policies = java.util.Collections.unmodifiableMap(loaded);
            log.info("Loaded {} finance signoff baseline policy(s) from {}", policies.size(), REGISTRY_RESOURCE);
        } catch (Exception e) {
            log.warn("Failed to load finance signoff baseline registry from {}: {}", REGISTRY_RESOURCE, e.getMessage());
            this.policies = Map.of();
        }
    }

    public Optional<SignoffBaselinePolicy> policy(String id) {
        return Optional.ofNullable(policies.get(id));
    }

    public List<SignoffBaselinePolicy> policies() {
        return new ArrayList<>(policies.values());
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private record SignoffBaselineDocument(String version, List<SignoffBaselinePolicy> policies) {
        private SignoffBaselineDocument {
            policies = copyOrEmpty(policies);
        }
    }

    public record SignoffBaselinePolicy(
            String id,
            String title,
            String accountPeriod,
            String scorecardPolicyId,
            List<RequiredEvidence> requiredEvidence,
            List<RequiredSignatureRole> requiredSignatureRoles,
            String assetPath,
            String itScript) {
        public SignoffBaselinePolicy {
            id = textOrEmpty(id);
            title = textOrEmpty(title);
            accountPeriod = textOrEmpty(accountPeriod);
            scorecardPolicyId = textOrEmpty(scorecardPolicyId);
            requiredEvidence = copyOrEmpty(requiredEvidence);
            requiredSignatureRoles = copyOrEmpty(requiredSignatureRoles);
            assetPath = textOrEmpty(assetPath);
            itScript = textOrEmpty(itScript);
        }
    }

    public record RequiredEvidence(
            String id,
            String feature,
            String label,
            String command,
            String evidencePath) {
        public RequiredEvidence {
            id = textOrEmpty(id);
            feature = textOrEmpty(feature);
            label = textOrEmpty(label);
            command = textOrEmpty(command);
            evidencePath = textOrEmpty(evidencePath);
        }
    }

    public record RequiredSignatureRole(String role, String label) {
        public RequiredSignatureRole {
            role = textOrEmpty(role);
            label = textOrEmpty(label);
        }
    }
}
