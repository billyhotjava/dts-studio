package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class Nl2SqlAccuracyGoldenSetRegistryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loadsSprint34GoldenCasesForFinanceVoucherFinanceYearAndFlowerbizOrder() {
        Nl2SqlAccuracyGoldenSetRegistry registry = new Nl2SqlAccuracyGoldenSetRegistry(objectMapper);
        registry.init();

        List<Nl2SqlAccuracyGoldenSetRegistry.GoldenCase> cases = registry.cases();

        assertThat(cases)
                .extracting(Nl2SqlAccuracyGoldenSetRegistry.GoldenCase::id)
                .contains(
                        "qa-finance-voucher-2026-main",
                        "qa-finance-year-2026-main",
                        "qa-flowerbiz-order-monthly-main");
        Nl2SqlAccuracyGoldenSetRegistry.GoldenCase voucher =
                registry.caseById("qa-finance-voucher-2026-main").orElseThrow();
        assertThat(voucher.question()).isEqualTo("2026年凭证的数据统计下");
        assertThat(voucher.expectedTemplate()).isEqualTo("TPL-57");
        assertThat(voucher.expectedTarget()).isEqualTo("public.xycyl_ads_finance_voucher_monthly");
        assertThat(voucher.expectedGradeAtLeast()).isEqualTo("HIGH");
        assertThat(voucher.requiredEvidence()).contains("route", "sql", "freshness", "tieout");
        assertThat(voucher.expectedSqlFragments())
                .contains("public.xycyl_ads_finance_voucher_monthly")
                .doesNotContain("mysql.rs_cloud_flower.f_voucher");
    }

    @Test
    void keepsVoucherMetamorphicVariantsOnTheSameTemplateTargetAndYear() {
        Nl2SqlAccuracyGoldenSetRegistry registry = new Nl2SqlAccuracyGoldenSetRegistry(objectMapper);
        registry.init();

        List<Nl2SqlAccuracyGoldenSetRegistry.GoldenCase> variants = registry.cases().stream()
                .filter(item -> "finance-voucher-yearly-2026".equals(item.metamorphicGroup()))
                .toList();

        assertThat(variants).hasSizeGreaterThanOrEqualTo(2);
        assertThat(variants)
                .extracting(Nl2SqlAccuracyGoldenSetRegistry.GoldenCase::expectedTemplate)
                .containsOnly("TPL-57");
        assertThat(variants)
                .extracting(Nl2SqlAccuracyGoldenSetRegistry.GoldenCase::expectedTarget)
                .containsOnly("public.xycyl_ads_finance_voucher_monthly");
        assertThat(variants)
                .extracting(item -> item.expectedParameters().get("year"))
                .containsOnly("2026");
    }

    @Test
    void scorecardPassesWhenAllGoldenCasesMeetRouteSqlTargetGradeAndEvidenceExpectations() {
        Nl2SqlAccuracyGoldenSetRegistry registry = new Nl2SqlAccuracyGoldenSetRegistry(objectMapper);
        registry.init();
        Nl2SqlAccuracyGoldenSetScorecardService service = new Nl2SqlAccuracyGoldenSetScorecardService();

        Nl2SqlAccuracyGoldenSetScorecardService.ScorecardReport report = service.score(
                registry.cases(),
                registry.cases().stream().map(this::passingResult).toList());

        assertThat(report.passed()).isTrue();
        assertThat(report.healthStatus()).isEqualTo("PASS");
        assertThat(report.passRate()).isEqualTo(100.0d);
        assertThat(report.failures()).isEmpty();
    }

    @Test
    void scorecardFailsWhenGoldenCaseFallsBackToL0ProfileOrApplicationMysql() {
        Nl2SqlAccuracyGoldenSetRegistry registry = new Nl2SqlAccuracyGoldenSetRegistry(objectMapper);
        registry.init();
        Nl2SqlAccuracyGoldenSetScorecardService service = new Nl2SqlAccuracyGoldenSetScorecardService();

        Nl2SqlAccuracyGoldenSetScorecardService.ScorecardReport report = service.score(
                registry.cases(),
                List.of(
                        passingResult(registry.caseById("qa-finance-year-2026-main").orElseThrow()),
                        passingResult(registry.caseById("qa-flowerbiz-order-monthly-main").orElseThrow()),
                        new Nl2SqlAccuracyGoldenSetScorecardService.GoldenCaseRun(
                                "qa-finance-voucher-2026-main",
                                "TPL-57",
                                "prs.finance.voucher.profile",
                                "L0_BUSINESS_OBJECT_PROFILE",
                                "HIGH",
                                "select count(*) from mysql.rs_cloud_flower.f_voucher",
                                Map.of("freshness", "UNKNOWN", "tieout", "MISSING"))));

        assertThat(report.passed()).isFalse();
        assertThat(report.healthStatus()).isEqualTo("FAIL");
        assertThat(report.failures())
                .anySatisfy(failure -> assertThat(failure.reason())
                        .contains("qa-finance-voucher-2026-main", "L0_BUSINESS_OBJECT_PROFILE"));
        assertThat(report.failures())
                .anySatisfy(failure -> assertThat(failure.reason())
                        .contains("应用 MySQL", "mysql.rs_cloud_flower.f_voucher"));
    }

    @Test
    void scorecardFailsWhenMetamorphicVariantDriftsToAnotherTemplateOrTarget() {
        Nl2SqlAccuracyGoldenSetRegistry registry = new Nl2SqlAccuracyGoldenSetRegistry(objectMapper);
        registry.init();
        Nl2SqlAccuracyGoldenSetScorecardService service = new Nl2SqlAccuracyGoldenSetScorecardService();

        List<Nl2SqlAccuracyGoldenSetScorecardService.GoldenCaseRun> runs = registry.cases().stream()
                .map(this::passingResult)
                .map(run -> "qa-finance-voucher-2026-variant".equals(run.caseId())
                        ? new Nl2SqlAccuracyGoldenSetScorecardService.GoldenCaseRun(
                                run.caseId(),
                                "TPL-56",
                                "public.xycyl_ads_finance_month_settlement",
                                run.dataSurface(),
                                run.grade(),
                                run.sql(),
                                run.evidence())
                        : run)
                .toList();

        Nl2SqlAccuracyGoldenSetScorecardService.ScorecardReport report = service.score(registry.cases(), runs);

        assertThat(report.passed()).isFalse();
        assertThat(report.failures())
                .anySatisfy(failure -> assertThat(failure.reason())
                        .contains("metamorphic drift", "finance-voucher-yearly-2026", "TPL-56"));
    }

    private Nl2SqlAccuracyGoldenSetScorecardService.GoldenCaseRun passingResult(
            Nl2SqlAccuracyGoldenSetRegistry.GoldenCase goldenCase) {
        return new Nl2SqlAccuracyGoldenSetScorecardService.GoldenCaseRun(
                goldenCase.id(),
                goldenCase.expectedTemplate(),
                goldenCase.expectedTarget(),
                goldenCase.expectedDataSurface(),
                goldenCase.expectedGradeAtLeast(),
                selectSql(goldenCase),
                Map.of("freshness", "FRESH", "tieout", "PASS", "route", "PASS", "sql", "PASS"));
    }

    private String selectSql(Nl2SqlAccuracyGoldenSetRegistry.GoldenCase goldenCase) {
        String target = goldenCase.expectedTarget();
        return "select * from " + target
                + " where fiscal_year = 2026 /* "
                + String.join(" ", goldenCase.expectedSqlFragments())
                + " */";
    }
}
