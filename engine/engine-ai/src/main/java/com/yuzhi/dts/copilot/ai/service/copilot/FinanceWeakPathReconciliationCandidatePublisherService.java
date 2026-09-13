package com.yuzhi.dts.copilot.ai.service.copilot;

import com.yuzhi.dts.copilot.ai.domain.FinanceWeakPathReconciliationCandidateSnapshot;
import com.yuzhi.dts.copilot.ai.service.chat.RouteTelemetryService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FinanceWeakPathReconciliationCandidatePublisherService {

    private static final String POLICY_MISSING = "MISSING_POLICY";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_NO_CANDIDATES = "NO_CANDIDATES";
    private static final String ACTION_CREATE_DRAFT = "CREATE_SPRINT31_DRAFT";

    private final RouteTelemetryService routeTelemetryService;
    private final FinanceWeakPathReconciliationCandidateRegistry registry;
    private final FinanceWeakPathReconciliationCandidateService candidateService;
    private final FinanceWeakPathReconciliationCandidateSnapshotService snapshotService;
    private final SemanticDraftService semanticDraftService;

    public FinanceWeakPathReconciliationCandidatePublisherService(
            RouteTelemetryService routeTelemetryService,
            FinanceWeakPathReconciliationCandidateRegistry registry,
            FinanceWeakPathReconciliationCandidateService candidateService,
            FinanceWeakPathReconciliationCandidateSnapshotService snapshotService,
            SemanticDraftService semanticDraftService) {
        this.routeTelemetryService = routeTelemetryService;
        this.registry = registry;
        this.candidateService = candidateService;
        this.snapshotService = snapshotService;
        this.semanticDraftService = semanticDraftService;
    }

    public PublishResult publishFromTelemetry(String policyId, int days, int candidateLimit) {
        String safePolicyId = text(policyId);
        return registry.policy(safePolicyId)
                .map(policy -> publish(policy, days, candidateLimit))
                .orElseGet(() -> new PublishResult(
                        POLICY_MISSING,
                        safePolicyId,
                        "",
                        emptySummary(days),
                        List.of(),
                        List.of(),
                        List.of()));
    }

    private PublishResult publish(
            FinanceWeakPathReconciliationCandidateRegistry.WeakPathCandidatePolicy policy,
            int days,
            int candidateLimit) {
        int safeDays = days <= 0 ? 7 : days;
        int safeLimit = candidateLimit <= 0 ? 10 : candidateLimit;
        RouteTelemetryService.RouteTelemetrySummary summary =
                routeTelemetryService.summarize(safeDays, safeLimit);
        List<FinanceWeakPathReconciliationCandidateService.ReconciliationCandidate> candidates =
                candidateService.selectCandidates(policy.candidateSpec(), summary);
        String runId = runId(policy.id());
        List<FinanceWeakPathReconciliationCandidateSnapshot> snapshots = new ArrayList<>();
        List<SemanticDraftService.SemanticDraft> semanticDrafts = new ArrayList<>();
        for (FinanceWeakPathReconciliationCandidateService.ReconciliationCandidate candidate : candidates) {
            SemanticDraftService.SemanticDraft draft = createDraftIfNeeded(candidate);
            if (draft != null) {
                semanticDrafts.add(draft);
            }
            snapshots.add(snapshotService.publish(runId, policy.id(), candidate, draft));
        }
        return new PublishResult(
                candidates.isEmpty() ? STATUS_NO_CANDIDATES : STATUS_PUBLISHED,
                policy.id(),
                runId,
                summary == null ? emptySummary(safeDays) : summary,
                candidates,
                snapshots,
                semanticDrafts);
    }

    private SemanticDraftService.SemanticDraft createDraftIfNeeded(
            FinanceWeakPathReconciliationCandidateService.ReconciliationCandidate candidate) {
        if (!ACTION_CREATE_DRAFT.equals(candidate.semanticDraftAction())) {
            return null;
        }
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("ruleCode", ruleCode(candidate));
        content.put("ruleName", "弱路径财务问题对账候选");
        content.put("guardrailText", candidate.reason());
        content.put("candidateKey", candidate.candidateKey());
        content.put("target", candidate.target());
        content.put("dataSurface", candidate.dataSurface());
        content.put("reconciliationSets", candidate.reconciliationSets());
        return semanticDraftService.createDraft(new SemanticDraftService.SemanticDraftRequest(
                "caliber-rule",
                candidate.domain(),
                content,
                candidate.questionSamples().isEmpty() ? candidate.candidateKey() : candidate.questionSamples().getFirst(),
                evidence(candidate),
                "automatic"));
    }

    private static List<String> evidence(
            FinanceWeakPathReconciliationCandidateService.ReconciliationCandidate candidate) {
        return List.of(
                "routeTier=" + candidate.finalTier(),
                "candidateKey=" + candidate.candidateKey(),
                "sourceCount=" + candidate.sourceCount(),
                "reconciliationSets=" + String.join(",", candidate.reconciliationSets()));
    }

    private static String ruleCode(
            FinanceWeakPathReconciliationCandidateService.ReconciliationCandidate candidate) {
        String suffix = Integer.toUnsignedString(candidate.candidateKey().hashCode(), 16)
                .toUpperCase(Locale.ROOT);
        return "CAL-WEAK-PATH-" + suffix;
    }

    private static String runId(String policyId) {
        return text(policyId) + "-" + Instant.now();
    }

    private static RouteTelemetryService.RouteTelemetrySummary emptySummary(int days) {
        return new RouteTelemetryService.RouteTelemetrySummary(days <= 0 ? 7 : days, 0, Map.of(), List.of());
    }

    private static String text(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    public record PublishResult(
            String status,
            String policyId,
            String runId,
            RouteTelemetryService.RouteTelemetrySummary summary,
            List<FinanceWeakPathReconciliationCandidateService.ReconciliationCandidate> candidates,
            List<FinanceWeakPathReconciliationCandidateSnapshot> snapshots,
            List<SemanticDraftService.SemanticDraft> semanticDrafts) {
        public PublishResult {
            status = text(status);
            policyId = text(policyId);
            runId = text(runId);
            summary = summary == null ? emptySummary(7) : summary;
            candidates = candidates == null ? List.of() : List.copyOf(candidates);
            snapshots = snapshots == null ? List.of() : List.copyOf(snapshots);
            semanticDrafts = semanticDrafts == null ? List.of() : List.copyOf(semanticDrafts);
        }
    }
}
