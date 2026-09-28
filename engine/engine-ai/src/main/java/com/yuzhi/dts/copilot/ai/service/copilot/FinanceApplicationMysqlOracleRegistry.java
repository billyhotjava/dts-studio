package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.databind.ObjectMapper;

@Deprecated(since = "sprint-33", forRemoval = false)
public class FinanceApplicationMysqlOracleRegistry extends FinanceApplicationMysqlAuthorityRegistry {

    public FinanceApplicationMysqlOracleRegistry(
            ObjectMapper objectMapper,
            FinanceAuthorityRegistry financeAuthorityRegistry) {
        super(objectMapper, financeAuthorityRegistry);
    }
}
