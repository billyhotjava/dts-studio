# EL-04: 现场业务事件事实表

**优先级**: P0
**状态**: READY
**依赖**: EL-01, EL-02

## 目标

实现 `fact_field_operation_event` 的增量同步 Job，将业务库的 `t_flower_biz_info` 增量同步到 analytics 库，翻译状态码，支持事件趋势分析。

## 技术设计

### 同步逻辑

```java
@Component
public class FieldOperationSyncJob implements EltSyncJob {

    @Override
    public String getTargetTable() {
        return "fact_field_operation_event";
    }

    @Override
    public int sync(Instant lastWatermark) {
        // 1. 从业务库增量查询
        List<RawBizEvent> events = queryIncrementalEvents(lastWatermark, batchSize);

        // 2. 翻译状态码（复用 BizEnumService）
        List<FactRow> factRows = events.stream()
            .map(this::translateAndMap)
            .toList();

        // 3. UPSERT 到事实表
        int rows = batchUpsert(factRows);

        return rows;
    }
}
```

### 增量查询 SQL（在业务库执行）

```sql
SELECT
    bi.id,
    bi.code,
    bi.biz_type,
    bi.status,
    bi.urgent,
    bi.project_id,
    p.name AS project_name,
    c.name AS customer_name,
    bi.apply_use_name,
    bi.curing_user_name,
    bi.bear_cost_type,
    bi.apply_time,
    bi.finish_time,
    bi.biz_total_rent,
    bi.biz_total_cost,
    bi.update_time,
    -- 汇总明细数量
    (SELECT IFNULL(SUM(item.plant_number), 0)
     FROM t_flower_biz_item item
     WHERE item.flower_biz_id = bi.id) AS total_plant_number
FROM t_flower_biz_info bi
LEFT JOIN p_project p ON bi.project_id = p.id
LEFT JOIN p_contract ct ON p.contract_id = ct.id
LEFT JOIN p_customer c ON ct.customer_id = c.id
WHERE bi.update_time > :lastWatermark
ORDER BY bi.update_time ASC
LIMIT :batchSize
```

### 高频查询场景

**场景 1：加花月度趋势**
```
Q: 最近半年各月加花次数趋势
SQL: SELECT event_month, count(*) as 加花次数, sum(plant_number) as 加花总量
     FROM fact_field_operation_event
     WHERE biz_type_name = '加花'
       AND event_date >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH)
     GROUP BY event_month
     ORDER BY event_month
```

**场景 2：各类型业务月度分布趋势**
```
Q: 最近3个月各类型报花业务趋势
SQL: SELECT event_month, biz_type_name, count(*) as 单数
     FROM fact_field_operation_event
     WHERE event_date >= DATE_SUB(CURDATE(), INTERVAL 3 MONTH)
       AND biz_status_name NOT IN ('作废', '驳回', '草稿')
     GROUP BY event_month, biz_type_name
     ORDER BY event_month, 单数 DESC
```

**场景 3：紧急业务占比趋势**
```
Q: 各月紧急报花单占比
SQL: SELECT event_month,
            count(*) as 总单数,
            SUM(CASE WHEN is_urgent = '是' THEN 1 ELSE 0 END) as 紧急单数,
            ROUND(SUM(CASE WHEN is_urgent = '是' THEN 1 ELSE 0 END) / count(*), 2) as 紧急占比
     FROM fact_field_operation_event
     WHERE event_date >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH)
     GROUP BY event_month
     ORDER BY event_month
```

## 完成标准

- [ ] FieldOperationSyncJob 实现
- [ ] 增量查询正确（watermark 驱动）
- [ ] 状态码翻译正确（复用 BizEnumService）
- [ ] UPSERT 幂等（同一 biz_id 重复同步不产生重复）
- [ ] 3 个高频查询场景可正确返回数据
