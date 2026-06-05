package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class FinanceInvariantRegistryTest {

    private static final String INVARIANT_RESOURCE = "governance/finance-invariants.v1.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CaliberRuleRegistry caliberRuleRegistry = new CaliberRuleRegistry(objectMapper);
    private final FinanceInvariantRegistry registry =
            new FinanceInvariantRegistry(objectMapper, caliberRuleRegistry);

    @Test
    void shouldLoadEightMachineCheckableFinanceInvariants() {
        registry.init();

        assertThat(registry.invariants())
                .extracting(FinanceInvariantRegistry.FinanceInvariant::id)
                .containsExactly(
                        "FIN-INV-01-VOUCHER-BALANCE",
                        "FIN-INV-02-AMOUNT-TIER-ORDER",
                        "FIN-INV-03-PAYMENT-NOT-EXCEED-DISCOUNTED",
                        "FIN-INV-04-ADDITIVITY",
                        "FIN-INV-05-MONOTONICITY",
                        "FIN-INV-06-SETTLEMENT-CHAIN-NOT-MIXED",
                        "FIN-INV-07-BAD-DEBT-EXCLUDED-FROM-INCOME",
                        "FIN-INV-08-SOURCE-TYPE-8-DEDUP");
        assertThat(registry.invariants())
                .allSatisfy(invariant -> {
                    assertThat(invariant.statement()).isNotBlank();
                    assertThat(invariant.appliesTo()).isNotEmpty();
                    assertThat(invariant.severity()).isEqualTo("error");
                    assertThat(invariant.check().type()).isNotBlank();
                    assertThat(invariant.positiveExample().isObject()).isTrue();
                    assertThat(invariant.negativeExample().isObject()).isTrue();
                });
    }

    @Test
    void shouldValidatePositiveAndNegativeExamplesForEveryInvariant() {
        caliberRuleRegistry.init();
        registry.init();

        for (FinanceInvariantRegistry.FinanceInvariant invariant : registry.invariants()) {
            assertThat(registry.validateExample(invariant, true).allowed())
                    .as("positive example should pass: %s", invariant.id())
                    .isTrue();
            FinanceInvariantRegistry.InvariantValidation negative =
                    registry.validateExample(invariant, false);
            assertThat(negative.allowed())
                    .as("negative example should fail: %s", invariant.id())
                    .isFalse();
            assertThat(negative.violations())
                    .extracting(FinanceInvariantRegistry.InvariantViolation::invariantId)
                    .contains(invariant.id());
        }
    }

    @Test
    void shouldReuseCaliberRuleIdsForSqlStaticInvariants() {
        caliberRuleRegistry.init();
        registry.init();
        Set<String> caliberRuleIds = caliberRuleRegistry.rules().stream()
                .map(CaliberRuleRegistry.CaliberRule::id)
                .collect(Collectors.toSet());

        assertThat(registry.invariants().stream()
                        .filter(invariant -> "SQL_STATIC".equals(invariant.check().type()))
                        .toList())
                .hasSize(2)
                .allSatisfy(invariant -> {
                    assertThat(invariant.sourceRuleIds()).isNotEmpty();
                    assertThat(caliberRuleIds).containsAll(invariant.sourceRuleIds());
                });
    }

    @Test
    void shouldPublishSameSpecToSprintAsset() throws Exception {
        JsonNode resource;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(INVARIANT_RESOURCE)) {
            assertThat(is).isNotNull();
            resource = objectMapper.readTree(is);
        }

        Path asset = resolveSprintAsset();
        assertThat(Files.exists(asset)).isTrue();
        assertThat(objectMapper.readTree(asset.toFile())).isEqualTo(resource);
    }

    private static Path resolveSprintAsset() {
        Path fromRoot = Path.of("worklog/v1.0.0/sprint-33-202607/assets/finance-invariants.v1.json");
        if (Files.exists(fromRoot)) {
            return fromRoot;
        }
        return Path.of("../worklog/v1.0.0/sprint-33-202607/assets/finance-invariants.v1.json");
    }
}
