# Sprint-25 IT 验收入口

## 当前可运行检查

```bash
bash worklog/v1.0.0/sprint-25-202605/it/test_sprint25_datasurface_plan.sh
```

该脚本验证：

- 顶层 rollout plan 已修正为 `public.ods_ptr_mysql_*` 入湖约定。
- Sprint-25 README、P0 资产、dbt 模型目录、feature 入口存在。
- 项目域源表清单包含关键 ODS 物理表。
- 口径决策和模型目录包含 Sprint-25 必需项。
- F0 ODS 补表 DDL、source-profile SQL、project dbt 包、Golden Questions 和本地证据存在。

## F0 入湖核验

```bash
bash worklog/v1.0.0/sprint-25-202605/it/test_project_source_profile_sql.sh
RUN_LIVE=1 bash worklog/v1.0.0/sprint-25-202605/it/test_project_source_profile_sql.sh
```

`RUN_LIVE=1` 默认连接本地 `v223-dts-pg-1` / `biadmin`，执行 `it/sql/project_source_profile.sql`。2026-05-29 本地结果见 `it/evidence/20260529-local/project-source-profile.md`。

2026-05-30 首轮权限重试结果见 `it/evidence/20260530-local/project-source-profile-permission-retry.md`：当时本地 DTS ODS 仍只有 `p_project` / `p_customer` 有行；后续权限恢复重跑已确认 11 张 ODS 均已入数。

2026-05-30 后续恢复结果见 `it/evidence/20260530-local/project-ingestion-runtime.md`：新增 `sprint25_project_datasurface` task `46` 后，execution `83` 成功，11 张 Sprint-25 ODS 均已入数。

```bash
bash worklog/v1.0.0/sprint-25-202605/it/test_project_ingestion_runtime.sh
RUN_LIVE=1 bash worklog/v1.0.0/sprint-25-202605/it/test_project_ingestion_runtime.sh
```

## F0 ODS 补表

```bash
docker exec -i v223-dts-pg-1 psql -U biadmin -d biadmin -f - < worklog/v1.0.0/sprint-25-202605/it/sql/project_ods_create_tables.sql
```

DDL 只 `CREATE TABLE IF NOT EXISTS public.ods_ptr_mysql_*`，不 drop、不改已有 `p_project` / `p_customer`。补表证据见 `it/evidence/20260529-local/project-ods-create-tables.md`。

## F1 dbt 包核验

```bash
bash worklog/v1.0.0/sprint-25-202605/it/test_project_dbt_package.sh
RUN_DBT_PARSE=1 bash worklog/v1.0.0/sprint-25-202605/it/test_project_dbt_package.sh
```

交付包：

```text
worklog/v1.0.0/sprint-25-202605/assets/xycyl-project-dbt-model.zip
```

该包可导入 DTS 平台，包含 11 个 source、27 个 model、50 个 data tests。2026-05-29 已在本地 DTS PostgreSQL 使用 dbt-core 容器完成 `dbt run --select tag:xycyl-project` 和 `dbt test --select tag:xycyl-project`，证据见 `it/evidence/20260529-local/project-dbt-package.md`。

2026-05-30 入数后已使用 `dts-dbt:1.10.0` 容器完成 `dbt build --select tag:xycyl-project`，结果 `PASS=76 WARN=1 ERROR=0`，证据见 `it/evidence/20260530-local/project-dbt-build-after-ingestion.md`。

## F1/F3 adminweb 对账核验

```bash
bash worklog/v1.0.0/sprint-25-202605/it/test_project_adminweb_reconcile.sh
RUN_LIVE=1 bash worklog/v1.0.0/sprint-25-202605/it/test_project_adminweb_reconcile.sh
```

该脚本锁定 adminweb `ProjectSummaryMapper.listPage` 作为项目域固定报表对账基准，比较 `xycyl_ads_project_overview` 中 `*_adminweb_*` 字段与 adminapi SQL 当前运营口径。2026-05-31 live 结果：7 项指标 `7/7 PASS`，最大误差 `0.0000%`，证据见 `it/evidence/20260530-local/project-adminweb-reconcile.md`。

## F2 NL2SQL dbt 接入核验

```bash
mvn -pl dts-copilot-ai -Dtest=SemanticPackServiceTest,AgentBiReportCatalogServiceTest,BusinessObjectCatalogServiceTest test
```

2026-05-30 已将项目域 semantic pack、report catalog、business object catalog 和 runtime query templates 指向 Sprint-25 dbt marts；证据见 `it/evidence/20260530-local/project-nl2sql-dbt-routing.md`。

## F3 Golden Questions 核验

```bash
bash worklog/v1.0.0/sprint-25-202605/it/test_project_golden_questions.sh
RUN_LIVE=1 bash worklog/v1.0.0/sprint-25-202605/it/test_project_golden_questions.sh
```

2026-05-30 已补齐 15 条项目域 Golden Questions baseline，其中 12 条命中 mart fast path，覆盖 `TPL-44` 至 `TPL-51` 8 个项目域 dbt query templates。live 检查确认 7 个 `public.xycyl_*` 目标 mart/DWD 均存在且有数据，证据见 `it/evidence/20260530-local/project-golden-questions.md`。

## 后续证据目录

实际数据画像、dbt 构建和 NL2SQL 回归结果统一放到：

```text
worklog/v1.0.0/sprint-25-202605/it/evidence/YYYYMMDD-<env>/
```

## 后续验证命令

- `mvn -pl dts-copilot-ai -Dtest=AssetBackedPlannerPolicyTest test`：业务方给出项目域自由问句样例后，补 planner 级别准确率验证。
- `RUN_LIVE=1 bash worklog/v1.0.0/sprint-25-202605/it/test_project_golden_questions.sh`：复核 Golden Questions 对应 mart/DWD 是否仍存在且有数据。
