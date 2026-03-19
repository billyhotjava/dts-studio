package com.yuzhi.dts.copilot.analytics.service.elt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
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
public class FieldOperationSyncJob implements EltSyncJob {

    private static final Logger log = LoggerFactory.getLogger(FieldOperationSyncJob.class);

    private static final String TARGET_TABLE = "fact_field_operation_event";

    private static final Map<Integer, String> BIZ_TYPE_MAP = Map.ofEntries(
            Map.entry(1, "换花"),
            Map.entry(2, "加花"),
            Map.entry(3, "减花"),
            Map.entry(4, "调花"),
            Map.entry(5, "售花"),
            Map.entry(6, "坏账"),
            Map.entry(7, "销售"),
            Map.entry(8, "内购"),
            Map.entry(10, "辅料"),
            Map.entry(11, "加盆架"),
            Map.entry(12, "减盆架"));

    private static final Map<Integer, String> STATUS_MAP = Map.ofEntries(
            Map.entry(-1, "作废"),
            Map.entry(1, "审核中"),
            Map.entry(2, "备货中"),
            Map.entry(3, "核算中"),
            Map.entry(4, "待结算"),
            Map.entry(5, "已完成"),
            Map.entry(20, "草稿"),
            Map.entry(21, "驳回"));

    private static final Map<Integer, String> URGENT_MAP = Map.of(
            1, "是",
            2, "否");

    private static final Map<Integer, String> BEAR_COST_TYPE_MAP = Map.of(
            1, "养护人",
            2, "领导",
            3, "公司",
            4, "客户");

    private static final String QUERY_BIZ = """
            SELECT id, biz_type, status, project_id, project_name,
                   apply_use_name, curing_user_name, apply_time, finish_time,
                   biz_total_rent, biz_total_cost, urgent, bear_cost_type,
                   project_manage_name, update_time
            FROM t_flower_biz_info
            WHERE del_flag = '0' AND update_time > ?
            ORDER BY update_time ASC
            """;

    private static final String UPSERT_SQL = """
            INSERT INTO fact_field_operation_event (
                biz_id, biz_type, biz_type_label, status, status_label,
                project_id, project_name, apply_use_name, curing_user_name,
                apply_time, finish_time, biz_total_rent, biz_total_cost,
                urgent, urgent_label, bear_cost_type, bear_cost_type_label,
                project_manage_name, source_update_time, batch_id, synced_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
            ON CONFLICT (biz_id) DO UPDATE SET
                biz_type = EXCLUDED.biz_type,
                biz_type_label = EXCLUDED.biz_type_label,
                status = EXCLUDED.status,
                status_label = EXCLUDED.status_label,
                project_id = EXCLUDED.project_id,
                project_name = EXCLUDED.project_name,
                apply_use_name = EXCLUDED.apply_use_name,
                curing_user_name = EXCLUDED.curing_user_name,
                apply_time = EXCLUDED.apply_time,
                finish_time = EXCLUDED.finish_time,
                biz_total_rent = EXCLUDED.biz_total_rent,
                biz_total_cost = EXCLUDED.biz_total_cost,
                urgent = EXCLUDED.urgent,
                urgent_label = EXCLUDED.urgent_label,
                bear_cost_type = EXCLUDED.bear_cost_type,
                bear_cost_type_label = EXCLUDED.bear_cost_type_label,
                project_manage_name = EXCLUDED.project_manage_name,
                source_update_time = EXCLUDED.source_update_time,
                batch_id = EXCLUDED.batch_id,
                synced_at = NOW()
            """;

    private final JdbcTemplate businessJdbcTemplate;
    private final JdbcTemplate analyticsJdbcTemplate;

    public FieldOperationSyncJob(EltDataSourceProvider dataSourceProvider, DataSource dataSource) {
        this.businessJdbcTemplate = dataSourceProvider.getBusinessJdbcTemplate();
        this.analyticsJdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public String getTargetTable() {
        return TARGET_TABLE;
    }

    @Override
    public int sync(Instant lastWatermark, String batchId) throws Exception {
        Timestamp watermarkTs = Timestamp.from(lastWatermark);
        List<Object[]> rows = businessJdbcTemplate.query(QUERY_BIZ, (rs, rowNum) -> mapRow(rs, batchId), watermarkTs);

        if (rows.isEmpty()) {
            log.info("No new rows in t_flower_biz_info since {}", lastWatermark);
            return 0;
        }

        List<List<Object[]>> batches = partition(rows, 500);
        for (List<Object[]> batch : batches) {
            analyticsJdbcTemplate.batchUpdate(UPSERT_SQL, batch);
        }

        log.info("Upserted {} rows into {}", rows.size(), TARGET_TABLE);
        return rows.size();
    }

    private Object[] mapRow(ResultSet rs, String batchId) throws SQLException {
        long bizId = rs.getLong("id");
        int bizType = rs.getInt("biz_type");
        int status = rs.getInt("status");
        int urgent = rs.getInt("urgent");
        int bearCostType = rs.getInt("bear_cost_type");

        return new Object[] {
                bizId,
                bizType,
                BIZ_TYPE_MAP.getOrDefault(bizType, String.valueOf(bizType)),
                status,
                STATUS_MAP.getOrDefault(status, String.valueOf(status)),
                rs.getObject("project_id"),
                rs.getString("project_name"),
                rs.getString("apply_use_name"),
                rs.getString("curing_user_name"),
                rs.getTimestamp("apply_time"),
                rs.getTimestamp("finish_time"),
                rs.getBigDecimal("biz_total_rent"),
                rs.getBigDecimal("biz_total_cost"),
                urgent,
                URGENT_MAP.getOrDefault(urgent, String.valueOf(urgent)),
                bearCostType,
                BEAR_COST_TYPE_MAP.getOrDefault(bearCostType, String.valueOf(bearCostType)),
                rs.getString("project_manage_name"),
                rs.getTimestamp("update_time"),
                batchId
        };
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}
