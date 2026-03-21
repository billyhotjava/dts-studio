package com.yuzhi.dts.copilot.analytics.service.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.copilot.analytics.domain.AnalyticsDatabase;
import com.yuzhi.dts.copilot.analytics.domain.AnalyticsReportTemplate;
import com.yuzhi.dts.copilot.analytics.repository.AnalyticsDatabaseRepository;
import com.yuzhi.dts.copilot.analytics.service.DatasetQueryService;
import com.yuzhi.dts.copilot.analytics.service.DatasetQueryService.DatasetConstraints;
import com.yuzhi.dts.copilot.analytics.service.DatasetQueryService.DatasetResult;
import com.yuzhi.dts.copilot.analytics.service.report.ReportExecutionPlanService.ReportExecutionPlan;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DefaultFixedReportExecutionService implements FixedReportExecutionService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String DEFAULT_DATABASE_NAME = "园林业务库";
    private static final int PREVIEW_LIMIT = 50;
    private static final int QUERY_TIMEOUT_SECONDS = 60;

    private final AnalyticsDatabaseRepository databaseRepository;
    private final DatasetQueryService datasetQueryService;

    public DefaultFixedReportExecutionService(
            AnalyticsDatabaseRepository databaseRepository, DatasetQueryService datasetQueryService) {
        this.databaseRepository = databaseRepository;
        this.datasetQueryService = datasetQueryService;
    }

    @Override
    public Optional<ExecutionResult> execute(
            AnalyticsReportTemplate template, Map<String, Object> parameters, ReportExecutionPlan plan) throws SQLException {
        if (template == null || plan == null) {
            return Optional.empty();
        }

        QueryContract contract = resolveQueryContract(template);
        if (contract == null) {
            return Optional.empty();
        }

        String normalizedTarget = normalize(contract.targetObject());
        if ("authority.procurement.purchase_summary".equals(normalizedTarget)) {
            return Optional.of(executeProcurementPurchaseSummary(contract, parameters));
        }
        if ("authority.inventory.stock_overview".equals(normalizedTarget)) {
            return Optional.of(executeWarehouseStockOverview(contract, parameters));
        }
        if ("authority.inventory.low_stock_alert".equals(normalizedTarget)) {
            return Optional.of(executeWarehouseLowStockAlert(contract, parameters));
        }
        if ("authority.finance.settlement_summary".equals(normalizedTarget)) {
            return Optional.of(executeFinanceSettlementSummary(contract, parameters));
        }
        return Optional.empty();
    }

    private ExecutionResult executeProcurementPurchaseSummary(QueryContract contract, Map<String, Object> parameters)
            throws SQLException {
        AnalyticsDatabase database = resolveDatabase(contract.databaseName());
        StringBuilder sql = new StringBuilder("""
                SELECT
                    DATE_FORMAT( a.purchase_time, '%Y-%m-%d' ) AS purchaseDate,
                    b.purchase_user_id AS purchaseUserId,
                    b.purchase_user_name AS purchaseUserName,
                    ROUND(COALESCE(SUM(a.parchase_number * a.parchase_price), 0), 2) AS totalAmount,
                    ROUND(COALESCE(SUM(CASE WHEN a.pay_type = 1 THEN a.parchase_number * a.parchase_price ELSE 0 END), 0), 2) AS totalCashAmount,
                    ROUND(COALESCE(SUM(CASE WHEN a.pay_type = 2 THEN a.parchase_number * a.parchase_price ELSE 0 END), 0), 2) AS totalChargeAmount
                FROM t_purchase_price_item a
                LEFT JOIN t_purchase_info b ON b.id = a.purchase_info_id
                LEFT JOIN t_plan_purchase_item c ON c.purchase_price_id = a.id
                LEFT JOIN t_flower_biz_item d ON d.id = c.flower_item_id
                LEFT JOIN t_flower_biz_info f ON f.id = d.flower_biz_id
                WHERE d.status <> -1
                  AND d.id IS NOT NULL
                  AND c.status <> -1
                """);
        List<Object> bindings = new ArrayList<>();

        String purchaseUserId = stringParam(parameters, "purchaseUserId");
        if (purchaseUserId != null) {
            sql.append(" AND b.purchase_user_id = ?");
            bindings.add(purchaseUserId);
        }

        String startDate = stringParam(parameters, "startDate");
        if (startDate != null) {
            sql.append(" AND a.purchase_time >= CONCAT(?, ' 00:00:00')");
            bindings.add(startDate);
        }

        String endDate = stringParam(parameters, "endDate");
        if (endDate != null) {
            sql.append(" AND a.purchase_time <= CONCAT(?, ' 23:59:59')");
            bindings.add(endDate);
        }

        sql.append("""

                GROUP BY DATE_FORMAT( a.purchase_time, '%Y-%m-%d' ), b.purchase_user_id, b.purchase_user_name
                ORDER BY purchaseDate DESC, purchaseUserName ASC
                """);

        DatasetResult result = datasetQueryService.runNative(
                database.getId(),
                sql.toString(),
                new DatasetConstraints(PREVIEW_LIMIT, QUERY_TIMEOUT_SECONDS, null),
                bindings);
        return mapPreview(database, result);
    }

    private ExecutionResult executeWarehouseStockOverview(QueryContract contract, Map<String, Object> parameters)
            throws SQLException {
        AnalyticsDatabase database = resolveDatabase(contract.databaseName());
        StringBuilder sql = new StringBuilder("""
                SELECT
                    a.storehouse_name AS storehouseName,
                    a.good_name AS goodName,
                    a.good_specs AS goodSpecs,
                    a.good_number AS goodNumber,
                    ROUND(COALESCE(a.out_cost, 0), 4) AS outCost,
                    ROUND(COALESCE(gp.guidance_price, 0), 2) AS salePrice
                FROM s_stock_info a
                LEFT JOIN b_goods_price gp ON gp.id = a.good_price_id
                WHERE a.del_flag = '0'
                """);
        List<Object> bindings = new ArrayList<>();

        String storehouseInfoId = stringParam(parameters, "storehouseInfoId");
        if (storehouseInfoId != null) {
            sql.append(" AND a.storehouse_info_id = ?");
            bindings.add(storehouseInfoId);
        }

        String status = stringParam(parameters, "status");
        if (status != null) {
            sql.append(" AND a.status = ?");
            bindings.add(status);
        }

        String goodType = stringParam(parameters, "goodType");
        if (goodType != null) {
            sql.append(" AND a.good_type = ?");
            bindings.add(goodType);
        }

        String goodName = stringParam(parameters, "goodName");
        if (goodName != null) {
            sql.append(" AND a.good_name LIKE ?");
            bindings.add('%' + goodName + '%');
        }

        String goodSpecs = stringParam(parameters, "goodSpecs");
        if (goodSpecs != null) {
            sql.append(" AND a.good_specs LIKE ?");
            bindings.add('%' + goodSpecs + '%');
        }

        String passNumber = stringParam(parameters, "passNumber");
        if (passNumber != null) {
            sql.append(" AND a.good_number >= ?");
            bindings.add(passNumber);
        }

        String underNumber = stringParam(parameters, "underNumber");
        if (underNumber != null) {
            sql.append(" AND a.good_number <= ?");
            bindings.add(underNumber);
        }

        sql.append("""

                ORDER BY a.good_name DESC, a.good_norms DESC, a.good_specs DESC, a.good_number DESC
                """);

        DatasetResult result = datasetQueryService.runNative(
                database.getId(),
                sql.toString(),
                new DatasetConstraints(PREVIEW_LIMIT, QUERY_TIMEOUT_SECONDS, null),
                bindings);
        return mapPreview(database, result);
    }

    private ExecutionResult executeWarehouseLowStockAlert(QueryContract contract, Map<String, Object> parameters)
            throws SQLException {
        AnalyticsDatabase database = resolveDatabase(contract.databaseName());
        StringBuilder sql = new StringBuilder("""
                SELECT
                    a.storehouse_name AS storehouseName,
                    a.good_name AS goodName,
                    COALESCE(a.good_norms, '') AS goodNorms,
                    COALESCE(a.good_specs, '') AS goodSpecs,
                    a.good_number AS goodNumber,
                    ROUND(COALESCE(a.out_cost, 0), 4) AS outCost,
                    ROUND(COALESCE(gp.guidance_price, 0), 2) AS salePrice,
                    COALESCE(a.leader_user_name, '') AS leaderUserName
                FROM s_stock_info a
                LEFT JOIN b_goods_price gp ON gp.id = a.good_price_id
                WHERE a.del_flag = '0'
                """);
        List<Object> bindings = new ArrayList<>();

        String storehouseInfoId = stringParam(parameters, "storehouseInfoId");
        if (storehouseInfoId != null) {
            sql.append(" AND a.storehouse_info_id = ?");
            bindings.add(storehouseInfoId);
        }

        String goodType = stringParam(parameters, "goodType");
        if (goodType != null) {
            sql.append(" AND a.good_type = ?");
            bindings.add(goodType);
        }

        String goodName = stringParam(parameters, "goodName");
        if (goodName != null) {
            sql.append(" AND a.good_name LIKE ?");
            bindings.add('%' + goodName + '%');
        }

        String goodSpecs = stringParam(parameters, "goodSpecs");
        if (goodSpecs != null) {
            sql.append(" AND a.good_specs LIKE ?");
            bindings.add('%' + goodSpecs + '%');
        }

        String underNumber = stringParam(parameters, "underNumber");
        if (underNumber == null) {
            underNumber = "10";
        }
        sql.append(" AND a.good_number <= ?");
        bindings.add(underNumber);

        String status = stringParam(parameters, "status");
        if (status != null) {
            sql.append(" AND a.status = ?");
            bindings.add(status);
        }

        sql.append("""

                ORDER BY a.good_number ASC, a.storehouse_name ASC, a.good_name ASC, a.good_specs ASC
                """);

        DatasetResult result = datasetQueryService.runNative(
                database.getId(),
                sql.toString(),
                new DatasetConstraints(PREVIEW_LIMIT, QUERY_TIMEOUT_SECONDS, null),
                bindings);
        return mapPreview(database, result);
    }

    private ExecutionResult executeFinanceSettlementSummary(QueryContract contract, Map<String, Object> parameters)
            throws SQLException {
        AnalyticsDatabase database = resolveDatabase(contract.databaseName());
        StringBuilder sql = new StringBuilder("""
                SELECT
                    a.account_period AS accountPeriod,
                    a.project_name AS projectName,
                    COALESCE(a.biz_type_name, '') AS bizTypeName,
                    COALESCE(a.subject_full_name, '') AS subjectFullName,
                    COALESCE(a.fee_name, '') AS feeName,
                    ROUND(COALESCE(a.fee_number, 0), 4) AS feeNumber,
                    CASE a.fee_belong
                        WHEN 1 THEN '养护人员'
                        WHEN 2 THEN '负责人'
                        WHEN 3 THEN '公司'
                        ELSE '客户'
                    END AS feeBelongName,
                    CASE a.settle_handle
                        WHEN 0 THEN '无需处理'
                        WHEN 1 THEN '成本/采购报销'
                        WHEN 2 THEN '成本/采购冲抵备用金'
                        WHEN 3 THEN '成本/供应商挂帐'
                        WHEN 4 THEN '成本/养护工资挂帐'
                        WHEN 5 THEN '成本/项目备品成本挂帐'
                        WHEN 6 THEN '收入/项目销售额挂帐'
                        WHEN 7 THEN '收入/项目租摆费挂帐'
                        WHEN 8 THEN '收入/项目经理挂帐'
                        ELSE ''
                    END AS settleHandleName,
                    COALESCE(a.fee_user_name, '') AS feeUserName,
                    COALESCE(a.remark, '') AS remark
                FROM f_settled_items a
                WHERE a.status > 0
                """);
        List<Object> bindings = new ArrayList<>();

        String accountPeriod = stringParam(parameters, "accountPeriod");
        if (accountPeriod != null) {
            sql.append(" AND a.account_period = ?");
            bindings.add(accountPeriod);
        }

        String projectId = stringParam(parameters, "projectId");
        if (projectId != null) {
            sql.append(" AND a.project_id = ?");
            bindings.add(projectId);
        }

        String feeUserId = stringParam(parameters, "feeUserId");
        if (feeUserId != null) {
            sql.append(" AND a.fee_user_id = ?");
            bindings.add(feeUserId);
        }

        String status = stringParam(parameters, "status");
        if (status != null) {
            sql.append(" AND a.status = ?");
            bindings.add(status);
        }

        sql.append("""

                ORDER BY a.account_period ASC, a.biz_type ASC, a.biz_id ASC, a.biz_type_name ASC,
                         a.settle_type ASC, a.fee_user_id DESC, a.fee_type ASC, a.fee_user_id ASC
                """);

        DatasetResult result = datasetQueryService.runNative(
                database.getId(),
                sql.toString(),
                new DatasetConstraints(PREVIEW_LIMIT, QUERY_TIMEOUT_SECONDS, null),
                bindings);
        return mapPreview(database, result);
    }

    private AnalyticsDatabase resolveDatabase(String preferredDatabaseName) {
        String databaseName = trimToNull(preferredDatabaseName) == null ? DEFAULT_DATABASE_NAME : preferredDatabaseName.trim();
        return databaseRepository
                .findFirstByNameIgnoreCase(databaseName)
                .orElseThrow(() -> new IllegalArgumentException("Fixed report database not found: " + databaseName));
    }

    private ExecutionResult mapPreview(AnalyticsDatabase database, DatasetResult result) {
        List<PreviewColumn> columns = result.cols().stream()
                .map(column -> new PreviewColumn(
                        stringValue(column.get("name")),
                        firstNonBlank(stringValue(column.get("display_name")), stringValue(column.get("name"))),
                        stringValue(column.get("base_type"))))
                .toList();

        List<Map<String, Object>> rows = new ArrayList<>();
        for (List<Object> row : result.rows()) {
            Map<String, Object> mapped = new LinkedHashMap<>();
            for (int i = 0; i < columns.size() && i < row.size(); i++) {
                mapped.put(columns.get(i).key(), row.get(i));
            }
            rows.add(mapped);
        }

        return new ExecutionResult(
                database.getId(),
                database.getName(),
                columns,
                rows,
                rows.size(),
                rows.size() >= PREVIEW_LIMIT);
    }

    private QueryContract resolveQueryContract(AnalyticsReportTemplate template) {
        JsonNode spec = parseJson(template.getSpecJson());
        JsonNode queryContract = spec == null ? null : spec.get("queryContract");
        if (queryContract == null || !queryContract.isObject()) {
            return null;
        }
        String targetObject = firstNonBlank(
                stringValue(queryContract.get("targetObject")),
                trimToNull(template.getTargetObject()));
        if (targetObject == null) {
            return null;
        }
        String sourceType = firstNonBlank(
                stringValue(queryContract.get("sourceType")),
                trimToNull(template.getDataSourceType()));
        String databaseName = stringValue(queryContract.get("databaseName"));
        return new QueryContract(sourceType, targetObject, databaseName);
    }

    private static JsonNode parseJson(String rawJson) {
        String raw = trimToNull(rawJson);
        if (raw == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(raw);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String stringParam(Map<String, Object> parameters, String key) {
        if (parameters == null || key == null) {
            return null;
        }
        Object value = parameters.get(key);
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof JsonNode node) {
            return trimToNull(node.isTextual() ? node.asText() : node.toString());
        }
        return trimToNull(value.toString());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String firstNonBlank(String first, String second) {
        return trimToNull(first) != null ? first.trim() : trimToNull(second);
    }

    private record QueryContract(String sourceType, String targetObject, String databaseName) {}
}
