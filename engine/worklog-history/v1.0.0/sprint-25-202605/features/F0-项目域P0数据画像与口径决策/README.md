# F0: 项目域 P0 数据画像与口径决策

**优先级**: P0  
**状态**: IN_PROGRESS

## 目标

在写 dbt 生产模型前，确认项目域源表是否已经按 `public.ods_ptr_mysql_*` 入湖，并把实摆组数、金额、快照粒度和停用项目过滤等口径交给业务方拍板。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 项目域源表入湖范围确认 | P0 | DONE | - |
| T02 | 项目实摆与摆位调整数据画像 | P0 | DONE | T01 |
| T03 | 5 个项目域口径决策访谈 | P0 | BLOCKED | T02；等待业务方拍板 |
| T04 | adminweb 项目固定报表对账面锁定 | P0 | DONE | T02 |

## 完成标准

- [x] `assets/project-source-catalog.md` 中每张表都有生产入湖状态。
- [x] `p_project_green` 的 status/import_status/parent_id 分布完成画像。
- [ ] `assets/project-caliber-decisions.md` 五个决策均为 `RESOLVED`。
- [x] adminweb `ProjectSummaryMapper.listPage` 被选为首个固定报表对账基准，并有 live 误差证据。

## 当前阻塞

2026-05-30 task `46` 已将 11 张 Sprint-25 ODS 入湖，T02 数据画像已归档到 `it/evidence/20260530-local/project-profile-after-ingestion.md`。

剩余阻塞不再是入数或 adminweb 对账面，而是业务口径拍板：`rent/cost` 最终业务口径、`parent_id=-1` 是否计为业务组数、停用项目默认是否过滤。

T04 已用 `ProjectSummaryMapper.listPage` 锁定首个固定报表对账基准。`RUN_LIVE=1 bash worklog/v1.0.0/sprint-25-202605/it/test_project_adminweb_reconcile.sh` 对 `xycyl_ads_project_overview` 的 7 项 adminweb 对账字段验证为 `7/7 PASS`，最大误差 `0.0000%`。
