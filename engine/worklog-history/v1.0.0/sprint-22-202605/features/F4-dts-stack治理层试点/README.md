# F4: dts-stack 治理层试点（报花域）

**优先级**: P0
**状态**: READY
**轨**: 2（dts-stack 仓 / 长期产出）

## 目标

在已成型的 dts-stack dbt 工作流中新建花卉业务（馨懿诚绿植租摆）命名空间 `xycyl`，**从 0 把报花域 5 层（ods → stg → dwd → dws → ads）建齐**：

- ods sources：报花域 7 张关键源表显式声明
- stg / dwd / dws：分层职责清晰的转换层
- ads：8 张报花 mart 模型，覆盖 13 种 bizType 的查询场景

完成后 dts-copilot 通过 datasource 直读 dbt 产出，但 dts-copilot 自身**保留独立部署能力**。

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
- 报花域 7 张源表的 dbt sources 声明
- 8 张报花 ads mart + 中间 stg/dwd/dws 模型
- OpenMetadata 上"花卉报花" service 与 Glossary
- dts-copilot ↔ dbt 产出库 的 datasource 通路
- 花卉 dbt 编译产物的"独立部署导出"机制

### 报花域的特殊复杂度（来自代码 review）

- **13 种 bizType**：1 换花 / 2 加花 / 3 减花 / 4 调花 / 6 坏账 / 7 售花 / 8 赠花 / 10 配料 / 11 加盆架 / 12 减盆架 + 其他
- **金额方向不一**：bizType=2/10/11 为 +正，bizType=3/6/12 为 -负，bizType=1/4 为中性
- **结算链路分叉**：bizType=7/8 走 `ISaleAccountService`，**不进** SettlementItem
- **6 个真实陷阱**（异步触发 / 软外键 / 状态竞态 / 金额符号 / 链路分叉 / 时间字段不可靠）—— 见 sprint README "报花联动的 6 个真实陷阱" 节

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | dbt sources 与命名空间 / 分层规范落地 | P0 | READY | F0-T03（口径决策完成）|
| T02 | 报花 mart 模型实现（stg → dwd → dws → ads） | P0 | READY | T01 |
| T03 | Airflow DAG 调度（xycyl-flowerbiz） | P0 | READY | T02 |
| T04 | OpenMetadata 集成 + Glossary 同步 | P1 | READY | T02, T03 |
| T05 | dts-copilot datasource 切换 + 独立部署导出 | P0 | READY | T02, T03 |

## 完成标准

- [ ] dts-stack dbt 项目下 `models/xycyl/` 命名空间建立，包含 `sources/`、`stg/`、`dwd/`、`dws/`、`ads/` 五层
- [ ] **报花 7 张源表**全部声明为 dbt sources（`t_flower_biz_info` / `t_flower_biz_item` / `t_flower_biz_item_detailed` / `t_change_info` / `t_recovery_info` / `t_recovery_info_item` / `t_flower_rent_time_log`）
- [ ] **8 张 ads mart 模型**实现（按 `assets/flowerbiz-mart-catalog.md` 清单），schema.yml 测试覆盖关键字段
- [ ] dbt 模型全部带 `tags=['xycyl', 'xycyl-flowerbiz', '<topic>']`，`xycyl-flowerbiz` 可被 Airflow `selector=tag:xycyl-flowerbiz` 命中
- [ ] **关键 dim 表**：`xycyl_dim_flowerbiz_status_alias`（7 状态）、`xycyl_dim_flowerbiz_biztype_alias`（13 类型）、`xycyl_dim_flowerbiz_recovery_type_alias`（3 回收去处）落地
- [ ] **金额符号在 ads 层规范化**：按 F0-T03 决策（推荐"统一变正 + amount_direction 字段"）实施
- [ ] **租赁 vs 销售拆分**：`lease` 与 `sale` 两类 ads 分离，禁止混合 SUM
- [ ] `dwh_xycyl_flowerbiz_dbt_manual.py` DAG 在 Airflow 触发后跑通 `dbt run + dbt test + dbt docs generate`
- [ ] OpenMetadata 中可看到 `xycyl_flowerbiz` service 的模型血缘 + 字段 description + Glossary 词条
- [ ] dts-copilot 注册"花卉报花 mart" datasource，通过该 datasource 执行的 SQL 与现网 adminweb 报花列表页结果一致（行数 100% / 数量 ±1 / 金额差 < 0.01 元）
- [ ] `bin/export-dbt-artifacts.sh xycyl-flowerbiz ./dist/xycyl-flowerbiz` 可产出独立部署 tarball
- [ ] `it/test_xycyl_dbt_consistency.sh` 通过：同一组报花问句在 dbt 产出 vs adminapi 实时查询的结果一致

## 与 F1/F2/F3 的衔接

- **F4 不阻塞 F1/F2/F3 上线**：sprint-22 中期可以先发布 F1/F2/F3（dts-copilot 内部，立即可用），F4 在 sprint 末完成后切换 datasource
- **F4 完成后**：F1-T03 / F1-T04 写入的元数据成为兜底；新元数据通过 OpenMetadata API 派生
- **回退路径**：sprint-22 上线时 dts-copilot 仍可通过 adminapi 直读模式工作（datasource 切换前），F4 出问题秒级切回
