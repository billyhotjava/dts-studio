# 多场景接入手册

**适用范围**: dts-copilot / dts-stack 中新增一个业务场景，让 Agent 能按自然语言选择固定资产、dbt ADS/DWS、semantic pack、本体对象图、Trino 联邦或业务只读明细。

本手册把 Sprint-31 的口径收口、Sprint-32 的五层路由和场景接入套件合成一条可执行路径。它不是自动建模器；它要求业务域先补齐口径、源表和验证证据。

## 0. 先选隔离方式

| 场景 | 推荐方式 | 说明 |
|------|----------|------|
| 同一个客户/同一套业务库内新增业务域 | 软隔离 | 使用 `domain`、`<scene>_*` dbt namespace、semantic pack 和 Trino catalog/schema 分组。 |
| 不同客户、不同租户、不同安全边界 | deploy-per-scenario | 独立部署 dts-stack / dts-copilot，不在同一部署里靠 domain 做强隔离。 |
| 涉及敏感列、角色差异、行级权限 | deploy-per-scenario 或 Ranger 强治理 | 软隔离不能替代 Ranger 行列级权限，也不能替代数据库账号隔离。 |

当前 sprint 的结论：`CatalogDomain` 是自由参数化数据行，适合做场景分组；不是租户隔离机制。

## 1. 六个必交付物

每个新场景必须补齐以下六项，缺一项就不要上线给 Agent 使用。

| 编号 | 交付物 | 位置 | 上线门槛 |
|------|--------|------|----------|
| 1 | CatalogDomain | `catalog-domain.json` 或平台治理数据 | owner、源系统、核心对象、口径陷阱、质量等级齐全。 |
| 2 | dbt namespace | `dbt/models/{stg,dwd,dws,ads}` | 至少有真实 STG/DWD/DWS/ADS；ADS/DWS 字段有类型和 owner/classification 元信息。 |
| 3 | semantic pack | `semantic-packs/<scene>.json` | 对象、同义词、fewShot、guardrail、sourceRefs 能解释主要问法。 |
| 4 | Trino catalog | `trino-catalog/<scene>.properties` | 只读账号、读副本或受限源、catalog/schema 明确。 |
| 5 | glossary | `glossary/<scene>-glossary.yml` | 字段名和业务页面/Excel 语言一致，金额、数量、状态口径清楚。 |
| 6 | routing map | `routing/<scene>-route-map.md` | 明确五层路由优先级、弱路径和何时该建 ADS。 |

## 2. 脚手架生成骨架

从 `dts-copilot` 根目录执行：

```bash
node scripts/scaffold-scenario-kit.mjs \
  --scene-code inventory \
  --domain-name "库存" \
  --owner "warehouse-owner" \
  --force
```

默认输出：

```text
worklog/v1.0.0/sprint-32-202607/assets/scenario-onboarding-kit/generated/inventory/
```

先用 `--dry-run` 查看将生成的文件；已有目录需要 `--force` 覆盖。生成物只是正确骨架，不能直接当成已建模资产上线。

## 3. 建模顺序

1. **源表发现**
   从 adminapi/adminweb 页面出发确认真实业务对象，不从表名猜业务。记录源表、页面路径、关键字段、状态枚举、金额/数量口径。

2. **ODS/STG**
   ODS 只保留源表结构和血缘。STG 做字段命名、时间字段、状态标签、软删除过滤，不做跨域汇总。

3. **DWD**
   对齐业务对象，例如库存域应先确认 `s_stock_info`、物品主数据、出入库流水、价格或成本来源。

4. **DWS**
   做可复用汇总，例如按仓库、物品、月份、状态汇总库存现量、入库、出库、低库存候选。

5. **ADS**
   只沉淀高频、稳定、口径已确认的报表资产。Agent 如果反复落到 Tier 4/5，优先把该问题沉淀到 ADS。

6. **元信息治理**
   dbt 模型和字段必须补齐 `expected_data_type`、owner、classification。缺治理元信息的模型不能发布。

## 4. Agent 五层路由接线

新增场景进入 Agent 后，按以下顺序取数：

| Tier | 路径 | 适合问题 | 接线要求 |
|------|------|----------|----------|
| T1 | 已发布指标 | 有权威口径的 KPI | 指标来自 dts-platform，不由 Agent 自己定义公式。 |
| T2 | dbt mart/template/固定资产 | 已沉淀 ADS/DWS 或固定报表资产 | SQL 只能使用已认证模型和字段。 |
| T3 | 本体对象图 / signal | 跨对象追溯、风险信号 | semantic pack 需要 links、metrics、signals、sourceRefs。 |
| T4 | guardrail 联邦 | 临时分析、缺 ADS 但可只读联邦 | 必须走 Trino，只允许授权 catalog，限制行数、超时和 SQL 方言。 |
| T5 | 业务只读明细 | 字段画像、状态分布、页面明细 | 使用业务对象目录和只读 ODS/adminapi，不直接执行业务写操作。 |

弱路径原则：

- Tier 4/5 可用于探索，但不能长期承载高频经营报表。
- telemetry 中同一 domain/target 高频落 Tier 4/5 时，创建 ADS 或固定资产候选。
- 不存在资产时，Agent 不能提示用户去资产库查看不存在的报表。

## 5. Trino 和 SQL 约束

联邦查询统一走 Trino，SQL 必须遵守：

- 表名使用 `catalog.schema.table`，例如 `postgres.public.xycyl_ads_flowerbiz_sale_summary`。
- MySQL 业务库使用 `mysql.rs_cloud_flower.<table>`。
- 不使用未授权 catalog，例如 `PRODUCTION`、`FLOWER_BIZ`。
- 避免 PostgreSQL-only 方言；历史资产可由运行时兜底转换，但新模板应直接写 Trino 兼容 SQL。
- 聚合 varchar 数值列时先 `TRY_CAST(... AS DOUBLE/DECIMAL)`。
- 只读查询，不允许 DDL/DML，不允许业务写接口。

## 6. 验证清单

上线前至少跑以下检查：

```bash
bash worklog/v1.0.0/sprint-32-202607/it/test_f3_scenario_onboarding_kit.sh
bash worklog/v1.0.0/sprint-32-202607/it/test_f4_inventory_source_discovery.sh
bash worklog/v1.0.0/sprint-32-202607/it/test_f4_inventory_dbt_ads_models.sh
bash worklog/v1.0.0/sprint-32-202607/it/test_f4_multi_scenario_isolation.sh
bash worklog/v1.0.0/sprint-32-202607/it/test_f1_route_trace.sh
bash worklog/v1.0.0/sprint-32-202607/it/test_f1_route_telemetry.sh
```

如果涉及联邦 SQL 或历史资产，还要跑：

```bash
bash worklog/v1.0.0/sprint-32-202607/it/test_f2_federated_sql_execution_gate.sh
bash worklog/v1.0.0/sprint-32-202607/it/test_f2_trino_mysql_access_policy.sh
bash worklog/v1.0.0/sprint-32-202607/it/test_sprint32_completion_gate.sh
```

每个验证都要沉淀到 `it/evidence/{date-env}/`，不能只写“已验证”。

## 7. 库存域实战状态

本 sprint 已把库存作为空白域样例推进：

- 已生成 `generated/inventory` 骨架。
- runtime 已加载 `warehouse` semantic pack。
- 低库存预警已从不存在的固定资产路径修正为 `business-object:prs.warehouse.stock_info`。
- 库存 ODS 已从业务 MySQL 导入 `ods_ptr_mysql_s_stock_info`。
- 库存 STG/DWD/DWS/ADS runtime build 已通过，`inventory_ads_overview` 11660 行，`inventory_ads_low_stock_alert` 9971 行。
- 多场景隔离回归已覆盖采购、报花、库存，不串域。

因此库存域当前已可作为 Sprint-32 的空白域接入样例；后续产品化时还应把临时 runtime build 纳入正式 dbt 发布包。

## 8. 交付判定

一个新场景可以认为接入完成，必须同时满足：

- 六个交付物齐全。
- 至少一条业务高频问法可命中 T1/T2/T3 中的强路径。
- Tier 4/5 只作为探索路径，并有 telemetry 记录。
- dbt 模型治理元信息完整。
- Trino 联邦只读、授权、超时、行数上限和审计可验证。
- `it/README.md` 有可重跑命令和证据文件。

如果这些条件不满足，只能标记为 `PARTIAL_PASS` 或 `IN_PROGRESS`，不能标记 DONE。
