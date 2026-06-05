package com.yuzhi.dts.copilot.ai.service.copilot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.stereotype.Service;

@Service
public class FinanceReconciliationScorecardService {

    private static final BigDecimal ZERO_CENTS = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public ScorecardReport score(
            ScorecardSpec spec,
            List<CheckRun> currentRuns,
            List<ReconciliationFailure> baselineFailures) {
        ScorecardSpec safeSpec = spec == null
                ? new ScorecardSpec("", ZERO_CENTS, true, List.of())
                : spec;
        List<CheckRun> safeRuns = currentRuns == null ? List.of() : currentRuns;
        List<ReconciliationFailure> safeBaseline = baselineFailures == null ? List.of() : baselineFailures;

        String contractFailure = firstContractFailure(safeSpec, safeRuns);
        if (!contractFailure.isEmpty()) {
            return new ScorecardReport(false, false, "FAIL", 0, 0, ZERO_CENTS, ZERO_CENTS, List.of(), List.of(), List.of(), contractFailure);
        }

        List<CategoryScore> categoryScores = categoryScores(safeSpec, safeRuns);
        List<ReconciliationFailure> currentFailures = failures(safeRuns);
        currentFailures.addAll(thresholdFailures(safeSpec, safeRuns));
        List<ReconciliationFailure> newDrifts = newDrifts(currentFailures, safeBaseline);
        int totalChecks = safeRuns.size();
        int passedChecks = (int) safeRuns.stream()
                .filter(run -> effectivelyPassed(safeSpec, run))
                .count();
        BigDecimal passRate = rate(passedChecks, totalChecks);
        BigDecimal maxDifference = safeRuns.stream()
                .map(CheckRun::maxDifference)
                .max(BigDecimal::compareTo)
                .orElse(ZERO_CENTS);
        boolean drifted = safeSpec.failOnNewDrift() && !newDrifts.isEmpty();
        boolean passed = currentFailures.isEmpty() && !drifted;
        String healthStatus = healthStatus(passed, drifted);
        String failureMessage = firstFailureMessage(safeSpec, currentFailures, newDrifts, drifted);

        return new ScorecardReport(
                passed,
                drifted,
                healthStatus,
                totalChecks,
                passedChecks,
                passRate,
                maxDifference,
                categoryScores,
                currentFailures,
                newDrifts,
                failureMessage);
    }

    private static String firstContractFailure(ScorecardSpec spec, List<CheckRun> runs) {
        Set<String> required = new LinkedHashSet<>(spec.requiredCategories());
        TreeSet<String> seenChecks = new TreeSet<>();
        TreeSet<String> seenCategories = new TreeSet<>();
        for (CheckRun run : runs) {
            if (!required.contains(run.category())) {
                return "Reconciliation scorecard failed: scorecardId=" + spec.scorecardId()
                        + ", reason=unknown category"
                        + ", category=" + run.category()
                        + ", checkId=" + run.checkId();
            }
            String checkKey = run.category() + "/" + run.checkId();
            if (!seenChecks.add(checkKey)) {
                return "Reconciliation scorecard failed: scorecardId=" + spec.scorecardId()
                        + ", reason=duplicate check"
                        + ", check=" + checkKey;
            }
            seenCategories.add(run.category());
        }
        for (String category : spec.requiredCategories()) {
            if (!seenCategories.contains(category)) {
                return "Reconciliation scorecard failed: scorecardId=" + spec.scorecardId()
                        + ", reason=missing required category"
                        + ", category=" + category;
            }
        }
        return "";
    }

    private static List<CategoryScore> categoryScores(ScorecardSpec spec, List<CheckRun> runs) {
        Map<String, List<CheckRun>> byCategory = new TreeMap<>();
        for (CheckRun run : runs) {
            byCategory.computeIfAbsent(run.category(), ignored -> new ArrayList<>()).add(run);
        }
        List<CategoryScore> scores = new ArrayList<>();
        for (String category : spec.requiredCategories()) {
            List<CheckRun> categoryRuns = byCategory.getOrDefault(category, List.of());
            int totalChecks = categoryRuns.size();
            int passedChecks = (int) categoryRuns.stream()
                    .filter(run -> effectivelyPassed(spec, run))
                    .count();
            int failedCells = categoryRuns.stream().mapToInt(CheckRun::failedCells).sum();
            BigDecimal maxDifference = categoryRuns.stream()
                    .map(CheckRun::maxDifference)
                    .max(BigDecimal::compareTo)
                    .orElse(ZERO_CENTS);
            scores.add(new CategoryScore(category, totalChecks, passedChecks, rate(passedChecks, totalChecks), failedCells, maxDifference));
        }
        return scores;
    }

    private static List<ReconciliationFailure> failures(List<CheckRun> runs) {
        List<ReconciliationFailure> failures = new ArrayList<>();
        for (CheckRun run : runs) {
            failures.addAll(run.failures());
        }
        return failures;
    }

    private static boolean effectivelyPassed(ScorecardSpec spec, CheckRun run) {
        return run.passed() && run.maxDifference().compareTo(spec.maxAllowedDifference()) <= 0;
    }

    private static List<ReconciliationFailure> thresholdFailures(ScorecardSpec spec, List<CheckRun> runs) {
        return runs.stream()
                .filter(run -> run.failures().isEmpty())
                .filter(run -> run.maxDifference().compareTo(spec.maxAllowedDifference()) > 0)
                .map(run -> new ReconciliationFailure(
                        run.category(),
                        run.checkId(),
                        "check:maxDifference",
                        "difference threshold exceeded",
                        run.maxDifference(),
                        "maxDifference exceeds " + amountText(spec.maxAllowedDifference())))
                .toList();
    }

    private static List<ReconciliationFailure> newDrifts(
            List<ReconciliationFailure> currentFailures,
            List<ReconciliationFailure> baselineFailures) {
        TreeSet<String> baselineKeys = baselineFailures.stream()
                .map(ReconciliationFailure::fingerprint)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        return currentFailures.stream()
                .filter(failure -> !baselineKeys.contains(failure.fingerprint()))
                .toList();
    }

    private static String healthStatus(boolean passed, boolean drifted) {
        if (passed) {
            return "PASS";
        }
        return drifted ? "DRIFT" : "FAIL";
    }

    private static String firstFailureMessage(
            ScorecardSpec spec,
            List<ReconciliationFailure> currentFailures,
            List<ReconciliationFailure> newDrifts,
            boolean drifted) {
        if (drifted) {
            ReconciliationFailure drift = newDrifts.getFirst();
            return "Reconciliation scorecard new drift: scorecardId=" + spec.scorecardId()
                    + ", category=" + drift.category()
                    + ", checkId=" + drift.checkId()
                    + ", cell=" + drift.cellKey()
                    + ", status=" + drift.status()
                    + ", difference=" + amountText(drift.difference());
        }
        if (!currentFailures.isEmpty()) {
            ReconciliationFailure failure = currentFailures.getFirst();
            return "Reconciliation scorecard known reconciliation failure: scorecardId=" + spec.scorecardId()
                    + ", category=" + failure.category()
                    + ", checkId=" + failure.checkId()
                    + ", cell=" + failure.cellKey()
                    + ", status=" + failure.status()
                    + ", difference=" + amountText(failure.difference());
        }
        return "";
    }

    private static BigDecimal rate(int passed, int total) {
        if (total <= 0) {
            return ZERO_CENTS;
        }
        return BigDecimal.valueOf(passed)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
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

    public record ScorecardSpec(
            String scorecardId,
            BigDecimal maxAllowedDifference,
            boolean failOnNewDrift,
            List<String> requiredCategories) {
        public ScorecardSpec {
            scorecardId = textOrEmpty(scorecardId);
            maxAllowedDifference = cents(maxAllowedDifference);
            requiredCategories = copyOrEmpty(requiredCategories);
        }
    }

    public record CheckRun(
            String category,
            String checkId,
            String checkName,
            boolean passed,
            int totalCells,
            int failedCells,
            BigDecimal maxDifference,
            List<ReconciliationFailure> failures) {
        public CheckRun {
            category = textOrEmpty(category);
            checkId = textOrEmpty(checkId);
            checkName = textOrEmpty(checkName);
            totalCells = Math.max(totalCells, 0);
            failedCells = Math.max(failedCells, 0);
            maxDifference = cents(maxDifference);
            failures = copyOrEmpty(failures);
        }

        public static CheckRun fromDifferentialGrid(
                String category,
                String checkId,
                String checkName,
                FinanceDifferentialGridService.GridReport report) {
            FinanceDifferentialGridService.GridReport safeReport = report == null
                    ? new FinanceDifferentialGridService.GridReport(false, List.of(), "missing grid report")
                    : report;
            List<ReconciliationFailure> failures = safeReport.diffs().stream()
                    .filter(diff -> !"matched".equals(diff.status()) && !"empty matched".equals(diff.status()))
                    .map(diff -> new ReconciliationFailure(
                            category,
                            checkId,
                            gridCellKey(diff.key()),
                            diff.status(),
                            diff.difference(),
                            safeReport.failureMessage()))
                    .toList();
            BigDecimal maxDifference = safeReport.diffs().stream()
                    .map(FinanceDifferentialGridService.GridDiff::difference)
                    .max(BigDecimal::compareTo)
                    .orElse(ZERO_CENTS);
            return new CheckRun(
                    category,
                    checkId,
                    checkName,
                    safeReport.passed(),
                    safeReport.diffs().size(),
                    failures.size(),
                    maxDifference,
                    failures);
        }

        private static String gridCellKey(FinanceDifferentialGridService.GridKey key) {
            String dimensionsText = key.dimensions().entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .reduce((left, right) -> left + "," + right)
                    .orElse("");
            return dimensionsText.isEmpty() ? key.sliceId() : key.sliceId() + "/" + dimensionsText;
        }
    }

    public record ReconciliationFailure(
            String category,
            String checkId,
            String cellKey,
            String status,
            BigDecimal difference,
            String message) {
        public ReconciliationFailure {
            category = textOrEmpty(category);
            checkId = textOrEmpty(checkId);
            cellKey = textOrEmpty(cellKey);
            status = textOrEmpty(status);
            difference = cents(difference);
            message = textOrEmpty(message);
        }

        String fingerprint() {
            return category + "|" + checkId + "|" + cellKey + "|" + status;
        }
    }

    public record CategoryScore(
            String category,
            int totalChecks,
            int passedChecks,
            BigDecimal passRate,
            int failedCells,
            BigDecimal maxDifference) {
        public CategoryScore {
            category = textOrEmpty(category);
            totalChecks = Math.max(totalChecks, 0);
            passedChecks = Math.max(passedChecks, 0);
            passRate = cents(passRate);
            failedCells = Math.max(failedCells, 0);
            maxDifference = cents(maxDifference);
        }
    }

    public record ScorecardReport(
            boolean passed,
            boolean drifted,
            String healthStatus,
            int totalChecks,
            int passedChecks,
            BigDecimal passRate,
            BigDecimal maxDifference,
            List<CategoryScore> categoryScores,
            List<ReconciliationFailure> failures,
            List<ReconciliationFailure> newDrifts,
            String failureMessage) {
        public ScorecardReport {
            healthStatus = textOrEmpty(healthStatus);
            totalChecks = Math.max(totalChecks, 0);
            passedChecks = Math.max(passedChecks, 0);
            passRate = cents(passRate);
            maxDifference = cents(maxDifference);
            categoryScores = copyOrEmpty(categoryScores);
            failures = copyOrEmpty(failures);
            newDrifts = copyOrEmpty(newDrifts);
            failureMessage = textOrEmpty(failureMessage);
        }
    }
}
