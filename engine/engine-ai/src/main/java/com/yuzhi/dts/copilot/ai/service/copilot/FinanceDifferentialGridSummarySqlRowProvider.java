package com.yuzhi.dts.copilot.ai.service.copilot;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(
        prefix = "copilot.finance.reconciliation.differential-grid-summary-sql-row-provider",
        name = "enabled",
        havingValue = "true")
@ConditionalOnBean(name = {
        "financeApplicationMysqlAuthorityCopilotJdbcQueryExecutor",
        "financeApplicationMysqlAuthorityJdbcQueryExecutor"
})
public class FinanceDifferentialGridSummarySqlRowProvider implements FinanceDifferentialGridRowProvider {

    private static final Set<String> SUPPORTED_NON_DIMENSION_FILTERS = Set.of(
            "accountPeriodStart",
            "accountPeriodEnd",
            "dateStart",
            "dateEndExclusive",
            "bizTypes");

    private final FinanceSummaryDualReconciliationRegistry summaryRegistry;
    private final FinanceApplicationMysqlAuthorityProofService.QueryExecutor copilotExecutor;
    private final FinanceApplicationMysqlAuthorityProofService.QueryExecutor authorityExecutor;

    public FinanceDifferentialGridSummarySqlRowProvider(
            FinanceSummaryDualReconciliationRegistry summaryRegistry,
            @Qualifier("financeApplicationMysqlAuthorityCopilotJdbcQueryExecutor")
                    FinanceApplicationMysqlAuthorityProofService.QueryExecutor copilotExecutor,
            @Qualifier("financeApplicationMysqlAuthorityJdbcQueryExecutor")
                    FinanceApplicationMysqlAuthorityProofService.QueryExecutor authorityExecutor) {
        this.summaryRegistry = summaryRegistry;
        this.copilotExecutor = copilotExecutor;
        this.authorityExecutor = authorityExecutor;
    }

    @Override
    public List<FinanceDifferentialGridService.GridRow> copilotRows(
            FinanceDifferentialGridRegistry.DifferentialGridCase gridCase) {
        FinanceSummaryDualReconciliationRegistry.SummaryCase summaryCase = summaryCase(gridCase);
        return rowsFor(gridCase, summaryCase.copilotQuery(), copilotExecutor);
    }

    @Override
    public List<FinanceDifferentialGridService.GridRow> authorityRows(
            FinanceDifferentialGridRegistry.DifferentialGridCase gridCase) {
        FinanceSummaryDualReconciliationRegistry.SummaryCase summaryCase = summaryCase(gridCase);
        return rowsFor(gridCase, summaryCase.authorityQuery(), authorityExecutor);
    }

    private FinanceSummaryDualReconciliationRegistry.SummaryCase summaryCase(
            FinanceDifferentialGridRegistry.DifferentialGridCase gridCase) {
        return summaryRegistry.caseById(gridCase.summaryCaseId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Missing finance summary case for differential grid: " + gridCase.summaryCaseId()));
    }

    private List<FinanceDifferentialGridService.GridRow> rowsFor(
            FinanceDifferentialGridRegistry.DifferentialGridCase gridCase,
            FinanceSummaryDualReconciliationRegistry.SummaryQuery query,
            FinanceApplicationMysqlAuthorityProofService.QueryExecutor executor) {
        List<Map<String, Object>> rows = executor.query(query.database(), query.nativeSql());
        return gridCase.slices().stream()
                .flatMap(slice -> rows.stream()
                        .filter(row -> matchesSlice(gridCase, slice, row))
                        .map(row -> gridRow(gridCase, slice, row)))
                .toList();
    }

    private static FinanceDifferentialGridService.GridRow gridRow(
            FinanceDifferentialGridRegistry.DifferentialGridCase gridCase,
            FinanceDifferentialGridRegistry.GridSlice slice,
            Map<String, Object> row) {
        Map<String, String> dimensions = new LinkedHashMap<>();
        for (String dimensionKey : gridCase.dimensionKeys()) {
            dimensions.put(dimensionKey, text(rowValue(row, dimensionKey)));
        }
        return new FinanceDifferentialGridService.GridRow(
                slice.id(),
                gridCase.chain(),
                gridCase.metricId(),
                dimensions,
                amount(rowValue(row, "amount")));
    }

    private static boolean matchesSlice(
            FinanceDifferentialGridRegistry.DifferentialGridCase gridCase,
            FinanceDifferentialGridRegistry.GridSlice slice,
            Map<String, Object> row) {
        for (Map.Entry<String, String> filter : slice.filters().entrySet()) {
            String key = filter.getKey();
            String value = filter.getValue();
            if (!StringUtils.hasText(value) || "ALL".equalsIgnoreCase(value)) {
                continue;
            }
            if (gridCase.dimensionKeys().contains(key)) {
                if (!value.equals(text(rowValue(row, key)))) {
                    return false;
                }
                continue;
            }
            if ("accountPeriodStart".equals(key)) {
                if (accountPeriod(row).compareTo(period(value)) < 0) {
                    return false;
                }
                continue;
            }
            if ("accountPeriodEnd".equals(key)) {
                if (accountPeriod(row).compareTo(period(value)) > 0) {
                    return false;
                }
                continue;
            }
            if ("dateStart".equals(key)) {
                if (accountPeriod(row).compareTo(period(value)) < 0) {
                    return false;
                }
                continue;
            }
            if ("dateEndExclusive".equals(key)) {
                if (accountPeriod(row).compareTo(period(value)) >= 0) {
                    return false;
                }
                continue;
            }
            if (!SUPPORTED_NON_DIMENSION_FILTERS.contains(key)) {
                throw new IllegalArgumentException("unsupported differential grid filter: gridId="
                        + gridCase.id() + ", sliceId=" + slice.id() + ", filter=" + key);
            }
        }
        return true;
    }

    private static String accountPeriod(Map<String, Object> row) {
        return period(text(rowValue(row, "accountPeriod")));
    }

    private static String period(String value) {
        String text = text(value).replace("-", "");
        if (text.length() >= 6) {
            return text.substring(0, 6);
        }
        return text;
    }

    private static Object rowValue(Map<String, Object> row, String key) {
        if (row == null || key == null) {
            return "";
        }
        Object exact = row.get(key);
        if (exact != null) {
            return exact;
        }
        String canonicalKey = canonicalKey(key);
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (canonicalKey(entry.getKey()).equals(canonicalKey)) {
                return entry.getValue();
            }
        }
        return "";
    }

    private static String canonicalKey(String key) {
        String safeKey = text(key);
        return switch (safeKey.toLowerCase(java.util.Locale.ROOT)) {
            case "account_period", "accountperiod" -> "accountPeriod";
            case "project_id", "projectid" -> "projectId";
            default -> safeKey;
        };
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

    private static String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
