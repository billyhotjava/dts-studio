package com.yuzhi.dts.copilot.ai.service.tool.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.service.tool.ToolConnectionProvider;
import com.yuzhi.dts.copilot.ai.service.tool.ToolContext;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

class SchemaLookupToolTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void failsWhenNoExternalDatasourceIsSelected() {
        SchemaLookupTool tool = new SchemaLookupTool(context -> {
            throw new IllegalArgumentException("Please select a datasource before using schema lookup.");
        });

        var result = tool.execute(new ToolContext("billy", "session-1", null), MAPPER.createObjectNode());

        assertThat(result.success()).isFalse();
        assertThat(result.output()).contains("select a datasource");
    }

    @Test
    void listsTablesFromSelectedExternalDatasource() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:schema_lookup_tool;MODE=MySQL;DB_CLOSE_DELAY=-1");

        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("create table customer_order (id bigint primary key, customer_name varchar(64))");
        }

        ToolConnectionProvider provider = context -> dataSource.getConnection();
        SchemaLookupTool tool = new SchemaLookupTool(provider);

        var result = tool.execute(new ToolContext("billy", "session-1", 42L), MAPPER.createObjectNode());

        assertThat(result.success()).isTrue();
        assertThat(result.output()).containsIgnoringCase("customer_order");
    }
}
