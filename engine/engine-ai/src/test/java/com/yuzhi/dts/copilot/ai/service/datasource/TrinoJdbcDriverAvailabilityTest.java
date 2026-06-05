package com.yuzhi.dts.copilot.ai.service.datasource;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class TrinoJdbcDriverAvailabilityTest {

    @Test
    void trinoJdbcDriverIsAvailableForFederatedAgentDatasource() {
        assertThatCode(() -> Class.forName("io.trino.jdbc.TrinoDriver"))
                .doesNotThrowAnyException();
    }
}
