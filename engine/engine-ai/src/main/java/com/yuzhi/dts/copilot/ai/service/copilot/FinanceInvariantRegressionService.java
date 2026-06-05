package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FinanceInvariantRegressionService {

    private static final Logger log = LoggerFactory.getLogger(FinanceInvariantRegressionService.class);
    private static final String REGRESSION_GRID_RESOURCE = "governance/finance-invariant-regression-grid.v1.json";

    private final ObjectMapper objectMapper;
    private final FinanceInvariantRegistry invariantRegistry;
    private List<RegressionCase> cases = List.of();

    public FinanceInvariantRegressionService(
            ObjectMapper objectMapper,
            FinanceInvariantRegistry invariantRegistry) {
        this.objectMapper = objectMapper;
        this.invariantRegistry = invariantRegistry;
    }

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(REGRESSION_GRID_RESOURCE)) {
            if (is == null) {
                log.warn("Finance invariant regression grid not found: {}", REGRESSION_GRID_RESOURCE);
                this.cases = List.of();
                return;
            }
            RegressionGridDocument document = objectMapper.readValue(is, RegressionGridDocument.class);
            this.cases = List.copyOf(document.cases());
            log.info("Loaded {} finance invariant regression case(s) from {}", cases.size(), REGRESSION_GRID_RESOURCE);
        } catch (Exception e) {
            log.warn("Failed to load finance invariant regression grid from {}: {}",
                    REGRESSION_GRID_RESOURCE,
                    e.getMessage());
            this.cases = List.of();
        }
    }

    public List<RegressionCase> cases() {
        return cases;
    }

    public RegressionResult runAll() {
        List<CaseResult> results = cases.stream().map(this::runCase).toList();
        List<CaseResult> failures = results.stream()
                .filter(result -> !result.allowed())
                .toList();
        return new RegressionResult(failures.isEmpty(), results, failures);
    }

    public CaseResult runCase(RegressionCase regressionCase) {
        Optional<FinanceInvariantRegistry.FinanceInvariant> invariant = findInvariant(regressionCase.invariantId());
        if (invariant.isEmpty()) {
            String reason = "Unknown finance invariant: " + regressionCase.invariantId();
            return new CaseResult(
                    regressionCase.id(),
                    regressionCase.invariantId(),
                    regressionCase.filter(),
                    false,
                    List.of(reason),
                    reproducibleFailure(regressionCase, List.of(reason)));
        }
        FinanceInvariantRegistry.InvariantValidation validation =
                invariantRegistry.validate(invariant.get(), regressionCase.observedData());
        List<String> reasons = validation.violations().stream()
                .map(FinanceInvariantRegistry.InvariantViolation::reason)
                .toList();
        return new CaseResult(
                regressionCase.id(),
                regressionCase.invariantId(),
                regressionCase.filter(),
                validation.allowed(),
                reasons,
                validation.allowed() ? "" : reproducibleFailure(regressionCase, reasons));
    }

    private Optional<FinanceInvariantRegistry.FinanceInvariant> findInvariant(String invariantId) {
        Map<String, FinanceInvariantRegistry.FinanceInvariant> byId = invariantRegistry.invariants().stream()
                .collect(Collectors.toMap(FinanceInvariantRegistry.FinanceInvariant::id, Function.identity()));
        return Optional.ofNullable(byId.get(invariantId));
    }

    private static String reproducibleFailure(RegressionCase regressionCase, List<String> reasons) {
        return "caseId=" + regressionCase.id()
                + ", invariantId=" + regressionCase.invariantId()
                + ", filter=" + regressionCase.filter()
                + ", reasons=" + reasons;
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static Map<String, String> copyMapOrEmpty(Map<String, String> values) {
        return values == null ? Map.of() : Map.copyOf(values);
    }

    private record RegressionGridDocument(String version, List<RegressionCase> cases) {
        private RegressionGridDocument {
            cases = copyOrEmpty(cases);
        }
    }

    public record RegressionCase(
            String id,
            String invariantId,
            Map<String, String> filter,
            JsonNode observedData,
            String source) {

        public RegressionCase {
            filter = copyMapOrEmpty(filter);
            observedData = observedData == null ? MissingNode.getInstance() : observedData;
            source = source == null ? "" : source;
        }
    }

    public record CaseResult(
            String caseId,
            String invariantId,
            Map<String, String> filter,
            boolean allowed,
            List<String> reasons,
            String reproducibleFailure) {

        public CaseResult {
            filter = copyMapOrEmpty(filter);
            reasons = copyOrEmpty(reasons);
            reproducibleFailure = reproducibleFailure == null ? "" : reproducibleFailure;
        }
    }

    public record RegressionResult(boolean allowed, List<CaseResult> caseResults, List<CaseResult> failures) {
        public RegressionResult {
            caseResults = copyOrEmpty(caseResults);
            failures = copyOrEmpty(failures);
        }
    }
}
