package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.domain.FinanceWeakPathReconciliationCandidateSnapshot;
import com.yuzhi.dts.copilot.ai.repository.FinanceWeakPathReconciliationCandidateSnapshotRepository;
import com.yuzhi.dts.copilot.ai.service.chat.RouteTelemetryService;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class FinanceWeakPathReconciliationCandidatePublisherServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void publishesLiveTelemetryCandidatesToSnapshotStoreAndLocalSemanticDrafts() {
        RouteTelemetryService routeTelemetryService = mock(RouteTelemetryService.class);
        when(routeTelemetryService.summarize(7, 10)).thenReturn(new RouteTelemetryService.RouteTelemetrySummary(
                7,
                3,
                java.util.Map.of("TIER_5_DIRECT_DETAIL", 3L),
                List.of(new RouteTelemetryService.MartCandidateSignal(
                        "TIER_5_DIRECT_DETAIL",
                        "flowerbiz",
                        "BUSINESS_INSIGHT",
                        "business-object:prs.flower.finance.month_accounting",
                        "ods_ptr_mysql_a_month_accounting",
                        3,
                        List.of("6月月对账折后实收为什么不一致")))));

        FinanceWeakPathReconciliationCandidateSnapshotRepository repository =
                mock(FinanceWeakPathReconciliationCandidateSnapshotRepository.class);
        AtomicReference<FinanceWeakPathReconciliationCandidateSnapshot> savedSnapshot = new AtomicReference<>();
        when(repository.save(any(FinanceWeakPathReconciliationCandidateSnapshot.class))).thenAnswer(invocation -> {
            FinanceWeakPathReconciliationCandidateSnapshot snapshot = invocation.getArgument(0);
            snapshot.setId(1L);
            savedSnapshot.set(snapshot);
            return snapshot;
        });

        FinanceWeakPathReconciliationCandidateRegistry registry =
                new FinanceWeakPathReconciliationCandidateRegistry(objectMapper);
        registry.init();
        SemanticDraftService semanticDraftService = new SemanticDraftService();
        FinanceWeakPathReconciliationCandidatePublisherService publisher =
                new FinanceWeakPathReconciliationCandidatePublisherService(
                        routeTelemetryService,
                        registry,
                        new FinanceWeakPathReconciliationCandidateService(),
                        new FinanceWeakPathReconciliationCandidateSnapshotService(repository, objectMapper),
                        semanticDraftService);

        FinanceWeakPathReconciliationCandidatePublisherService.PublishResult result =
                publisher.publishFromTelemetry("sprint33-finance-weak-path-reconciliation", 7, 10);

        assertThat(result.status()).isEqualTo("PUBLISHED");
        assertThat(result.summary().totalMessages()).isEqualTo(3);
        assertThat(result.snapshots()).hasSize(1);
        assertThat(result.semanticDrafts()).hasSize(1);
        assertThat(result.semanticDrafts().getFirst().draftType()).isEqualTo("caliber-rule");
        assertThat(result.semanticDrafts().getFirst().status()).isEqualTo("LOCAL_STAGED");
        assertThat(result.semanticDrafts().getFirst().content())
                .containsEntry("ruleName", "弱路径财务问题对账候选")
                .containsEntry("guardrailText", "frequent finance weak path");

        FinanceWeakPathReconciliationCandidateSnapshot snapshot = savedSnapshot.get();
        assertThat(snapshot.getPolicyId()).isEqualTo("sprint33-finance-weak-path-reconciliation");
        assertThat(snapshot.getCandidateKey())
                .isEqualTo("TIER_5_DIRECT_DETAIL|flowerbiz|business-object:prs.flower.finance.month_accounting");
        assertThat(snapshot.getSourceCount()).isEqualTo(3L);
        assertThat(snapshot.getPriorityScore()).isEqualTo(15);
        assertThat(snapshot.getSemanticDraftAction()).isEqualTo("CREATE_SPRINT31_DRAFT");
        assertThat(snapshot.getSemanticDraftId()).isEqualTo(result.semanticDrafts().getFirst().draftId());
        assertThat(snapshot.getQuestionSamplesJson()).contains("6月月对账折后实收为什么不一致");
        assertThat(snapshot.getReconciliationSetsJson()).contains("f4-differential-grid");
        verify(routeTelemetryService).summarize(7, 10);
    }

    @Test
    void reportsMissingPolicyWithoutPublishingSnapshots() {
        FinanceWeakPathReconciliationCandidateSnapshotRepository repository =
                mock(FinanceWeakPathReconciliationCandidateSnapshotRepository.class);
        FinanceWeakPathReconciliationCandidateRegistry registry =
                new FinanceWeakPathReconciliationCandidateRegistry(objectMapper);
        registry.init();
        FinanceWeakPathReconciliationCandidatePublisherService publisher =
                new FinanceWeakPathReconciliationCandidatePublisherService(
                        mock(RouteTelemetryService.class),
                        registry,
                        new FinanceWeakPathReconciliationCandidateService(),
                        new FinanceWeakPathReconciliationCandidateSnapshotService(repository, objectMapper),
                        new SemanticDraftService());

        FinanceWeakPathReconciliationCandidatePublisherService.PublishResult result =
                publisher.publishFromTelemetry("missing-policy", 7, 10);

        assertThat(result.status()).isEqualTo("MISSING_POLICY");
        assertThat(result.snapshots()).isEmpty();
        assertThat(result.semanticDrafts()).isEmpty();
    }
}
