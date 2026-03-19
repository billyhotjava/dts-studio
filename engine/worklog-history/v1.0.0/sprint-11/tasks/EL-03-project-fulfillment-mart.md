# EL-03: 项目履约日维度主题表

**优先级**: P0
**状态**: READY
**依赖**: EL-01, EL-02

## 目标

实现 `mart_project_fulfillment_daily` 的同步 Job，每天每项目产出一行快照数据，支持项目经营趋势分析。

## 技术设计

### 同步 Job 实现

```java
@Component
public class ProjectFulfillmentSyncJob implements EltSyncJob {

    @Override
    public String getTargetTable() {
        return "mart_project_fulfillment_daily";
    }

    @Override
    public int sync(Instant lastWatermark) {
        // 1. 查询所有活跃项目
        List<Long> projectIds = queryActiveProjectIds();

        // 2. 对每个项目构建当日快照
        LocalDate today = LocalDate.now();
        int rows = 0;
        for (Long projectId : projectIds) {
            MartRow row = buildSnapshotRow(projectId, today);
            upsertRow(row);
            rows++;
        }

        return rows;
    }
}
```

### 高频查询场景与预期 SQL

**场景 1：项目绿植数月度趋势**
```
Q: 翠湖项目最近3个月绿植数量趋势
SQL: SELECT snapshot_date, green_count
     FROM mart_project_fulfillment_daily
     WHERE project_name LIKE '%翠湖%'
       AND snapshot_date >= DATE_SUB(CURDATE(), INTERVAL 3 MONTH)
     ORDER BY snapshot_date
```

**场景 2：各项目加花次数月度对比**
```
Q: 各项目最近3个月加花次数对比
SQL: SELECT project_name,
            DATE_FORMAT(snapshot_date, '%Y-%m') as 月份,
            SUM(add_flower_count) as 加花次数
     FROM mart_project_fulfillment_daily
     WHERE snapshot_date >= DATE_SUB(CURDATE(), INTERVAL 3 MONTH)
     GROUP BY project_name, DATE_FORMAT(snapshot_date, '%Y-%m')
     ORDER BY project_name, 月份
```

**场景 3：项目经营综合排名**
```
Q: 本月项目经营综合排名（绿植数、换花率、养护覆盖）
SQL: SELECT project_name,
            AVG(green_count) as 平均绿植数,
            SUM(change_flower_count) as 换花总次数,
            SUM(curing_count) as 养护总次数
     FROM mart_project_fulfillment_daily
     WHERE snapshot_date >= DATE_FORMAT(CURDATE(), '%Y-%m-01')
     GROUP BY project_name
     ORDER BY 平均绿植数 DESC
```

**场景 4：月度应收环比**
```
Q: 各项目上月和本月应收对比
SQL: SELECT project_name, settlement_month, monthly_receivable
     FROM mart_project_fulfillment_daily
     WHERE settlement_month IN ('2026-02', '2026-03')
       AND snapshot_date IN (
           SELECT MAX(snapshot_date) FROM mart_project_fulfillment_daily
           GROUP BY project_id, settlement_month
       )
     ORDER BY project_name, settlement_month
```

## 完成标准

- [ ] ProjectFulfillmentSyncJob 实现
- [ ] 每日快照逻辑正确（库存快照 + 当日增量事件）
- [ ] UPSERT 逻辑（同一项目同一天重复同步幂等）
- [ ] 4 个高频查询场景可正确返回数据
