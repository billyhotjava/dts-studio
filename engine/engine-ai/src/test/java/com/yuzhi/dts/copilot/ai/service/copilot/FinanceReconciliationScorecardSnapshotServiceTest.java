package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.domain.FinanceReconciliationScorecardSnapshot;
import com.yuzhi.dts.copilot.ai.repository.FinanceReconciliationScorecardSnapshotRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinanceReconciliationScorecardSnapshotServiceTest {

    @Mock
    private FinanceReconciliationScorecardSnapshotRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void publishesScorecardSnapshotAndReadsLatestReportByOracleBinding() {
        FinanceReconciliationScorecardSnapshotService service =
                new FinanceReconciliationScorecardSnapshotService(repository, objectMapper);
        FinanceReconciliationScorecardService.ScorecardReport report = passingScorecard("PASS");
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.publish("month-settlement", "sprint33-finance-daily-scorecard", report);

        ArgumentCaptor<FinanceReconciliationScorecardSnapshot> captor =
                ArgumentCaptor.forClass(FinanceReconciliationScorecardSnapshot.class);
        verify(repository).save(captor.capture());
        FinanceReconciliationScorecardSnapshot saved = captor.getValue();
        assertThat(saved.getOracleBindingId()).isEqualTo("month-settlement");
        assertThat(saved.getScorecardId()).isEqualTo("sprint33-finance-daily-scorecard");
        assertThat(saved.getHealthStatus()).isEqualTo("PASS");
        assertThat(saved.getPassed()).isTrue();
        assertThat(saved.getPassRate()).isEqualByComparingTo("100.00");
        assertThat(saved.getReportJson()).contains("\"healthStatus\":\"PASS\"");

        when(repository.findFirstByOracleBindingIdOrderByCreatedAtDescIdDesc("month-settlement"))
                .thenReturn(Optional.of(saved));

        Optional<FinanceReconciliationScorecardService.ScorecardReport> latest =
                service.latestScorecard("month-settlement");

        assertThat(latest).isPresent();
        assertThat(latest.orElseThrow().healthStatus()).isEqualTo("PASS");
        assertThat(latest.orElseThrow().passed()).isTrue();
    }

    @Test
    void ignoresMalformedSnapshotJsonInsteadOfFabricatingScorecardStatus() {
        FinanceReconciliationScorecardSnapshotService service =
                new FinanceReconciliationScorecardSnapshotService(repository, objectMapper);
        FinanceReconciliationScorecardSnapshot snapshot = new FinanceReconciliationScorecardSnapshot();
        snapshot.setOracleBindingId("month-settlement");
        snapshot.setReportJson("{bad json");
        when(repository.findFirstByOracleBindingIdOrderByCreatedAtDescIdDesc("month-settlement"))
                .thenReturn(Optional.of(snapshot));

        assertThat(service.latestScorecard("month-settlement")).isEmpty();
    }

    private static FinanceReconciliationScorecardService.ScorecardReport passingScorecard(String status) {
        return new FinanceReconciliationScorecardService.ScorecardReport(
                true,
                false,
                status,
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
