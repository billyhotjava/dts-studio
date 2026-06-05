package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.domain.FinanceReconciliationScorecardSnapshot;
import com.yuzhi.dts.copilot.ai.repository.FinanceReconciliationScorecardSnapshotRepository;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ConversationPlan;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.PlanMode;
import com.yuzhi.dts.copilot.ai.service.copilot.ConversationPlannerService.ResponseKind;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class FinanceChatAuditTrailServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withUserConfiguration(
                    CaliberRuleRegistry.class,
                    FinanceInvariantRegistry.class,
                    FinanceOracleRegistry.class,
                    FinanceAnswerAuditTrailRegistry.class,
                    FinanceAnswerAuditTrailService.class,
                    FinanceChatAuditTrailService.class);

    @Test
    void buildsFinanceAuditTrailFromMatchedReportCodeWithoutFabricatingLiveScorecardPass() {
        FinanceChatAuditTrailService service = financeChatAuditTrailService();

        FinanceAnswerAuditTrailService.AuditTrailReport report = service.buildAuditTrail(
                        financeMonthSettlementPlan(),
                        "select sum(folding_after_total_amount) from xycyl_ads_month_settlement_summary")
                .orElseThrow();

        assertThat(report.sanitizedSql()).contains("xycyl_ads_month_settlement_summary");
        assertThat(report.appliedRules())
                .extracting(FinanceAnswerAuditTrailService.AppliedCaliberRule::ruleId)
                .contains("CAL-MONTH-AMOUNT-TIER");
        assertThat(report.appliedInvariants())
                .extracting(FinanceAnswerAuditTrailService.AppliedInvariant::invariantId)
                .contains("FIN-INV-03-PAYMENT-NOT-EXCEED-DISCOUNTED");
        assertThat(report.lineage())
                .extracting(FinanceAnswerAuditTrailService.LineageNode::name)
                .contains("xycyl_ads_month_settlement_summary", "a_month_accounting");
        assertThat(report.oracleStatus().bindingId()).isEqualTo("month-settlement");
        assertThat(report.oracleStatus().covered()).isFalse();
        assertThat(report.oracleStatus().healthStatus()).isEqualTo("MISSING_SCORECARD");
        assertThat(report.passed()).isFalse();
        assertThat(report.failureMessage()).contains("missing oracle reconciliation status");
    }

    @Test
    void usesLatestScorecardForMatchedOracleBindingWhenAvailable() {
        FinanceChatAuditTrailService service = financeChatAuditTrailService(
                oracleBindingId -> "month-settlement".equals(oracleBindingId)
                        ? Optional.of(passingScorecard())
                        : Optional.empty());

        FinanceAnswerAuditTrailService.AuditTrailReport report = service.buildAuditTrail(
                        financeMonthSettlementPlan(),
                        "select sum(folding_after_total_amount) from xycyl_ads_month_settlement_summary")
                .orElseThrow();

        assertThat(report.oracleStatus().bindingId()).isEqualTo("month-settlement");
        assertThat(report.oracleStatus().covered()).isTrue();
        assertThat(report.oracleStatus().healthStatus()).isEqualTo("PASS");
        assertThat(report.oracleStatus().maxDifference()).isEqualByComparingTo("0.00");
        assertThat(report.passed()).isTrue();
        assertThat(report.failureMessage()).isEmpty();
    }

    @Test
    void springConstructsWithOptionalScorecardSource() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(FinanceChatAuditTrailService.class);

            FinanceAnswerAuditTrailService.AuditTrailReport report =
                    context.getBean(FinanceChatAuditTrailService.class)
                            .buildAuditTrail(
                                    financeMonthSettlementPlan(),
                                    "select sum(folding_after_total_amount) from xycyl_ads_month_settlement_summary")
                            .orElseThrow();

            assertThat(report.oracleStatus().healthStatus()).isEqualTo("MISSING_SCORECARD");
        });

        contextRunner
                .withBean(
                        FinanceReconciliationScorecardSource.class,
                        () -> oracleBindingId -> "month-settlement".equals(oracleBindingId)
                                ? Optional.of(passingScorecard())
                                : Optional.empty())
                .run(context -> {
                    FinanceAnswerAuditTrailService.AuditTrailReport report =
                            context.getBean(FinanceChatAuditTrailService.class)
                                    .buildAuditTrail(
                                            financeMonthSettlementPlan(),
                                            "select sum(folding_after_total_amount) from xycyl_ads_month_settlement_summary")
                                    .orElseThrow();

                    assertThat(report.oracleStatus().healthStatus()).isEqualTo("PASS");
                    assertThat(report.passed()).isTrue();
                });
    }

    @Test
    void keepsAuditTrailWhenScorecardSourceFails() {
        FinanceChatAuditTrailService service = financeChatAuditTrailService(oracleBindingId -> {
            throw new IllegalStateException("scorecard store unavailable");
        });

        FinanceAnswerAuditTrailService.AuditTrailReport report = service.buildAuditTrail(
                        financeMonthSettlementPlan(),
                        "select sum(folding_after_total_amount) from xycyl_ads_month_settlement_summary")
                .orElseThrow();

        assertThat(report.oracleStatus().healthStatus()).isEqualTo("MISSING_SCORECARD");
        assertThat(report.failureMessage()).contains("missing oracle reconciliation status");
    }

    @Test
    void readsPublishedScorecardSnapshotThroughScorecardSource() {
        FinanceReconciliationScorecardSnapshotRepository repository =
                mock(FinanceReconciliationScorecardSnapshotRepository.class);
        AtomicReference<FinanceReconciliationScorecardSnapshot> stored = new AtomicReference<>();
        when(repository.save(any())).thenAnswer(invocation -> {
            FinanceReconciliationScorecardSnapshot snapshot = invocation.getArgument(0);
            stored.set(snapshot);
            return snapshot;
        });
        when(repository.findFirstByOracleBindingIdOrderByCreatedAtDescIdDesc("month-settlement"))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        FinanceReconciliationScorecardSnapshotService scorecardSource =
                new FinanceReconciliationScorecardSnapshotService(repository, objectMapper);
        scorecardSource.publish(
                "month-settlement",
                "sprint33-finance-daily-scorecard",
                passingScorecard());
        FinanceChatAuditTrailService service = financeChatAuditTrailService(scorecardSource);

        FinanceAnswerAuditTrailService.AuditTrailReport report = service.buildAuditTrail(
                        financeMonthSettlementPlan(),
                        "select sum(folding_after_total_amount) from xycyl_ads_month_settlement_summary")
                .orElseThrow();

        assertThat(report.oracleStatus().healthStatus()).isEqualTo("PASS");
        assertThat(report.passed()).isTrue();
    }

    @Test
    void skipsNonFinanceReportCodes() {
        FinanceChatAuditTrailService service = financeChatAuditTrailService();

        assertThat(service.buildAuditTrail(
                new ConversationPlan(
                        PlanMode.AGENT_WORKFLOW,
                        ResponseKind.REPORT_DRAFT,
                        null,
                        "warehouse",
                        "public.ads_stock",
                        List.of(),
                        null,
                        null,
                        "MART",
                        "public.ads_stock",
                        "库存",
                        "L2_ADS",
                        "HIGH",
                        List.of(),
                        "table",
                        "prs.warehouse.stock",
                        List.of()),
                "select * from public.ads_stock")).isEmpty();
    }

    private FinanceChatAuditTrailService financeChatAuditTrailService() {
        return financeChatAuditTrailService(FinanceReconciliationScorecardSource.empty());
    }

    private FinanceChatAuditTrailService financeChatAuditTrailService(
            FinanceReconciliationScorecardSource scorecardSource) {
        CaliberRuleRegistry caliberRuleRegistry = new CaliberRuleRegistry(objectMapper);
        caliberRuleRegistry.init();
        FinanceInvariantRegistry invariantRegistry = new FinanceInvariantRegistry(objectMapper, caliberRuleRegistry);
        invariantRegistry.init();
        FinanceOracleRegistry oracleRegistry = new FinanceOracleRegistry(objectMapper);
        oracleRegistry.init();
        FinanceAnswerAuditTrailRegistry auditTrailRegistry = new FinanceAnswerAuditTrailRegistry(objectMapper);
        auditTrailRegistry.init();
        FinanceAnswerAuditTrailService auditTrailService =
                new FinanceAnswerAuditTrailService(caliberRuleRegistry, invariantRegistry, oracleRegistry);
        return new FinanceChatAuditTrailService(auditTrailRegistry, auditTrailService, scorecardSource);
    }

    private static ConversationPlan financeMonthSettlementPlan() {
        return new ConversationPlan(
                PlanMode.AGENT_WORKFLOW,
                ResponseKind.REPORT_DRAFT,
                null,
                "finance",
                "xycyl_ads_month_settlement_summary",
                List.of(),
                null,
                null,
                "MART",
                "xycyl_ads_month_settlement_summary",
                "月对账折后实收",
                "L3_ADS",
                "HIGH",
                List.of("财务回答必须附审计溯源"),
                "table",
                "finance.month_settlement",
                List.of("dbt-model:xycyl_ads_month_settlement_summary"),
                null,
                List.of(new ConversationPlan.RouteStep(
                        "TIER_2_MART_TEMPLATE",
                        "ADS 模型",
                        "HIT",
                        "命中月对账 ADS",
                        "xycyl_ads_month_settlement_summary")));
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
