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
public class FinanceOracleRegistry {

    private static final Logger log = LoggerFactory.getLogger(FinanceOracleRegistry.class);
    private static final String REGISTRY_RESOURCE = "governance/finance-oracle-registry.v1.json";

    private final ObjectMapper objectMapper;
    private Map<String, OracleBinding> bindings = Map.of();

    public FinanceOracleRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(REGISTRY_RESOURCE)) {
            if (is == null) {
                log.warn("Finance oracle registry resource not found: {}", REGISTRY_RESOURCE);
                this.bindings = Map.of();
                return;
            }
            OracleRegistryDocument document = objectMapper.readValue(is, OracleRegistryDocument.class);
            Map<String, OracleBinding> loaded = new LinkedHashMap<>();
            for (OracleBinding binding : document.bindings()) {
                loaded.put(binding.id(), binding);
            }
            this.bindings = java.util.Collections.unmodifiableMap(loaded);
            log.info("Loaded {} finance oracle binding(s) from {}", bindings.size(), REGISTRY_RESOURCE);
        } catch (Exception e) {
            log.warn("Failed to load finance oracle registry from {}: {}", REGISTRY_RESOURCE, e.getMessage());
            this.bindings = Map.of();
        }
    }

    public List<OracleBinding> bindings() {
        return new ArrayList<>(bindings.values());
    }

    public Optional<OracleBinding> binding(String id) {
        return Optional.ofNullable(bindings.get(id));
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private record OracleRegistryDocument(String version, List<OracleBinding> bindings) {
        private OracleRegistryDocument {
            bindings = copyOrEmpty(bindings);
        }
    }

    public record OracleBinding(
            String id,
            String reportName,
            String oracleLevel,
            String chain,
            List<String> sourceTables,
            List<String> amountColumns,
            List<OracleEndpoint> endpoints,
            Ledger ledger,
            List<String> adminWebEvidence,
            String notes) {

        public OracleBinding {
            sourceTables = copyOrEmpty(sourceTables);
            amountColumns = copyOrEmpty(amountColumns);
            endpoints = copyOrEmpty(endpoints);
            ledger = ledger == null ? Ledger.empty() : ledger;
            adminWebEvidence = copyOrEmpty(adminWebEvidence);
            notes = notes == null ? "" : notes;
        }
    }

    public record OracleEndpoint(
            String level,
            String method,
            String path,
            String controller,
            String handler,
            List<String> requestParams) {

        public OracleEndpoint {
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
