# F4: dts-stack 治理层试点（花卉财务域）

**优先级**: P0
**状态**: READY
**轨**: 2（dts-stack 仓 / 长期产出）

## 目标

在已成型的 dts-stack dbt 工作流中新建花卉业务（馨懿诚绿植租摆）命名空间 `xycyl`，把 sprint-21 已落地的 10 张财务 mart 视图从"PG 物化视图 + Liquibase 手维护"迁移成"dbt 模型 + Airflow 调度 + OpenMetadata 治理"。完成后 dts-copilot 通过 datasource 直读 dbt 产出，但 dts-copilot 自身**保留独立部署能力**（通过 dbt 编译产物导出实现）。

本 Feature 是 sprint-22 的**长期治理产出**，与 F1/F2/F3（dts-copilot 智能层短期产出）并行，不互相阻塞。

## 现状（基于 dts-stack 2026-05-01 盘点）

### 已具备

- dbt 项目骨架成型：`services/dts-dbt/` 含 `dbt_project.yml`、6 个工具宏（`truncate_relation` / `parse_date_safe` / `parse_numeric_safe` / `nullif_placeholder` / `ensure_date_helpers` / `get_custom_schema`）
- 分层规范成型：`models/{ods,stg,dwd,dws,ads}/`
- 已有业务命名空间：`fin_*`（基金管理）、`pm_*` / `pm_*_v2`（项目管理）；它们用 `tags=['finance', 'biz', 'ads', ...]` 区分
- Airflow DAG 模板：`dags/dwh/dwh_finance_dbt_manual.py` / `dwh_project_management_dbt_manual.py` / `dwh_dbt_dbt_manual.py`，selector 用 `tag:finance` 等
- OpenMetadata 集成：`services/dts-openmetadata/ingestion/dbt.yml` 配置了从 `target/manifest.json` + `catalog.json` + `run_results.json` 自动入库
- dts-platform 后端服务：68 个 governance 类 + 2 个 openmetadata 集成类，提供 `/api/etl/dbt/models/sync`、`/api/modeling/sql-models/import` 等 API

### 尚未具备（本 Feature 要建立）

- 花卉业务命名空间（`xycyl_*`）—— 0 模型 0 source 0 DAG
- sprint-21 落地的 mart 视图迁移
- OpenMetadata 上"花卉财务" service 与 Glossary
- dts-copilot ↔ dbt 产出库 的 datasource 通路
- 花卉 dbt 编译产物的"独立部署导出"机制

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | dbt sources 与命名空间 / 分层规范落地 | P0 | READY | F1-T01 |
| T02 | 花卉财务 mart 模型迁移到 dbt | P0 | READY | T01 |
| T03 | Airflow DAG 调度（xycyl-finance） | P0 | READY | T02 |
| T04 | OpenMetadata 集成 + Glossary 同步 | P1 | READY | T02, T03 |
| T05 | dts-copilot datasource 切换 + 独立部署导出 | P0 | READY | T02, T03 |

## 完成标准

- [ ] dts-stack dbt 项目下 `models/xycyl/` 命名空间建立，包含 `sources/`、`stg/`、`dwd/`、`dws/`、`ads/` 五层
- [ ] 10 张 sprint-21 mart 视图全部由 dbt 模型实现，schema.yml 测试覆盖关键字段
- [ ] dbt 模型全部带 `tags=['xycyl', 'xycyl-finance', '<topic>']`，`xycyl-finance` 可被 Airflow `selector=tag:xycyl-finance` 命中
- [ ] `dwh_xycyl_finance_dbt_manual.py` DAG 在 Airflow 触发后跑通 `dbt run + dbt test + dbt docs generate`
- [ ] OpenMetadata 中可看到 `xycyl_finance` service 的模型血缘 + 字段 description + Glossary 词条
- [ ] dts-copilot 注册"花卉 mart" datasource，通过该 datasource 执行的 SQL 与 sprint-21 PG 视图返回结果差异 < 0.1%（金额）/ 行数完全一致
- [ ] `bin/export-dbt-artifacts.sh` 可在 dts-stack 编译完成后导出 mart DDL + manifest 元数据，供 dts-copilot 独立部署场景使用
- [ ] `it/test_xycyl_dbt_consistency.sh` 通过：同一组财务问句在两侧 datasource 跑 SQL 结果一致

## 与 F1/F2/F3 的衔接

- **F4 不阻塞 F1/F2/F3 上线**：sprint-22 中期可以先发布 F1/F2/F3（dts-copilot 内部，立即可用），F4 在 sprint 末完成后切换 datasource
- **F4 完成后**：F1-T03 / F1-T04 写入的元数据成为兜底；新元数据通过 OpenMetadata API 派生
- **回退路径**：sprint-21 落地的 PG 物化视图保留 30 天，F4 出问题可秒级切回