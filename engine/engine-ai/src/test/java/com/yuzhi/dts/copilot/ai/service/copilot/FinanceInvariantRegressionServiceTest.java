package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class FinanceInvariantRegressionServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CaliberRuleRegistry caliberRuleRegistry = new CaliberRuleRegistry(objectMapper);
    private final FinanceInvariantRegistry invariantRegistry =
            new FinanceInvariantRegistry(objectMapper, caliberRuleRegistry);
    private final FinanceInvariantRegressionService regressionService =
            new FinanceInvariantRegressionService(objectMapper, invariantRegistry);

    @Test
    void shouldPassRepresentativeFinanceInvariantRegressionGrid() {
        caliberRuleRegistry.init();
        invariantRegistry.init();
        regressionService.init();

        FinanceInvariantRegressionService.RegressionResult result = regressionService.runAll();

        assertThat(result.allowed()).isTrue();
        assertThat(result.failures()).isEmpty();
        assertThat(result.caseResults()).hasSizeGreaterThanOrEqualTo(8);
        assertThat(result.caseResults())
                .extracting(FinanceInvariantRegressionService.CaseResult::invariantId)
                .contains(
                        "FIN-INV-01-VOUCHER-BALANCE",
                        "FIN-INV-02-AMOUNT-TIER-ORDER",
                        "FIN-INV-03-PAYMENT-NOT-EXCEED-DISCOUNTED",
                        "FIN-INV-04-ADDITIVITY",
                        "FIN-INV-05-MONOTONICITY",
                        "FIN-INV-06-SETTLEMENT-CHAIN-NOT-MIXED",
                        "FIN-INV-07-BAD-DEBT-EXCLUDED-FROM-INCOME",
                        "FIN-INV-08-SOURCE-TYPE-8-DEDUP");
        Set<String> filterKeys = regressionService.cases().stream()
                .flatMap(testCase -> testCase.filter().keySet().stream())
                .collect(Collectors.toSet());
        assertThat(filterKeys).contains("project", "month", "dateRange", "bizType", "status");
    }

    @Test
    void shouldLocateInjectedAdditivityViolationWithReproducibleFilter() throws Exception {
        caliberRuleRegistry.init();
        invariantRegistry.init();
        FinanceInvariantRegressionService.RegressionCase injected =
                new FinanceInvariantRegressionService.RegressionCase(
                        "injected-additivity-mismatch",
                        "FIN-INV-04-ADDITIVITY",
                        Map.of(
                                "project", "P-102",
                                "month", "2026-02",
                                "dateRange", "2026-02-01..2026-02-28",
                                "bizType", "rent",
                                "status", "settled"),
                        objectMapper.readTree(
                                """
                                {
                                  "total": {"amount": 300.00},
                                  "partitions": [
                                    {"partition": "2026-02-A", "amount": 120.00},
                                    {"partition": "2026-02-B", "amount": 170.00}
                                  ]
                                }
                                """),
                        "injected mutation");

        FinanceInvariantRegressionService.CaseResult result = regressionService.runCase(injected);

        assertThat(result.allowed()).isFalse();
        assertThat(result.caseId()).isEqualTo("injected-additivity-mismatch");
        assertThat(result.invariantId()).isEqualTo("FIN-INV-04-ADDITIVITY");
        assertThat(result.filter()).containsEntry("project", "P-102");
        assertThat(result.reproducibleFailure())
                .contains("injected-additivity-mismatch")
                .contains("FIN-INV-04-ADDITIVITY")
                .contains("P-102")
                .contains("total amount must equal partition sum");
    }
}
