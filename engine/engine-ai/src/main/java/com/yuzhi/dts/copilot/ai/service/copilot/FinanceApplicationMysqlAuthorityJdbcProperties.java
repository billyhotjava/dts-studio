package com.yuzhi.dts.copilot.ai.service.copilot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "copilot.finance.application-mysql-authority")
public class FinanceApplicationMysqlAuthorityJdbcProperties extends FinanceApplicationMysqlOracleJdbcProperties {
}
