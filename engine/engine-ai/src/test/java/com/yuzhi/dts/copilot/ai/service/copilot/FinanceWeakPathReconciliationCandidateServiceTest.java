package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.service.chat.RouteTelemetryService;
import java.util.List;
import org.junit.jupiter.api.Test;

class FinanceWeakPathReconciliationCandidateServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldLoadWeakPathCandidatePolicyFromGovernanceAsset() {
        FinanceWeakPathReconciliationCandidateRegistry registry =
                new FinanceWeakPathReconciliationCandidateRegistry(objectMapper);
        registry.init();

        FinanceWeakPathReconciliationCandidateRegistry.WeakPathCandidatePolicy policy =
                registry.policy("sprint33-finance-weak-path-reconciliation").orElseThrow();

        assertThat(policy.financeDomains()).containsExactly("finance", "flowerbiz");
        assertThat(policy.weakTiers()).containsExactly("TIER_4_GUARDRAIL_FEDERATED", "TIER_5_DIRECT_DETAIL");
        assertThat(policy.minCount()).isEqualTo(2);
        assertThat(policy.semanticDraftThreshold()).isEqualTo(3);
        assertThat(policy.reconciliationSets()).containsExactly("f1-detail", "f3-invariant-seed", "f4-differential-grid");
        assertThat(policy.itScript()).isEqualTo("worklog/v1.0.0/sprint-33-202607/it/test_f4_weak_path_reconciliation_candidates.sh");
    }

    @Test
    void shouldSelectFrequentFinanceWeakPathTelemetryAsReconciliationCandidates() {
        FinanceWeakPathReconciliationCandidateService service =
                new FinanceWeakPathReconciliationCandidateService();
        FinanceWeakPathReconciliationCandidateService.WeakPathCandidateSpec spec = spec();
        RouteTelemetryService.RouteTelemetrySummary summary = new RouteTelemetryService.RouteTelemetrySummary(
                7,
                8,
                java.util.Map.of("TIER_5_DIRECT_DETAIL", 5L, "TIER_2_MART_TEMPLATE", 3L),
                List.of(
                        signal(
                                "TIER_5_DIRECT_DETAIL",
                                "flowerbiz",
                                "BUSINESS_INSIGHT",
                                "business-object:prs.flower.finance.month_accounting",
                                "ods_ptr_mysql_a_month_accounting",
                                3,
                                "6月月对账折后实收为什么不一致"),
                        signal(
                                "TIER_4_GUARDRAIL_FEDERATED",
                                "warehouse",
                                "BUSINESS_INSIGHT",
                                "business-object:prs.warehouse.stock_info",
                                "ods_ptr_mysql_s_stock_info",
                                4,
                                "低库存预警"),
                        signal(
                                "TIER_2_MART_TEMPLATE",
                                "flowerbiz",
                                "FIXED_REPORT",
                                "public.xycyl_ads_flowerbiz_lease_summary",
                                "ads",
                                5,
                                "月对账折后实收"),
                        signal(
                                "TIER_5_DIRECT_DETAIL",
                                "flowerbiz",
                                "BUSINESS_INSIGHT",
                                "business-object:prs.flower.finance.voucher",
                                "ods_ptr_mysql_a_voucher",
                                1,
                                "凭证借贷差异")));

        List<FinanceWeakPathReconciliationCandidateService.ReconciliationCandidate> candidates =
                service.selectCandidates(spec, summary);

        assertThat(candidates).hasSize(1);
        FinanceWeakPathReconciliationCandidateService.ReconciliationCandidate candidate = candidates.getFirst();
        assertThat(candidate.candidateKey())
                .contains("TIER_5_DIRECT_DETAIL", "flowerbiz", "business-object:prs.flower.finance.month_accounting");
        assertThat(candidate.sourceCount()).isEqualTo(3);
        assertThat(candidate.priorityScore()).isEqualTo(15);
        assertThat(candidate.reconciliationSets()).containsExactly("f1-detail", "f3-invariant-seed", "f4-differential-grid");
        assertThat(candidate.semanticDraftAction()).isEqualTo("CREATE_SPRINT31_DRAFT");
        assertThat(candidate.questionSamples()).containsExactly("6月月对账折后实收为什么不一致");
    }

    @Test
    void shouldTurnUncoveredWeakPathCandidatesIntoScorecardDriftUntilCovered() {
        FinanceWeakPathReconciliationCandidateService service =
                new FinanceWeakPathReconciliationCandidateService();
        FinanceWeakPathReconciliationCandidateService.ReconciliationCandidate candidate =
                candidate("TIER_5_DIRECT_DETAIL|flowerbiz|business-object:prs.flower.finance.month_accounting");

        FinanceReconciliationScorecardService.CheckRun uncovered = service.toScorecardCheck(
                "f4-differential-grid",
                "telemetry-weak-path-candidates",
                List.of(candidate),
                List.of());

        assertThat(uncovered.passed()).isFalse();
        assertThat(uncovered.totalCells()).isEqualTo(1);
        assertThat(uncovered.failedCells()).isEqualTo(1);
        assertThat(uncovered.failures()).hasSize(1);
        assertThat(uncovered.failures().getFirst().status()).isEqualTo("weak path candidate not covered");

        FinanceReconciliationScorecardService.CheckRun covered = service.toScorecardCheck(
                "f4-differential-grid",
                "telemetry-weak-path-candidates",
                List.of(candidate),
                List.of(candidate.candidateKey()));

        assertThat(covered.passed()).isTrue();
        assertThat(covered.failedCells()).isZero();
        assertThat(covered.failures()).isEmpty();
    }

    @Test
    void shouldFailPolicyWithDuplicateFinanceDomainsOrMissingWeakTier() {
        FinanceWeakPathReconciliationCandidateService service =
                new FinanceWeakPathReconciliationCandidateService();

        FinanceWeakPathReconciliationCandidateService.WeakPathCandidateSpec duplicateDomain =
                new FinanceWeakPathReconciliationCandidateService.WeakPathCandidateSpec(
                        "bad-policy",
                        List.of("finance", "finance"),
                        List.of("TIER_5_DIRECT_DETAIL"),
                        List.of("月对账"),
                        1,
                        2,
                        List.of("f4-differential-grid"));

        assertThat(service.selectCandidates(duplicateDomain, emptySummary()))
                .singleElement()
                .extracting(FinanceWeakPathReconciliationCandidateService.ReconciliationCandidate::reason)
                .asString()
                .contains("duplicate finance domain");

        FinanceWeakPathReconciliationCandidateService.WeakPathCandidateSpec missingWeakTier =
                new FinanceWeakPathReconciliationCandidateService.WeakPathCandidateSpec(
                        "bad-policy",
                        List.of("finance"),
                        List.of(),
                        List.of("月对账"),
                        1,
                        2,
                        List.of("f4-differential-grid"));

        assertThat(service.selectCandidates(missingWeakTier, emptySummary()))
                .singleElement()
                .extracting(FinanceWeakPathReconciliationCandidateService.ReconciliationCandidate::reason)
                .asString()
                .contains("missing weak tier");
    }

    private static FinanceWeakPathReconciliationCandidateService.WeakPathCandidateSpec spec() {
        return new FinanceWeakPathReconciliationCandidateService.WeakPathCandidateSpec(
                "sprint33-finance-weak-path-reconciliation",
                List.of("finance", "flowerbiz"),
                List.of("TIER_4_GUARDRAIL_FEDERATED", "TIER_5_DIRECT_DETAIL"),
                List.of("财务", "月对账", "折后实收", "凭证", "应收", "回款", "坏账", "finance", "voucher", "receivable"),
                2,
                3,
                List.of("f1-detail", "f3-invariant-seed", "f4-differential-grid"));
    }

    private static RouteTelemetryService.MartCandidateSignal signal(
            String finalTier,
            String domain,
            String responseKind,
            String target,
            String dataSurface,
            long count,
            String question) {
        return new RouteTelemetryService.MartCandidateSignal(
                finalTier,
                domain,
                responseKind,
                target,
                dataSurface,
                count,
                List.of(question));
    }

    private static RouteTelemetryService.RouteTelemetrySummary emptySummary() {
        return new RouteTelemetryService.RouteTelemetrySummary(7, 0, java.util.Map.of(), List.of());
    }

    private static FinanceWeakPathReconciliationCandidateService.ReconciliationCandidate candidate(String key) {
        return new FinanceWeakPathReconciliationCandidateService.ReconciliationCandidate(
                key,
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
}
