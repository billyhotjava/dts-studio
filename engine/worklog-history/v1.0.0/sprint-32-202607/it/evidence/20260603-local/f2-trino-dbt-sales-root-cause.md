# F2/T03 Trino dbt 销售查询根因复盘

**时间**: 2026-06-03
**环境**: 本地 dts-copilot / dts-stack，联邦查询入口 database_id=9
**问题**: Agent 问句 `查询2026年销售情况` 自动预览失败，历史报错包含 `联邦查询表名必须使用 catalog.schema.table: public.xycyl_ads_flowerbiz_sale_summary`

## 根因

这是同一类联邦契约漂移，不是单个 SQL 拼错：

1. **表名契约漂移**：dbt/模板资产保存为 Postgres 直连习惯 `public.xycyl_*`，但联邦入口是 Trino，运行时必须使用 `postgres.public.xycyl_*`。
2. **SQL 方言契约漂移**：销售模板 `TPL-37/TPL-52` 使用 Postgres 方言 `::numeric`、`to_char(DATE ...)`，即使表名补全后也会在 Trino 解析阶段失败。
3. **Trino catalog 类型暴露问题**：`xycyl_ads_flowerbiz_sale_summary` 的金额/单数字段是 PostgreSQL 无精度 `numeric`。Trino Postgres catalog 默认忽略不支持映射的列，`DESCRIBE` 只暴露 6 个非 numeric 字段，导致指标列无法解析。

## 修复

- `dts-stack/services/dts-trino/init/catalog/postgres.properties` 新增：

```properties
unsupported-type-handling=CONVERT_TO_VARCHAR
```

- `v1_0_0_026__trino_compatible_flowerbiz_sales_templates.xml` 增量更新 `TPL-37/TPL-52`：
  - 金额/单数聚合改为 `CAST(NULLIF(CAST(col AS VARCHAR), '') AS DECIMAL(18,2))`；
  - 年度销售使用 `s."年份" = :year`，避免 `to_char(DATE ...)`。

## 验证

- Trino `DESCRIBE postgres.public.xycyl_ads_flowerbiz_sale_summary` 已能看到 `销售金额全口径`、`销售单数`、`赠送成本全口径` 等指标列，类型为 `varchar`。
- Agent 返回：

```text
templateCode=TPL-52
responseKind=TEMPLATE_SQL
targetView=public.xycyl_ads_flowerbiz_sale_summary
```

- `/api/dataset` 使用 database_id=9 自动预览成功，返回：

```json
{
  "status": "completed",
  "row_count": 5,
  "database_id": 9,
  "first_row": ["2026-01", "朔黄大厦", 6900.00, 1.00, 0.00, 0.00]
}
```

关键执行 SQL 已被补全为：

```sql
FROM postgres.public.xycyl_ads_flowerbiz_sale_summary s
WHERE s."年份" = 2026
```

## 回归命令

```bash
mvn -q -Dmaven.repo.local=/opt/prod/prs/source/.m2 -pl dts-copilot-ai -Dtest=TemplateMatcherServiceTest,AssetBackedPlannerPolicyTest test
mvn -q -Dmaven.repo.local=/opt/prod/prs/source/.m2 -pl dts-copilot-analytics -Dtest=DefaultFederatedNativeSqlQualifierTest,DatasetQueryServiceTest,QueryExecutionFacadeTest test
```

结果：均通过。

## 同类问题扩展：screen/历史资产 SQL

**触发**: 大屏 screen 资产和历史固定模板仍保留旧 PostgreSQL SQL，例如：

```sql
SELECT ROUND(SUM(r."回收成本金额")::numeric, 2) AS "回收成本"
FROM public.xycyl_ads_flowerbiz_recovery_detail r
WHERE r."业务时间" >= {{dateFrom}}::date
  AND r."业务时间" <= {{dateTo}}::date
```

最小复现显示它不是 Trino 单点故障，而是同一类“联邦执行契约漂移”的连续三层：

1. `public.xycyl_*` 需要补全为 `postgres.public.xycyl_*`。
2. 模板参数渲染后形成 `?::date`，Trino 报 `mismatched input ':'`。
3. `unsupported-type-handling=CONVERT_TO_VARCHAR` 后，numeric 指标列以 `varchar` 暴露，旧 `SUM(col)` 会报 `Unexpected parameters (varchar) for function sum`。
4. `to_char(x, 'YYYY-MM-DD')` 在 Trino 下报 `Failed to tokenize string [Y]`，需要改为 `date_format(CAST(x AS timestamp), '%Y-%m-%d')`。

**补充修复**:

- `QueryExecutionFacade` 的 native SQL 自动重试增加 Trino 兼容重写：
  - `?::date` -> `CAST(? AS date)`
  - `SUM(x)::numeric` / `AVG(x)::numeric` -> `SUM(TRY_CAST(x AS DOUBLE))` / `AVG(TRY_CAST(x AS DOUBLE))`
  - 裸 `SUM(x)` / `AVG(x)` 在 Trino cast 语法错误重试路径中同步包裹 `TRY_CAST(x AS DOUBLE)`
  - `to_char(x, 'YYYY-MM-DD')` / `to_char(x, 'YYYY-MM')` -> `date_format(CAST(x AS timestamp), '%Y-%m-%d')` / `'%Y-%m'`

**live 验证**（2026-06-03，本地容器已重建）：

```text
旧 screen cast + varchar aggregate: status=completed, row_count=1
旧 screen to_char: status=completed, row_count=1, first_row=["2023-11-16"]
真实销售 2026 SQL: status=completed, row_count=5, first_row=["2026-01","26310.39","42179.11","13866.19","31.0","20.0"]
```

**回归命令补充**:

```bash
mvn -q -Dmaven.repo.local=/opt/prod/prs/source/.m2 -pl dts-copilot-analytics -Dtest=QueryExecutionFacadeTest,DatasetQueryServiceTest,DefaultFederatedNativeSqlQualifierTest,FederatedQueryGuardrailTest test
```

结果：通过；`dts-copilot-analytics` 已重建并重启，健康状态为 `healthy`。
