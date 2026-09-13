package com.yuzhi.dts.copilot.ai.web.rest;

import com.yuzhi.dts.copilot.ai.service.copilot.FinanceReconciliationScorecardService;
import com.yuzhi.dts.copilot.ai.service.copilot.FinanceSignoffBaselineRegistry;
import com.yuzhi.dts.copilot.ai.service.copilot.FinanceSignoffBaselineService;
import com.yuzhi.dts.copilot.ai.web.rest.dto.ApiResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/finance/signoff-baselines")
public class FinanceSignoffBaselineResource {

    private final FinanceSignoffBaselineRegistry registry;
    private final FinanceSignoffBaselineService service;

    public FinanceSignoffBaselineResource(
            FinanceSignoffBaselineRegistry registry,
            FinanceSignoffBaselineService service) {
        this.registry = registry;
        this.service = service;
    }

    @GetMapping("/policies")
    public ResponseEntity<ApiResponse<List<PolicySummary>>> policies() {
        List<PolicySummary> policies = registry.policies().stream()
                .map(policy -> new PolicySummary(
                        policy.id(),
                        policy.title(),
                        policy.accountPeriod(),
                        policy.scorecardPolicyId(),
                        policy.requiredEvidence().stream()
                                .map(FinanceSignoffBaselineRegistry.RequiredEvidence::id)
                                .toList(),
                        policy.requiredSignatureRoles().stream()
                                .map(FinanceSignoffBaselineRegistry.RequiredSignatureRole::role)
                                .toList(),
                        policy.assetPath(),
                        policy.itScript()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(policies));
    }

    @PostMapping("/build")
    public ResponseEntity<ApiResponse<FinanceSignoffBaselineService.SignoffBaselineReport>> build(
            @RequestBody(required = false) BuildRequest request) {
        String policyId = request == null ? "" : request.policyId();
        if (policyId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("policyId is required"));
        }
        return registry.policy(policyId)
                .map(policy -> ResponseEntity.ok(ApiResponse.ok(service.buildBaseline(
                        policy,
                        request.scorecardReport(),
                        request.evidenceRecords(),
                        request.signatures()))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Finance signoff baseline policy not found: " + policyId)));
    }

    public record BuildRequest(
            String policyId,
            FinanceReconciliationScorecardService.ScorecardReport scorecardReport,
            List<FinanceSignoffBaselineService.EvidenceRecord> evidenceRecords,
            List<FinanceSignoffBaselineService.SignatureRecord> signatures) {
        public BuildRequest {
            policyId = policyId == null ? "" : policyId;
            evidenceRecords = evidenceRecords == null ? List.of() : List.copyOf(evidenceRecords);
            signatures = signatures == null ? List.of() : List.copyOf(signatures);
        }
    }

    public record PolicySummary(
            String id,
            String title,
            String accountPeriod,
            String scorecardPolicyId,
            List<String> requiredEvidence,
            List<String> requiredSignatureRoles,
            String assetPath,
            String itScript) {
        public PolicySummary {
            requiredEvidence = requiredEvidence == null ? List.of() : List.copyOf(requiredEvidence);
            requiredSignatureRoles = requiredSignatureRoles == null ? List.of() : List.copyOf(requiredSignatureRoles);
        }
    }
}
