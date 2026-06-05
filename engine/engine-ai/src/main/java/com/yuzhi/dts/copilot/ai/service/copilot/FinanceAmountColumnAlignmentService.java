package com.yuzhi.dts.copilot.ai.service.copilot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class FinanceAmountColumnAlignmentService {

    private static final BigDecimal ZERO_CENTS = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final FinanceAmountColumnAlignmentRegistry registry;

    public FinanceAmountColumnAlignmentService(FinanceAmountColumnAlignmentRegistry registry) {
        this.registry = registry;
    }

    public AlignmentReport validateSelections(List<ColumnSelection> selections) {
        List<ColumnSelection> safeSelections = selections == null ? List.of() : selections;
        List<SelectionDiff> diffs = new ArrayList<>();
        String failureMessage = "";

        for (ColumnSelection selection : safeSelections) {
            List<FinanceAmountColumnAlignmentRegistry.AmountTier> tiers =
                    registry.tiersForTerm(selection.requestedMeaning());
            SelectionDiff diff;
            if (tiers.isEmpty()) {
                diff = new SelectionDiff(selection.requestedMeaning(), selection.selectedField(), "", "unknown meaning");
            } else if (tiers.size() > 1) {
                diff = new SelectionDiff(
                        selection.requestedMeaning(),
                        selection.selectedField(),
                        tierIds(tiers),
                        "ambiguous meaning");
            } else {
                FinanceAmountColumnAlignmentRegistry.AmountTier tier = tiers.getFirst();
                String status = tier.acceptsField(selection.selectedField()) ? "matched" : "wrong column";
                diff = new SelectionDiff(selection.requestedMeaning(), selection.selectedField(), tier.apiField(), status);
            }
            diffs.add(diff);
            if (failureMessage.isEmpty() && !"matched".equals(diff.status())) {
                failureMessage = selectionFailure(diff);
            }
        }

        return new AlignmentReport(failureMessage.isEmpty(), diffs, failureMessage);
    }

    public ProbeReport validateProbeAmounts(AmountProbe oracleProbe, Map<String, BigDecimal> copilotAmountsByMeaning) {
        AmountProbe safeProbe = oracleProbe == null ? new AmountProbe(Map.of()) : oracleProbe;
        Map<String, BigDecimal> safeCopilotAmounts = normalizeAmounts(copilotAmountsByMeaning);
        if (safeProbe.allConfiguredTierAmountsEqual(registry.tiers())) {
            return new ProbeReport(false, false, "Amount column alignment is inconclusive: discount=1 trap keeps all configured tier amounts equal.");
        }

        for (Map.Entry<String, BigDecimal> entry : safeCopilotAmounts.entrySet()) {
            String meaning = entry.getKey();
            List<FinanceAmountColumnAlignmentRegistry.AmountTier> tiers = registry.tiersForTerm(meaning);
            if (tiers.isEmpty()) {
                return new ProbeReport(false, true, "Amount probe failed: unknown meaning=" + meaning);
            }
            if (tiers.size() > 1) {
                return new ProbeReport(false, true, "Amount probe failed: ambiguous meaning=" + meaning + ", candidates=" + tierIds(tiers));
            }
            FinanceAmountColumnAlignmentRegistry.AmountTier tier = tiers.getFirst();
            BigDecimal expected = safeProbe.amount(tier.apiField());
            BigDecimal actual = cents(entry.getValue());
            if (expected.compareTo(actual) != 0) {
                return new ProbeReport(
                        false,
                        true,
                        "Amount probe failed: meaning=" + meaning
                                + ", expectedField=" + tier.apiField()
                                + ", expected=" + amountText(expected)
                                + ", actual=" + amountText(actual));
            }
        }
        return new ProbeReport(true, true, "");
    }

    private static String tierIds(List<FinanceAmountColumnAlignmentRegistry.AmountTier> tiers) {
        return tiers.stream()
                .map(FinanceAmountColumnAlignmentRegistry.AmountTier::id)
                .collect(Collectors.joining(","));
    }

    private static String selectionFailure(SelectionDiff diff) {
        return "Amount column alignment failed: meaning=" + diff.requestedMeaning()
                + ", status=" + diff.status()
                + ", expected=" + diff.expectedField()
                + ", actual=" + diff.selectedField();
    }

    private static Map<String, BigDecimal> normalizeAmounts(Map<String, BigDecimal> amounts) {
        Map<String, BigDecimal> normalized = new LinkedHashMap<>();
        if (amounts != null) {
            amounts.forEach((meaning, amount) -> normalized.put(textOrEmpty(meaning), cents(amount)));
        }
        return normalized;
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

    public record ColumnSelection(String requestedMeaning, String selectedField) {
        public ColumnSelection {
            requestedMeaning = textOrEmpty(requestedMeaning);
            selectedField = textOrEmpty(selectedField);
        }
    }

    public record AlignmentReport(boolean passed, List<SelectionDiff> diffs, String failureMessage) {
        public AlignmentReport {
            diffs = diffs == null ? List.of() : List.copyOf(diffs);
            failureMessage = textOrEmpty(failureMessage);
        }
    }

    public record SelectionDiff(String requestedMeaning, String selectedField, String expectedField, String status) {
        public SelectionDiff {
            requestedMeaning = textOrEmpty(requestedMeaning);
            selectedField = textOrEmpty(selectedField);
            expectedField = textOrEmpty(expectedField);
            status = textOrEmpty(status);
        }
    }

    public record AmountProbe(Map<String, BigDecimal> amountsByApiField) {
        public AmountProbe {
            amountsByApiField = java.util.Collections.unmodifiableMap(normalizeAmounts(amountsByApiField));
        }

        BigDecimal amount(String apiField) {
            return amountsByApiField.getOrDefault(textOrEmpty(apiField), ZERO_CENTS);
        }

        boolean allConfiguredTierAmountsEqual(List<FinanceAmountColumnAlignmentRegistry.AmountTier> tiers) {
            List<BigDecimal> values = tiers.stream()
                    .map(tier -> amount(tier.apiField()))
                    .distinct()
                    .toList();
            return values.size() <= 1;
        }
    }

    public record ProbeReport(boolean passed, boolean conclusive, String failureMessage) {
        public ProbeReport {
            failureMessage = textOrEmpty(failureMessage);
        }
    }
}
