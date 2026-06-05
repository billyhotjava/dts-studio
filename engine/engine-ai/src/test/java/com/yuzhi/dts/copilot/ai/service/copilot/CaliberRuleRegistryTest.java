package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class CaliberRuleRegistryTest {

    private final CaliberRuleRegistry registry = new CaliberRuleRegistry(new ObjectMapper());

    @Test
    void shouldLoadNineGovernanceCaliberRules() {
        registry.init();

        assertThat(registry.rules())
                .extracting(CaliberRuleRegistry.CaliberRule::id)
                .containsExactly(
                        "CAL-BIZTYPE-SCOPE",
                        "CAL-SETTLEMENT-CHAIN",
                        "CAL-MONTH-AMOUNT-TIER",
                        "CAL-SALE-IN-RENT",
                        "CAL-RENT-HISTORY",
                        "CAL-INVENTORY-COST",
                        "CAL-JSON-EXPAND",
                        "CAL-VARCHAR-AMOUNT-CAST",
                        "CAL-EXTRA-COST-VS-EXPENSE");

        assertThat(registry.rules())
                .allSatisfy(rule -> {
                    assertThat(rule.description()).isNotBlank();
                    assertThat(rule.appliesTo()).isNotEmpty();
                    assertThat(rule.severity()).isNotBlank();
                    assertThat(rule.check()).isNotNull();
                });
    }

    @Test
    void shouldRejectStaticFinanceCaliberViolationsWithRuleIds() {
        registry.init();

        assertViolation(
                "CAL-BIZTYPE-SCOPE",
                """
                SELECT COUNT(*)
                FROM t_flower_biz_info f
                JOIN t_flower_biz_item i ON i.flower_biz_id = f.id
                WHERE biz_type = 1
                """);
        assertViolation(
                "CAL-SETTLEMENT-CHAIN",
                """
                SELECT SUM(m.receivable_total_amount) + SUM(s.receivable) AS amount
                FROM a_month_accounting m
                JOIN a_sale_account s ON s.project_id = m.project_id
                """);
        assertViolation(
                "CAL-SALE-IN-RENT",
                """
                SELECT SUM(m.sale_total_amount) + SUM(s.receivable) AS amount
                FROM a_month_accounting m
                JOIN a_green_accounting g ON g.month_accounting_id = m.id
                JOIN a_sale_account s ON s.id = g.source_id
                WHERE g.source_type=8
                """);
        assertViolation(
                "CAL-SETTLEMENT-CHAIN",
                """
                SELECT SUM(s.receivable_amount) AS income_amount
                FROM a_sale_account s
                JOIN t_flower_biz_info b ON b.id = s.biz_id
                WHERE b.finish_time >= DATE '2026-01-01'
                """);
        assertViolation(
                "CAL-JSON-EXPAND",
                """
                SELECT f.code, s.code
                FROM t_flower_biz_info f
                JOIN f_settlement s ON s.biz_ids_json = f.id
                """);
        assertViolation(
                "CAL-VARCHAR-AMOUNT-CAST",
                "SELECT SUM(r.rent) FROM a_sale_account_rent_item r");
    }

    @Test
    void shouldAllowCompliantStaticFinanceExamples() {
        registry.init();

        List<String> allowedSql = List.of(
                """
                SELECT COUNT(*)
                FROM t_flower_biz_info f
                JOIN t_flower_biz_item i ON i.flower_biz_id = f.id
                WHERE f.biz_type = 1
                """,
                "SELECT SUM(m.receivable_total_amount) FROM a_month_accounting m",
                """
                SELECT SUM(s.receivable_amount) AS income_amount
                FROM a_sale_account s
                JOIN t_flower_biz_info b ON b.id = s.biz_id
                WHERE b.biz_type <> 6
                """,
                """
                SELECT SUM(s.receivable_amount) AS sale_income_amount
                FROM a_sale_account s
                JOIN t_flower_biz_info b ON b.id = s.biz_id
                WHERE b.biz_type IN (7, 8)
                """,
                """
                SELECT f.code, s.code
                FROM t_flower_biz_info f
                JOIN f_settlement s ON f.id IN (
                    SELECT CAST(value AS bigint)
                    FROM jsonb_array_elements_text(s.biz_ids_json::jsonb)
                )
                """,
                "SELECT SUM(CAST(NULLIF(r.rent, '') AS numeric)) FROM a_sale_account_rent_item r");

        for (String sql : allowedSql) {
            CaliberRuleRegistry.CaliberValidation validation = registry.validateSql("finance", sql);
            assertThat(validation.allowed()).as(sql).isTrue();
            assertThat(validation.violations()).as(sql).isEmpty();
        }
    }

    @Test
    void shouldGenerateDomainGuardrailTextFromRules() {
        registry.init();

        assertThat(registry.guardrailsForDomain("finance"))
                .anySatisfy(text -> assertThat(text).contains("[CAL-MONTH-AMOUNT-TIER]"))
                .anySatisfy(text -> assertThat(text).contains("[CAL-EXTRA-COST-VS-EXPENSE]"));
        assertThat(registry.guardrailsForDomain("procurement"))
                .anySatisfy(text -> assertThat(text).contains("[CAL-INVENTORY-COST]"));
    }

    private void assertViolation(String expectedRuleId, String sql) {
        CaliberRuleRegistry.CaliberValidation validation = registry.validateSql("finance", sql);

        assertThat(validation.allowed()).isFalse();
        assertThat(validation.violations())
                .extracting(CaliberRuleRegistry.CaliberViolation::ruleId)
                .contains(expectedRuleId);
    }
}
