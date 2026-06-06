package com.yuzhi.dts.copilot.ai.service.copilot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class FinanceApplicationMysqlOracleJdbcQueryExecutorTest {

    @Test
    void shouldQueryApplicationMysqlTablesThroughJdbc() {
        JdbcTemplate jdbcTemplate = mysqlModeJdbcTemplate();
        jdbcTemplate.execute("CREATE TABLE f_voucher (id BIGINT PRIMARY KEY, account_priod VARCHAR(16))");
        jdbcTemplate.execute("CREATE TABLE f_voucher_item (id BIGINT PRIMARY KEY, voucher_id BIGINT, status INT)");
        jdbcTemplate.update("INSERT INTO f_voucher (id, account_priod) VALUES (1, '2026-01'), (2, '2026-01'), (3, '2026-02')");
        jdbcTemplate.update("INSERT INTO f_voucher_item (id, voucher_id, status) VALUES (10, 1, 1), (11, 1, 1), (12, 2, 0), (13, 3, 1)");
        FinanceApplicationMysqlOracleJdbcQueryExecutor executor =
                new FinanceApplicationMysqlOracleJdbcQueryExecutor(jdbcTemplate, "rs_cloud_flower");

        List<Map<String, Object>> rows = executor.query("rs_cloud_flower",
                """
                SELECT v.account_priod AS accountPeriod,
                       COUNT(DISTINCT v.id) AS amount
                FROM f_voucher v
                JOIN f_voucher_item i ON i.voucher_id = v.id
                WHERE v.account_priod LIKE '2026-%'
                  AND COALESCE(i.status, 0) > 0
                GROUP BY v.account_priod
                ORDER BY v.account_priod
                """);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0))
                .containsEntry("accountPeriod", "2026-01")
                .containsEntry("amount", BigDecimal.ONE);
        assertThat(rows.get(1))
                .containsEntry("accountPeriod", "2026-02")
                .containsEntry("amount", BigDecimal.ONE);
    }

    @Test
    void shouldRejectUnexpectedDatabaseAndUnsafeSql() {
        FinanceApplicationMysqlOracleJdbcQueryExecutor executor =
                new FinanceApplicationMysqlOracleJdbcQueryExecutor(mysqlModeJdbcTemplate(), "rs_cloud_flower");

        assertThatThrownBy(() -> executor.query("warehouse", "SELECT 1 AS amount"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unexpected application MySQL oracle database");
        assertThatThrownBy(() -> executor.query("rs_cloud_flower", "UPDATE f_voucher SET id = 1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("read-only");
    }

    private static JdbcTemplate mysqlModeJdbcTemplate() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:finance_app_mysql_oracle_"
                + System.nanoTime()
                + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        return new JdbcTemplate(dataSource);
    }
}
