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
public class FinanceVoucherSubjectTieoutRegistry {

    private static final Logger log = LoggerFactory.getLogger(FinanceVoucherSubjectTieoutRegistry.class);
    private static final String REGISTRY_RESOURCE = "governance/finance-voucher-subject-tieout.v1.json";

    private final ObjectMapper objectMapper;
    private final FinanceOracleRegistry financeOracleRegistry;
    private Map<String, SubjectTieoutMapping> mappings = Map.of();

    public FinanceVoucherSubjectTieoutRegistry(
            ObjectMapper objectMapper,
            FinanceOracleRegistry financeOracleRegistry) {
        this.objectMapper = objectMapper;
        this.financeOracleRegistry = financeOracleRegistry;
    }

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(REGISTRY_RESOURCE)) {
            if (is == null) {
                log.warn("Finance voucher subject tie-out resource not found: {}", REGISTRY_RESOURCE);
                this.mappings = Map.of();
                return;
            }
            TieoutDocument document = objectMapper.readValue(is, TieoutDocument.class);
            Map<String, SubjectTieoutMapping> loaded = new LinkedHashMap<>();
            for (SubjectTieoutMapping mapping : document.mappings()) {
                assertAlignedWithOracleRegistry(mapping);
                loaded.put(mapping.id(), mapping);
            }
            this.mappings = java.util.Collections.unmodifiableMap(loaded);
            log.info("Loaded {} finance voucher subject tie-out mapping(s) from {}", mappings.size(), REGISTRY_RESOURCE);
        } catch (Exception e) {
            log.warn("Failed to load finance voucher subject tie-out mappings from {}: {}", REGISTRY_RESOURCE, e.getMessage());
            this.mappings = Map.of();
        }
    }

    public List<SubjectTieoutMapping> mappings() {
        return new ArrayList<>(mappings.values());
    }

    public Optional<SubjectTieoutMapping> mapping(String id) {
        return Optional.ofNullable(mappings.get(id));
    }

    private void assertAlignedWithOracleRegistry(SubjectTieoutMapping mapping) {
        FinanceOracleRegistry.OracleBinding binding = financeOracleRegistry.binding(mapping.oracleBindingId())
                .orElseGet(() -> {
                    financeOracleRegistry.init();
                    return financeOracleRegistry.binding(mapping.oracleBindingId()).orElseThrow(
                            () -> new IllegalStateException("Missing finance oracle binding: " + mapping.oracleBindingId()));
                });
        if (!"voucher-ledger".equals(binding.chain())) {
            throw new IllegalStateException("Finance voucher subject tie-out must bind to voucher-ledger oracle: "
                    + mapping.id());
        }
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private record TieoutDocument(String version, List<SubjectTieoutMapping> mappings) {
        private TieoutDocument {
            mappings = copyOrEmpty(mappings);
        }
    }

    public record SubjectTieoutMapping(
            String id,
            String oracleBindingId,
            String chain,
            String metricId,
            String metricName,
            Integer voucherBusinessType,
            String voucherSide,
            String subjectGroup,
            List<Long> subjectIds,
            List<String> dimensionKeys,
            String adminApiEndpoint,
            List<String> adminWebEvidence,
            String notes) {

        public SubjectTieoutMapping {
            id = textOrEmpty(id);
            oracleBindingId = textOrEmpty(oracleBindingId);
            chain = textOrEmpty(chain);
            metricId = textOrEmpty(metricId);
            metricName = textOrEmpty(metricName);
            voucherBusinessType = voucherBusinessType == null ? 0 : voucherBusinessType;
            voucherSide = textOrEmpty(voucherSide);
            subjectGroup = textOrEmpty(subjectGroup);
            subjectIds = subjectIds == null ? List.of() : List.copyOf(subjectIds);
            dimensionKeys = copyOrEmpty(dimensionKeys);
            adminApiEndpoint = textOrEmpty(adminApiEndpoint);
            adminWebEvidence = copyOrEmpty(adminWebEvidence);
            notes = textOrEmpty(notes);
        }

        public FinanceVoucherSubjectTieoutService.SubjectTieoutSpec tieoutSpec() {
            return new FinanceVoucherSubjectTieoutService.SubjectTieoutSpec(
                    id,
                    chain,
                    metricId,
                    voucherBusinessType,
                    voucherSide,
                    subjectIds,
                    dimensionKeys);
        }
    }
}
