package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.domain.FinanceWeakPathReconciliationCandidateSnapshot;
import com.yuzhi.dts.copilot.ai.repository.FinanceWeakPathReconciliationCandidateSnapshotRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FinanceWeakPathReconciliationCandidateSnapshotService {

    private final FinanceWeakPathReconciliationCandidateSnapshotRepository repository;
    private final ObjectMapper objectMapper;

    public FinanceWeakPathReconciliationCandidateSnapshotService(
            FinanceWeakPathReconciliationCandidateSnapshotRepository repository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public FinanceWeakPathReconciliationCandidateSnapshot publish(
            String runId,
            String policyId,
            FinanceWeakPathReconciliationCandidateService.ReconciliationCandidate candidate,
            SemanticDraftService.SemanticDraft semanticDraft) {
        if (!StringUtils.hasText(runId) || !StringUtils.hasText(policyId) || candidate == null) {
            throw new IllegalArgumentException("runId, policyId and candidate are required");
        }
        try {
            FinanceWeakPathReconciliationCandidateSnapshot snapshot =
                    new FinanceWeakPathReconciliationCandidateSnapshot();
            snapshot.setRunId(runId.trim());
            snapshot.setPolicyId(policyId.trim());
            snapshot.setCandidateKey(candidate.candidateKey());
            snapshot.setFinalTier(candidate.finalTier());
            snapshot.setDomain(candidate.domain());
            snapshot.setResponseKind(candidate.responseKind());
            snapshot.setTarget(candidate.target());
            snapshot.setDataSurface(candidate.dataSurface());
            snapshot.setSourceCount(candidate.sourceCount());
            snapshot.setPriorityScore(candidate.priorityScore());
            snapshot.setSemanticDraftAction(candidate.semanticDraftAction());
            snapshot.setSemanticDraftId(semanticDraft == null ? "" : semanticDraft.draftId());
            snapshot.setSemanticDraftStatus(semanticDraft == null ? "" : semanticDraft.status());
            snapshot.setReason(candidate.reason());
            snapshot.setQuestionSamplesJson(objectMapper.writeValueAsString(candidate.questionSamples()));
            snapshot.setReconciliationSetsJson(objectMapper.writeValueAsString(candidate.reconciliationSets()));
            return repository.save(snapshot);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish finance weak path reconciliation candidate snapshot", e);
        }
    }

    public List<FinanceWeakPathReconciliationCandidateSnapshot> latest(String policyId) {
        if (!StringUtils.hasText(policyId)) {
            return List.of();
        }
        return repository.findTop20ByPolicyIdOrderByCreatedAtDescIdDesc(policyId.trim());
    }
}
