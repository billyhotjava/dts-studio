package com.yuzhi.dts.copilot.ai.web.rest;

import com.yuzhi.dts.copilot.ai.service.copilot.FinanceApplicationMysqlOracleProofRunner;
import com.yuzhi.dts.copilot.ai.service.copilot.FinanceApplicationMysqlOracleRegistry;
import com.yuzhi.dts.copilot.ai.web.rest.dto.ApiResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/finance/application-mysql-oracle")
public class FinanceApplicationMysqlOracleProofResource {

    private final FinanceApplicationMysqlOracleRegistry registry;
    private final FinanceApplicationMysqlOracleProofRunner runner;

    public FinanceApplicationMysqlOracleProofResource(
            FinanceApplicationMysqlOracleRegistry registry,
            FinanceApplicationMysqlOracleProofRunner runner) {
        this.registry = registry;
        this.runner = runner;
    }

    @GetMapping("/cases")
    public ResponseEntity<ApiResponse<List<CaseSummary>>> cases() {
        List<CaseSummary> cases = registry.cases().stream()
                .map(oracleCase -> new CaseSummary(
                        oracleCase.id(),
                        oracleCase.oracleBindingId(),
                        oracleCase.chain(),
                        oracleCase.metricId(),
                        oracleCase.metricName(),
                        oracleCase.dimensionKeys()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(cases));
    }

    @PostMapping("/prove")
    public ResponseEntity<ApiResponse<FinanceApplicationMysqlOracleProofRunner.RunResult>> prove(
            @RequestBody(required = false) ProofRequest request) {
        String caseId = request == null ? "" : request.caseId();
        FinanceApplicationMysqlOracleProofRunner.RunResult result = runner.prove(caseId);
        return switch (result.status()) {
            case DISABLED -> ResponseEntity.status(409).body(ApiResponse.ok(result));
            case NOT_FOUND -> ResponseEntity.status(404).body(ApiResponse.ok(result));
            default -> ResponseEntity.ok(ApiResponse.ok(result));
        };
    }

    public record ProofRequest(String caseId) {}

    public record CaseSummary(
            String id,
            String oracleBindingId,
            String chain,
            String metricId,
            String metricName,
            List<String> dimensionKeys) {

        public CaseSummary {
            dimensionKeys = dimensionKeys == null ? List.of() : List.copyOf(dimensionKeys);
        }
    }
}
