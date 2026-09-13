package com.yuzhi.dts.copilot.ai.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.service.copilot.FinanceApplicationMysqlAuthorityProofRunner;
import com.yuzhi.dts.copilot.ai.service.copilot.FinanceApplicationMysqlAuthorityProofService;
import com.yuzhi.dts.copilot.ai.service.copilot.FinanceApplicationMysqlAuthorityRegistry;
import com.yuzhi.dts.copilot.ai.service.copilot.FinanceAuthorityRegistry;
import com.yuzhi.dts.copilot.ai.service.copilot.FinanceSummaryDualReconciliationService;
import com.yuzhi.dts.copilot.ai.web.rest.dto.ApiResponse;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class FinanceApplicationMysqlAuthorityProofResourceTest {

    @Test
    void shouldExposeFinanceApplicationMysqlAuthorityProofEndpointsWithLegacyOracleAlias() throws Exception {
        RequestMapping mapping = FinanceApplicationMysqlAuthorityProofResource.class.getAnnotation(RequestMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly(
                "/api/ai/finance/application-mysql-authority",
                "/api/ai/finance/application-mysql-oracle");
        assertThat(FinanceApplicationMysqlAuthorityProofResource.class.getDeclaredMethod("cases")
                .getAnnotation(GetMapping.class).value()).containsExactly("/cases");
        assertThat(FinanceApplicationMysqlAuthorityProofResource.class.getDeclaredMethod(
                        "prove",
                        FinanceApplicationMysqlAuthorityProofResource.ProofRequest.class)
                .getAnnotation(PostMapping.class).value()).containsExactly("/prove");
    }

    @Test
    void shouldListProofCasesWithoutExecutingSql() {
        FinanceApplicationMysqlAuthorityProofResource resource = resource(false);

        ResponseEntity<ApiResponse<List<FinanceApplicationMysqlAuthorityProofResource.CaseSummary>>> response =
                resource.cases();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data())
                .extracting(FinanceApplicationMysqlAuthorityProofResource.CaseSummary::id)
                .containsExactly(
                        "month-settlement-discounted-receivable",
                        "sale-account-receivable",
                        "voucher-year-2026-count");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldExposeAuthorityBindingIdInCaseSummariesWithLegacyOracleBindingAlias() {
        FinanceApplicationMysqlAuthorityProofResource resource = resource(false);

        ResponseEntity<ApiResponse<List<FinanceApplicationMysqlAuthorityProofResource.CaseSummary>>> response =
                resource.cases();

        assertThat(response.getBody()).isNotNull();
        Map<String, Object> firstCase = new ObjectMapper()
                .convertValue(response.getBody().data().getFirst(), Map.class);
        assertThat(firstCase)
                .containsEntry("authorityBindingId", "month-settlement")
                .containsEntry("oracleBindingId", "month-settlement");
    }

    @Test
    void shouldReturnConflictWhenProofExecutorsAreNotConfigured() {
        FinanceApplicationMysqlAuthorityProofResource resource = resource(false);

        ResponseEntity<ApiResponse<FinanceApplicationMysqlAuthorityProofRunner.RunResult>> response =
                resource.prove(new FinanceApplicationMysqlAuthorityProofResource.ProofRequest("voucher-year-2026-count"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().status())
                .isEqualTo(FinanceApplicationMysqlAuthorityProofRunner.RunStatus.DISABLED);
    }

    @Test
    void shouldReturnNotFoundForUnknownProofCase() {
        FinanceApplicationMysqlAuthorityProofResource resource = resource(true);

        ResponseEntity<ApiResponse<FinanceApplicationMysqlAuthorityProofRunner.RunResult>> response =
                resource.prove(new FinanceApplicationMysqlAuthorityProofResource.ProofRequest("missing-case"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().status())
                .isEqualTo(FinanceApplicationMysqlAuthorityProofRunner.RunStatus.NOT_FOUND);
    }

    @Test
    void shouldRunProofCaseWhenExecutorsAreConfigured() {
        FinanceApplicationMysqlAuthorityProofResource resource = resource(true);

        ResponseEntity<ApiResponse<FinanceApplicationMysqlAuthorityProofRunner.RunResult>> response =
                resource.prove(new FinanceApplicationMysqlAuthorityProofResource.ProofRequest("voucher-year-2026-count"));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().status())
                .isEqualTo(FinanceApplicationMysqlAuthorityProofRunner.RunStatus.PASSED);
    }

    private FinanceApplicationMysqlAuthorityProofResource resource(boolean configured) {
        ObjectMapper objectMapper = new ObjectMapper();
        FinanceAuthorityRegistry authorityRegistry = new FinanceAuthorityRegistry(objectMapper);
        authorityRegistry.init();
        FinanceApplicationMysqlAuthorityRegistry registry =
                new FinanceApplicationMysqlAuthorityRegistry(objectMapper, authorityRegistry);
        registry.init();
        FinanceApplicationMysqlAuthorityProofService service =
                new FinanceApplicationMysqlAuthorityProofService(new FinanceSummaryDualReconciliationService());
        FinanceApplicationMysqlAuthorityProofService.QueryExecutor executor = configured
                ? (database, nativeSql) -> List.of(row())
                : null;
        return new FinanceApplicationMysqlAuthorityProofResource(
                registry,
                new FinanceApplicationMysqlAuthorityProofRunner(registry, service, executor, executor));
    }

    private static Map<String, Object> row() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("chain", "voucher-ledger");
        row.put("metricId", "voucher-count");
        row.put("accountPeriod", "2026-01");
        row.put("amount", new BigDecimal("31.00"));
        return row;
    }
}
