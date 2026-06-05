package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqlSafetyCheckerCaliberGuardrailTest {

    private SqlSafetyChecker checker;

    @BeforeEach
    void setUp() {
        CaliberRuleRegistry registry = new CaliberRuleRegistry(new ObjectMapper());
        registry.init();
        checker = new SqlSafetyChecker(registry);
    }

    @Test
    void shouldBlockFinanceSqlThatViolatesCaliberRules() {
        String sql = """
                SELECT SUM(m.receivable_total_amount) + SUM(s.receivable) AS amount
                FROM a_month_accounting m
                JOIN a_sale_account s ON s.project_id = m.project_id
                """;

        SqlSafetyChecker.SqlSafetyValidation validation = checker.validate("finance", sql);

        assertThat(validation.safe()).isFalse();
        assertThat(validation.reasons()).anySatisfy(reason -> assertThat(reason)
                .contains("CAL-SETTLEMENT-CHAIN")
                .contains("租摆链和售赠坏链混算风险"));
        assertThat(checker.isSafe("finance", sql)).isFalse();
    }

    @Test
    void shouldApplyFinanceCaliberRulesForSettlementAlias() {
        String sql = """
                SELECT SUM(m.sale_total_amount) + SUM(s.receivable) AS amount
                FROM a_month_accounting m
                JOIN a_green_accounting g ON g.month_accounting_id = m.id
                JOIN a_sale_account s ON s.id = g.source_id
                WHERE g.source_type=8
                """;

        SqlSafetyChecker.SqlSafetyValidation validation = checker.validate("settlement", sql);

        assertThat(validation.safe()).isFalse();
        assertThat(validation.reasons()).anySatisfy(reason -> assertThat(reason)
                .contains("CAL-SALE-IN-RENT")
                .contains("双重计数风险"));
    }

    @Test
    void shouldBlockSaleIncomeAggregationWhenBadDebtIsNotExcluded() {
        String sql = """
                SELECT SUM(s.receivable_amount) AS income_amount
                FROM a_sale_account s
                JOIN t_flower_biz_info b ON b.id = s.biz_id
                WHERE b.finish_time >= DATE '2026-01-01'
                """;

        SqlSafetyChecker.SqlSafetyValidation validation = checker.validate("finance", sql);

        assertThat(validation.safe()).isFalse();
        assertThat(validation.reasons()).anySatisfy(reason -> assertThat(reason)
                .contains("CAL-SETTLEMENT-CHAIN")
                .contains("坏账")
                .contains("收入"));
    }

    @Test
    void shouldNotApplyFinanceCaliberRulesToNonFinanceDomains() {
        String sql = """
                SELECT SUM(m.receivable_total_amount) + SUM(s.receivable) AS amount
                FROM a_month_accounting m
                JOIN a_sale_account s ON s.project_id = m.project_id
                """;

        SqlSafetyChecker.SqlSafetyValidation validation = checker.validate("procurement", sql);

        assertThat(validation.safe()).isTrue();
        assertThat(validation.reasons()).isEmpty();
    }

    @Test
    void shouldKeepExistingSqlSafetyRulesForEveryDomain() {
        SqlSafetyChecker.SqlSafetyValidation validation = checker.validate("finance", "DROP TABLE a_month_accounting");

        assertThat(validation.safe()).isFalse();
        assertThat(validation.reasons()).anySatisfy(reason -> assertThat(reason).contains("Only SELECT"));
    }
}
