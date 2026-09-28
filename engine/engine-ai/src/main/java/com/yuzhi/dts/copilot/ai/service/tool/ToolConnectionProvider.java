package com.yuzhi.dts.copilot.ai.service.tool;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface ToolConnectionProvider {

    Connection openConnection(ToolContext context) throws SQLException;
}
