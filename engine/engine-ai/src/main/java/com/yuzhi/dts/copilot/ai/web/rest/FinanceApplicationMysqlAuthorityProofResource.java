package com.yuzhi.dts.copilot.ai.web.rest;

import com.yuzhi.dts.copilot.ai.service.copilot.FinanceApplicationMysqlAuthorityRegistry;
import com.yuzhi.dts.copilot.ai.service.copilot.FinanceApplicationMysqlAuthorityProofRunner;
import com.yuzhi.dts.copilot.ai.web.rest.dto.ApiResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({
        "/api/ai/finance/application-mysql-authority",
        "/api/ai/finance/application-mysql-oracle"
})
public class FinanceApplicationMysqlAuthorityProofResource {

    private final FinanceApplicationMysqlAuthorityRegistry registry;
    private final FinanceApplicationMysqlAuthorityProofRunner runner;

    public FinanceApplicationMysqlAuthorityProofResource(
            FinanceApplicationMysqlAuthorityRegistry registry,
            FinanceApplicationMysqlAuthorityProofRunner runner) {
        this.registry = registry;
        this.runner = runner;
    }

    @GetMapping("/cases")
    public ResponseEntity<ApiResponse<List<CaseSummary>>> cases() {
        List<CaseSummary> cases = registry.cases().stream()
                .map(authorityCase -> new CaseSummary(
                        authorityCase.id(),
                        authorityCase.authorityBindingId(),
                        authorityCase.oracleBindingId(),
                        authorityCase.chain(),
                        authorityCase.metricId(),
                        authorityCase.metricName(),
                        authorityCase.dimensionKeys()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(cases));
    }

    @PostMapping("/prove")
    public ResponseEntity<ApiResponse<FinanceApplicationMysqlAuthorityProofRunner.RunResult>> prove(
            @RequestBody(required = false) ProofRequest request) {
        String caseId = request == null ? "" : request.caseId();
        FinanceApplicationMysqlAuthorityProofRunner.RunResult result = runner.prove(caseId);
        return switch (result.status()) {
            case DISABLED -> ResponseEntity.status(409).body(ApiResponse.ok(result));
            case NOT_FOUND -> ResponseEntity.status(404).body(ApiResponse.ok(result));
            default -> ResponseEntity.ok(ApiResponse.ok(result));
        };
    }

    public record ProofRequest(String caseId) {}

    public record CaseSummary(
            String id,
            String authorityBindingId,
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
