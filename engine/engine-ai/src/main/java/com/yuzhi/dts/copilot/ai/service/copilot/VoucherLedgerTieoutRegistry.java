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
public class VoucherLedgerTieoutRegistry {

    private static final Logger log = LoggerFactory.getLogger(VoucherLedgerTieoutRegistry.class);
    private static final String REGISTRY_RESOURCE = "governance/voucher-ledger-tieout-mapping.v1.json";

    private final ObjectMapper objectMapper;
    private final FinanceOracleRegistry financeOracleRegistry;
    private Map<String, TieoutMapping> mappings = Map.of();

    public VoucherLedgerTieoutRegistry(ObjectMapper objectMapper, FinanceOracleRegistry financeOracleRegistry) {
        this.objectMapper = objectMapper;
        this.financeOracleRegistry = financeOracleRegistry;
    }

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(REGISTRY_RESOURCE)) {
            if (is == null) {
                log.warn("Voucher ledger tie-out mapping resource not found: {}", REGISTRY_RESOURCE);
                this.mappings = Map.of();
                return;
            }
            TieoutMappingDocument document = objectMapper.readValue(is, TieoutMappingDocument.class);
            Map<String, TieoutMapping> loaded = new LinkedHashMap<>();
            for (TieoutMapping mapping : document.mappings()) {
                assertAlignedWithOracleRegistry(mapping);
                loaded.put(mapping.id(), mapping);
            }
            this.mappings = java.util.Collections.unmodifiableMap(loaded);
            log.info("Loaded {} voucher ledger tie-out mapping(s) from {}", mappings.size(), REGISTRY_RESOURCE);
        } catch (Exception e) {
            log.warn("Failed to load voucher ledger tie-out mappings from {}: {}", REGISTRY_RESOURCE, e.getMessage());
            this.mappings = Map.of();
        }
    }

    public List<TieoutMapping> mappings() {
        return new ArrayList<>(mappings.values());
    }

    public Optional<TieoutMapping> mapping(String id) {
        return Optional.ofNullable(mappings.get(id));
    }

    private void assertAlignedWithOracleRegistry(TieoutMapping mapping) {
        FinanceOracleRegistry.OracleBinding binding = financeOracleRegistry.binding(mapping.oracleBindingId())
                .orElseGet(() -> {
                    financeOracleRegistry.init();
                    return financeOracleRegistry.binding(mapping.oracleBindingId()).orElseThrow(
                            () -> new IllegalStateException("Missing finance oracle binding: " + mapping.oracleBindingId()));
                });

        if (!mapping.sourceTables().containsAll(binding.sourceTables())) {
            throw new IllegalStateException("Voucher tie-out source tables are not aligned with oracle binding");
        }
        List<String> oracleEndpoints = binding.endpoints().stream()
                .map(FinanceOracleRegistry.OracleEndpoint::signature)
                .toList();
        if (!mapping.adminApiEndpoints().containsAll(oracleEndpoints)) {
            throw new IllegalStateException("Voucher tie-out endpoints are not aligned with oracle binding");
        }
        FinanceOracleRegistry.Ledger oracleLedger = binding.ledger();
        LedgerColumns columns = mapping.ledgerColumns();
        if (!columns.voucherTable().equals(oracleLedger.voucherTable())
                || !columns.itemTable().equals(oracleLedger.itemTable())
                || !columns.voucherPrimaryCodeColumn().equals(oracleLedger.voucherCodeColumn())
                || !columns.subjectColumn().equals(oracleLedger.subjectColumn())
                || !columns.debitColumn().equals(oracleLedger.debitColumn())
                || !columns.creditColumn().equals(oracleLedger.creditColumn())) {
            throw new IllegalStateException("Voucher tie-out ledger columns are not aligned with oracle binding");
        }
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private record TieoutMappingDocument(String version, List<TieoutMapping> mappings) {
        private TieoutMappingDocument {
            mappings = copyOrEmpty(mappings);
        }
    }

    public record TieoutMapping(
            String id,
            String oracleBindingId,
            String description,
            List<String> sourceTables,
            List<String> adminApiEndpoints,
            LedgerColumns ledgerColumns,
            List<String> joinRules,
            List<String> tieoutKeys,
            List<String> adminWebEvidence,
            String notes) {

        public TieoutMapping {
            id = textOrEmpty(id);
            oracleBindingId = textOrEmpty(oracleBindingId);
            description = textOrEmpty(description);
            sourceTables = copyOrEmpty(sourceTables);
            adminApiEndpoints = copyOrEmpty(adminApiEndpoints);
            ledgerColumns = ledgerColumns == null ? LedgerColumns.empty() : ledgerColumns;
            joinRules = copyOrEmpty(joinRules);
            tieoutKeys = copyOrEmpty(tieoutKeys);
            adminWebEvidence = copyOrEmpty(adminWebEvidence);
            notes = textOrEmpty(notes);
        }
    }

    public record LedgerColumns(
            String voucherTable,
            String itemTable,
            String businessCodeColumn,
            String businessTypeColumn,
            String periodColumn,
            String voucherPrimaryCodeColumn,
            String voucherCodeColumn,
            String subjectColumn,
            String debitColumn,
            String creditColumn,
            String statusColumn) {

        public LedgerColumns {
            voucherTable = textOrEmpty(voucherTable);
            itemTable = textOrEmpty(itemTable);
            businessCodeColumn = textOrEmpty(businessCodeColumn);
            businessTypeColumn = textOrEmpty(businessTypeColumn);
            periodColumn = textOrEmpty(periodColumn);
            voucherPrimaryCodeColumn = textOrEmpty(voucherPrimaryCodeColumn);
            voucherCodeColumn = textOrEmpty(voucherCodeColumn);
            subjectColumn = textOrEmpty(subjectColumn);
            debitColumn = textOrEmpty(debitColumn);
            creditColumn = textOrEmpty(creditColumn);
            statusColumn = textOrEmpty(statusColumn);
        }

        public static LedgerColumns empty() {
            return new LedgerColumns("", "", "", "", "", "", "", "", "", "", "");
        }
    }
}
