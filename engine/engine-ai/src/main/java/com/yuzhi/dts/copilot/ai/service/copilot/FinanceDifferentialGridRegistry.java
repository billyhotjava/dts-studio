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
public class FinanceDifferentialGridRegistry {

    private static final Logger log = LoggerFactory.getLogger(FinanceDifferentialGridRegistry.class);
    private static final String REGISTRY_RESOURCE = "governance/finance-differential-grid-cases.v1.json";

    private final ObjectMapper objectMapper;
    private final FinanceSummaryDualReconciliationRegistry summaryRegistry;
    private Map<String, DifferentialGridCase> cases = Map.of();

    public FinanceDifferentialGridRegistry(
            ObjectMapper objectMapper,
            FinanceSummaryDualReconciliationRegistry summaryRegistry) {
        this.objectMapper = objectMapper;
        this.summaryRegistry = summaryRegistry;
    }

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(REGISTRY_RESOURCE)) {
            if (is == null) {
                log.warn("Finance differential grid resource not found: {}", REGISTRY_RESOURCE);
                this.cases = Map.of();
                return;
            }
            DifferentialGridDocument document = objectMapper.readValue(is, DifferentialGridDocument.class);
            Map<String, DifferentialGridCase> loaded = new LinkedHashMap<>();
            for (DifferentialGridCase gridCase : document.cases()) {
                assertAlignedWithSummaryCase(gridCase);
                loaded.put(gridCase.id(), gridCase);
            }
            this.cases = java.util.Collections.unmodifiableMap(loaded);
            log.info("Loaded {} finance differential grid case(s) from {}", cases.size(), REGISTRY_RESOURCE);
        } catch (Exception e) {
            log.warn("Failed to load finance differential grid cases from {}: {}", REGISTRY_RESOURCE, e.getMessage());
            this.cases = Map.of();
        }
    }

    public List<DifferentialGridCase> cases() {
        return new ArrayList<>(cases.values());
    }

    public Optional<DifferentialGridCase> caseById(String id) {
        return Optional.ofNullable(cases.get(id));
    }

    private void assertAlignedWithSummaryCase(DifferentialGridCase gridCase) {
        FinanceSummaryDualReconciliationRegistry.SummaryCase summaryCase = summaryRegistry.caseById(gridCase.summaryCaseId())
                .orElseGet(() -> {
                    summaryRegistry.init();
                    return summaryRegistry.caseById(gridCase.summaryCaseId()).orElseThrow(
                            () -> new IllegalStateException("Missing finance summary case: " + gridCase.summaryCaseId()));
                });
        if (!summaryCase.chain().equals(gridCase.chain()) || !summaryCase.metricId().equals(gridCase.metricId())) {
            throw new IllegalStateException("Differential grid case is not aligned with summary case: " + gridCase.id());
        }
        if (!summaryCase.dimensionKeys().containsAll(gridCase.dimensionKeys())) {
            throw new IllegalStateException("Differential grid dimensions are not aligned with summary case: " + gridCase.id());
        }
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static Map<String, String> copyMapOrEmpty(Map<String, String> values) {
        if (values == null) {
            return Map.of();
        }
        return java.util.Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    private record DifferentialGridDocument(String version, List<DifferentialGridCase> cases) {
        private DifferentialGridDocument {
            cases = copyOrEmpty(cases);
        }
    }

    public record DifferentialGridCase(
            String id,
            String summaryCaseId,
            String oracleBindingId,
            String chain,
            String metricId,
            String metricName,
            List<String> dimensionKeys,
            List<GridSlice> slices,
            String copilotQuestion,
            String oracleEndpoint,
            String notes) {
        public DifferentialGridCase {
            id = textOrEmpty(id);
            summaryCaseId = textOrEmpty(summaryCaseId);
            oracleBindingId = textOrEmpty(oracleBindingId);
            chain = textOrEmpty(chain);
            metricId = textOrEmpty(metricId);
            metricName = textOrEmpty(metricName);
            dimensionKeys = copyOrEmpty(dimensionKeys);
            slices = copyOrEmpty(slices);
            copilotQuestion = textOrEmpty(copilotQuestion);
            oracleEndpoint = textOrEmpty(oracleEndpoint);
            notes = textOrEmpty(notes);
        }

        public FinanceDifferentialGridService.GridSpec gridSpec() {
            return new FinanceDifferentialGridService.GridSpec(
                    id,
                    chain,
                    metricId,
                    dimensionKeys,
                    slices.stream()
                            .map(slice -> new FinanceDifferentialGridService.GridSlice(
                                    slice.id(),
                                    slice.filters(),
                                    slice.boundary()))
                            .toList());
        }
    }

    public record GridSlice(String id, Map<String, String> filters, String boundary, String notes) {
        public GridSlice {
            id = textOrEmpty(id);
            filters = copyMapOrEmpty(filters);
            boundary = textOrEmpty(boundary);
            notes = textOrEmpty(notes);
        }
    }
}
