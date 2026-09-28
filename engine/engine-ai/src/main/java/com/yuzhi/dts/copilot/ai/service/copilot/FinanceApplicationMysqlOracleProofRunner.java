package com.yuzhi.dts.copilot.ai.service.copilot;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;

@Deprecated
public class FinanceApplicationMysqlOracleProofRunner extends FinanceApplicationMysqlAuthorityProofRunner {

    public FinanceApplicationMysqlOracleProofRunner(
            FinanceApplicationMysqlAuthorityRegistry registry,
            FinanceApplicationMysqlOracleProofService proofService,
            @Qualifier("financeApplicationMysqlAuthorityCopilotJdbcQueryExecutor")
                    ObjectProvider<FinanceApplicationMysqlOracleProofService.QueryExecutor> copilotExecutorProvider,
            @Qualifier("financeApplicationMysqlAuthorityJdbcQueryExecutor")
                    ObjectProvider<FinanceApplicationMysqlOracleProofService.QueryExecutor> applicationMysqlExecutorProvider) {
        super(
                registry,
                proofService,
                copilotExecutorProvider.getIfAvailable(),
                applicationMysqlExecutorProvider.getIfAvailable());
    }

    public FinanceApplicationMysqlOracleProofRunner(
            FinanceApplicationMysqlAuthorityRegistry registry,
            FinanceApplicationMysqlOracleProofService proofService,
            FinanceApplicationMysqlOracleProofService.QueryExecutor copilotExecutor,
            FinanceApplicationMysqlOracleProofService.QueryExecutor applicationMysqlExecutor) {
        super(registry, proofService, copilotExecutor, applicationMysqlExecutor);
    }
}
