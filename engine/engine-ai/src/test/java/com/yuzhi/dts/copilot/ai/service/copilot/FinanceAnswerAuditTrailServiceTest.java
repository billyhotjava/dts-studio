package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class FinanceAnswerAuditTrailServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void buildsAuditableFinanceAnswerTraceFromGovernanceRulesLineageAndScorecard() {
        FinanceAnswerAuditTrailRegistry registry = auditRegistry();
        FinanceAnswerAuditTrailService service = auditTrailService();
        FinanceAnswerAuditTrailRegistry.AuditTrailPolicy policy = registry.policy("sprint33-finance-answer-audit-trail")
                .orElseThrow();
        FinanceAnswerAuditTrailRegistry.AuditTrailBindingPolicy bindingPolicy = registry.bindingPolicy("month-settlement")
                .orElseThrow();
        FinanceReconciliationScorecardService.ScorecardReport scorecard = new FinanceReconciliationScorecardService.ScorecardReport(
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

        FinanceAnswerAuditTrailService.AuditTrailReport report = service.buildAuditTrail(
                policy,
                bindingPolicy,
                new FinanceAnswerAuditTrailService.AuditTrailRequest(
                        "answer-20260605-001",
                        "finance",
                        "select project_id, account_period, sum(folding_after_total_amount) as discounted_receivable "
                                + "from xycyl_ads_month_settlement_summary group by project_id, account_period "
                                + "/* jdbc:mysql://prod-db:3306/prs password=should_not_leak token=secret */",
                        "month-settlement",
                        List.of(
                                new FinanceAnswerAuditTrailService.RouteTraceStep(
                                        "TIER_1_PUBLISHED_INDICATOR",
                                        "published indicator",
                                        "HIT",
                                        "finance month-settlement metric",
                                        "indicator:month-settlement")),
                        scorecard));

        assertThat(report.passed()).isTrue();
        assertThat(report.failureMessage()).isEmpty();
        assertThat(report.sections()).containsExactly("sql", "caliberRules", "lineage", "oracleStatus", "routeTrace");
        assertThat(report.sanitizedSql()).contains("xycyl_ads_month_settlement_summary");
        assertThat(report.sanitizedSql()).doesNotContain("jdbc:mysql", "password=should_not_leak", "token=secret");
        assertThat(report.appliedRules())
                .extracting(FinanceAnswerAuditTrailService.AppliedCaliberRule::ruleId)
                .contains("CAL-SETTLEMENT-CHAIN", "CAL-MONTH-AMOUNT-TIER", "CAL-SALE-IN-RENT");
        assertThat(report.appliedInvariants())
                .extracting(FinanceAnswerAuditTrailService.AppliedInvariant::invariantId)
                .contains("FIN-INV-02-AMOUNT-TIER-ORDER", "FIN-INV-03-PAYMENT-NOT-EXCEED-DISCOUNTED");
        assertThat(report.lineage())
                .extracting(FinanceAnswerAuditTrailService.LineageNode::name)
                .contains(
                        "answer-20260605-001",
                        "xycyl_ads_month_settlement_summary",
                        "a_month_accounting",
                        "a_green_accounting",
                        "POST /rs-flowers-base/operate/monthAccount/getMonthSettlementData");
        assertThat(report.oracleStatus().covered()).isTrue();
        assertThat(report.oracleStatus().bindingId()).isEqualTo("month-settlement");
        assertThat(report.oracleStatus().healthStatus()).isEqualTo("PASS");
        assertThat(report.oracleStatus().maxDifference()).isEqualByComparingTo("0.00");
        assertThat(report.routeTrace())
                .extracting(FinanceAnswerAuditTrailService.RouteTraceStep::tier)
                .containsExactly("TIER_1_PUBLISHED_INDICATOR");
    }

    @Test
    void failsClosedWhenRequiredAuditSectionsAreMissing() {
        FinanceAnswerAuditTrailRegistry registry = auditRegistry();
        FinanceAnswerAuditTrailService service = auditTrailService();
        FinanceAnswerAuditTrailRegistry.AuditTrailPolicy policy = registry.policy("sprint33-finance-answer-audit-trail")
                .orElseThrow();
        FinanceAnswerAuditTrailRegistry.AuditTrailBindingPolicy bindingPolicy = registry.bindingPolicy("month-settlement")
                .orElseThrow();

        FinanceAnswerAuditTrailService.AuditTrailReport report = service.buildAuditTrail(
                policy,
                bindingPolicy,
                new FinanceAnswerAuditTrailService.AuditTrailRequest(
                        "answer-missing",
                        "finance",
                        "",
                        "month-settlement",
                        List.of(),
                        null));

        assertThat(report.passed()).isFalse();
        assertThat(report.failureMessage()).contains("missing sanitized sql");
        assertThat(report.oracleStatus().covered()).isFalse();
    }

    @Test
    void rejectsRequestsOutsideFinanceAuditPolicyDomains() {
        FinanceAnswerAuditTrailRegistry registry = auditRegistry();
        FinanceAnswerAuditTrailService service = auditTrailService();
        FinanceAnswerAuditTrailRegistry.AuditTrailPolicy policy = registry.policy("sprint33-finance-answer-audit-trail")
                .orElseThrow();
        FinanceAnswerAuditTrailRegistry.AuditTrailBindingPolicy bindingPolicy = registry.bindingPolicy("month-settlement")
                .orElseThrow();

        FinanceAnswerAuditTrailService.AuditTrailReport report = service.buildAuditTrail(
                policy,
                bindingPolicy,
                new FinanceAnswerAuditTrailService.AuditTrailRequest(
                        "answer-warehouse",
                        "warehouse",
                        "select sum(amount) from xycyl_ads_month_settlement_summary",
                        "month-settlement",
                        List.of(
                                new FinanceAnswerAuditTrailService.RouteTraceStep(
                                        "TIER_1_PUBLISHED_INDICATOR",
                                        "published indicator",
                                        "HIT",
                                        "warehouse metric",
                                        "indicator:warehouse")),
                        new FinanceReconciliationScorecardService.ScorecardReport(
                                true,
                                false,
                                "PASS",
                                1,
                                1,
                                new BigDecimal("100.00"),
                                BigDecimal.ZERO,
                                List.of(),
                                List.of(),
                                List.of(),
                                "")));

        assertThat(report.passed()).isFalse();
        assertThat(report.failureMessage()).contains("domain not covered");
    }

    private FinanceAnswerAuditTrailService auditTrailService() {
        CaliberRuleRegistry caliberRuleRegistry = caliberRuleRegistry();
        FinanceInvariantRegistry invariantRegistry = new FinanceInvariantRegistry(objectMapper, caliberRuleRegistry);
        invariantRegistry.init();
        FinanceOracleRegistry oracleRegistry = new FinanceOracleRegistry(objectMapper);
        oracleRegistry.init();
        return new FinanceAnswerAuditTrailService(caliberRuleRegistry, invariantRegistry, oracleRegistry);
    }

    private FinanceAnswerAuditTrailRegistry auditRegistry() {
        FinanceAnswerAuditTrailRegistry registry = new FinanceAnswerAuditTrailRegistry(objectMapper);
        registry.init();
        return registry;
    }

    private CaliberRuleRegistry caliberRuleRegistry() {
        CaliberRuleRegistry registry = new CaliberRuleRegistry(objectMapper);
        registry.init();
        return registry;
    }
}
