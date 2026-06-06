package com.yuzhi.dts.copilot.ai.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.ai.service.copilot.FinanceApplicationMysqlOracleProofRunner;
import com.yuzhi.dts.copilot.ai.service.copilot.FinanceApplicationMysqlOracleProofService;
import com.yuzhi.dts.copilot.ai.service.copilot.FinanceApplicationMysqlOracleRegistry;
import com.yuzhi.dts.copilot.ai.service.copilot.FinanceOracleRegistry;
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

class FinanceApplicationMysqlOracleProofResourceTest {

    @Test
    void shouldExposeFinanceApplicationMysqlOracleProofEndpoints() throws Exception {
        RequestMapping mapping = FinanceApplicationMysqlOracleProofResource.class.getAnnotation(RequestMapping.class);
        assertThat(mapping).isNotNull();
        assertThat(mapping.value()).containsExactly("/api/ai/finance/application-mysql-oracle");
        assertThat(FinanceApplicationMysqlOracleProofResource.class.getDeclaredMethod("cases")
                .getAnnotation(GetMapping.class).value()).containsExactly("/cases");
        assertThat(FinanceApplicationMysqlOracleProofResource.class.getDeclaredMethod(
                        "prove",
                        FinanceApplicationMysqlOracleProofResource.ProofRequest.class)
                .getAnnotation(PostMapping.class).value()).containsExactly("/prove");
    }

    @Test
    void shouldListProofCasesWithoutExecutingSql() {
        FinanceApplicationMysqlOracleProofResource resource = resource(false);

        ResponseEntity<ApiResponse<List<FinanceApplicationMysqlOracleProofResource.CaseSummary>>> response =
                resource.cases();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data())
                .extracting(FinanceApplicationMysqlOracleProofResource.CaseSummary::id)
                .containsExactly(
                        "month-settlement-discounted-receivable",
                        "sale-account-receivable",
                        "voucher-year-2026-count");
    }

    @Test
    void shouldReturnConflictWhenProofExecutorsAreNotConfigured() {
        FinanceApplicationMysqlOracleProofResource resource = resource(false);

        ResponseEntity<ApiResponse<FinanceApplicationMysqlOracleProofRunner.RunResult>> response =
                resource.prove(new FinanceApplicationMysqlOracleProofResource.ProofRequest("voucher-year-2026-count"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().status())
                .isEqualTo(FinanceApplicationMysqlOracleProofRunner.RunStatus.DISABLED);
    }

    @Test
    void shouldReturnNotFoundForUnknownProofCase() {
        FinanceApplicationMysqlOracleProofResource resource = resource(true);

        ResponseEntity<ApiResponse<FinanceApplicationMysqlOracleProofRunner.RunResult>> response =
                resource.prove(new FinanceApplicationMysqlOracleProofResource.ProofRequest("missing-case"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().status())
                .isEqualTo(FinanceApplicationMysqlOracleProofRunner.RunStatus.NOT_FOUND);
    }

    @Test
    void shouldRunProofCaseWhenExecutorsAreConfigured() {
        FinanceApplicationMysqlOracleProofResource resource = resource(true);

        ResponseEntity<ApiResponse<FinanceApplicationMysqlOracleProofRunner.RunResult>> response =
                resource.prove(new FinanceApplicationMysqlOracleProofResource.ProofRequest("voucher-year-2026-count"));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().status())
                .isEqualTo(FinanceApplicationMysqlOracleProofRunner.RunStatus.PASSED);
    }

    private FinanceApplicationMysqlOracleProofResource resource(boolean configured) {
        ObjectMapper objectMapper = new ObjectMapper();
        FinanceOracleRegistry oracleRegistry = new FinanceOracleRegistry(objectMapper);
        oracleRegistry.init();
        FinanceApplicationMysqlOracleRegistry registry =
                new FinanceApplicationMysqlOracleRegistry(objectMapper, oracleRegistry);
        registry.init();
        FinanceApplicationMysqlOracleProofService service =
                new FinanceApplicationMysqlOracleProofService(new FinanceSummaryDualReconciliationService());
        FinanceApplicationMysqlOracleProofService.QueryExecutor executor = configured
                ? (database, nativeSql) -> List.of(row())
                : null;
        return new FinanceApplicationMysqlOracleProofResource(
                registry,
                new FinanceApplicationMysqlOracleProofRunner(registry, service, executor, executor));
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
