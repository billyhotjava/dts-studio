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
public class FinanceSummaryDualReconciliationRegistry {

    private static final Logger log = LoggerFactory.getLogger(FinanceSummaryDualReconciliationRegistry.class);
    private static final String REGISTRY_RESOURCE = "governance/finance-summary-dual-reconciliation-cases.v1.json";

    private final ObjectMapper objectMapper;
    private final FinanceOracleRegistry financeOracleRegistry;
    private Map<String, SummaryCase> cases = Map.of();

    public FinanceSummaryDualReconciliationRegistry(
            ObjectMapper objectMapper,
            FinanceOracleRegistry financeOracleRegistry) {
        this.objectMapper = objectMapper;
        this.financeOracleRegistry = financeOracleRegistry;
    }

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(REGISTRY_RESOURCE)) {
            if (is == null) {
                log.warn("Finance summary dual reconciliation resource not found: {}", REGISTRY_RESOURCE);
                this.cases = Map.of();
                return;
            }
            SummaryCaseDocument document = objectMapper.readValue(is, SummaryCaseDocument.class);
            Map<String, SummaryCase> loaded = new LinkedHashMap<>();
            for (SummaryCase summaryCase : document.cases()) {
                assertAlignedWithOracleRegistry(summaryCase);
                loaded.put(summaryCase.id(), summaryCase);
            }
            this.cases = java.util.Collections.unmodifiableMap(loaded);
            log.info("Loaded {} finance summary dual reconciliation case(s) from {}", cases.size(), REGISTRY_RESOURCE);
        } catch (Exception e) {
            log.warn("Failed to load finance summary dual reconciliation cases from {}: {}", REGISTRY_RESOURCE, e.getMessage());
            this.cases = Map.of();
        }
    }

    public List<SummaryCase> cases() {
        return new ArrayList<>(cases.values());
    }

    public Optional<SummaryCase> caseById(String id) {
        return Optional.ofNullable(cases.get(id));
    }

    private void assertAlignedWithOracleRegistry(SummaryCase summaryCase) {
        FinanceOracleRegistry.OracleBinding binding = financeOracleRegistry.binding(summaryCase.oracleBindingId())
                .orElseGet(() -> {
                    financeOracleRegistry.init();
                    return financeOracleRegistry.binding(summaryCase.oracleBindingId()).orElseThrow(
                            () -> new IllegalStateException("Missing finance oracle binding: " + summaryCase.oracleBindingId()));
                });
        if (!binding.chain().equals(summaryCase.chain())) {
            throw new IllegalStateException("Summary dual reconciliation case chain is not aligned with oracle binding: "
                    + summaryCase.id());
        }
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private record SummaryCaseDocument(String version, List<SummaryCase> cases) {
        private SummaryCaseDocument {
            cases = copyOrEmpty(cases);
        }
    }

    public record SummaryCase(
            String id,
            String oracleBindingId,
            String chain,
            String metricId,
            String metricName,
            String amountField,
            List<String> dimensionKeys,
            String copilotQuestion,
            SummaryQuery copilotQuery,
            SummaryQuery oracleQuery,
            String notes) {

        public SummaryCase {
            id = textOrEmpty(id);
            oracleBindingId = textOrEmpty(oracleBindingId);
            chain = textOrEmpty(chain);
            metricId = textOrEmpty(metricId);
            metricName = textOrEmpty(metricName);
            amountField = textOrEmpty(amountField);
            dimensionKeys = copyOrEmpty(dimensionKeys);
            copilotQuestion = textOrEmpty(copilotQuestion);
            copilotQuery = copilotQuery == null ? SummaryQuery.empty() : copilotQuery;
            oracleQuery = oracleQuery == null ? SummaryQuery.empty() : oracleQuery;
            notes = textOrEmpty(notes);
        }

        public FinanceSummaryDualReconciliationService.SummarySpec reconciliationSpec() {
            return new FinanceSummaryDualReconciliationService.SummarySpec(id, chain, metricId, dimensionKeys);
        }
    }

    public record SummaryQuery(String kind, String database, String nativeSql, String endpoint) {
        public SummaryQuery {
            kind = textOrEmpty(kind);
            database = textOrEmpty(database);
            nativeSql = textOrEmpty(nativeSql);
            endpoint = textOrEmpty(endpoint);
        }

        public static SummaryQuery empty() {
            return new SummaryQuery("", "", "", "");
        }
    }
}
