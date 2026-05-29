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
- F0 source-profile SQL 和 2026-05-29 本地证据存在。

## F0 入湖核验

```bash
bash worklog/v1.0.0/sprint-25-202605/it/test_project_source_profile_sql.sh
RUN_LIVE=1 bash worklog/v1.0.0/sprint-25-202605/it/test_project_source_profile_sql.sh
```

`RUN_LIVE=1` 默认连接本地 `v223-dts-pg-1` / `biadmin`，执行 `it/sql/project_source_profile.sql`。2026-05-29 本地结果见 `it/evidence/20260529-local/project-source-profile.md`。

## 后续证据目录

实际数据画像、dbt 构建和 NL2SQL 回归结果统一放到：

```text
worklog/v1.0.0/sprint-25-202605/it/evidence/YYYYMMDD-<env>/
```

## 后续验证命令

- `dbt build --select tag:xycyl-project`：P1 dbt 模型落地后执行。
- `mvn -pl dts-copilot-ai -Dtest=SemanticPackServiceTest,AssetBackedPlannerPolicyTest,AgentBiReportCatalogServiceTest test`：项目域 NL2SQL 接入后执行。
- `bash worklog/v1.0.0/sprint-25-202605/it/test_project_golden_questions.sh`：Golden Questions 成型后执行。
