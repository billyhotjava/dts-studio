package com.yuzhi.dts.copilot.ai.web.rest;

import com.yuzhi.dts.copilot.ai.service.copilot.FinanceApplicationMysqlAuthorityRegistry;
import com.yuzhi.dts.copilot.ai.service.copilot.FinanceApplicationMysqlAuthorityProofRunner;

@Deprecated(since = "sprint-33", forRemoval = false)
public class FinanceApplicationMysqlOracleProofResource extends FinanceApplicationMysqlAuthorityProofResource {

    public FinanceApplicationMysqlOracleProofResource(
            FinanceApplicationMysqlAuthorityRegistry registry,
            FinanceApplicationMysqlAuthorityProofRunner runner) {
        super(registry, runner);
    }
}
