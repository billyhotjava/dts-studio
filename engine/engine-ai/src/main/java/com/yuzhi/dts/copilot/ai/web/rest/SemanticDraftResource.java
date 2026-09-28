package com.yuzhi.dts.copilot.ai.web.rest;

import com.yuzhi.dts.copilot.ai.service.copilot.SemanticDraftGovernanceSubmissionService;
import com.yuzhi.dts.copilot.ai.service.copilot.SemanticDraftService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/copilot/semantic-drafts")
public class SemanticDraftResource {

    private final SemanticDraftService semanticDraftService;
    private final SemanticDraftGovernanceSubmissionService governanceSubmissionService;

    public SemanticDraftResource(
            SemanticDraftService semanticDraftService,
            SemanticDraftGovernanceSubmissionService governanceSubmissionService) {
        this.semanticDraftService = semanticDraftService;
        this.governanceSubmissionService = governanceSubmissionService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody(required = false) CreateSemanticDraftRequest request) {
        SemanticDraftService.SemanticDraft draft = semanticDraftService.createDraft(new SemanticDraftService.SemanticDraftRequest(
                request == null ? "" : request.draftType(),
                request == null ? "" : request.domain(),
                request == null ? Map.of() : request.content(),
                request == null ? "" : request.triggerQuestion(),
                request == null ? List.of() : request.evidence(),
                request == null ? "" : request.source()));
        return ResponseEntity.ok(toResponse(draft));
    }

    @PostMapping("/{draftId}/submit")
    public ResponseEntity<Map<String, Object>> submit(@PathVariable String draftId) {
        return semanticDraftService.findDraft(draftId)
                .map(draft -> {
                    SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmissionResult result =
                            governanceSubmissionService.submitDraft(draft);
                    semanticDraftService.recordGovernanceSubmission(result);
                    return ResponseEntity.ok(toSubmissionResponse(result));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "draftId", draftId == null ? "" : draftId,
                        "governanceStatus", "NOT_SUBMITTED",
                        "submitted", false,
                        "error", "semantic draft not found")));
    }

    private static Map<String, Object> toResponse(SemanticDraftService.SemanticDraft draft) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("draftId", draft.draftId());
        response.put("draftType", draft.draftType());
        response.put("domain", draft.domain());
        response.put("content", draft.content());
        response.put("triggerQuestion", draft.triggerQuestion());
        response.put("evidence", draft.evidence());
        response.put("source", draft.source());
        response.put("status", draft.status());
        response.put("governanceStatus", draft.governanceStatus());
        response.put("nextAction", draft.nextAction());
        response.put("requiredContentKeys", draft.requiredContentKeys());
        response.put("validationErrors", draft.validationErrors());
        response.put("sotTouched", draft.sotTouched());
        response.put("businessDatabaseTouched", draft.businessDatabaseTouched());
        response.put("createdAt", draft.createdAt());
        response.put("governanceTargetType", draft.governanceTargetType());
        response.put("governanceTargetPath", draft.governanceTargetPath());
        response.put("platformDraftId", draft.platformDraftId());
        response.put("platformDraftStatus", draft.platformDraftStatus());
        response.put("governanceSubmittedAt", draft.governanceSubmittedAt());
        return response;
    }

    private static Map<String, Object> toSubmissionResponse(
            SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmissionResult result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("draftId", result.draftId());
        response.put("targetType", result.targetType());
        response.put("targetPath", result.targetPath());
        response.put("platformId", result.platformId());
        response.put("platformStatus", result.platformStatus());
        response.put("governanceStatus", result.governanceStatus());
        response.put("submitted", result.submitted());
        response.put("error", result.error());
        return response;
    }

    public record CreateSemanticDraftRequest(
            String draftType,
            String domain,
            Map<String, Object> content,
            String triggerQuestion,
            List<String> evidence,
            String source) {
        public CreateSemanticDraftRequest {
            content = content == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(content));
            evidence = evidence == null ? List.of() : List.copyOf(evidence);
        }
    }
}
