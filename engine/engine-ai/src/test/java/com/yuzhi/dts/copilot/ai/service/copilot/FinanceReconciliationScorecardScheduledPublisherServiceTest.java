package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class FinanceReconciliationScorecardScheduledPublisherServiceTest {

    private static final String SCORECARD_ID = "sprint33-finance-daily-scorecard";

    private final FinanceReconciliationScorecardPublisherService publisher =
            mock(FinanceReconciliationScorecardPublisherService.class);

    @Test
    void scheduledRunSkipsWhenNoEvidenceProviderIsRegistered() {
        FinanceReconciliationScorecardScheduledPublisherService service =
                new FinanceReconciliationScorecardScheduledPublisherService(List.of(), publisher);

        FinanceReconciliationScorecardScheduledPublisherService.ScheduledPublishResult result =
                service.publishScheduledScorecards();

        assertThat(result.status()).isEqualTo("SKIPPED");
        assertThat(result.totalProviders()).isZero();
        assertThat(result.publishedCount()).isZero();
        assertThat(result.results()).isEmpty();
        assertThat(result.failureMessage()).contains("no finance scorecard evidence provider");
        verifyNoInteractions(publisher);
    }

    @Test
    void scheduledRunPublishesEachEvidenceProviderThroughPublisher() {
        List<FinanceReconciliationScorecardService.CheckRun> runs = passingRuns();
        List<FinanceReconciliationScorecardService.ReconciliationFailure> baseline = List.of();
        FinanceReconciliationScorecardEvidenceProvider provider = provider(
                "month-settlement",
                SCORECARD_ID,
                runs,
                baseline);
        FinanceReconciliationScorecardPublisherService.PublishResult publishResult =
                new FinanceReconciliationScorecardPublisherService.PublishResult(
                        true,
                        "month-settlement",
                        SCORECARD_ID,
                        "PASS",
                        List.of(),
                        passingScorecard(),
                        "");
        when(publisher.publishLatest(eq("month-settlement"), eq(SCORECARD_ID), eq(runs), eq(baseline)))
                .thenReturn(publishResult);
        FinanceReconciliationScorecardScheduledPublisherService service =
                new FinanceReconciliationScorecardScheduledPublisherService(List.of(provider), publisher);

        FinanceReconciliationScorecardScheduledPublisherService.ScheduledPublishResult result =
                service.publishScheduledScorecards();

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.totalProviders()).isEqualTo(1);
        assertThat(result.publishedCount()).isEqualTo(1);
        assertThat(result.results()).containsExactly(publishResult);
        verify(publisher).publishLatest(eq("month-settlement"), eq(SCORECARD_ID), eq(runs), eq(baseline));
    }

    private static FinanceReconciliationScorecardEvidenceProvider provider(
            String oracleBindingId,
            String scorecardId,
            List<FinanceReconciliationScorecardService.CheckRun> runs,
            List<FinanceReconciliationScorecardService.ReconciliationFailure> baseline) {
        return new FinanceReconciliationScorecardEvidenceProvider() {
            @Override
            public String oracleBindingId() {
                return oracleBindingId;
            }

            @Override
            public String scorecardId() {
                return scorecardId;
            }

            @Override
            public List<FinanceReconciliationScorecardService.CheckRun> currentRuns() {
                return runs;
            }

            @Override
            public List<FinanceReconciliationScorecardService.ReconciliationFailure> baselineFailures() {
                return baseline;
            }
        };
    }

    private static List<FinanceReconciliationScorecardService.CheckRun> passingRuns() {
        return List.of(
                check("f1-detail"),
                check("f2-summary-voucher"),
                check("f3-invariants"),
                check("f4-differential-grid"));
    }

    private static FinanceReconciliationScorecardService.CheckRun check(String category) {
        return new FinanceReconciliationScorecardService.CheckRun(
                category,
                category + "-check",
                category + " live evidence",
                true,
                1,
                0,
                BigDecimal.ZERO,
                List.of());
    }

    private static FinanceReconciliationScorecardService.ScorecardReport passingScorecard() {
        return new FinanceReconciliationScorecardService.ScorecardReport(
                true,
                false,
                "PASS",
                4,
                4,
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                List.of(),
                List.of(),
                List.of(),
                "");
    }
}
