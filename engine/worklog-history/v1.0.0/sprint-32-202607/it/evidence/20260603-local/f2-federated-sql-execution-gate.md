# F2/T03 联邦 SQL 执行门验证

**时间**: 2026-06-03
**环境**: 本地 dts-copilot / dts-stack，联邦查询入口 database_id=9
**范围**: 验证两段式 PRS dbt 表名在 Trino 联邦入口运行前自动补全为 `catalog.schema.table`

## 结论

- `/api/dataset` 主路径已覆盖：输入 `public.xycyl_ads_flowerbiz_sale_summary`，返回 `native_form.query` 为 `postgres.public.xycyl_ads_flowerbiz_sale_summary`。
- `DatasetQueryService.runNative()` 最终执行门已覆盖：固定报表、大屏预热等直接执行路径不会再绕过补全。
- `dts-stack` Trino 已补配置级资源护栏：查询时长、执行时长、规划时长、扫描字节、SQL 长度和全局 resource group 并发/队列限制。
- 当前验证覆盖的是 F2/T03 的 SQL 执行门与配置级资源护栏切片；运行态压测、Ranger 行列脱敏和数据库侧账号切换仍按后续项推进。

## 回归命令

```bash
bash worklog/v1.0.0/sprint-32-202607/it/test_f2_federated_sql_execution_gate.sh
mvn -q -Dmaven.repo.local=/opt/prod/prs/source/.m2 -pl dts-copilot-analytics -DskipTests package
```

结果：均通过。

## 运行时验证

```bash
curl -fsS http://127.0.0.1:50092/actuator/health
```

结果：`{"status":"UP"}`

重建并重启 `copilot-analytics` 后再次验证通过。

`/api/dataset` 验证使用本地有效会话调用，敏感会话值不落文档。

关键返回字段：

```json
{
  "rows": [[1580]],
  "native_form": {
    "query": "SELECT COUNT(*) AS cnt FROM postgres.public.xycyl_ads_flowerbiz_sale_summary"
  },
  "source_metadata": {
    "federated": true,
    "engine": "trino",
    "catalogs": ["postgres"],
    "relations": ["postgres.public.xycyl_ads_flowerbiz_sale_summary"]
  }
}
```

## 根因说明

历史资产、Agent SQL 和 screen JSON 多数以 Postgres 直连习惯保存为 `public.xycyl_*`。联邦入口 database_id=9 使用 Trino，必须执行三段式 `catalog.schema.table`。此前只在部分 `/api/dataset` prepare 路径做治理，固定报表、大屏预热等直接 `runNative()` 的路径仍可能绕过，导致同类 `catalog.schema.table` 报错反复出现。

后续相同问题继续排查时，需同时检查 SQL 方言和 Trino catalog 暴露的字段。销售查询复现显示：表名补全后，`::numeric`/`to_char()` 仍会在 Trino 侧失败；并且 PostgreSQL 无精度 `numeric` 列在默认 `unsupported-type-handling=IGNORE` 下会从 Trino metadata 中消失。详见 `f2-trino-dbt-sales-root-cause.md`。

## 配置级护栏

`dts-stack/services/dts-trino/config.properties`：

```properties
query.max-run-time=5m
query.max-execution-time=3m
query.max-planning-time=1m
query.max-scan-physical-bytes=2GB
query.max-length=100000
```

`dts-stack/services/dts-trino/resource-groups.json`：

```json
{
  "rootGroups": [
    {
      "name": "global",
      "softMemoryLimit": "80%",
      "hardConcurrencyLimit": 4,
      "maxQueued": 20
    }
  ]
}
```
