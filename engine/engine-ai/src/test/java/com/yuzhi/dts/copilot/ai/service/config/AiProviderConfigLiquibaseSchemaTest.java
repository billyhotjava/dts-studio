package com.yuzhi.dts.copilot.ai.service.config;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AiProviderConfigLiquibaseSchemaTest {

    @Test
    void masterChangelogCreatesProviderConfigColumnsRequiredByJpaEntity() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:ai-provider-config-schema;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS copilot_ai\\;SET SCHEMA copilot_ai",
                "sa",
                "")) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName("copilot_ai");

            Liquibase liquibase = new Liquibase(
                    "config/liquibase/provider-config-schema-test.xml",
                    new ClassLoaderResourceAccessor(),
                    database);
            liquibase.update(new Contexts(), new LabelExpression());

            Set<String> columns = loadColumns(connection);

            assertThat(columns)
                    .contains(
                            "name",
                            "base_url",
                            "api_key",
                            "model",
                            "is_default",
                            "max_tokens",
                            "temperature",
                            "timeout_seconds",
                            "enabled",
                            "priority",
                            "provider_type",
                            "created_at",
                            "updated_at")
                    .doesNotContain("provider_name");
        }
    }

    private Set<String> loadColumns(Connection connection) throws Exception {
        Set<String> columns = new LinkedHashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "select column_name from information_schema.columns where table_schema = ? and table_name = ? order by ordinal_position")) {
            statement.setString(1, "copilot_ai");
            statement.setString(2, "ai_provider_config");
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    columns.add(resultSet.getString(1));
                }
            }
        }
        return columns;
    }
}
