package com.yuzhi.dts.copilot.ai.service.copilot;

import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FinanceApplicationMysqlOracleProofRunner {

    private final FinanceApplicationMysqlOracleRegistry registry;
    private final FinanceApplicationMysqlOracleProofService proofService;
    private final FinanceApplicationMysqlOracleProofService.QueryExecutor copilotExecutor;
    private final FinanceApplicationMysqlOracleProofService.QueryExecutor applicationMysqlExecutor;

    public FinanceApplicationMysqlOracleProofRunner(
            FinanceApplicationMysqlOracleRegistry registry,
            FinanceApplicationMysqlOracleProofService proofService,
            @Qualifier("financeApplicationMysqlOracleCopilotJdbcQueryExecutor")
                    ObjectProvider<FinanceApplicationMysqlOracleProofService.QueryExecutor> copilotExecutorProvider,
            @Qualifier("financeApplicationMysqlOracleJdbcQueryExecutor")
                    ObjectProvider<FinanceApplicationMysqlOracleProofService.QueryExecutor> applicationMysqlExecutorProvider) {
        this(
                registry,
                proofService,
                copilotExecutorProvider.getIfAvailable(),
                applicationMysqlExecutorProvider.getIfAvailable());
    }

    public FinanceApplicationMysqlOracleProofRunner(
            FinanceApplicationMysqlOracleRegistry registry,
            FinanceApplicationMysqlOracleProofService proofService,
            FinanceApplicationMysqlOracleProofService.QueryExecutor copilotExecutor,
            FinanceApplicationMysqlOracleProofService.QueryExecutor applicationMysqlExecutor) {
        this.registry = registry;
        this.proofService = proofService;
        this.copilotExecutor = copilotExecutor;
        this.applicationMysqlExecutor = applicationMysqlExecutor;
    }

    public RunResult proveAll() {
        if (!executorsConfigured()) {
            return disabled("");
        }
        List<FinanceApplicationMysqlOracleProofService.ProofReport> reports = registry.cases().stream()
                .map(oracleCase -> proofService.prove(oracleCase, copilotExecutor, applicationMysqlExecutor))
                .toList();
        return result("", reports);
    }

    public RunResult prove(String caseId) {
        String requestedCaseId = caseId == null ? "" : caseId.strip();
        if (!StringUtils.hasText(requestedCaseId)) {
            return proveAll();
        }
        return registry.caseById(requestedCaseId)
                .map(oracleCase -> {
                    if (!executorsConfigured()) {
                        return disabled(requestedCaseId);
                    }
                    return result(
                            requestedCaseId,
                            List.of(proofService.prove(oracleCase, copilotExecutor, applicationMysqlExecutor)));
                })
                .orElseGet(() -> new RunResult(
                        requestedCaseId,
                        RunStatus.NOT_FOUND,
                        "Finance application MySQL oracle proof case not found: " + requestedCaseId,
                        List.of()));
    }

    private boolean executorsConfigured() {
        return copilotExecutor != null && applicationMysqlExecutor != null;
    }

    private static RunResult disabled(String caseId) {
        return new RunResult(
                caseId,
                RunStatus.DISABLED,
                "Finance application MySQL oracle proof executors are not configured",
                List.of());
    }

    private static RunResult result(
            String caseId,
            List<FinanceApplicationMysqlOracleProofService.ProofReport> reports) {
        boolean passed = reports.stream().allMatch(FinanceApplicationMysqlOracleProofService.ProofReport::passed);
        String message = passed ? "" : reports.stream()
                .filter(report -> !report.passed())
                .map(FinanceApplicationMysqlOracleProofService.ProofReport::failureMessage)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("Finance application MySQL oracle proof failed");
        return new RunResult(caseId, passed ? RunStatus.PASSED : RunStatus.FAILED, message, reports);
    }

    public enum RunStatus {
        PASSED,
        FAILED,
        DISABLED,
        NOT_FOUND
    }

    public record RunResult(
            String caseId,
            RunStatus status,
            String message,
            List<FinanceApplicationMysqlOracleProofService.ProofReport> reports) {

        public RunResult {
            caseId = caseId == null ? "" : caseId;
            message = message == null ? "" : message;
            reports = reports == null ? List.of() : List.copyOf(reports);
        }
    }
}
