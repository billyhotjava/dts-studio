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
public class FinanceDifferentialGridService {

    private static final BigDecimal ZERO_CENTS = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public GridReport reconcile(GridSpec spec, List<GridRow> copilotRows, List<GridRow> oracleRows) {
        GridSpec safeSpec = spec == null ? new GridSpec("", "", "", List.of(), List.of()) : spec;
        List<GridRow> safeCopilotRows = copilotRows == null ? List.of() : copilotRows;
        List<GridRow> safeOracleRows = oracleRows == null ? List.of() : oracleRows;

        String contractFailure = firstContractMismatch(safeSpec, safeCopilotRows, "copilot");
        if (contractFailure.isEmpty()) {
            contractFailure = firstContractMismatch(safeSpec, safeOracleRows, "oracle");
        }
        if (!contractFailure.isEmpty()) {
            return new GridReport(false, List.of(), contractFailure);
        }

        String duplicateFailure = firstDuplicateFailure(safeSpec, safeCopilotRows, "copilot");
        if (duplicateFailure.isEmpty()) {
            duplicateFailure = firstDuplicateFailure(safeSpec, safeOracleRows, "oracle");
        }
        if (!duplicateFailure.isEmpty()) {
            return new GridReport(false, List.of(), duplicateFailure);
        }

        Map<GridKey, GridRow> copilotByKey = indexRows(safeSpec, safeCopilotRows);
        Map<GridKey, GridRow> oracleByKey = indexRows(safeSpec, safeOracleRows);
        TreeSet<GridKey> keys = new TreeSet<>();
        keys.addAll(copilotByKey.keySet());
        keys.addAll(oracleByKey.keySet());
        TreeSet<String> populatedSliceIds = keys.stream()
                .map(GridKey::sliceId)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        for (GridSlice slice : safeSpec.slices()) {
            if (!populatedSliceIds.contains(slice.id())) {
                keys.add(GridKey.emptySlice(slice.id()));
            }
        }

        List<GridDiff> diffs = new ArrayList<>();
        String failureMessage = "";
        for (GridKey key : keys) {
            GridRow copilotRow = copilotByKey.get(key);
            GridRow oracleRow = oracleByKey.get(key);
            GridDiff diff = diffFor(key, copilotRow, oracleRow);
            diffs.add(diff);
            if (!"matched".equals(diff.status()) && !"empty matched".equals(diff.status())) {
                failureMessage = firstFailure(failureMessage, safeSpec, diff);
            }
        }
        return new GridReport(failureMessage.isEmpty(), diffs, failureMessage);
    }

    private static GridDiff diffFor(GridKey key, GridRow copilotRow, GridRow oracleRow) {
        if (copilotRow == null && oracleRow == null) {
            return new GridDiff(key, "empty matched", ZERO_CENTS, ZERO_CENTS, ZERO_CENTS);
        }
        if (copilotRow == null) {
            BigDecimal oracleAmount = oracleRow.amount();
            return new GridDiff(key, "missing copilot cell", ZERO_CENTS, oracleAmount, oracleAmount);
        }
        if (oracleRow == null) {
            BigDecimal copilotAmount = copilotRow.amount();
            return new GridDiff(key, "missing oracle cell", copilotAmount, ZERO_CENTS, copilotAmount);
        }
        BigDecimal difference = cents(copilotRow.amount().subtract(oracleRow.amount())).abs();
        String status = difference.compareTo(ZERO_CENTS) == 0 ? "matched" : "amount mismatch";
        return new GridDiff(key, status, copilotRow.amount(), oracleRow.amount(), difference);
    }

    private static String firstContractMismatch(GridSpec spec, List<GridRow> rows, String source) {
        TreeSet<String> expectedSliceIds = spec.slices().stream()
                .map(GridSlice::id)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        for (GridRow row : rows) {
            if (!expectedSliceIds.contains(row.sliceId())) {
                return "Differential grid unknown grid slice: gridId=" + spec.gridId()
                        + ", source=" + source
                        + ", sliceId=" + row.sliceId();
            }
            if (!spec.chain().equals(row.chain())) {
                return "Differential grid chain mismatch: gridId=" + spec.gridId()
                        + ", source=" + source
                        + ", sliceId=" + row.sliceId()
                        + ", expected=" + spec.chain()
                        + ", actual=" + row.chain();
            }
            if (!spec.metricId().equals(row.metricId())) {
                return "Differential grid metric mismatch: gridId=" + spec.gridId()
                        + ", source=" + source
                        + ", sliceId=" + row.sliceId()
                        + ", expected=" + spec.metricId()
                        + ", actual=" + row.metricId();
            }
        }
        return "";
    }

    private static String firstDuplicateFailure(GridSpec spec, List<GridRow> rows, String source) {
        TreeSet<GridKey> seen = new TreeSet<>();
        for (GridRow row : rows) {
            GridKey key = GridKey.from(spec, row);
            if (!seen.add(key)) {
                return "Differential grid failed: gridId=" + spec.gridId()
                        + ", reason=duplicate " + source + " grid cell"
                        + ", sliceId=" + key.sliceId()
                        + ", dimensions=" + key.dimensionsText();
            }
        }
        return "";
    }

    private static Map<GridKey, GridRow> indexRows(GridSpec spec, List<GridRow> rows) {
        Map<GridKey, GridRow> indexed = new TreeMap<>();
        for (GridRow row : rows) {
            indexed.put(GridKey.from(spec, row), row);
        }
        return indexed;
    }

    private static String firstFailure(String currentFailure, GridSpec spec, GridDiff diff) {
        if (!currentFailure.isEmpty()) {
            return currentFailure;
        }
        return "Differential grid failed: gridId=" + spec.gridId()
                + ", chain=" + spec.chain()
                + ", metricId=" + spec.metricId()
                + ", sliceId=" + diff.key().sliceId()
                + ", dimensions=" + diff.key().dimensionsText()
                + ", status=" + diff.status()
                + ", copilot=" + amountText(diff.copilotAmount())
                + ", oracle=" + amountText(diff.oracleAmount())
                + ", difference=" + amountText(diff.difference());
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

    public record GridSpec(
            String gridId,
            String chain,
            String metricId,
            List<String> dimensionKeys,
            List<GridSlice> slices) {
        public GridSpec {
            gridId = textOrEmpty(gridId);
            chain = textOrEmpty(chain);
            metricId = textOrEmpty(metricId);
            dimensionKeys = copyOrEmpty(dimensionKeys);
            slices = copyOrEmpty(slices);
        }
    }

    public record GridSlice(String id, Map<String, String> filters, String boundary) {
        public GridSlice {
            id = textOrEmpty(id);
            filters = copyMapOrEmpty(filters);
            boundary = textOrEmpty(boundary);
        }
    }

    public record GridRow(
            String sliceId,
            String chain,
            String metricId,
            Map<String, String> dimensions,
            BigDecimal amount) {
        public GridRow {
            sliceId = textOrEmpty(sliceId);
            chain = textOrEmpty(chain);
            metricId = textOrEmpty(metricId);
            dimensions = copyMapOrEmpty(dimensions);
            amount = cents(amount);
        }
    }

    public record GridReport(boolean passed, List<GridDiff> diffs, String failureMessage) {
        public GridReport {
            diffs = diffs == null ? List.of() : List.copyOf(diffs);
            failureMessage = textOrEmpty(failureMessage);
        }
    }

    public record GridDiff(
            GridKey key,
            String status,
            BigDecimal copilotAmount,
            BigDecimal oracleAmount,
            BigDecimal difference) {
        public GridDiff {
            status = textOrEmpty(status);
            copilotAmount = cents(copilotAmount);
            oracleAmount = cents(oracleAmount);
            difference = cents(difference).abs();
        }
    }

    public record GridKey(String sliceId, Map<String, String> dimensions) implements Comparable<GridKey> {
        public GridKey {
            sliceId = textOrEmpty(sliceId);
            dimensions = copyMapOrEmpty(dimensions);
        }

        static GridKey from(GridSpec spec, GridRow row) {
            Map<String, String> selected = new LinkedHashMap<>();
            for (String dimensionKey : spec.dimensionKeys()) {
                selected.put(dimensionKey, row.dimensions().getOrDefault(dimensionKey, ""));
            }
            return new GridKey(row.sliceId(), selected);
        }

        static GridKey emptySlice(String sliceId) {
            return new GridKey(sliceId, Map.of());
        }

        String dimensionsText() {
            return dimensions.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .reduce((left, right) -> left + "," + right)
                    .orElse("");
        }

        @Override
        public int compareTo(GridKey other) {
            int sliceCompare = sliceId.compareTo(other.sliceId);
            if (sliceCompare != 0) {
                return sliceCompare;
            }
            return dimensionsText().compareTo(other.dimensionsText());
        }
    }
}
