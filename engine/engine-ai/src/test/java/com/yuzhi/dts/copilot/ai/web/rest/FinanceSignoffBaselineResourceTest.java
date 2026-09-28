package com.yuzhi.dts.copilot.ai.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.service.copilot.FinanceReconciliationScorecardService;
import com.yuzhi.dts.copilot.ai.service.copilot.FinanceSignoffBaselineRegistry;
import com.yuzhi.dts.copilot.ai.service.copilot.FinanceSignoffBaselineService;
import com.yuzhi.dts.copilot.ai.web.rest.dto.ApiResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class FinanceSignoffBaselineResourceTest {

    @Test
    void shouldExposeFinanceSignoffBaselineEndpoints() throws Exception {
        RequestMapping mapping = FinanceSignoffBaselineResource.class.getAnnotation(RequestMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/api/ai/finance/signoff-baselines");
        assertThat(FinanceSignoffBaselineResource.class.getDeclaredMethod("policies")
                .getAnnotation(GetMapping.class).value()).containsExactly("/policies");
        assertThat(FinanceSignoffBaselineResource.class.getDeclaredMethod(
                        "build",
                        FinanceSignoffBaselineResource.BuildRequest.class)
                .getAnnotation(PostMapping.class).value()).containsExactly("/build");
    }

    @Test
    void shouldListSignoffBaselinePoliciesWithoutBuildingReports() {
        FinanceSignoffBaselineResource resource = resource();

        ResponseEntity<ApiResponse<List<FinanceSignoffBaselineResource.PolicySummary>>> response = resource.policies();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data())
                .extracting(FinanceSignoffBaselineResource.PolicySummary::id)
                .containsExactly("sprint33-finance-signoff-baseline");
        assertThat(response.getBody().data().getFirst().requiredSignatureRoles())
                .containsExactly("FINANCE_OWNER", "AUDITOR");
    }

    @Test
    void shouldBuildPendingSignatureBaselineForKnownPolicy() {
        FinanceSignoffBaselineRegistry.SignoffBaselinePolicy policy = policy();
        FinanceSignoffBaselineResource resource = resource();
        FinanceSignoffBaselineResource.BuildRequest request = new FinanceSignoffBaselineResource.BuildRequest(
                policy.id(),
                passingScorecard(),
                evidenceRecords(policy.requiredEvidence()),
                List.of());

        ResponseEntity<ApiResponse<FinanceSignoffBaselineService.SignoffBaselineReport>> response =
                resource.build(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        FinanceSignoffBaselineService.SignoffBaselineReport report = response.getBody().data();
        assertThat(report.baselineId()).isEqualTo("sprint33-finance-signoff-baseline");
        assertThat(report.engineeringReady()).isTrue();
        assertThat(report.accepted()).isFalse();
        assertThat(report.signoffStatus()).isEqualTo("PENDING_SIGNATURE");
        assertThat(report.baselineMarkdown()).contains("未采信", "PENDING_SIGNATURE");
    }

    @Test
    void shouldReturnNotFoundForUnknownPolicy() {
        FinanceSignoffBaselineResource resource = resource();
        FinanceSignoffBaselineResource.BuildRequest request = new FinanceSignoffBaselineResource.BuildRequest(
                "missing-policy",
                passingScorecard(),
                List.of(),
                List.of());

        ResponseEntity<ApiResponse<FinanceSignoffBaselineService.SignoffBaselineReport>> response =
                resource.build(request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().error()).contains("missing-policy");
    }

    private static FinanceSignoffBaselineResource resource() {
        return new FinanceSignoffBaselineResource(registry(), new FinanceSignoffBaselineService());
    }

    private static FinanceSignoffBaselineRegistry.SignoffBaselinePolicy policy() {
        return registry().policy("sprint33-finance-signoff-baseline").orElseThrow();
    }

    private static FinanceSignoffBaselineRegistry registry() {
        FinanceSignoffBaselineRegistry registry = new FinanceSignoffBaselineRegistry(new ObjectMapper());
        registry.init();
        return registry;
    }

    private static FinanceReconciliationScorecardService.ScorecardReport passingScorecard() {
        return new FinanceReconciliationScorecardService.ScorecardReport(
                true,
                false,
                "PASS",
                4,
                4,
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                List.of(),
                List.of(),
                List.of(),
                "");
    }

    private static List<FinanceSignoffBaselineService.EvidenceRecord> evidenceRecords(
            List<FinanceSignoffBaselineRegistry.RequiredEvidence> requiredEvidence) {
        return requiredEvidence.stream()
                .map(evidence -> new FinanceSignoffBaselineService.EvidenceRecord(
                        evidence.id(),
                        evidence.feature(),
                        evidence.command(),
                        evidence.evidencePath(),
                        "PASS"))
                .toList();
    }
}
