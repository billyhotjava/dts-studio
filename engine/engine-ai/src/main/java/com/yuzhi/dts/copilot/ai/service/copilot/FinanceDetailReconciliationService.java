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
public class FinanceDetailReconciliationService {

    private static final BigDecimal ZERO_CENTS = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public DetailReconciliationReport reconcile(
            DetailReconciliationSpec spec,
            List<DetailRow> copilotRows,
            List<DetailRow> oracleRows) {
        DetailReconciliationSpec safeSpec = spec == null
                ? new DetailReconciliationSpec("", "", List.of())
                : spec;
        List<DetailRow> safeCopilotRows = copilotRows == null ? List.of() : copilotRows;
        List<DetailRow> safeOracleRows = oracleRows == null ? List.of() : oracleRows;

        String chainFailure = firstChainMismatch(safeSpec, safeCopilotRows, "copilot");
        if (chainFailure.isEmpty()) {
            chainFailure = firstChainMismatch(safeSpec, safeOracleRows, "oracle");
        }
        if (!chainFailure.isEmpty()) {
            return new DetailReconciliationReport(false, List.of(), chainFailure);
        }

        String duplicateFailure = firstDuplicateFailure(safeSpec, safeCopilotRows, "copilot");
        if (duplicateFailure.isEmpty()) {
            duplicateFailure = firstDuplicateFailure(safeSpec, safeOracleRows, "oracle");
        }
        if (!duplicateFailure.isEmpty()) {
            return new DetailReconciliationReport(false, List.of(), duplicateFailure);
        }

        Map<DetailKey, DetailRow> copilotByKey = indexRows(safeCopilotRows);
        Map<DetailKey, DetailRow> oracleByKey = indexRows(safeOracleRows);
        TreeSet<DetailKey> keys = new TreeSet<>();
        keys.addAll(copilotByKey.keySet());
        keys.addAll(oracleByKey.keySet());

        List<DetailDiff> diffs = new ArrayList<>();
        String failureMessage = "";
        for (DetailKey key : keys) {
            DetailRow copilotRow = copilotByKey.get(key);
            DetailRow oracleRow = oracleByKey.get(key);
            if (copilotRow == null) {
                DetailDiff diff = DetailDiff.missing(key, "missing copilot row");
                diffs.add(diff);
                failureMessage = firstFailure(failureMessage, safeSpec, diff, "");
                continue;
            }
            if (oracleRow == null) {
                DetailDiff diff = DetailDiff.missing(key, "missing oracle row");
                diffs.add(diff);
                failureMessage = firstFailure(failureMessage, safeSpec, diff, "");
                continue;
            }

            List<AmountDifference> amountDifferences = new ArrayList<>();
            for (String amountField : safeSpec.amountFields()) {
                BigDecimal copilotAmount = copilotRow.amount(amountField);
                BigDecimal oracleAmount = oracleRow.amount(amountField);
                amountDifferences.add(new AmountDifference(
                        amountField,
                        copilotAmount,
                        oracleAmount,
                        cents(copilotAmount.subtract(oracleAmount)).abs()));
            }
            DetailDiff diff = new DetailDiff(key, "matched", amountDifferences);
            diffs.add(diff);
            AmountDifference firstMismatch = diff.firstNonZeroDifference();
            if (firstMismatch != null) {
                failureMessage = firstFailure(failureMessage, safeSpec, diff, firstMismatch.field());
            }
        }

        return new DetailReconciliationReport(failureMessage.isEmpty(), diffs, failureMessage);
    }

    private static Map<DetailKey, DetailRow> indexRows(List<DetailRow> rows) {
        Map<DetailKey, DetailRow> indexed = new TreeMap<>();
        for (DetailRow row : rows) {
            indexed.put(DetailKey.from(row), row);
        }
        return indexed;
    }

    private static String firstChainMismatch(
            DetailReconciliationSpec spec,
            List<DetailRow> rows,
            String source) {
        for (DetailRow row : rows) {
            if (!spec.chain().equals(row.chain())) {
                return "Detail reconciliation chain mismatch: oracleBindingId=" + spec.oracleBindingId()
                        + ", source=" + source
                        + ", businessKey=" + row.businessKey()
                        + ", expected=" + spec.chain()
                        + ", actual=" + row.chain();
            }
        }
        return "";
    }

    private static String firstDuplicateFailure(
            DetailReconciliationSpec spec,
            List<DetailRow> rows,
            String source) {
        TreeSet<DetailKey> seen = new TreeSet<>();
        for (DetailRow row : rows) {
            DetailKey key = DetailKey.from(row);
            if (!seen.add(key)) {
                return "Detail reconciliation failed: oracleBindingId=" + spec.oracleBindingId()
                        + ", chain=" + spec.chain()
                        + ", reason=duplicate " + source + " row"
                        + ", businessKey=" + key.businessKey()
                        + ", projectId=" + key.projectId()
                        + ", accountPeriod=" + key.accountPeriod();
            }
        }
        return "";
    }

    private static String firstFailure(
            String currentFailure,
            DetailReconciliationSpec spec,
            DetailDiff diff,
            String amountField) {
        if (!currentFailure.isEmpty()) {
            return currentFailure;
        }
        if (diff.missing()) {
            return "Detail reconciliation failed: oracleBindingId=" + spec.oracleBindingId()
                    + ", chain=" + spec.chain()
                    + ", businessKey=" + diff.key().businessKey()
                    + ", projectId=" + diff.key().projectId()
                    + ", accountPeriod=" + diff.key().accountPeriod()
                    + ", reason=" + diff.status();
        }
        AmountDifference firstMismatch = diff.firstNonZeroDifference();
        if (firstMismatch == null) {
            return "";
        }
        return "Detail reconciliation failed: oracleBindingId=" + spec.oracleBindingId()
                + ", chain=" + spec.chain()
                + ", businessKey=" + diff.key().businessKey()
                + ", projectId=" + diff.key().projectId()
                + ", accountPeriod=" + diff.key().accountPeriod()
                + ", field=" + textOrEmpty(amountField)
                + ", copilot=" + amountText(firstMismatch.copilotAmount())
                + ", oracle=" + amountText(firstMismatch.oracleAmount())
                + ", difference=" + amountText(firstMismatch.difference());
    }

    private static BigDecimal cents(BigDecimal amount) {
        if (amount == null) {
            return ZERO_CENTS;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String amountText(BigDecimal amount) {
        return cents(amount).toPlainString();
    }

    public record DetailReconciliationSpec(String oracleBindingId, String chain, List<String> amountFields) {
        public DetailReconciliationSpec {
            oracleBindingId = textOrEmpty(oracleBindingId);
            chain = textOrEmpty(chain);
            amountFields = amountFields == null ? List.of() : List.copyOf(amountFields);
        }
    }

    public record DetailRow(
            String chain,
            String businessKey,
            String projectId,
            String accountPeriod,
            Map<String, BigDecimal> amounts) {

        public DetailRow {
            chain = textOrEmpty(chain);
            businessKey = textOrEmpty(businessKey);
            projectId = textOrEmpty(projectId);
            accountPeriod = textOrEmpty(accountPeriod);
            Map<String, BigDecimal> normalizedAmounts = new LinkedHashMap<>();
            if (amounts != null) {
                amounts.forEach((field, amount) -> normalizedAmounts.put(textOrEmpty(field), cents(amount)));
            }
            amounts = Collections.unmodifiableMap(normalizedAmounts);
        }

        BigDecimal amount(String field) {
            return amounts.getOrDefault(textOrEmpty(field), ZERO_CENTS);
        }
    }

    public record DetailReconciliationReport(boolean passed, List<DetailDiff> diffs, String failureMessage) {
        public DetailReconciliationReport {
            diffs = diffs == null ? List.of() : List.copyOf(diffs);
            failureMessage = textOrEmpty(failureMessage);
        }
    }

    public record DetailDiff(DetailKey key, String status, List<AmountDifference> amountDifferences) {
        public DetailDiff {
            status = textOrEmpty(status);
            amountDifferences = amountDifferences == null ? List.of() : List.copyOf(amountDifferences);
        }

        static DetailDiff missing(DetailKey key, String status) {
            return new DetailDiff(key, status, List.of());
        }

        public boolean missing() {
            return amountDifferences.isEmpty() && !"matched".equals(status);
        }

        public BigDecimal maxDifference() {
            return amountDifferences.stream()
                    .map(AmountDifference::difference)
                    .max(BigDecimal::compareTo)
                    .orElse(ZERO_CENTS);
        }

        AmountDifference firstNonZeroDifference() {
            return amountDifferences.stream()
                    .filter(difference -> difference.difference().compareTo(ZERO_CENTS) != 0)
                    .findFirst()
                    .orElse(null);
        }
    }

    public record AmountDifference(
            String field,
            BigDecimal copilotAmount,
            BigDecimal oracleAmount,
            BigDecimal difference) {
        public AmountDifference {
            field = textOrEmpty(field);
            copilotAmount = cents(copilotAmount);
            oracleAmount = cents(oracleAmount);
            difference = cents(difference).abs();
        }
    }

    public record DetailKey(String businessKey, String projectId, String accountPeriod)
            implements Comparable<DetailKey> {
        public DetailKey {
            businessKey = textOrEmpty(businessKey);
            projectId = textOrEmpty(projectId);
            accountPeriod = textOrEmpty(accountPeriod);
        }

        static DetailKey from(DetailRow row) {
            return new DetailKey(row.businessKey(), row.projectId(), row.accountPeriod());
        }

        @Override
        public int compareTo(DetailKey other) {
            int businessCompare = businessKey.compareTo(other.businessKey);
            if (businessCompare != 0) {
                return businessCompare;
            }
            int projectCompare = projectId.compareTo(other.projectId);
            if (projectCompare != 0) {
                return projectCompare;
            }
            return accountPeriod.compareTo(other.accountPeriod);
        }
    }
}
