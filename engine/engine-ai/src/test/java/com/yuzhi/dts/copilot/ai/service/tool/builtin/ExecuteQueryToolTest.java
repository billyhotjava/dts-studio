package com.yuzhi.dts.copilot.ai.service.tool.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.service.safety.SqlSandbox;
import com.yuzhi.dts.copilot.ai.service.tool.ToolConnectionProvider;
import com.yuzhi.dts.copilot.ai.service.tool.ToolContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

class ExecuteQueryToolTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void executesAgainstSelectedExternalDatasource() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:execute_query_tool;MODE=MySQL;DB_CLOSE_DELAY=-1");

        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("create table sales_order (id bigint primary key, amount decimal(10,2))");
            statement.execute("insert into sales_order(id, amount) values (1, 99.90)");
        }

        ToolConnectionProvider provider = context -> dataSource.getConnection();
        ExecuteQueryTool tool = new ExecuteQueryTool(new SqlSandbox(), provider);

        var arguments = MAPPER.createObjectNode();
        arguments.put("sql", "select id, amount from sales_order");

        var result = tool.execute(new ToolContext("billy", "session-1", 42L), arguments);

        assertThat(result.success()).isTrue();
        assertThat(result.output()).contains("Query returned 1 row");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().get(0).get("ID").asLong()).isEqualTo(1L);
    }
}
