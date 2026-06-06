package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class FinanceApplicationMysqlOracleJdbcConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(FinanceApplicationMysqlOracleJdbcConfiguration.class);

    @Test
    void shouldCreateApplicationMysqlOracleExecutorOnlyWhenEnabled() {
        contextRunner.run(context -> assertThat(context)
                .doesNotHaveBean(FinanceApplicationMysqlOracleProofService.QueryExecutor.class));

        contextRunner
                .withPropertyValues(
                        "copilot.finance.application-mysql-oracle.enabled=true",
                        "copilot.finance.application-mysql-oracle.jdbc-url=jdbc:h2:mem:finance_app_mysql_oracle_config;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                        "copilot.finance.application-mysql-oracle.driver-class-name=org.h2.Driver",
                        "copilot.finance.application-mysql-oracle.username=sa",
                        "copilot.finance.application-mysql-oracle.password=secret",
                        "copilot.finance.application-mysql-oracle.database=rs_cloud_flower")
                .run(context -> {
                    assertThat(context).hasBean("financeApplicationMysqlOracleJdbcQueryExecutor");
                    assertThat(context).doesNotHaveBean("financeApplicationMysqlOracleCopilotJdbcQueryExecutor");
                    FinanceApplicationMysqlOracleProofService.QueryExecutor executor =
                            context.getBean(
                                    "financeApplicationMysqlOracleJdbcQueryExecutor",
                                    FinanceApplicationMysqlOracleProofService.QueryExecutor.class);

                    assertThat(executor.query("rs_cloud_flower", "SELECT 1 AS amount"))
                            .singleElement()
                            .satisfies(row -> assertThat(row).containsEntry("amount", BigDecimal.ONE));
                });
    }

    @Test
    void shouldCreateCopilotAdsExecutorWhenCopilotJdbcIsConfigured() {
        contextRunner
                .withPropertyValues(
                        "copilot.finance.application-mysql-oracle.enabled=true",
                        "copilot.finance.application-mysql-oracle.jdbc-url=jdbc:h2:mem:finance_app_mysql_oracle_app;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                        "copilot.finance.application-mysql-oracle.driver-class-name=org.h2.Driver",
                        "copilot.finance.application-mysql-oracle.username=sa",
                        "copilot.finance.application-mysql-oracle.password=secret",
                        "copilot.finance.application-mysql-oracle.database=rs_cloud_flower",
                        "copilot.finance.application-mysql-oracle.copilot-jdbc-url=jdbc:h2:mem:finance_app_mysql_oracle_ads;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                        "copilot.finance.application-mysql-oracle.copilot-driver-class-name=org.h2.Driver",
                        "copilot.finance.application-mysql-oracle.copilot-username=sa",
                        "copilot.finance.application-mysql-oracle.copilot-password=secret",
                        "copilot.finance.application-mysql-oracle.copilot-database=prs.flowerbiz.federated")
                .run(context -> {
                    assertThat(context).hasBean("financeApplicationMysqlOracleJdbcQueryExecutor");
                    assertThat(context).hasBean("financeApplicationMysqlOracleCopilotJdbcQueryExecutor");
                    FinanceApplicationMysqlOracleProofService.QueryExecutor executor =
                            context.getBean(
                                    "financeApplicationMysqlOracleCopilotJdbcQueryExecutor",
                                    FinanceApplicationMysqlOracleProofService.QueryExecutor.class);

                    assertThat(executor.query("prs.flowerbiz.federated", "SELECT 1 AS amount"))
                            .singleElement()
                            .satisfies(row -> assertThat(row).containsEntry("amount", BigDecimal.ONE));
                });
    }

    @Test
    void shouldKeepSpringBootPrimaryDataSourceWhenProofJdbcIsEnabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
                .withUserConfiguration(FinanceApplicationMysqlOracleJdbcConfiguration.class)
                .withPropertyValues(
                        "spring.datasource.url=jdbc:h2:mem:copilot_primary;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                        "spring.datasource.driver-class-name=org.h2.Driver",
                        "spring.datasource.username=sa",
                        "spring.datasource.password=",
                        "copilot.finance.application-mysql-oracle.enabled=true",
                        "copilot.finance.application-mysql-oracle.jdbc-url=jdbc:h2:mem:finance_app_mysql_oracle_app_primary_guard;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                        "copilot.finance.application-mysql-oracle.driver-class-name=org.h2.Driver",
                        "copilot.finance.application-mysql-oracle.username=sa",
                        "copilot.finance.application-mysql-oracle.password=secret",
                        "copilot.finance.application-mysql-oracle.database=rs_cloud_flower",
                        "copilot.finance.application-mysql-oracle.copilot-jdbc-url=jdbc:h2:mem:finance_app_mysql_oracle_ads_primary_guard;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                        "copilot.finance.application-mysql-oracle.copilot-driver-class-name=org.h2.Driver",
                        "copilot.finance.application-mysql-oracle.copilot-username=sa",
                        "copilot.finance.application-mysql-oracle.copilot-password=secret",
                        "copilot.finance.application-mysql-oracle.copilot-database=prs.flowerbiz.federated")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("dataSource");
                    assertThat(context.getBeanNamesForType(DataSource.class))
                            .containsExactly("dataSource");
                    assertThat(context).hasBean("financeApplicationMysqlOracleJdbcQueryExecutor");
                    assertThat(context).hasBean("financeApplicationMysqlOracleCopilotJdbcQueryExecutor");
                });
    }
}
