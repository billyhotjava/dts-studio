package com.yuzhi.dts.copilot.ai.service.copilot;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class FinanceApplicationMysqlOracleProofService {

    public static final String ORACLE_SOURCE = "APPLICATION_MYSQL";

    private final FinanceSummaryDualReconciliationService reconciliationService;

    public FinanceApplicationMysqlOracleProofService(FinanceSummaryDualReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    public ProofReport prove(
            FinanceApplicationMysqlOracleRegistry.OracleSqlCase oracleCase,
            QueryExecutor copilotExecutor,
            QueryExecutor applicationMysqlExecutor) {
        FinanceApplicationMysqlOracleRegistry.OracleSqlCase safeCase = oracleCase == null
                ? new FinanceApplicationMysqlOracleRegistry.OracleSqlCase(
                        "", "", "", "", "", List.of(), "", null, null, "")
                : oracleCase;
        List<FinanceSummaryDualReconciliationService.SummaryRow> copilotRows = toSummaryRows(
                safeCase,
                copilotExecutor.query(safeCase.copilotQuery().database(), safeCase.copilotQuery().nativeSql()));
        List<FinanceSummaryDualReconciliationService.SummaryRow> oracleRows = toSummaryRows(
                safeCase,
                applicationMysqlExecutor.query(
                        safeCase.applicationMysqlQuery().database(),
                        safeCase.applicationMysqlQuery().nativeSql()));
        FinanceSummaryDualReconciliationService.SummaryReconciliationReport reconciliation =
                reconciliationService.reconcile(safeCase.reconciliationSpec(), copilotRows, oracleRows);
        return new ProofReport(
                safeCase.id(),
                ORACLE_SOURCE,
                reconciliation.passed(),
                reconciliation.failureMessage(),
                reconciliation);
    }

    private static List<FinanceSummaryDualReconciliationService.SummaryRow> toSummaryRows(
            FinanceApplicationMysqlOracleRegistry.OracleSqlCase oracleCase,
            List<Map<String, Object>> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
                .map(row -> toSummaryRow(oracleCase, row))
                .toList();
    }

    private static FinanceSummaryDualReconciliationService.SummaryRow toSummaryRow(
            FinanceApplicationMysqlOracleRegistry.OracleSqlCase oracleCase,
            Map<String, Object> row) {
        Map<String, Object> safeRow = row == null ? Map.of() : row;
        Map<String, String> dimensions = new LinkedHashMap<>();
        for (String dimensionKey : oracleCase.dimensionKeys()) {
            dimensions.put(dimensionKey, text(safeRow.get(dimensionKey)));
        }
        return new FinanceSummaryDualReconciliationService.SummaryRow(
                textOrDefault(safeRow.get("chain"), oracleCase.chain()),
                textOrDefault(safeRow.get("metricId"), oracleCase.metricId()),
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
            String oracleSource,
            boolean passed,
            String failureMessage,
            FinanceSummaryDualReconciliationService.SummaryReconciliationReport reconciliation) {

        public ProofReport {
            caseId = caseId == null ? "" : caseId;
            oracleSource = oracleSource == null ? "" : oracleSource;
            failureMessage = failureMessage == null ? "" : failureMessage;
            reconciliation = reconciliation == null
                    ? new FinanceSummaryDualReconciliationService.SummaryReconciliationReport(false, List.of(), "")
                    : reconciliation;
        }
    }
}
