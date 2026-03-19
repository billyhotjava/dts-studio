# EL-02: 增量同步引擎

**优先级**: P0
**状态**: READY
**依赖**: EL-01

## 目标

在 `dts-copilot-analytics` 中实现轻量 ELT 增量同步引擎，基于 watermark 驱动，Spring `@Scheduled` 定时执行，不引入外部调度依赖。

## 技术设计

### 核心组件

```
EltSyncScheduler (@Scheduled)
  ├── EltSyncService (编排)
  │     ├── ProjectFulfillmentSyncJob (mart_project_fulfillment_daily)
  │     └── FieldOperationSyncJob (fact_field_operation_event)
  ├── EltWatermarkService (watermark 管理)
  └── EltSyncMonitor (监控/告警)
```

### EltSyncScheduler

```java
@Component
@ConditionalOnProperty(name = "dts.elt.enabled", havingValue = "true", matchIfMissing = false)
public class EltSyncScheduler {

    @Scheduled(cron = "${dts.elt.cron:0 0 * * * *}")  // 默认每小时
    public void syncAll() {
        eltSyncService.runSync("mart_project_fulfillment_daily");
        eltSyncService.runSync("fact_field_operation_event");
    }
}
```

### EltSyncService

```java
@Service
public class EltSyncService {

    public void runSync(String targetTable) {
        // 1. 获取 watermark
        EltSyncWatermark wm = watermarkService.getWatermark(targetTable);
        if ("RUNNING".equals(wm.getSyncStatus())) {
            log.warn("Sync already running for {}, skipping", targetTable);
            return;
        }

        // 2. 标记 RUNNING
        watermarkService.markRunning(targetTable);

        try {
            long startTime = System.currentTimeMillis();

            // 3. 查询源数据（增量）
            // 通过 analytics 已注册的外部数据源 JDBC 连接查询业务库
            // WHERE updated_at > :lastWatermark
            int rows = syncJobRegistry.get(targetTable).sync(wm.getLastWatermark());

            // 4. 更新 watermark
            long duration = System.currentTimeMillis() - startTime;
            watermarkService.markCompleted(targetTable, rows, duration);

            log.info("ELT sync completed: {} - {} rows in {}ms", targetTable, rows, duration);
        } catch (Exception e) {
            watermarkService.markFailed(targetTable, e.getMessage());
            log.error("ELT sync failed for {}: {}", targetTable, e.getMessage(), e);
        }
    }
}
```

### 增量查询策略

**fact_field_operation_event（简单增量）**
```sql
-- 从业务库查询增量数据
SELECT bi.id, bi.code, bi.biz_type, bi.status, bi.project_id,
       bi.apply_time, bi.finish_time, bi.urgent,
       bi.apply_use_name, bi.curing_user_name, bi.bear_cost_type,
       bi.biz_total_rent, bi.biz_total_cost,
       p.name as project_name, c.name as customer_name,
       bi.update_time
FROM t_flower_biz_info bi
LEFT JOIN p_project p ON bi.project_id = p.id
LEFT JOIN p_contract ct ON p.contract_id = ct.id
LEFT JOIN p_customer c ON ct.customer_id = c.id
WHERE bi.update_time > :lastWatermark
ORDER BY bi.update_time ASC
LIMIT :batchSize
```

然后在应用层做状态码翻译（复用 `BizEnumService`）后 UPSERT 到 fact 表。

**mart_project_fulfillment_daily（快照 + 增量混合）**
```
1. 获取所有活跃项目 ID 列表
2. 对每个项目，查询当日各维度的值：
   - green_count: SELECT count(*) FROM p_project_green WHERE project_id=? AND status=1
   - 当日报花事件: SELECT biz_type, count(*), sum(plant_number) FROM t_flower_biz_info WHERE project_id=? AND DATE(apply_time)=CURDATE() GROUP BY biz_type
   - 养护: SELECT count(*) FROM t_curing_record WHERE project_id=? AND DATE(curing_time)=CURDATE()
   - 任务: SELECT status, count(*) FROM t_daily_task_info WHERE project_id=? GROUP BY status
3. UPSERT 到 mart 表（ON CONFLICT (project_id, snapshot_date) DO UPDATE）
```

### 配置项

```yaml
dts:
  elt:
    enabled: false                          # 默认关闭，需显式开启
    cron: "0 0 * * * *"                    # 同步频率，默认每小时
    batch-size: 1000                        # 每批次最大行数
    datasource-id: ${DTS_BIZ_DATASOURCE_ID:1}  # 业务库数据源 ID
    retry-count: 3                          # 失败重试次数
    retry-delay-ms: 5000                    # 重试间隔
```

### JDBC 连接复用

通过 `copilot-ai` 已注册的 `AiDataSource` 获取业务库连接信息，或通过 `analytics` 的数据库管理功能。不新建连接池，复用现有基础设施。

## 完成标准

- [ ] EltSyncScheduler 可通过配置启停
- [ ] EltSyncService 编排逻辑完成
- [ ] watermark 管理（获取/更新/标记状态）
- [ ] 增量查询 SQL 正确
- [ ] 状态码翻译复用 BizEnumService
- [ ] UPSERT 逻辑（INSERT ON CONFLICT UPDATE）
- [ ] 配置项可通过 application.yml 或环境变量控制
- [ ] 同步失败不影响正常服务
