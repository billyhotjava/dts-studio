package com.yuzhi.dts.copilot.ai.service.copilot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.stereotype.Service;

@Service
public class FinanceSummaryDualReconciliationService {

    private static final BigDecimal ZERO_CENTS = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public SummaryReconciliationReport reconcile(
            SummarySpec spec,
            List<SummaryRow> copilotRows,
            List<SummaryRow> oracleRows) {
        SummarySpec safeSpec = spec == null ? new SummarySpec("", "", "", List.of()) : spec;
        List<SummaryRow> safeCopilotRows = copilotRows == null ? List.of() : copilotRows;
        List<SummaryRow> safeOracleRows = oracleRows == null ? List.of() : oracleRows;

        String contractFailure = firstContractMismatch(safeSpec, safeCopilotRows, "copilot");
        if (contractFailure.isEmpty()) {
            contractFailure = firstContractMismatch(safeSpec, safeOracleRows, "oracle");
        }
        if (!contractFailure.isEmpty()) {
            return new SummaryReconciliationReport(false, List.of(), contractFailure);
        }

        String duplicateFailure = firstDuplicateFailure(safeSpec, safeCopilotRows, "copilot");
        if (duplicateFailure.isEmpty()) {
            duplicateFailure = firstDuplicateFailure(safeSpec, safeOracleRows, "oracle");
        }
        if (!duplicateFailure.isEmpty()) {
            return new SummaryReconciliationReport(false, List.of(), duplicateFailure);
        }

        Map<SummaryKey, SummaryRow> copilotByKey = indexRows(safeSpec, safeCopilotRows);
        Map<SummaryKey, SummaryRow> oracleByKey = indexRows(safeSpec, safeOracleRows);
        TreeSet<SummaryKey> keys = new TreeSet<>();
        keys.addAll(copilotByKey.keySet());
        keys.addAll(oracleByKey.keySet());

        List<SummaryDiff> diffs = new ArrayList<>();
        String failureMessage = "";
        for (SummaryKey key : keys) {
            SummaryRow copilotRow = copilotByKey.get(key);
            SummaryRow oracleRow = oracleByKey.get(key);
            if (copilotRow == null) {
                SummaryDiff diff = SummaryDiff.missingCopilot(key, oracleRow.amount());
                diffs.add(diff);
                failureMessage = firstFailure(failureMessage, safeSpec, diff);
                continue;
            }
            if (oracleRow == null) {
                SummaryDiff diff = SummaryDiff.missingOracle(key, copilotRow.amount());
                diffs.add(diff);
                failureMessage = firstFailure(failureMessage, safeSpec, diff);
                continue;
            }
            BigDecimal difference = cents(copilotRow.amount().subtract(oracleRow.amount())).abs();
            SummaryDiff diff = new SummaryDiff(key, "matched", copilotRow.amount(), oracleRow.amount(), difference);
            diffs.add(diff);
            if (difference.compareTo(ZERO_CENTS) != 0) {
                failureMessage = firstFailure(failureMessage, safeSpec, diff);
            }
        }

        return new SummaryReconciliationReport(failureMessage.isEmpty(), diffs, failureMessage);
    }

    private static String firstContractMismatch(SummarySpec spec, List<SummaryRow> rows, String source) {
        for (SummaryRow row : rows) {
            if (!spec.chain().equals(row.chain())) {
                return "Summary dual reconciliation chain mismatch: caseId=" + spec.caseId()
                        + ", source=" + source
                        + ", expected=" + spec.chain()
                        + ", actual=" + row.chain()
                        + ", dimensions=" + dimensionsText(spec, row.dimensions());
            }
            if (!spec.metricId().equals(row.metricId())) {
                return "Summary dual reconciliation metric mismatch: caseId=" + spec.caseId()
                        + ", source=" + source
                        + ", expected=" + spec.metricId()
                        + ", actual=" + row.metricId()
                        + ", dimensions=" + dimensionsText(spec, row.dimensions());
            }
        }
        return "";
    }

    private static String firstDuplicateFailure(SummarySpec spec, List<SummaryRow> rows, String source) {
        TreeSet<SummaryKey> seen = new TreeSet<>();
        for (SummaryRow row : rows) {
            SummaryKey key = SummaryKey.from(spec, row);
            if (!seen.add(key)) {
                return "Summary dual reconciliation failed: caseId=" + spec.caseId()
                        + ", metricId=" + spec.metricId()
                        + ", reason=duplicate " + source + " summary cell"
                        + ", dimensions=" + key.dimensionsText();
            }
        }
        return "";
    }

    private static Map<SummaryKey, SummaryRow> indexRows(SummarySpec spec, List<SummaryRow> rows) {
        Map<SummaryKey, SummaryRow> indexed = new TreeMap<>();
        for (SummaryRow row : rows) {
            indexed.put(SummaryKey.from(spec, row), row);
        }
        return indexed;
    }

    private static String firstFailure(String currentFailure, SummarySpec spec, SummaryDiff diff) {
        if (!currentFailure.isEmpty()) {
            return currentFailure;
        }
        return "Summary dual reconciliation failed: caseId=" + spec.caseId()
                + ", chain=" + spec.chain()
                + ", metricId=" + spec.metricId()
                + ", dimensions=" + diff.key().dimensionsText()
                + ", status=" + diff.status()
                + ", copilot=" + amountText(diff.copilotAmount())
                + ", oracle=" + amountText(diff.oracleAmount())
                + ", difference=" + amountText(diff.difference());
    }

    private static String dimensionsText(SummarySpec spec, Map<String, String> dimensions) {
        return SummaryKey.from(spec, new SummaryRow(spec.chain(), spec.metricId(), dimensions, ZERO_CENTS))
                .dimensionsText();
    }

    private static BigDecimal cents(BigDecimal amount) {
        if (amount == null) {
            return ZERO_CENTS;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static String amountText(BigDecimal amount) {
        return cents(amount).toPlainString();
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static Map<String, String> copyMapOrEmpty(Map<String, String> values) {
        if (values == null) {
            return Map.of();
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        values.forEach((key, value) -> normalized.put(textOrEmpty(key), textOrEmpty(value)));
        return Collections.unmodifiableMap(normalized);
    }

    public record SummarySpec(String caseId, String chain, String metricId, List<String> dimensionKeys) {
        public SummarySpec {
            caseId = textOrEmpty(caseId);
            chain = textOrEmpty(chain);
            metricId = textOrEmpty(metricId);
            dimensionKeys = copyOrEmpty(dimensionKeys);
        }
    }

    public record SummaryRow(String chain, String metricId, Map<String, String> dimensions, BigDecimal amount) {
        public SummaryRow {
            chain = textOrEmpty(chain);
            metricId = textOrEmpty(metricId);
            dimensions = copyMapOrEmpty(dimensions);
            amount = cents(amount);
        }
    }

    public record SummaryReconciliationReport(boolean passed, List<SummaryDiff> diffs, String failureMessage) {
        public SummaryReconciliationReport {
            diffs = diffs == null ? List.of() : List.copyOf(diffs);
            failureMessage = textOrEmpty(failureMessage);
        }
    }

    public record SummaryDiff(
            SummaryKey key,
            String status,
            BigDecimal copilotAmount,
            BigDecimal oracleAmount,
            BigDecimal difference) {

        public SummaryDiff {
            status = textOrEmpty(status);
            copilotAmount = cents(copilotAmount);
            oracleAmount = cents(oracleAmount);
            difference = cents(difference).abs();
        }

        static SummaryDiff missingCopilot(SummaryKey key, BigDecimal oracleAmount) {
            return new SummaryDiff(key, "missing copilot cell", ZERO_CENTS, oracleAmount, cents(oracleAmount));
        }

        static SummaryDiff missingOracle(SummaryKey key, BigDecimal copilotAmount) {
            return new SummaryDiff(key, "missing oracle cell", copilotAmount, ZERO_CENTS, cents(copilotAmount));
        }
    }

    public record SummaryKey(String metricId, Map<String, String> dimensions) implements Comparable<SummaryKey> {
        public SummaryKey {
            metricId = textOrEmpty(metricId);
            dimensions = copyMapOrEmpty(dimensions);
        }

        static SummaryKey from(SummarySpec spec, SummaryRow row) {
            Map<String, String> selected = new LinkedHashMap<>();
            for (String dimensionKey : spec.dimensionKeys()) {
                selected.put(dimensionKey, row.dimensions().getOrDefault(dimensionKey, ""));
            }
            return new SummaryKey(spec.metricId(), selected);
        }

        String dimensionsText() {
            return dimensions.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .reduce((left, right) -> left + "," + right)
                    .orElse("");
        }

        @Override
        public int compareTo(SummaryKey other) {
            int metricCompare = metricId.compareTo(other.metricId);
            if (metricCompare != 0) {
                return metricCompare;
            }
            return dimensionsText().compareTo(other.dimensionsText());
        }
    }
}
