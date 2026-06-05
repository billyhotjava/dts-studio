package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class CaliberCrossSourceRegressionServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldPassCurrentCrossSourceRegressionFixture() {
        CaliberCrossSourceRegressionService service = service();

        CaliberCrossSourceRegressionService.RegressionReport report = service.runDefault();

        assertThat(report.passed()).isTrue();
        assertThat(report.drifts()).isEmpty();
        assertThat(report.domainReports()).extracting(CaliberCrossSourceRegressionService.DomainReport::domain)
                .containsExactly("finance", "procurement");

        CaliberCrossSourceRegressionService.RuleReport monthAmountTier =
                rule(report, "finance", "CAL-MONTH-AMOUNT-TIER");
        assertThat(monthAmountTier.packEvidence()).contains("semantic-pack:finance:generatedGuardrails");
        assertThat(monthAmountTier.dbtEvidence())
                .anySatisfy(evidence -> assertThat(evidence)
                        .contains("dbt:worklog/v1.0.0/sprint-30-202606/assets/finance-mart-catalog.md")
                        .contains("名义租金")
                        .contains("折后实收")
                        .contains("已回款"));
        assertThat(monthAmountTier.glossaryEvidence())
                .contains("openmetadata:glossary.finance.month_amount_tier");
    }

    @Test
    void shouldReportMissingSourceEvidenceAsDrift() {
        CaliberCrossSourceRegressionService service = service();
        CaliberCrossSourceRegressionService.RegressionSpec spec =
                new CaliberCrossSourceRegressionService.RegressionSpec(
                        "test-missing-source",
                        List.of(new CaliberCrossSourceRegressionService.DomainSpec(
                                "finance",
                                List.of("CAL-SETTLEMENT-CHAIN"),
                                List.of(),
                                List.of(new CaliberCrossSourceRegressionService.SourceEvidence(
                                        "CAL-SETTLEMENT-CHAIN",
                                        "openmetadata:glossary.finance.settlement_chain",
                                        List.of("租摆链", "售赠坏链"))))));

        CaliberCrossSourceRegressionService.RegressionReport report = service.run(spec);

        assertThat(report.passed()).isFalse();
        assertThat(report.drifts())
                .anySatisfy(drift -> assertThat(drift)
                        .extracting(
                                CaliberCrossSourceRegressionService.RegressionDrift::domain,
                                CaliberCrossSourceRegressionService.RegressionDrift::ruleId,
                                CaliberCrossSourceRegressionService.RegressionDrift::source)
                        .containsExactly("finance", "CAL-SETTLEMENT-CHAIN", "DBT"));
    }

    private CaliberCrossSourceRegressionService service() {
        CaliberRuleRegistry registry = new CaliberRuleRegistry(objectMapper);
        registry.init();
        SemanticPackService semanticPackService = new SemanticPackService(objectMapper);
        semanticPackService.init();
        return new CaliberCrossSourceRegressionService(registry, semanticPackService, objectMapper);
    }

    private static CaliberCrossSourceRegressionService.RuleReport rule(
            CaliberCrossSourceRegressionService.RegressionReport report,
            String domain,
            String ruleId) {
        return report.domainReports().stream()
                .filter(domainReport -> domain.equals(domainReport.domain()))
                .flatMap(domainReport -> domainReport.ruleReports().stream())
                .filter(ruleReport -> ruleId.equals(ruleReport.ruleId()))
                .findFirst()
                .orElseThrow();
    }
}
