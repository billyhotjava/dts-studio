package com.yuzhi.dts.copilot.ai.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.copilot.ai.service.copilot.SemanticDraftGovernanceSubmissionService;
import com.yuzhi.dts.copilot.ai.service.copilot.SemanticDraftService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class SemanticDraftResourceTest {

    @Test
    void exposesSemanticDraftEndpointUnderApiCopilotSemanticDrafts() throws Exception {
        RequestMapping mapping = SemanticDraftResource.class.getAnnotation(RequestMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/api/copilot/semantic-drafts");
        assertThat(SemanticDraftResource.class.getDeclaredMethod("create", SemanticDraftResource.CreateSemanticDraftRequest.class)
                .getAnnotation(PostMapping.class)).isNotNull();
        assertThat(SemanticDraftResource.class.getDeclaredMethod("submit", String.class)
                .getAnnotation(PostMapping.class).value()).containsExactly("/{draftId}/submit");
    }

    @Test
    void createsLocalStagedSemanticDraftResponse() {
        SemanticDraftResource resource = newResource(new SemanticDraftService(), new FakeGovernanceDraftClient());

        ResponseEntity<Map<String, Object>> response = resource.create(new SemanticDraftResource.CreateSemanticDraftRequest(
                "indicator",
                "finance",
                Map.of(
                        "indicatorCode", "finance.discounted_receivable",
                        "indicatorName", "折后应收",
                        "formula", "sum(folding_after_total_amount)",
                        "objectCode", "finance.month_settlement"),
                "新增折后应收指标",
                List.of("weak-path-count=5"),
                "automatic"));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body)
                .containsEntry("draftType", "indicator")
                .containsEntry("domain", "finance")
                .containsEntry("status", "LOCAL_STAGED")
                .containsEntry("governanceStatus", "NOT_SUBMITTED")
                .containsEntry("nextAction", "SUBMIT_TO_GOVERNANCE_DRAFT")
                .containsEntry("sotTouched", false)
                .containsEntry("businessDatabaseTouched", false);
        assertThat(String.valueOf(body.get("draftId"))).startsWith("semantic-draft-");
    }

    @Test
    void rejectsNullRequiredContentWithoutThrowingBeforeValidation() {
        SemanticDraftResource resource = newResource(new SemanticDraftService(), new FakeGovernanceDraftClient());
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("indicatorCode", "finance.discounted_receivable");
        content.put("indicatorName", null);
        content.put("formula", "");
        content.put("objectCode", "finance.month_settlement");

        ResponseEntity<Map<String, Object>> response = resource.create(new SemanticDraftResource.CreateSemanticDraftRequest(
                "indicator",
                "finance",
                content,
                "缺少名称和公式",
                List.of(),
                "automatic"));

        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).containsEntry("status", "REJECTED");
        assertThat(body.get("validationErrors")).asList().contains(
                "missing required content key: indicatorName",
                "missing required content key: formula");
    }

    @Test
    void submitsLocalStagedDraftToGovernanceDraftFlow() {
        SemanticDraftService draftService = new SemanticDraftService();
        FakeGovernanceDraftClient client = new FakeGovernanceDraftClient();
        SemanticDraftResource resource = newResource(draftService, client);
        ResponseEntity<Map<String, Object>> createResponse = resource.create(new SemanticDraftResource.CreateSemanticDraftRequest(
                "indicator",
                "finance",
                Map.of(
                        "indicatorCode", "finance.discounted_receivable",
                        "indicatorName", "折后应收",
                        "formula", "sum(folding_after_total_amount)",
                        "objectCode", "finance.month_settlement"),
                "新增折后应收指标",
                List.of("weak-path-count=5"),
                "automatic"));
        String draftId = String.valueOf(createResponse.getBody().get("draftId"));

        ResponseEntity<Map<String, Object>> submitResponse = resource.submit(draftId);

        assertThat(submitResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(submitResponse.getBody())
                .containsEntry("draftId", draftId)
                .containsEntry("targetPath", "/api/governance/indicators")
                .containsEntry("platformStatus", "DRAFT")
                .containsEntry("governanceStatus", "DRAFT_SUBMITTED")
                .containsEntry("submitted", true);
        assertThat(client.submission.payload()).containsEntry("status", "DRAFT");
        SemanticDraftService.SemanticDraft updatedDraft = draftService.findDraft(draftId).orElseThrow();
        assertThat(updatedDraft.governanceStatus()).isEqualTo("DRAFT_SUBMITTED");
        assertThat(updatedDraft.nextAction()).isEqualTo("WAIT_FOR_GOVERNANCE_REVIEW");
        assertThat(updatedDraft.platformDraftId()).isEqualTo("platform-indicator-1");
        assertThat(updatedDraft.platformDraftStatus()).isEqualTo("DRAFT");
    }

    private static SemanticDraftResource newResource(SemanticDraftService draftService, FakeGovernanceDraftClient client) {
        return new SemanticDraftResource(draftService, new SemanticDraftGovernanceSubmissionService(client));
    }

    private static final class FakeGovernanceDraftClient implements SemanticDraftGovernanceSubmissionService.GovernanceDraftClient {
        private SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmission submission;

        @Override
        public SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmissionResult submit(
                SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmission submission) {
            this.submission = submission;
            return new SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmissionResult(
                    submission.draftId(),
                    submission.targetType(),
                    submission.targetPath(),
                    "platform-indicator-1",
                    "DRAFT",
                    "DRAFT_SUBMITTED",
                    true,
                    "");
        }
    }
}
