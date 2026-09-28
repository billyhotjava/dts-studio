package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SemanticDraftGovernanceSubmissionServiceTest {

    @Test
    void mapsObjectIndicatorAndCaliberRuleDraftsToExistingGovernanceDraftApis() {
        FakeGovernanceDraftClient client = new FakeGovernanceDraftClient();
        SemanticDraftGovernanceSubmissionService service = new SemanticDraftGovernanceSubmissionService(client);
        SemanticDraftService drafts = new SemanticDraftService();

        SemanticDraftService.SemanticDraft objectDraft = drafts.createDraft(new SemanticDraftService.SemanticDraftRequest(
                "object",
                "finance",
                Map.of(
                        "objectCode", "finance.month_settlement",
                        "objectName", "月对账",
                        "sourceRefs", List.of("adminapi:MonthAccountController")),
                "缺少月对账业务对象",
                List.of("routeTier=TIER_5_DIRECT_DETAIL"),
                "automatic"));
        SemanticDraftService.SemanticDraft indicatorDraft = drafts.createDraft(new SemanticDraftService.SemanticDraftRequest(
                "indicator",
                "finance",
                Map.of(
                        "indicatorCode", "finance.discounted_receivable",
                        "indicatorName", "折后应收",
                        "formula", "sum(folding_after_total_amount)",
                        "objectCode", "finance.month_settlement"),
                "新增折后应收指标",
                List.of("weak-path-count=5"),
                "human"));
        SemanticDraftService.SemanticDraft caliberDraft = drafts.createDraft(new SemanticDraftService.SemanticDraftRequest(
                "caliber-rule",
                "finance",
                Map.of(
                        "ruleCode", "CAL-MONTH-AMOUNT-TIER",
                        "ruleName", "月对账金额层级",
                        "guardrailText", "区分名义租金、应收折前、折后实收和已回款"),
                "金额层级容易混淆",
                List.of("sprint31:F1-T03"),
                "automatic"));

        assertThat(service.submitDraft(objectDraft).governanceStatus()).isEqualTo("DRAFT_SUBMITTED");
        assertThat(service.submitDraft(indicatorDraft).governanceStatus()).isEqualTo("DRAFT_SUBMITTED");
        assertThat(service.submitDraft(caliberDraft).governanceStatus()).isEqualTo("DRAFT_SUBMITTED");

        assertThat(client.submissions).extracting(SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmission::targetPath)
                .containsExactly("/api/semantic/business-objects", "/api/governance/indicators", "/api/modeling/standards");
        assertThat(client.submissions.get(0).payload())
                .containsEntry("code", "finance.month_settlement")
                .containsEntry("name", "月对账")
                .containsEntry("status", "DRAFT");
        assertThat(String.valueOf(client.submissions.get(0).payload().get("description")))
                .contains("source=copilot", "draftId=", "triggerQuestion=缺少月对账业务对象");
        assertThat(client.submissions.get(1).payload())
                .containsEntry("code", "finance.discounted_receivable")
                .containsEntry("name", "折后应收")
                .containsEntry("domain", "S10-FIN")
                .containsEntry("expressionSql", "sum(folding_after_total_amount)")
                .containsEntry("status", "DRAFT")
                .containsEntry("llmGenerated", false)
                .containsEntry("humanVerified", false);
        assertThat(String.valueOf(client.submissions.get(1).payload().get("llmSourceRef")))
                .contains("source=copilot", "weak-path-count=5");
        assertThat(client.submissions.get(2).payload())
                .containsEntry("code", "CAL-MONTH-AMOUNT-TIER")
                .containsEntry("name", "月对账金额层级")
                .containsEntry("domain", "finance")
                .containsEntry("scope", "CALIBER_RULE")
                .containsEntry("status", "DRAFT")
                .containsEntry("versionStatus", "DRAFT");
    }

    @Test
    void refusesToSubmitRejectedOrMissingDraftsWithoutCallingGovernance() {
        FakeGovernanceDraftClient client = new FakeGovernanceDraftClient();
        SemanticDraftGovernanceSubmissionService service = new SemanticDraftGovernanceSubmissionService(client);
        SemanticDraftService drafts = new SemanticDraftService();
        SemanticDraftService.SemanticDraft rejectedDraft = drafts.createDraft(new SemanticDraftService.SemanticDraftRequest(
                "sql",
                "finance",
                Map.of("sql", "select 1"),
                "SQL 不是语义草稿",
                List.of(),
                "human"));

        SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmissionResult rejected = service.submitDraft(rejectedDraft);
        SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmissionResult missing = service.submitDraft(null);

        assertThat(rejected.submitted()).isFalse();
        assertThat(rejected.governanceStatus()).isEqualTo("NOT_SUBMITTED");
        assertThat(rejected.error()).contains("LOCAL_STAGED");
        assertThat(missing.submitted()).isFalse();
        assertThat(missing.error()).contains("draft is required");
        assertThat(client.submissions).isEmpty();
    }

    @Test
    void preservesNotSubmittedStatusWhenGovernanceCallFails() {
        FakeGovernanceDraftClient client = new FakeGovernanceDraftClient();
        client.fail = true;
        SemanticDraftGovernanceSubmissionService service = new SemanticDraftGovernanceSubmissionService(client);
        SemanticDraftService.SemanticDraft draft = new SemanticDraftService().createDraft(new SemanticDraftService.SemanticDraftRequest(
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

        SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmissionResult result = service.submitDraft(draft);

        assertThat(result.submitted()).isFalse();
        assertThat(result.governanceStatus()).isEqualTo("NOT_SUBMITTED");
        assertThat(result.error()).contains("platform unavailable");
    }

    private static final class FakeGovernanceDraftClient implements SemanticDraftGovernanceSubmissionService.GovernanceDraftClient {
        private final List<SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmission> submissions = new ArrayList<>();
        private boolean fail;

        @Override
        public SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmissionResult submit(
                SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmission submission) {
            if (fail) {
                throw new IllegalStateException("platform unavailable");
            }
            submissions.add(submission);
            return new SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmissionResult(
                    submission.draftId(),
                    submission.targetType(),
                    submission.targetPath(),
                    "platform-" + submissions.size(),
                    "DRAFT",
                    "DRAFT_SUBMITTED",
                    true,
                    "");
        }
    }
}
