package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FinanceInvariantRegistry {

    private static final Logger log = LoggerFactory.getLogger(FinanceInvariantRegistry.class);
    private static final String INVARIANT_RESOURCE = "governance/finance-invariants.v1.json";

    private final ObjectMapper objectMapper;
    private final CaliberRuleRegistry caliberRuleRegistry;
    private List<FinanceInvariant> invariants = List.of();

    public FinanceInvariantRegistry(ObjectMapper objectMapper, CaliberRuleRegistry caliberRuleRegistry) {
        this.objectMapper = objectMapper;
        this.caliberRuleRegistry = caliberRuleRegistry;
    }

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(INVARIANT_RESOURCE)) {
            if (is == null) {
                log.warn("Finance invariant resource not found: {}", INVARIANT_RESOURCE);
                this.invariants = List.of();
                return;
            }
            InvariantDocument document = objectMapper.readValue(is, InvariantDocument.class);
            this.invariants = List.copyOf(document.invariants());
            log.info("Loaded {} finance invariant(s) from {}", invariants.size(), INVARIANT_RESOURCE);
        } catch (Exception e) {
            log.warn("Failed to load finance invariants from {}: {}", INVARIANT_RESOURCE, e.getMessage());
            this.invariants = List.of();
        }
    }

    public List<FinanceInvariant> invariants() {
        return invariants;
    }

    public InvariantValidation validateExample(FinanceInvariant invariant, boolean positive) {
        JsonNode example = positive ? invariant.positiveExample() : invariant.negativeExample();
        return validate(invariant, example);
    }

    public InvariantValidation validate(FinanceInvariant invariant, JsonNode example) {
        if (invariant == null) {
            return new InvariantValidation(false, List.of(new InvariantViolation("", "invariant is required")));
        }
        JsonNode safeExample = example == null ? MissingNode.getInstance() : example;
        List<InvariantViolation> violations = switch (invariant.check().type()) {
            case "VOUCHER_BALANCE" -> validateVoucherBalance(invariant, safeExample);
            case "AMOUNT_TIER_ORDER" -> validateAmountTierOrder(invariant, safeExample);
            case "PAYMENT_NOT_EXCEED_DISCOUNTED" -> validatePaymentNotExceedDiscounted(invariant, safeExample);
            case "ADDITIVITY" -> validateAdditivity(invariant, safeExample);
            case "MONOTONICITY" -> validateMonotonicity(invariant, safeExample);
            case "BAD_DEBT_EXCLUDED_FROM_INCOME" -> validateBadDebtExcludedFromIncome(invariant, safeExample);
            case "SQL_STATIC" -> validateSqlStatic(invariant, safeExample);
            default -> List.of(new InvariantViolation(invariant.id(), "Unsupported invariant check type: "
                    + invariant.check().type()));
        };
        return new InvariantValidation(violations.isEmpty(), violations);
    }

    private List<InvariantViolation> validateVoucherBalance(FinanceInvariant invariant, JsonNode example) {
        BigDecimal debit = sumRows(example, "debit_amount");
        BigDecimal credit = sumRows(example, "credit_amount");
        if (debit.compareTo(credit) == 0) {
            return List.of();
        }
        return List.of(new InvariantViolation(invariant.id(), "debit_amount sum must equal credit_amount sum"));
    }

    private List<InvariantViolation> validateAmountTierOrder(FinanceInvariant invariant, JsonNode example) {
        List<InvariantViolation> violations = new ArrayList<>();
        for (JsonNode row : rows(example)) {
            BigDecimal nominal = decimal(row, "nominal_amount");
            BigDecimal receivableBeforeDiscount = decimal(row, "receivable_before_discount");
            BigDecimal discountedReceivable = decimal(row, "discounted_receivable");
            if (nominal.compareTo(receivableBeforeDiscount) < 0
                    || receivableBeforeDiscount.compareTo(discountedReceivable) < 0
                    || discountedReceivable.compareTo(BigDecimal.ZERO) < 0) {
                violations.add(new InvariantViolation(
                        invariant.id(),
                        "nominal_amount >= receivable_before_discount >= discounted_receivable >= 0 is required"));
            }
        }
        return List.copyOf(violations);
    }

    private List<InvariantViolation> validatePaymentNotExceedDiscounted(FinanceInvariant invariant, JsonNode example) {
        List<InvariantViolation> violations = new ArrayList<>();
        for (JsonNode row : rows(example)) {
            if (decimal(row, "paid_amount").compareTo(decimal(row, "discounted_receivable")) > 0) {
                violations.add(new InvariantViolation(
                        invariant.id(),
                        "paid_amount must not exceed discounted_receivable"));
            }
        }
        return List.copyOf(violations);
    }

    private List<InvariantViolation> validateAdditivity(FinanceInvariant invariant, JsonNode example) {
        BigDecimal total = decimal(example.path("total"), "amount");
        BigDecimal partitions = BigDecimal.ZERO;
        for (JsonNode part : array(example.path("partitions"))) {
            partitions = partitions.add(decimal(part, "amount"));
        }
        if (total.compareTo(partitions) == 0) {
            return List.of();
        }
        return List.of(new InvariantViolation(invariant.id(), "total amount must equal partition sum"));
    }

    private List<InvariantViolation> validateMonotonicity(FinanceInvariant invariant, JsonNode example) {
        JsonNode base = example.path("base");
        JsonNode subset = example.path("subset");
        boolean rowCountOk = decimal(subset, "row_count").compareTo(decimal(base, "row_count")) <= 0;
        boolean amountOk = decimal(subset, "amount").compareTo(decimal(base, "amount")) <= 0;
        if (rowCountOk && amountOk) {
            return List.of();
        }
        return List.of(new InvariantViolation(invariant.id(), "subset row_count and amount must not exceed base"));
    }

    private List<InvariantViolation> validateBadDebtExcludedFromIncome(FinanceInvariant invariant, JsonNode example) {
        List<InvariantViolation> violations = new ArrayList<>();
        for (JsonNode row : rows(example)) {
            if (row.path("biz_type").asInt(-1) == 6
                    && decimal(row, "income_amount").compareTo(BigDecimal.ZERO) > 0) {
                violations.add(new InvariantViolation(
                        invariant.id(),
                        "biz_type=6 bad debt must not contribute to income_amount"));
            }
        }
        return List.copyOf(violations);
    }

    private List<InvariantViolation> validateSqlStatic(FinanceInvariant invariant, JsonNode example) {
        String sql = example.path("sql").asText("");
        CaliberRuleRegistry.CaliberValidation validation = caliberRuleRegistry.validateSql("finance", sql);
        if (validation.allowed()) {
            return List.of();
        }
        List<InvariantViolation> violations = new ArrayList<>();
        for (CaliberRuleRegistry.CaliberViolation violation : validation.violations()) {
            if (invariant.sourceRuleIds().isEmpty() || invariant.sourceRuleIds().contains(violation.ruleId())) {
                violations.add(new InvariantViolation(invariant.id(), violation.ruleId() + ": " + violation.reason()));
            }
        }
        return List.copyOf(violations);
    }

    private static BigDecimal sumRows(JsonNode example, String field) {
        BigDecimal sum = BigDecimal.ZERO;
        for (JsonNode row : rows(example)) {
            sum = sum.add(decimal(row, field));
        }
        return sum;
    }

    private static Iterable<JsonNode> rows(JsonNode example) {
        return array(example.path("rows"));
    }

    private static Iterable<JsonNode> array(JsonNode node) {
        return node != null && node.isArray() ? node : List.of();
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node == null ? MissingNode.getInstance() : node.path(field);
        if (value.isNumber()) {
            return value.decimalValue();
        }
        if (value.isTextual() && !value.asText().isBlank()) {
            return new BigDecimal(value.asText().trim());
        }
        return BigDecimal.ZERO;
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private record InvariantDocument(String version, List<FinanceInvariant> invariants) {
        private InvariantDocument {
            invariants = copyOrEmpty(invariants);
        }
    }

    public record FinanceInvariant(
            String id,
            String statement,
            List<String> appliesTo,
            String severity,
            InvariantCheck check,
            JsonNode positiveExample,
            JsonNode negativeExample,
            List<String> sourceRuleIds,
            List<String> sourceRefs) {

        public FinanceInvariant {
            appliesTo = copyOrEmpty(appliesTo);
            severity = severity == null || severity.isBlank() ? "error" : severity;
            check = check == null ? new InvariantCheck("", "", List.of(), Map.of()) : check;
            positiveExample = positiveExample == null ? MissingNode.getInstance() : positiveExample;
            negativeExample = negativeExample == null ? MissingNode.getInstance() : negativeExample;
            sourceRuleIds = copyOrEmpty(sourceRuleIds);
            sourceRefs = copyOrEmpty(sourceRefs);
        }
    }

    public record InvariantCheck(
            String type,
            String expression,
            List<String> resultColumns,
            Map<String, String> parameters) {

        public InvariantCheck {
            type = type == null ? "" : type;
            expression = expression == null ? "" : expression;
            resultColumns = copyOrEmpty(resultColumns);
            parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        }
    }

    public record InvariantViolation(String invariantId, String reason) {
    }

    public record InvariantValidation(boolean allowed, List<InvariantViolation> violations) {
        public InvariantValidation {
            violations = copyOrEmpty(violations);
        }
    }
}
