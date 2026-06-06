package com.yuzhi.dts.copilot.ai.service.copilot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FinanceApplicationMysqlOracleJdbcProperties.class)
public class FinanceApplicationMysqlOracleJdbcConfiguration {

    private static final String PREFIX = "copilot.finance.application-mysql-oracle";

    @Bean("financeApplicationMysqlOracleJdbcQueryExecutor")
    @ConditionalOnProperty(prefix = PREFIX, name = "enabled", havingValue = "true")
    public FinanceApplicationMysqlOracleProofService.QueryExecutor financeApplicationMysqlOracleJdbcQueryExecutor(
            FinanceApplicationMysqlOracleJdbcProperties properties) {
        if (!StringUtils.hasText(properties.getJdbcUrl())) {
            throw new IllegalStateException(PREFIX + ".jdbc-url is required when application MySQL oracle is enabled");
        }
        return new FinanceApplicationMysqlOracleJdbcQueryExecutor(
                new JdbcTemplate(dataSource(
                        properties.getJdbcUrl(),
                        properties.getDriverClassName(),
                        properties.getUsername(),
                        properties.getPassword())),
                properties.getDatabase());
    }

    @Bean("financeApplicationMysqlOracleCopilotJdbcQueryExecutor")
    @ConditionalOnExpression("'${copilot.finance.application-mysql-oracle.enabled:false}' == 'true' "
            + "&& '${copilot.finance.application-mysql-oracle.copilot-jdbc-url:}' != ''")
    public FinanceApplicationMysqlOracleProofService.QueryExecutor financeApplicationMysqlOracleCopilotJdbcQueryExecutor(
            FinanceApplicationMysqlOracleJdbcProperties properties) {
        return new FinanceApplicationMysqlOracleJdbcQueryExecutor(
                new JdbcTemplate(dataSource(
                        properties.getCopilotJdbcUrl(),
                        properties.getCopilotDriverClassName(),
                        properties.getCopilotUsername(),
                        properties.getCopilotPassword())),
                properties.getCopilotDatabase());
    }

    private DriverManagerDataSource dataSource(
            String jdbcUrl,
            String driverClassName,
            String username,
            String password) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(jdbcUrl);
        if (StringUtils.hasText(driverClassName)) {
            dataSource.setDriverClassName(driverClassName);
        }
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }
}
