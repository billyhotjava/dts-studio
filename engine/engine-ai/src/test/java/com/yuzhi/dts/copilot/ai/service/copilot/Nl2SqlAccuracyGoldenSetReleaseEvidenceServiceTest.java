package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class Nl2SqlAccuracyGoldenSetReleaseEvidenceServiceTest {

    private final Nl2SqlAccuracyGoldenSetReleaseEvidenceService service =
            new Nl2SqlAccuracyGoldenSetReleaseEvidenceService(
                    Clock.fixed(Instant.parse("2026-06-07T00:15:30Z"), ZoneOffset.UTC));

    @Test
    void publishesReleaseEvidenceForPassingScorecardWithRunTimeAndWeakPathSources() {
        Nl2SqlAccuracyGoldenSetScorecardService.ScorecardReport scorecard =
                new Nl2SqlAccuracyGoldenSetScorecardService.ScorecardReport(
                        true,
                        "PASS",
                        4,
                        4,
                        100.0d,
                        List.of());

        Nl2SqlAccuracyGoldenSetReleaseEvidenceService.ReleaseEvidence evidence = service.publish(
                "sprint34-local-gate",
                scorecard,
                List.of(new Nl2SqlAccuracyGoldenSetReleaseEvidenceService.WeakPathSource(
                        "finance-voucher-l0-profile",
                        "2026年凭证的数据统计下",
                        "L0_BUSINESS_OBJECT_PROFILE",
                        3,
                        "promote-to-golden-set")));

        assertThat(evidence.runId()).isEqualTo("sprint34-local-gate");
        assertThat(evidence.generatedAt()).isEqualTo(Instant.parse("2026-06-07T00:15:30Z"));
        assertThat(evidence.releaseGatePassed()).isTrue();
        assertThat(evidence.healthStatus()).isEqualTo("PASS");
        assertThat(evidence.totalCases()).isEqualTo(4);
        assertThat(evidence.passedCases()).isEqualTo(4);
        assertThat(evidence.passRate()).isEqualTo(100.0d);
        assertThat(evidence.weakPathSources())
                .extracting(Nl2SqlAccuracyGoldenSetReleaseEvidenceService.WeakPathSource::sourceId)
                .containsExactly("finance-voucher-l0-profile");

        assertThat(service.toMarkdown(evidence))
                .contains("sprint34-local-gate", "PASS", "2026-06-07T00:15:30Z", "finance-voucher-l0-profile")
                .doesNotContain("password", "jdbc:mysql:", "token");
    }

    @Test
    void blocksReleaseEvidenceWhenAnyGoldenCaseFailsAndKeepsFailureReasonsVisible() {
        Nl2SqlAccuracyGoldenSetScorecardService.ScorecardReport scorecard =
                new Nl2SqlAccuracyGoldenSetScorecardService.ScorecardReport(
                        false,
                        "FAIL",
                        4,
                        3,
                        75.0d,
                        List.of(new Nl2SqlAccuracyGoldenSetScorecardService.Failure(
                                "qa-finance-voucher-2026-main",
                                "golden case fell back to L0/profile path: L0_BUSINESS_OBJECT_PROFILE")));

        Nl2SqlAccuracyGoldenSetReleaseEvidenceService.ReleaseEvidence evidence =
                service.publish("sprint34-failing-gate", scorecard, List.of());

        assertThat(evidence.releaseGatePassed()).isFalse();
        assertThat(evidence.failureSummary())
                .contains("qa-finance-voucher-2026-main", "L0_BUSINESS_OBJECT_PROFILE");
        assertThat(service.toMarkdown(evidence))
                .contains("releaseGatePassed=false", "qa-finance-voucher-2026-main", "L0_BUSINESS_OBJECT_PROFILE");
    }

    @Test
    void redactsSecretsFromReleaseEvidenceMarkdown() {
        Nl2SqlAccuracyGoldenSetScorecardService.ScorecardReport scorecard =
                new Nl2SqlAccuracyGoldenSetScorecardService.ScorecardReport(
                        false,
                        "FAIL",
                        1,
                        0,
                        0.0d,
                        List.of(new Nl2SqlAccuracyGoldenSetScorecardService.Failure(
                                "qa-secret-leak",
                                "jdbc:mysql://db/app?password=secret&token=abc123")));

        Nl2SqlAccuracyGoldenSetReleaseEvidenceService.ReleaseEvidence evidence = service.publish(
                "sprint34-secret-redaction",
                scorecard,
                List.of(new Nl2SqlAccuracyGoldenSetReleaseEvidenceService.WeakPathSource(
                        "weak-secret",
                        "please use token=abc123 and password=secret",
                        "jdbc:mysql://db/app",
                        1,
                        "copy password=secret")));

        assertThat(service.toMarkdown(evidence))
                .doesNotContain("jdbc:mysql:", "password=secret", "token=abc123", "abc123");
    }

    @Test
    void doesNotFabricatePassWhenScorecardIsMissing() {
        Nl2SqlAccuracyGoldenSetReleaseEvidenceService.ReleaseEvidence evidence =
                service.publish("sprint34-missing-scorecard", null, List.of());

        assertThat(evidence.releaseGatePassed()).isFalse();
        assertThat(evidence.healthStatus()).isEqualTo("MISSING_SCORECARD");
        assertThat(evidence.failureSummary()).contains("scorecard report is missing");
    }
}
