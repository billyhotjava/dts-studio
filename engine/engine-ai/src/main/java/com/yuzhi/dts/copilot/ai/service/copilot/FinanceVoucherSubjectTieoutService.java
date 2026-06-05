package com.yuzhi.dts.copilot.ai.service.copilot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class FinanceVoucherSubjectTieoutService {

    private static final BigDecimal ZERO_CENTS = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public SubjectTieoutReport tieOut(
            SubjectTieoutSpec spec,
            List<SummaryMetricRow> summaryRows,
            List<VoucherLedgerTieoutService.VoucherLedgerRow> voucherRows) {
        SubjectTieoutSpec safeSpec = spec == null
                ? new SubjectTieoutSpec("", "", "", 0, "debit", List.of(), List.of())
                : spec;
        List<SummaryMetricRow> safeSummaryRows = summaryRows == null ? List.of() : summaryRows;
        List<VoucherLedgerTieoutService.VoucherLedgerRow> safeVoucherRows = voucherRows == null ? List.of() : voucherRows;

        String contractFailure = firstContractMismatch(safeSpec, safeSummaryRows);
        if (!contractFailure.isEmpty()) {
            return new SubjectTieoutReport(false, List.of(), contractFailure);
        }

        String duplicateFailure = firstDuplicateFailure(safeSpec, safeSummaryRows);
        if (!duplicateFailure.isEmpty()) {
            return new SubjectTieoutReport(false, List.of(), duplicateFailure);
        }

        Map<TieoutKey, SummaryMetricRow> summaryByKey = indexSummaryRows(safeSpec, safeSummaryRows);
        VoucherIndex voucherIndex = indexVoucherRows(safeSpec, safeVoucherRows);
        TreeSet<TieoutKey> keys = new TreeSet<>();
        keys.addAll(summaryByKey.keySet());
        keys.addAll(voucherIndex.mappedAmounts().keySet());

        List<SubjectTieoutDiff> diffs = new ArrayList<>();
        String failureMessage = "";
        for (TieoutKey key : keys) {
            SummaryMetricRow summaryRow = summaryByKey.get(key);
            BigDecimal summaryAmount = summaryRow == null ? ZERO_CENTS : summaryRow.amount();
            BigDecimal voucherAmount = voucherIndex.mappedAmounts().getOrDefault(key, ZERO_CENTS);
            BigDecimal ignoredVoucherAmount = voucherIndex.ignoredAmounts().getOrDefault(key, ZERO_CENTS);
            String status = status(summaryRow, voucherIndex.mappedAmounts().containsKey(key), summaryAmount, voucherAmount);
            BigDecimal difference = cents(summaryAmount.subtract(voucherAmount)).abs();
            SubjectTieoutDiff diff = new SubjectTieoutDiff(
                    key,
                    status,
                    summaryAmount,
                    voucherAmount,
                    ignoredVoucherAmount,
                    difference);
            diffs.add(diff);
            if (!"matched".equals(status)) {
                failureMessage = firstFailure(failureMessage, safeSpec, diff);
            }
        }

        return new SubjectTieoutReport(failureMessage.isEmpty(), diffs, failureMessage);
    }

    private static String firstContractMismatch(SubjectTieoutSpec spec, List<SummaryMetricRow> rows) {
        for (SummaryMetricRow row : rows) {
            if (!spec.chain().equals(row.chain())) {
                return "Voucher subject tie-out chain mismatch: mappingId=" + spec.mappingId()
                        + ", expected=" + spec.chain()
                        + ", actual=" + row.chain()
                        + ", dimensions=" + dimensionsText(spec, row.dimensions());
            }
            if (!spec.metricId().equals(row.metricId())) {
                return "Voucher subject tie-out metric mismatch: mappingId=" + spec.mappingId()
                        + ", expected=" + spec.metricId()
                        + ", actual=" + row.metricId()
                        + ", dimensions=" + dimensionsText(spec, row.dimensions());
            }
        }
        return "";
    }

    private static String firstDuplicateFailure(SubjectTieoutSpec spec, List<SummaryMetricRow> rows) {
        TreeSet<TieoutKey> seen = new TreeSet<>();
        for (SummaryMetricRow row : rows) {
            TieoutKey key = TieoutKey.from(spec, row.dimensions());
            if (!seen.add(key)) {
                return "Voucher subject tie-out failed: mappingId=" + spec.mappingId()
                        + ", reason=duplicate summary metric cell"
                        + ", dimensions=" + key.dimensionsText();
            }
        }
        return "";
    }

    private static Map<TieoutKey, SummaryMetricRow> indexSummaryRows(SubjectTieoutSpec spec, List<SummaryMetricRow> rows) {
        Map<TieoutKey, SummaryMetricRow> indexed = new TreeMap<>();
        for (SummaryMetricRow row : rows) {
            indexed.put(TieoutKey.from(spec, row.dimensions()), row);
        }
        return indexed;
    }

    private static VoucherIndex indexVoucherRows(
            SubjectTieoutSpec spec,
            List<VoucherLedgerTieoutService.VoucherLedgerRow> voucherRows) {
        Set<Long> subjectIds = Set.copyOf(spec.subjectIds());
        Map<TieoutKey, BigDecimal> mapped = new TreeMap<>();
        Map<TieoutKey, BigDecimal> ignored = new TreeMap<>();
        for (VoucherLedgerTieoutService.VoucherLedgerRow row : voucherRows) {
            if (spec.voucherBusinessType() > 0 && !spec.voucherBusinessType().equals(row.businessType())) {
                continue;
            }
            Map<String, String> dimensions = Map.of(
                    "businessCode", row.businessCode(),
                    "accountPeriod", row.accountPeriod());
            TieoutKey key = TieoutKey.from(spec, dimensions);
            BigDecimal amount = sideAmount(spec.voucherSide(), row);
            if (subjectIds.contains(row.subjectId())) {
                mapped.merge(key, amount, FinanceVoucherSubjectTieoutService::addCents);
            } else {
                ignored.merge(key, row.debitAmount().add(row.creditAmount()), FinanceVoucherSubjectTieoutService::addCents);
            }
        }
        return new VoucherIndex(mapped, ignored);
    }

    private static String status(
            SummaryMetricRow summaryRow,
            boolean hasMappedVoucher,
            BigDecimal summaryAmount,
            BigDecimal voucherAmount) {
        if (summaryRow == null) {
            return "missing summary metric cell";
        }
        if (!hasMappedVoucher) {
            return "missing voucher subject cell";
        }
        return summaryAmount.compareTo(voucherAmount) == 0 ? "matched" : "amount mismatch";
    }

    private static String firstFailure(String currentFailure, SubjectTieoutSpec spec, SubjectTieoutDiff diff) {
        if (!currentFailure.isEmpty()) {
            return currentFailure;
        }
        return "Voucher subject tie-out failed: mappingId=" + spec.mappingId()
                + ", chain=" + spec.chain()
                + ", metricId=" + spec.metricId()
                + ", businessType=" + spec.voucherBusinessType()
                + ", side=" + spec.voucherSide()
                + ", subjects=" + subjectText(spec.subjectIds())
                + ", dimensions=" + diff.key().dimensionsText()
                + ", status=" + diff.status()
                + ", summary=" + amountText(diff.summaryAmount())
                + ", voucher=" + amountText(diff.voucherAmount())
                + ", ignoredVoucher=" + amountText(diff.ignoredVoucherAmount())
                + ", difference=" + amountText(diff.difference());
    }

    private static String dimensionsText(SubjectTieoutSpec spec, Map<String, String> dimensions) {
        return TieoutKey.from(spec, dimensions).dimensionsText();
    }

    private static String subjectText(List<Long> subjectIds) {
        return subjectIds.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static BigDecimal sideAmount(String side, VoucherLedgerTieoutService.VoucherLedgerRow row) {
        return "credit".equalsIgnoreCase(side) ? row.creditAmount() : row.debitAmount();
    }

    private static BigDecimal addCents(BigDecimal left, BigDecimal right) {
        return cents(left.add(cents(right)));
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

    private static Integer intOrZero(Integer value) {
        return value == null ? 0 : value;
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

    public record SubjectTieoutSpec(
            String mappingId,
            String chain,
            String metricId,
            Integer voucherBusinessType,
            String voucherSide,
            List<Long> subjectIds,
            List<String> dimensionKeys) {
        public SubjectTieoutSpec {
            mappingId = textOrEmpty(mappingId);
            chain = textOrEmpty(chain);
            metricId = textOrEmpty(metricId);
            voucherBusinessType = intOrZero(voucherBusinessType);
            voucherSide = textOrEmpty(voucherSide);
            subjectIds = subjectIds == null ? List.of() : List.copyOf(subjectIds);
            dimensionKeys = copyOrEmpty(dimensionKeys);
        }

        public SubjectTieoutSpec(
                String mappingId,
                String chain,
                String metricId,
                String voucherSide,
                List<Long> subjectIds,
                List<String> dimensionKeys) {
            this(mappingId, chain, metricId, 0, voucherSide, subjectIds, dimensionKeys);
        }
    }

    public record SummaryMetricRow(String chain, String metricId, Map<String, String> dimensions, BigDecimal amount) {
        public SummaryMetricRow {
            chain = textOrEmpty(chain);
            metricId = textOrEmpty(metricId);
            dimensions = copyMapOrEmpty(dimensions);
            amount = cents(amount);
        }
    }

    public record SubjectTieoutReport(boolean passed, List<SubjectTieoutDiff> diffs, String failureMessage) {
        public SubjectTieoutReport {
            diffs = diffs == null ? List.of() : List.copyOf(diffs);
            failureMessage = textOrEmpty(failureMessage);
        }
    }

    public record SubjectTieoutDiff(
            TieoutKey key,
            String status,
            BigDecimal summaryAmount,
            BigDecimal voucherAmount,
            BigDecimal ignoredVoucherAmount,
            BigDecimal difference) {
        public SubjectTieoutDiff {
            status = textOrEmpty(status);
            summaryAmount = cents(summaryAmount);
            voucherAmount = cents(voucherAmount);
            ignoredVoucherAmount = cents(ignoredVoucherAmount);
            difference = cents(difference).abs();
        }
    }

    public record TieoutKey(Map<String, String> dimensions) implements Comparable<TieoutKey> {
        public TieoutKey {
            dimensions = copyMapOrEmpty(dimensions);
        }

        static TieoutKey from(SubjectTieoutSpec spec, Map<String, String> dimensions) {
            Map<String, String> selected = new LinkedHashMap<>();
            for (String dimensionKey : spec.dimensionKeys()) {
                selected.put(dimensionKey, dimensions.getOrDefault(dimensionKey, ""));
            }
            return new TieoutKey(selected);
        }

        String dimensionsText() {
            return dimensions.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .reduce((left, right) -> left + "," + right)
                    .orElse("");
        }

        @Override
        public int compareTo(TieoutKey other) {
            return dimensionsText().compareTo(other.dimensionsText());
        }
    }

    private record VoucherIndex(Map<TieoutKey, BigDecimal> mappedAmounts, Map<TieoutKey, BigDecimal> ignoredAmounts) {
    }
}
