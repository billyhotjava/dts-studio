package com.yuzhi.dts.copilot.analytics.service.elt;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "dts.elt.enabled", havingValue = "true")
public class ProjectFulfillmentSyncJob implements EltSyncJob {

    private static final Logger log = LoggerFactory.getLogger(ProjectFulfillmentSyncJob.class);

    private static final String TARGET_TABLE = "fact_project_fulfillment_snapshot";

    private static final String QUERY_ACTIVE_PROJECTS = """
            SELECT p.id AS project_id, p.name AS project_name, p.code AS project_code,
                   p.status AS project_status, p.type AS project_type,
                   p.contract_id, p.curing_director_name
            FROM p_project p
            WHERE p.del_flag = '0' AND p.status = 1
            """;

    private static final String QUERY_CONTRACT_CUSTOMER = """
            SELECT c.settlement_type, cu.name AS customer_name
            FROM p_contract c
            LEFT JOIN p_customer cu ON cu.id = c.customer_id AND cu.del_flag = '0'
            WHERE c.id = ? AND c.del_flag = '0'
            """;

    private static final String QUERY_PROJECT_ROLES = """
            SELECT user_name, project_manage, biz_manage, supervise
            FROM p_project_role
            WHERE project_id = ? AND status = 1
            """;

    private static final String QUERY_GREEN_STATS = """
            SELECT COUNT(*) AS green_count,
                   COALESCE(SUM(rent), 0) AS total_monthly_rent
            FROM p_project_green
            WHERE project_id = ? AND del_flag = '0' AND status = 1
            """;

    private static final String QUERY_POSITION_COUNT = """
            SELECT COUNT(*) AS position_count
            FROM p_position
            WHERE project_id = ? AND del_flag = '0' AND status = 1
            """;

    private static final String QUERY_BIZ_TODAY = """
            SELECT biz_type, COUNT(*) AS cnt
            FROM t_flower_biz_info
            WHERE project_id = ? AND del_flag = '0' AND DATE(apply_time) = CURDATE()
            GROUP BY biz_type
            """;

    private static final String QUERY_CURING_TODAY = """
            SELECT COUNT(*) AS curing_count
            FROM t_curing_record
            WHERE project_id = ? AND del_flag = '0' AND DATE(curing_time) = CURDATE()
            """;

    private static final String QUERY_TASK_COUNTS = """
            SELECT status, COUNT(*) AS cnt
            FROM t_daily_task_info
            WHERE project_id = ? AND del_flag = '0'
            GROUP BY status
            """;

    private static final String QUERY_LATEST_ACCOUNTING = """
            SELECT receivable_total_amount, net_receipt_total_amount, year_and_month
            FROM a_month_accounting
            WHERE project_id = ? AND status = 1
            ORDER BY year_and_month DESC
            LIMIT 1
            """;

    private static final String UPSERT_SQL = """
            INSERT INTO fact_project_fulfillment_snapshot (
                project_id, project_name, project_code, project_status, project_type,
                customer_name, settlement_type, curing_director_name,
                project_manager_name, biz_manager_name, supervisor_name,
                green_count, position_count, total_monthly_rent,
                flower_change_count, flower_add_count, flower_reduce_count,
                flower_transfer_count, flower_sell_count,
                curing_count_today, task_total_count, task_completed_count,
                receivable_total_amount, net_receipt_total_amount, latest_accounting_month,
                snapshot_date, batch_id, synced_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
            ON CONFLICT (project_id, snapshot_date) DO UPDATE SET
                project_name = EXCLUDED.project_name,
                project_code = EXCLUDED.project_code,
                project_status = EXCLUDED.project_status,
                project_type = EXCLUDED.project_type,
                customer_name = EXCLUDED.customer_name,
                settlement_type = EXCLUDED.settlement_type,
                curing_director_name = EXCLUDED.curing_director_name,
                project_manager_name = EXCLUDED.project_manager_name,
                biz_manager_name = EXCLUDED.biz_manager_name,
                supervisor_name = EXCLUDED.supervisor_name,
                green_count = EXCLUDED.green_count,
                position_count = EXCLUDED.position_count,
                total_monthly_rent = EXCLUDED.total_monthly_rent,
                flower_change_count = EXCLUDED.flower_change_count,
                flower_add_count = EXCLUDED.flower_add_count,
                flower_reduce_count = EXCLUDED.flower_reduce_count,
                flower_transfer_count = EXCLUDED.flower_transfer_count,
                flower_sell_count = EXCLUDED.flower_sell_count,
                curing_count_today = EXCLUDED.curing_count_today,
                task_total_count = EXCLUDED.task_total_count,
                task_completed_count = EXCLUDED.task_completed_count,
                receivable_total_amount = EXCLUDED.receivable_total_amount,
                net_receipt_total_amount = EXCLUDED.net_receipt_total_amount,
                latest_accounting_month = EXCLUDED.latest_accounting_month,
                batch_id = EXCLUDED.batch_id,
                synced_at = NOW()
            """;

    private final JdbcTemplate businessJdbcTemplate;
    private final JdbcTemplate analyticsJdbcTemplate;

    public ProjectFulfillmentSyncJob(EltDataSourceProvider dataSourceProvider, DataSource dataSource) {
        this.businessJdbcTemplate = dataSourceProvider.getBusinessJdbcTemplate();
        this.analyticsJdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public String getTargetTable() {
        return TARGET_TABLE;
    }

    @Override
    public int sync(Instant lastWatermark, String batchId) throws Exception {
        LocalDate snapshotDate = LocalDate.now();

        List<Map<String, Object>> projects = businessJdbcTemplate.queryForList(QUERY_ACTIVE_PROJECTS);
        if (projects.isEmpty()) {
            log.info("No active projects found for snapshot");
            return 0;
        }

        List<Object[]> rows = new ArrayList<>(projects.size());

        for (Map<String, Object> project : projects) {
            Object projectId = project.get("project_id");
            Object contractId = project.get("contract_id");

            // Contract + Customer
            String customerName = null;
            String settlementType = null;
            if (contractId != null) {
                List<Map<String, Object>> contractRows = businessJdbcTemplate.queryForList(
                        QUERY_CONTRACT_CUSTOMER, contractId);
                if (!contractRows.isEmpty()) {
                    Map<String, Object> contract = contractRows.get(0);
                    customerName = (String) contract.get("customer_name");
                    Object st = contract.get("settlement_type");
                    settlementType = st != null ? st.toString() : null;
                }
            }

            // Project roles
            String projectManagerName = null;
            String bizManagerName = null;
            String supervisorName = null;
            List<Map<String, Object>> roles = businessJdbcTemplate.queryForList(QUERY_PROJECT_ROLES, projectId);
            for (Map<String, Object> role : roles) {
                String userName = (String) role.get("user_name");
                if (isTrue(role.get("project_manage"))) {
                    projectManagerName = userName;
                }
                if (isTrue(role.get("biz_manage"))) {
                    bizManagerName = userName;
                }
                if (isTrue(role.get("supervise"))) {
                    supervisorName = userName;
                }
            }

            // Green stats
            Map<String, Object> greenStats = businessJdbcTemplate.queryForMap(QUERY_GREEN_STATS, projectId);
            long greenCount = ((Number) greenStats.get("green_count")).longValue();
            BigDecimal totalMonthlyRent = (BigDecimal) greenStats.get("total_monthly_rent");

            // Position count
            long positionCount = businessJdbcTemplate.queryForObject(QUERY_POSITION_COUNT, Long.class, projectId);

            // Flower event counts today
            int flowerChangeCount = 0;
            int flowerAddCount = 0;
            int flowerReduceCount = 0;
            int flowerTransferCount = 0;
            int flowerSellCount = 0;
            List<Map<String, Object>> bizToday = businessJdbcTemplate.queryForList(QUERY_BIZ_TODAY, projectId);
            for (Map<String, Object> row : bizToday) {
                int bizType = ((Number) row.get("biz_type")).intValue();
                int cnt = ((Number) row.get("cnt")).intValue();
                switch (bizType) {
                    case 1 -> flowerChangeCount = cnt;
                    case 2 -> flowerAddCount = cnt;
                    case 3 -> flowerReduceCount = cnt;
                    case 4 -> flowerTransferCount = cnt;
                    case 5 -> flowerSellCount = cnt;
                    default -> { /* ignore other types */ }
                }
            }

            // Curing count today
            long curingCountToday = businessJdbcTemplate.queryForObject(QUERY_CURING_TODAY, Long.class, projectId);

            // Task counts
            int taskTotalCount = 0;
            int taskCompletedCount = 0;
            List<Map<String, Object>> taskCounts = businessJdbcTemplate.queryForList(QUERY_TASK_COUNTS, projectId);
            for (Map<String, Object> tc : taskCounts) {
                int cnt = ((Number) tc.get("cnt")).intValue();
                taskTotalCount += cnt;
                Object statusObj = tc.get("status");
                if (statusObj != null && ((Number) statusObj).intValue() == 5) {
                    taskCompletedCount = cnt;
                }
            }

            // Latest accounting
            BigDecimal receivableTotal = null;
            BigDecimal netReceiptTotal = null;
            String latestAccountingMonth = null;
            List<Map<String, Object>> accountingRows = businessJdbcTemplate.queryForList(
                    QUERY_LATEST_ACCOUNTING, projectId);
            if (!accountingRows.isEmpty()) {
                Map<String, Object> acc = accountingRows.get(0);
                receivableTotal = (BigDecimal) acc.get("receivable_total_amount");
                netReceiptTotal = (BigDecimal) acc.get("net_receipt_total_amount");
                Object ym = acc.get("year_and_month");
                latestAccountingMonth = ym != null ? ym.toString() : null;
            }

            rows.add(new Object[] {
                    projectId,
                    project.get("project_name"),
                    project.get("project_code"),
                    project.get("project_status"),
                    project.get("project_type"),
                    customerName,
                    settlementType,
                    project.get("curing_director_name"),
                    projectManagerName,
                    bizManagerName,
                    supervisorName,
                    greenCount,
                    positionCount,
                    totalMonthlyRent,
                    flowerChangeCount,
                    flowerAddCount,
                    flowerReduceCount,
                    flowerTransferCount,
                    flowerSellCount,
                    curingCountToday,
                    taskTotalCount,
                    taskCompletedCount,
                    receivableTotal,
                    netReceiptTotal,
                    latestAccountingMonth,
                    Date.valueOf(snapshotDate),
                    batchId
            });
        }

        List<List<Object[]>> batches = partition(rows, 500);
        for (List<Object[]> batch : batches) {
            analyticsJdbcTemplate.batchUpdate(UPSERT_SQL, batch);
        }

        log.info("Upserted {} project snapshot rows into {}", rows.size(), TARGET_TABLE);
        return rows.size();
    }

    private static boolean isTrue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() == 1;
        }
        return "1".equals(value.toString()) || "true".equalsIgnoreCase(value.toString());
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}
