package com.yuzhi.dts.copilot.ai.service.copilot;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
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

    @Bean("financeApplicationMysqlOracleJdbcDataSource")
    @ConditionalOnProperty(prefix = PREFIX, name = "enabled", havingValue = "true")
    public DataSource financeApplicationMysqlOracleJdbcDataSource(
            FinanceApplicationMysqlOracleJdbcProperties properties) {
        if (!StringUtils.hasText(properties.getJdbcUrl())) {
            throw new IllegalStateException(PREFIX + ".jdbc-url is required when application MySQL oracle is enabled");
        }
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(properties.getJdbcUrl());
        if (StringUtils.hasText(properties.getDriverClassName())) {
            dataSource.setDriverClassName(properties.getDriverClassName());
        }
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        return dataSource;
    }

    @Bean("financeApplicationMysqlOracleJdbcQueryExecutor")
    @ConditionalOnProperty(prefix = PREFIX, name = "enabled", havingValue = "true")
    public FinanceApplicationMysqlOracleProofService.QueryExecutor financeApplicationMysqlOracleJdbcQueryExecutor(
            @Qualifier("financeApplicationMysqlOracleJdbcDataSource") DataSource dataSource,
            FinanceApplicationMysqlOracleJdbcProperties properties) {
        return new FinanceApplicationMysqlOracleJdbcQueryExecutor(
                new JdbcTemplate(dataSource),
                properties.getDatabase());
    }

    @Bean("financeApplicationMysqlOracleCopilotJdbcDataSource")
    @ConditionalOnExpression("'${copilot.finance.application-mysql-oracle.enabled:false}' == 'true' "
            + "&& '${copilot.finance.application-mysql-oracle.copilot-jdbc-url:}' != ''")
    public DataSource financeApplicationMysqlOracleCopilotJdbcDataSource(
            FinanceApplicationMysqlOracleJdbcProperties properties) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(properties.getCopilotJdbcUrl());
        if (StringUtils.hasText(properties.getCopilotDriverClassName())) {
            dataSource.setDriverClassName(properties.getCopilotDriverClassName());
        }
        dataSource.setUsername(properties.getCopilotUsername());
        dataSource.setPassword(properties.getCopilotPassword());
        return dataSource;
    }

    @Bean("financeApplicationMysqlOracleCopilotJdbcQueryExecutor")
    @ConditionalOnExpression("'${copilot.finance.application-mysql-oracle.enabled:false}' == 'true' "
            + "&& '${copilot.finance.application-mysql-oracle.copilot-jdbc-url:}' != ''")
    public FinanceApplicationMysqlOracleProofService.QueryExecutor financeApplicationMysqlOracleCopilotJdbcQueryExecutor(
            @Qualifier("financeApplicationMysqlOracleCopilotJdbcDataSource") DataSource dataSource,
            FinanceApplicationMysqlOracleJdbcProperties properties) {
        return new FinanceApplicationMysqlOracleJdbcQueryExecutor(
                new JdbcTemplate(dataSource),
                properties.getCopilotDatabase());
    }
}
