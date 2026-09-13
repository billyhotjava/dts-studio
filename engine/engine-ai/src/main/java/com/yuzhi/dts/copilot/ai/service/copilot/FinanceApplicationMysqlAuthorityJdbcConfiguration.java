package com.yuzhi.dts.copilot.ai.service.copilot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        FinanceApplicationMysqlAuthorityJdbcProperties.class,
        FinanceApplicationMysqlOracleJdbcProperties.class
})
public class FinanceApplicationMysqlAuthorityJdbcConfiguration {

    private static final String AUTHORITY_PREFIX = "copilot.finance.application-mysql-authority";
    private static final String LEGACY_PREFIX = "copilot.finance.application-mysql-oracle";

    @Bean(name = {
            "financeApplicationMysqlAuthorityJdbcQueryExecutor",
            "financeApplicationMysqlOracleJdbcQueryExecutor"
    })
    @ConditionalOnExpression("'${copilot.finance.application-mysql-authority.enabled:false}' == 'true' "
            + "|| '${copilot.finance.application-mysql-oracle.enabled:false}' == 'true'")
    public FinanceApplicationMysqlAuthorityProofService.QueryExecutor financeApplicationMysqlAuthorityJdbcQueryExecutor(
            FinanceApplicationMysqlAuthorityJdbcProperties authorityProperties,
            FinanceApplicationMysqlOracleJdbcProperties legacyProperties) {
        FinanceApplicationMysqlOracleJdbcProperties properties = properties(authorityProperties, legacyProperties);
        if (!StringUtils.hasText(properties.getJdbcUrl())) {
            throw new IllegalStateException(prefix(authorityProperties)
                    + ".jdbc-url is required when application MySQL authority proof is enabled");
        }
        return new FinanceApplicationMysqlOracleJdbcQueryExecutor(
                new JdbcTemplate(dataSource(
                        properties.getJdbcUrl(),
                        properties.getDriverClassName(),
                        properties.getUsername(),
                        properties.getPassword())),
                properties.getDatabase());
    }

    @Bean(name = {
            "financeApplicationMysqlAuthorityCopilotJdbcQueryExecutor",
            "financeApplicationMysqlOracleCopilotJdbcQueryExecutor"
    })
    @ConditionalOnExpression("('${copilot.finance.application-mysql-authority.enabled:false}' == 'true' "
            + "&& '${copilot.finance.application-mysql-authority.copilot-jdbc-url:}' != '') "
            + "|| ('${copilot.finance.application-mysql-oracle.enabled:false}' == 'true' "
            + "&& '${copilot.finance.application-mysql-oracle.copilot-jdbc-url:}' != '')")
    public FinanceApplicationMysqlAuthorityProofService.QueryExecutor financeApplicationMysqlAuthorityCopilotJdbcQueryExecutor(
            FinanceApplicationMysqlAuthorityJdbcProperties authorityProperties,
            FinanceApplicationMysqlOracleJdbcProperties legacyProperties) {
        FinanceApplicationMysqlOracleJdbcProperties properties = properties(authorityProperties, legacyProperties);
        return new FinanceApplicationMysqlOracleJdbcQueryExecutor(
                new JdbcTemplate(dataSource(
                        properties.getCopilotJdbcUrl(),
                        properties.getCopilotDriverClassName(),
                        properties.getCopilotUsername(),
                        properties.getCopilotPassword())),
                properties.getCopilotDatabase());
    }

    private FinanceApplicationMysqlOracleJdbcProperties properties(
            FinanceApplicationMysqlAuthorityJdbcProperties authorityProperties,
            FinanceApplicationMysqlOracleJdbcProperties legacyProperties) {
        return authorityProperties.isEnabled() ? authorityProperties : legacyProperties;
    }

    private String prefix(FinanceApplicationMysqlAuthorityJdbcProperties authorityProperties) {
        return authorityProperties.isEnabled() ? AUTHORITY_PREFIX : LEGACY_PREFIX;
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
