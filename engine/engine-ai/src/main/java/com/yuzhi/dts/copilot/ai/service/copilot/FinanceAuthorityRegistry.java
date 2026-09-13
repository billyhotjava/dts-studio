package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.annotation.JsonAlias;
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
public class FinanceAuthorityRegistry {

    private static final Logger log = LoggerFactory.getLogger(FinanceAuthorityRegistry.class);
    private static final String REGISTRY_RESOURCE = "governance/finance-authority-registry.v1.json";
    private static final String LEGACY_REGISTRY_RESOURCE = "governance/finance-oracle-registry.v1.json";

    private final ObjectMapper objectMapper;
    private Map<String, AuthorityBinding> bindings = Map.of();

    public FinanceAuthorityRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        String resource = REGISTRY_RESOURCE;
        InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
        if (is == null) {
            resource = LEGACY_REGISTRY_RESOURCE;
            is = getClass().getClassLoader().getResourceAsStream(resource);
        }
        if (is == null) {
            log.warn("Finance authority registry resource not found: {}", REGISTRY_RESOURCE);
            this.bindings = Map.of();
            return;
        }

        try (InputStream closeable = is) {
            AuthorityRegistryDocument document = objectMapper.readValue(closeable, AuthorityRegistryDocument.class);
            Map<String, AuthorityBinding> loaded = new LinkedHashMap<>();
            for (AuthorityBinding binding : document.bindings()) {
                loaded.put(binding.id(), binding);
            }
            this.bindings = java.util.Collections.unmodifiableMap(loaded);
            log.info("Loaded {} finance authority binding(s) from {}", bindings.size(), resource);
        } catch (Exception e) {
            log.warn("Failed to load finance authority registry from {}: {}", resource, e.getMessage());
            this.bindings = Map.of();
        }
    }

    public List<AuthorityBinding> bindings() {
        return new ArrayList<>(bindings.values());
    }

    public Optional<AuthorityBinding> binding(String id) {
        return Optional.ofNullable(bindings.get(id));
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private record AuthorityRegistryDocument(String version, List<AuthorityBinding> bindings) {
        private AuthorityRegistryDocument {
            bindings = copyOrEmpty(bindings);
        }
    }

    public record AuthorityBinding(
            String id,
            String reportName,
            @JsonAlias("oracleLevel")
            String authorityLevel,
            String chain,
            List<String> sourceTables,
            List<String> amountColumns,
            List<AuthorityEndpoint> endpoints,
            Ledger ledger,
            List<String> adminWebEvidence,
            String notes) {

        public AuthorityBinding {
            sourceTables = copyOrEmpty(sourceTables);
            amountColumns = copyOrEmpty(amountColumns);
            endpoints = copyOrEmpty(endpoints);
            ledger = ledger == null ? Ledger.empty() : ledger;
            adminWebEvidence = copyOrEmpty(adminWebEvidence);
            notes = notes == null ? "" : notes;
        }

        public String oracleLevel() {
            return authorityLevel;
        }
    }

    public record AuthorityEndpoint(
            String level,
            String method,
            String path,
            String controller,
            String handler,
            List<String> requestParams) {

        public AuthorityEndpoint {
            requestParams = copyOrEmpty(requestParams);
        }

        public String signature() {
            return method + " " + path;
        }
    }

    public record Ledger(
            String voucherTable,
            String itemTable,
            String debitColumn,
            String creditColumn,
            String subjectColumn,
            String voucherCodeColumn) {

        public static Ledger empty() {
            return new Ledger("", "", "", "", "", "");
        }
    }
}
