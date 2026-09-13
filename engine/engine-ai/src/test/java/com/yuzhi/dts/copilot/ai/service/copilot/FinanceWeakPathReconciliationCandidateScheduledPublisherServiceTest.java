package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yuzhi.dts.copilot.ai.service.chat.RouteTelemetryService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FinanceWeakPathReconciliationCandidateScheduledPublisherServiceTest {

    @Test
    void scheduledRunPublishesDefaultFinanceWeakPathPolicyFromTelemetry() {
        FinanceWeakPathReconciliationCandidatePublisherService publisher =
                mock(FinanceWeakPathReconciliationCandidatePublisherService.class);
        FinanceWeakPathReconciliationCandidatePublisherService.PublishResult publishResult =
                new FinanceWeakPathReconciliationCandidatePublisherService.PublishResult(
                        "PUBLISHED",
                        "sprint33-finance-weak-path-reconciliation",
                        "run-1",
                        new RouteTelemetryService.RouteTelemetrySummary(7, 3, Map.of("TIER_5_DIRECT_DETAIL", 3L), List.of()),
                        List.of(candidate()),
                        List.of(mock(com.yuzhi.dts.copilot.ai.domain.FinanceWeakPathReconciliationCandidateSnapshot.class)),
                        List.of(semanticDraft()));
        when(publisher.publishFromTelemetry("sprint33-finance-weak-path-reconciliation", 7, 10))
                .thenReturn(publishResult);
        FinanceWeakPathReconciliationCandidateScheduledPublisherService service =
                new FinanceWeakPathReconciliationCandidateScheduledPublisherService(
                        publisher,
                        "sprint33-finance-weak-path-reconciliation",
                        7,
                        10);

        FinanceWeakPathReconciliationCandidateScheduledPublisherService.ScheduledPublishResult result =
                service.publishScheduledCandidates();

        assertThat(result.status()).isEqualTo("PUBLISHED");
        assertThat(result.policyId()).isEqualTo("sprint33-finance-weak-path-reconciliation");
        assertThat(result.snapshotCount()).isEqualTo(1);
        assertThat(result.semanticDraftCount()).isEqualTo(1);
        assertThat(result.publishResult()).isEqualTo(publishResult);
        verify(publisher).publishFromTelemetry("sprint33-finance-weak-path-reconciliation", 7, 10);
    }

    private static FinanceWeakPathReconciliationCandidateService.ReconciliationCandidate candidate() {
        return new FinanceWeakPathReconciliationCandidateService.ReconciliationCandidate(
                "TIER_5_DIRECT_DETAIL|flowerbiz|business-object:prs.flower.finance.month_accounting",
                "TIER_5_DIRECT_DETAIL",
                "flowerbiz",
                "BUSINESS_INSIGHT",
                "business-object:prs.flower.finance.month_accounting",
                "ods_ptr_mysql_a_month_accounting",
                3,
                15,
                List.of("6月月对账折后实收为什么不一致"),
                List.of("f1-detail", "f3-invariant-seed", "f4-differential-grid"),
                "CREATE_SPRINT31_DRAFT",
                "frequent finance weak path");
    }

    private static SemanticDraftService.SemanticDraft semanticDraft() {
        return new SemanticDraftService.SemanticDraft(
                "semantic-draft-1",
                "caliber-rule",
                "flowerbiz",
                Map.of(
                        "ruleCode", "CAL-WEAK-PATH-1",
                        "ruleName", "弱路径财务问题对账候选",
                        "guardrailText", "frequent finance weak path"),
                "6月月对账折后实收为什么不一致",
                List.of("routeTier=TIER_5_DIRECT_DETAIL"),
                "automatic",
                "LOCAL_STAGED",
                "NOT_SUBMITTED",
                "SUBMIT_TO_GOVERNANCE_DRAFT",
                List.of("ruleCode", "ruleName", "guardrailText"),
                List.of(),
                false,
                false,
                "2026-06-07T00:00:00Z",
                "",
                "",
                "",
                "",
                "");
    }
}
