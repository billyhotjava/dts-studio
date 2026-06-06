package com.yuzhi.dts.copilot.ai.service.copilot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "copilot.finance.application-mysql-oracle")
public class FinanceApplicationMysqlOracleJdbcProperties {

    private boolean enabled;
    private String jdbcUrl = "";
    private String driverClassName = "com.mysql.cj.jdbc.Driver";
    private String username = "";
    private String password = "";
    private String database = "rs_cloud_flower";
    private String copilotJdbcUrl = "";
    private String copilotDriverClassName = "org.postgresql.Driver";
    private String copilotUsername = "";
    private String copilotPassword = "";
    private String copilotDatabase = "prs.flowerbiz.federated";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = textOrEmpty(jdbcUrl);
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = textOrEmpty(driverClassName);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = textOrEmpty(username);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = textOrEmpty(password);
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = textOrEmpty(database);
    }

    public String getCopilotJdbcUrl() {
        return copilotJdbcUrl;
    }

    public void setCopilotJdbcUrl(String copilotJdbcUrl) {
        this.copilotJdbcUrl = textOrEmpty(copilotJdbcUrl);
    }

    public String getCopilotDriverClassName() {
        return copilotDriverClassName;
    }

    public void setCopilotDriverClassName(String copilotDriverClassName) {
        this.copilotDriverClassName = textOrEmpty(copilotDriverClassName);
    }

    public String getCopilotUsername() {
        return copilotUsername;
    }

    public void setCopilotUsername(String copilotUsername) {
        this.copilotUsername = textOrEmpty(copilotUsername);
    }

    public String getCopilotPassword() {
        return copilotPassword;
    }

    public void setCopilotPassword(String copilotPassword) {
        this.copilotPassword = textOrEmpty(copilotPassword);
    }

    public String getCopilotDatabase() {
        return copilotDatabase;
    }

    public void setCopilotDatabase(String copilotDatabase) {
        this.copilotDatabase = textOrEmpty(copilotDatabase);
    }

    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
