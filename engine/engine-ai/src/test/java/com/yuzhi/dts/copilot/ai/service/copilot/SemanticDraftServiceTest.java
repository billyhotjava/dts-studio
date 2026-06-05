package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SemanticDraftServiceTest {

    @Test
    void createsLocalStagedObjectIndicatorAndCaliberRuleDraftsWithoutTouchingSot() {
        SemanticDraftService service = new SemanticDraftService();

        SemanticDraftService.SemanticDraft objectDraft = service.createDraft(new SemanticDraftService.SemanticDraftRequest(
                "object",
                "finance",
                Map.of(
                        "objectCode", "finance.month_settlement",
                        "objectName", "月对账",
                        "sourceRefs", List.of("adminapi:MonthAccountController", "adminweb:monthAccount.js")),
                "月对账折后实收为什么没有定义对象?",
                List.of("routeTier=TIER_5_DIRECT_DETAIL"),
                "automatic"));
        SemanticDraftService.SemanticDraft indicatorDraft = service.createDraft(new SemanticDraftService.SemanticDraftRequest(
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
        SemanticDraftService.SemanticDraft caliberDraft = service.createDraft(new SemanticDraftService.SemanticDraftRequest(
                "caliber-rule",
                "finance",
                Map.of(
                        "ruleCode", "CAL-MONTH-AMOUNT-TIER",
                        "ruleName", "月对账金额层级",
                        "guardrailText", "区分名义租金、应收折前、折后实收和已回款"),
                "金额层级容易混淆",
                List.of("sprint31:F1-T03"),
                "automatic"));

        assertThat(objectDraft.draftId()).startsWith("semantic-draft-");
        assertThat(objectDraft.status()).isEqualTo("LOCAL_STAGED");
        assertThat(objectDraft.governanceStatus()).isEqualTo("NOT_SUBMITTED");
        assertThat(objectDraft.nextAction()).isEqualTo("SUBMIT_TO_GOVERNANCE_DRAFT");
        assertThat(objectDraft.sotTouched()).isFalse();
        assertThat(objectDraft.businessDatabaseTouched()).isFalse();
        assertThat(objectDraft.requiredContentKeys()).containsExactly("objectCode", "objectName", "sourceRefs");
        assertThat(indicatorDraft.requiredContentKeys()).containsExactly("indicatorCode", "indicatorName", "formula", "objectCode");
        assertThat(caliberDraft.requiredContentKeys()).containsExactly("ruleCode", "ruleName", "guardrailText");
        assertThat(service.listDrafts()).extracting(SemanticDraftService.SemanticDraft::draftType)
                .containsExactly("object", "indicator", "caliber-rule");
    }

    @Test
    void rejectsDraftWhenTypeOrRequiredContentIsMissing() {
        SemanticDraftService service = new SemanticDraftService();

        SemanticDraftService.SemanticDraft invalidType = service.createDraft(new SemanticDraftService.SemanticDraftRequest(
                "sql",
                "finance",
                Map.of("sql", "select 1"),
                "不要把 SQL 草稿当语义草稿",
                List.of(),
                "human"));
        SemanticDraftService.SemanticDraft missingContent = service.createDraft(new SemanticDraftService.SemanticDraftRequest(
                "indicator",
                "finance",
                Map.of("indicatorCode", "finance.discounted_receivable"),
                "缺少名称和公式",
                List.of(),
                "automatic"));

        assertThat(invalidType.status()).isEqualTo("REJECTED");
        assertThat(invalidType.validationErrors()).contains("unsupported draft type: sql");
        assertThat(invalidType.sotTouched()).isFalse();
        assertThat(missingContent.status()).isEqualTo("REJECTED");
        assertThat(missingContent.validationErrors()).contains(
                "missing required content key: indicatorName",
                "missing required content key: formula",
                "missing required content key: objectCode");
        assertThat(service.listDrafts()).isEmpty();
    }

    @Test
    void recordsGovernanceDraftSubmissionOnLocalStagedDraft() {
        SemanticDraftService service = new SemanticDraftService();
        SemanticDraftService.SemanticDraft draft = service.createDraft(new SemanticDraftService.SemanticDraftRequest(
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
        SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmissionResult submission =
                new SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmissionResult(
                        draft.draftId(),
                        "GOVERNANCE_INDICATOR",
                        "/api/governance/indicators",
                        "platform-indicator-1",
                        "DRAFT",
                        "DRAFT_SUBMITTED",
                        true,
                        "");

        SemanticDraftService.SemanticDraft updated = service.recordGovernanceSubmission(submission);

        assertThat(updated.governanceStatus()).isEqualTo("DRAFT_SUBMITTED");
        assertThat(updated.nextAction()).isEqualTo("WAIT_FOR_GOVERNANCE_REVIEW");
        assertThat(updated.governanceTargetType()).isEqualTo("GOVERNANCE_INDICATOR");
        assertThat(updated.governanceTargetPath()).isEqualTo("/api/governance/indicators");
        assertThat(updated.platformDraftId()).isEqualTo("platform-indicator-1");
        assertThat(updated.platformDraftStatus()).isEqualTo("DRAFT");
        assertThat(updated.governanceSubmittedAt()).isNotBlank();
        assertThat(service.findDraft(draft.draftId())).contains(updated);
    }

    @Test
    void keepsLocalDraftNotSubmittedWhenGovernanceSubmissionFails() {
        SemanticDraftService service = new SemanticDraftService();
        SemanticDraftService.SemanticDraft draft = service.createDraft(new SemanticDraftService.SemanticDraftRequest(
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
        SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmissionResult failed =
                new SemanticDraftGovernanceSubmissionService.GovernanceDraftSubmissionResult(
                        draft.draftId(),
                        "GOVERNANCE_INDICATOR",
                        "/api/governance/indicators",
                        "",
                        "",
                        "NOT_SUBMITTED",
                        false,
                        "platform unavailable");

        SemanticDraftService.SemanticDraft unchanged = service.recordGovernanceSubmission(failed);

        assertThat(unchanged.governanceStatus()).isEqualTo("NOT_SUBMITTED");
        assertThat(unchanged.nextAction()).isEqualTo("SUBMIT_TO_GOVERNANCE_DRAFT");
        assertThat(unchanged.platformDraftId()).isBlank();
        assertThat(service.findDraft(draft.draftId())).contains(unchanged);
    }
}
