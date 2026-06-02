package com.yuzhi.dts.copilot.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class TrinoFederatedQuerySeedTest {

    private static final String LOGICAL_DATASOURCE_CHANGELOG =
            "config/liquibase/changelog/0065_prs_flowerbiz_screen_logical_datasource.xml";
    private static final String FEDERATED_CHANGELOG =
            "config/liquibase/changelog/0066_trino_federated_query_database.xml";
    private static final String FEDERATED_AGENT_DATASOURCE_CHANGELOG =
            "config/liquibase/changelog/0067_trino_federated_agent_datasource.xml";

    @Test
    void masterShouldIncludeTrinoFederatedSeedAfterLogicalDatasourceRepair() throws Exception {
        String master = readResource("config/liquibase/master.xml");

        assertThat(master).contains(
                LOGICAL_DATASOURCE_CHANGELOG,
                FEDERATED_CHANGELOG,
                FEDERATED_AGENT_DATASOURCE_CHANGELOG);
        assertThat(master.indexOf(LOGICAL_DATASOURCE_CHANGELOG))
                .isLessThan(master.indexOf(FEDERATED_CHANGELOG));
        assertThat(master.indexOf(FEDERATED_CHANGELOG))
                .isLessThan(master.indexOf(FEDERATED_AGENT_DATASOURCE_CHANGELOG));
    }

    @Test
    void changelogShouldSeedTrinoFederatedGatewayWithoutLiteralSecrets() throws Exception {
        String changelog = readResource(FEDERATED_CHANGELOG);

        assertThat(changelog)
                .contains("联邦查询入口")
                .contains("'trino'")
                .contains("jdbc:trino://${DTS_TRINO_HOST:dts-trino}:${DTS_TRINO_PORT:8080}/postgres/public")
                .contains("prs.flowerbiz.federated")
                .contains("prs.flowerbiz.trino")
                .contains("federatedQuery")
                .contains("queryContract")
                .contains("postgres")
                .contains("mysql");
        assertThat(changelog)
                .doesNotContain("connection-password=secret")
                .doesNotContain("password\":\"secret")
                .doesNotContain("root:");
    }

    @Test
    void agentDatasourceChangelogShouldLinkFederatedGatewayToCopilotAiDatasource() throws Exception {
        String changelog = readResource(FEDERATED_AGENT_DATASOURCE_CHANGELOG);

        assertThat(changelog)
                .contains("dbchangelog/dbchangelog-4.22.xsd")
                .contains("copilot_ai.data_source")
                .contains("联邦查询入口")
                .contains("'trino'")
                .contains("jdbc:trino://${DTS_TRINO_HOST:dts-trino}:${DTS_TRINO_PORT:8080}/postgres/public")
                .contains("${DTS_TRINO_USER:copilot}")
                .contains("copilotDataSourceId")
                .contains("copilot_analytics.analytics_database");
        assertThat(changelog)
                .doesNotContain("connection-password=secret")
                .doesNotContain("password\":\"secret")
                .doesNotContain("root:");
    }

    private static String readResource(String path) throws Exception {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        assertThat(stream)
                .as("Expected Liquibase resource to exist at %s", path)
                .isNotNull();
        try (InputStream input = stream) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
