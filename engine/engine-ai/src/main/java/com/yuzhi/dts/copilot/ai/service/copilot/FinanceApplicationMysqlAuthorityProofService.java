package com.yuzhi.dts.copilot.ai.service.copilot;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class FinanceApplicationMysqlAuthorityProofService {

    public static final String AUTHORITY_SOURCE = "APPLICATION_MYSQL";
    public static final String ORACLE_SOURCE = AUTHORITY_SOURCE;

    private final FinanceSummaryDualReconciliationService reconciliationService;

    public FinanceApplicationMysqlAuthorityProofService(FinanceSummaryDualReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    public ProofReport prove(
            FinanceApplicationMysqlAuthorityRegistry.AuthoritySqlCase authorityCase,
            QueryExecutor copilotExecutor,
            QueryExecutor applicationMysqlExecutor) {
        FinanceApplicationMysqlAuthorityRegistry.AuthoritySqlCase safeCase = authorityCase == null
                ? new FinanceApplicationMysqlAuthorityRegistry.AuthoritySqlCase(
                        "", "", "", "", "", List.of(), "", null, null, "")
                : authorityCase;
        List<FinanceSummaryDualReconciliationService.SummaryRow> copilotRows = toSummaryRows(
                safeCase,
                copilotExecutor.query(safeCase.copilotQuery().database(), safeCase.copilotQuery().nativeSql()));
        List<FinanceSummaryDualReconciliationService.SummaryRow> authorityRows = toSummaryRows(
                safeCase,
                applicationMysqlExecutor.query(
                        safeCase.applicationMysqlQuery().database(),
                        safeCase.applicationMysqlQuery().nativeSql()));
        FinanceSummaryDualReconciliationService.SummaryReconciliationReport reconciliation =
                reconciliationService.reconcile(safeCase.reconciliationSpec(), copilotRows, authorityRows);
        return new ProofReport(
                safeCase.id(),
                AUTHORITY_SOURCE,
                reconciliation.passed(),
                reconciliation.failureMessage(),
                reconciliation);
    }

    private static List<FinanceSummaryDualReconciliationService.SummaryRow> toSummaryRows(
            FinanceApplicationMysqlAuthorityRegistry.AuthoritySqlCase authorityCase,
            List<Map<String, Object>> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
                .map(row -> toSummaryRow(authorityCase, row))
                .toList();
    }

    private static FinanceSummaryDualReconciliationService.SummaryRow toSummaryRow(
            FinanceApplicationMysqlAuthorityRegistry.AuthoritySqlCase authorityCase,
            Map<String, Object> row) {
        Map<String, Object> safeRow = row == null ? Map.of() : row;
        Map<String, String> dimensions = new LinkedHashMap<>();
        for (String dimensionKey : authorityCase.dimensionKeys()) {
            dimensions.put(dimensionKey, text(safeRow.get(dimensionKey)));
        }
        return new FinanceSummaryDualReconciliationService.SummaryRow(
                textOrDefault(safeRow.get("chain"), authorityCase.chain()),
                textOrDefault(safeRow.get("metricId"), authorityCase.metricId()),
                dimensions,
                amount(safeRow.get("amount")));
    }

    private static String textOrDefault(Object value, String defaultValue) {
        String text = text(value);
        return text.isEmpty() ? defaultValue : text;
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private static BigDecimal amount(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        String text = text(value);
        return text.isEmpty() ? BigDecimal.ZERO : new BigDecimal(text);
    }

    public interface QueryExecutor {
        List<Map<String, Object>> query(String database, String nativeSql);
    }

    public record ProofReport(
            String caseId,
            String authoritySource,
            boolean passed,
            String failureMessage,
            FinanceSummaryDualReconciliationService.SummaryReconciliationReport reconciliation) {

        public ProofReport {
            caseId = caseId == null ? "" : caseId;
            authoritySource = authoritySource == null ? "" : authoritySource;
            failureMessage = failureMessage == null ? "" : failureMessage;
            reconciliation = reconciliation == null
                    ? new FinanceSummaryDualReconciliationService.SummaryReconciliationReport(false, List.of(), "")
                    : reconciliation;
        }

        @JsonProperty("oracleSource")
        public String oracleSource() {
            return authoritySource;
        }
    }
}
