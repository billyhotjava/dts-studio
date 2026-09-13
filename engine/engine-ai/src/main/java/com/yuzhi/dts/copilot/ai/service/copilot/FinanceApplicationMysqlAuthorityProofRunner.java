package com.yuzhi.dts.copilot.ai.service.copilot;

import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FinanceApplicationMysqlAuthorityProofRunner {

    private static final String AUTHORITY_PROOF_LABEL = "Finance application MySQL authority proof";

    private final FinanceApplicationMysqlAuthorityRegistry registry;
    private final FinanceApplicationMysqlAuthorityProofService proofService;
    private final FinanceApplicationMysqlAuthorityProofService.QueryExecutor copilotExecutor;
    private final FinanceApplicationMysqlAuthorityProofService.QueryExecutor applicationMysqlExecutor;

    @Autowired
    public FinanceApplicationMysqlAuthorityProofRunner(
            FinanceApplicationMysqlAuthorityRegistry registry,
            FinanceApplicationMysqlAuthorityProofService proofService,
            @Qualifier("financeApplicationMysqlAuthorityCopilotJdbcQueryExecutor")
                    ObjectProvider<FinanceApplicationMysqlAuthorityProofService.QueryExecutor> copilotExecutorProvider,
            @Qualifier("financeApplicationMysqlAuthorityJdbcQueryExecutor")
                    ObjectProvider<FinanceApplicationMysqlAuthorityProofService.QueryExecutor> applicationMysqlExecutorProvider) {
        this(
                registry,
                proofService,
                copilotExecutorProvider.getIfAvailable(),
                applicationMysqlExecutorProvider.getIfAvailable());
    }

    public FinanceApplicationMysqlAuthorityProofRunner(
            FinanceApplicationMysqlAuthorityRegistry registry,
            FinanceApplicationMysqlAuthorityProofService proofService,
            FinanceApplicationMysqlAuthorityProofService.QueryExecutor copilotExecutor,
            FinanceApplicationMysqlAuthorityProofService.QueryExecutor applicationMysqlExecutor) {
        this.registry = registry;
        this.proofService = proofService;
        this.copilotExecutor = copilotExecutor;
        this.applicationMysqlExecutor = applicationMysqlExecutor;
    }

    public RunResult proveAll() {
        if (!executorsConfigured()) {
            return disabled("");
        }
        List<FinanceApplicationMysqlAuthorityProofService.ProofReport> reports = registry.cases().stream()
                .map(authorityCase -> proofService.prove(authorityCase, copilotExecutor, applicationMysqlExecutor))
                .toList();
        return result("", reports);
    }

    public RunResult prove(String caseId) {
        String requestedCaseId = caseId == null ? "" : caseId.strip();
        if (!StringUtils.hasText(requestedCaseId)) {
            return proveAll();
        }
        return registry.caseById(requestedCaseId)
                .map(authorityCase -> {
                    if (!executorsConfigured()) {
                        return disabled(requestedCaseId);
                    }
                    try {
                        return result(
                                requestedCaseId,
                                List.of(proofService.prove(authorityCase, copilotExecutor, applicationMysqlExecutor)));
                    } catch (RuntimeException ex) {
                        return failed(requestedCaseId, ex);
                    }
                })
                .orElseGet(() -> new RunResult(
                        requestedCaseId,
                        RunStatus.NOT_FOUND,
                        AUTHORITY_PROOF_LABEL + " case not found: " + requestedCaseId,
                        List.of()));
    }

    private boolean executorsConfigured() {
        return copilotExecutor != null && applicationMysqlExecutor != null;
    }

    private static RunResult disabled(String caseId) {
        return new RunResult(
                caseId,
                RunStatus.DISABLED,
                AUTHORITY_PROOF_LABEL + " executors are not configured",
                List.of());
    }

    private static RunResult failed(String caseId, RuntimeException ex) {
        return new RunResult(
                caseId,
                RunStatus.FAILED,
                AUTHORITY_PROOF_LABEL + " failed: " + safeMessage(ex),
                List.of());
    }

    private static RunResult result(
            String caseId,
            List<FinanceApplicationMysqlAuthorityProofService.ProofReport> reports) {
        boolean passed = reports.stream().allMatch(FinanceApplicationMysqlAuthorityProofService.ProofReport::passed);
        String message = passed ? "" : reports.stream()
                .filter(report -> !report.passed())
                .map(FinanceApplicationMysqlAuthorityProofService.ProofReport::failureMessage)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(AUTHORITY_PROOF_LABEL + " failed");
        return new RunResult(caseId, passed ? RunStatus.PASSED : RunStatus.FAILED, message, reports);
    }

    private static String safeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        if (!StringUtils.hasText(message) && ex.getCause() != null) {
            message = ex.getCause().getMessage();
        }
        if (!StringUtils.hasText(message)) {
            return ex.getClass().getSimpleName();
        }
        return message.replaceAll("(?i)(password|pwd)=([^&;\\s]+)", "$1=***");
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
            List<FinanceApplicationMysqlAuthorityProofService.ProofReport> reports) {

        public RunResult {
            caseId = caseId == null ? "" : caseId;
            message = message == null ? "" : message;
            reports = reports == null ? List.of() : List.copyOf(reports);
        }
    }
}
