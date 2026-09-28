# F3: 项目域回归与验收

**优先级**: P1  
**状态**: DONE

## 目标

建立 Sprint-25 的可复现 IT 验收链，覆盖文档结构、dbt 构建、对账和 NL2SQL Golden Questions。

## 当前状态

F1/F2 baseline 已有产物，当前已覆盖结构、source profile、ingestion runtime、dbt build、NL2SQL 目录/模板验证、项目域 Golden Questions baseline，以及 adminweb ProjectSummary 固定报表对账。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 项目域 Golden Questions | P1 | DONE | 15 条 baseline，12 条 mart fast path |
| T02 | dbt 与 adminweb 指标对账脚本 | P1 | DONE | ProjectSummary listPage 7/7 PASS |
| T03 | IT 证据归档与验收矩阵 | P1 | DONE | T02 |

## 完成标准

- [x] 项目域 Golden Questions ≥ 15 条，其中 ≥ 8 条命中 mart 快路径。
- [x] mart baseline 覆盖率 12/15，8/8 query templates 均有可重跑证据。
- [x] adminweb ProjectSummary 对账误差 ≤ 0.5%，live 结果 7/7 PASS。
- [x] `it/evidence/<date>/` 包含命令、日志和结果摘要。

## 验收证据

- `it/sql/project_golden_questions.tsv`
- `it/test_project_golden_questions.sh`
- `it/evidence/20260530-local/project-golden-questions.md`
- `it/sql/project_adminweb_summary_reconcile.sql`
- `it/test_project_adminweb_reconcile.sh`
- `it/evidence/20260530-local/project-adminweb-reconcile.md`
