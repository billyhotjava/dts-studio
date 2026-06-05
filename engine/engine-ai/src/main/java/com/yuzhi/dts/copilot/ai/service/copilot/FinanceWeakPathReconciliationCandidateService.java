package com.yuzhi.dts.copilot.ai.service.copilot;

import com.yuzhi.dts.copilot.ai.service.chat.RouteTelemetryService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FinanceWeakPathReconciliationCandidateService {

    public List<ReconciliationCandidate> selectCandidates(
            WeakPathCandidateSpec spec,
            RouteTelemetryService.RouteTelemetrySummary summary) {
        WeakPathCandidateSpec safeSpec = spec == null
                ? new WeakPathCandidateSpec("", List.of(), List.of(), List.of(), 1, 1, List.of())
                : spec;
        String specFailure = firstSpecFailure(safeSpec);
        if (!specFailure.isEmpty()) {
            return List.of(new ReconciliationCandidate(
                    safeSpec.policyId(),
                    "",
                    "",
                    "",
                    "",
                    "",
                    0,
                    0,
                    List.of(),
                    safeSpec.reconciliationSets(),
                    "NONE",
                    specFailure));
        }

        List<RouteTelemetryService.MartCandidateSignal> signals = summary == null
                ? List.of()
                : summary.martCandidateSignals();
        List<ReconciliationCandidate> candidates = new ArrayList<>();
        for (RouteTelemetryService.MartCandidateSignal signal : signals) {
            if (!safeSpec.weakTiers().contains(signal.finalTier())) {
                continue;
            }
            if (signal.count() < safeSpec.minCount()) {
                continue;
            }
            if (!isFinanceSignal(safeSpec, signal)) {
                continue;
            }
            candidates.add(toCandidate(safeSpec, signal));
        }
        return candidates.stream()
                .sorted((left, right) -> {
                    int scoreCompare = Integer.compare(right.priorityScore(), left.priorityScore());
                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }
                    return left.candidateKey().compareTo(right.candidateKey());
                })
                .toList();
    }

    public FinanceReconciliationScorecardService.CheckRun toScorecardCheck(
            String category,
            String checkId,
            List<ReconciliationCandidate> candidates,
            List<String> coveredCandidateKeys) {
        List<ReconciliationCandidate> safeCandidates = candidates == null ? List.of() : candidates;
        Set<String> covered = new TreeSet<>(coveredCandidateKeys == null ? List.of() : coveredCandidateKeys);
        List<FinanceReconciliationScorecardService.ReconciliationFailure> failures = safeCandidates.stream()
                .filter(candidate -> !covered.contains(candidate.candidateKey()))
                .map(candidate -> new FinanceReconciliationScorecardService.ReconciliationFailure(
                        category,
                        checkId,
                        candidate.candidateKey(),
                        "weak path candidate not covered",
                        BigDecimal.ZERO,
                        candidate.reason()))
                .toList();
        return new FinanceReconciliationScorecardService.CheckRun(
                category,
                checkId,
                "telemetry weak path reconciliation candidates",
                failures.isEmpty(),
                safeCandidates.size(),
                failures.size(),
                BigDecimal.ZERO,
                failures);
    }

    private static String firstSpecFailure(WeakPathCandidateSpec spec) {
        if (hasDuplicate(spec.financeDomains())) {
            return "Weak path candidate policy failed: reason=duplicate finance domain";
        }
        if (spec.weakTiers().isEmpty()) {
            return "Weak path candidate policy failed: reason=missing weak tier";
        }
        if (spec.reconciliationSets().isEmpty()) {
            return "Weak path candidate policy failed: reason=missing reconciliation set";
        }
        return "";
    }

    private static boolean hasDuplicate(List<String> values) {
        Set<String> seen = new LinkedHashSet<>();
        for (String value : values) {
            if (!seen.add(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFinanceSignal(
            WeakPathCandidateSpec spec,
            RouteTelemetryService.MartCandidateSignal signal) {
        boolean domainMatched = spec.financeDomains().contains(textOrEmpty(signal.domain()));
        if (!domainMatched) {
            return false;
        }
        String haystack = (textOrEmpty(signal.target()) + " "
                + textOrEmpty(signal.dataSurface()) + " "
                + String.join(" ", signal.questionSamples()))
                .toLowerCase(Locale.ROOT);
        return spec.financeKeywords().stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(haystack::contains);
    }

    private static ReconciliationCandidate toCandidate(
            WeakPathCandidateSpec spec,
            RouteTelemetryService.MartCandidateSignal signal) {
        int priorityScore = (int) signal.count() * tierWeight(signal.finalTier());
        String key = String.join("|",
                textOrEmpty(signal.finalTier()),
                textOrEmpty(signal.domain()),
                textOrEmpty(signal.target()));
        String action = signal.count() >= spec.semanticDraftThreshold() ? "CREATE_SPRINT31_DRAFT" : "WATCH";
        return new ReconciliationCandidate(
                key,
                signal.finalTier(),
                signal.domain(),
                signal.responseKind(),
                signal.target(),
                signal.dataSurface(),
                signal.count(),
                priorityScore,
                signal.questionSamples(),
                spec.reconciliationSets(),
                action,
                "frequent finance weak path");
    }

    private static int tierWeight(String finalTier) {
        return "TIER_5_DIRECT_DETAIL".equals(finalTier) ? 5 : 4;
    }

    private static String textOrEmpty(String value) {
        return StringUtils.hasText(value) ? value : "";
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    public record WeakPathCandidateSpec(
            String policyId,
            List<String> financeDomains,
            List<String> weakTiers,
            List<String> financeKeywords,
            int minCount,
            int semanticDraftThreshold,
            List<String> reconciliationSets) {
        public WeakPathCandidateSpec {
            policyId = textOrEmpty(policyId);
            financeDomains = copyOrEmpty(financeDomains);
            weakTiers = copyOrEmpty(weakTiers);
            financeKeywords = copyOrEmpty(financeKeywords);
            minCount = Math.max(minCount, 1);
            semanticDraftThreshold = Math.max(semanticDraftThreshold, minCount);
            reconciliationSets = copyOrEmpty(reconciliationSets);
        }
    }

    public record ReconciliationCandidate(
            String candidateKey,
            String finalTier,
            String domain,
            String responseKind,
            String target,
            String dataSurface,
            long sourceCount,
            int priorityScore,
            List<String> questionSamples,
            List<String> reconciliationSets,
            String semanticDraftAction,
            String reason) {
        public ReconciliationCandidate {
            candidateKey = textOrEmpty(candidateKey);
            finalTier = textOrEmpty(finalTier);
            domain = textOrEmpty(domain);
            responseKind = textOrEmpty(responseKind);
            target = textOrEmpty(target);
            dataSurface = textOrEmpty(dataSurface);
            sourceCount = Math.max(sourceCount, 0);
            priorityScore = Math.max(priorityScore, 0);
            questionSamples = copyOrEmpty(questionSamples);
            reconciliationSets = copyOrEmpty(reconciliationSets);
            semanticDraftAction = textOrEmpty(semanticDraftAction);
            reason = textOrEmpty(reason);
        }
    }
}
