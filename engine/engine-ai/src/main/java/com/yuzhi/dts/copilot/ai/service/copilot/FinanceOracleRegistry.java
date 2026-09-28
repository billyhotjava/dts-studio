package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.databind.ObjectMapper;

@Deprecated(since = "sprint-33", forRemoval = false)
public class FinanceOracleRegistry extends FinanceAuthorityRegistry {

    public FinanceOracleRegistry(ObjectMapper objectMapper) {
        super(objectMapper);
    }
}
