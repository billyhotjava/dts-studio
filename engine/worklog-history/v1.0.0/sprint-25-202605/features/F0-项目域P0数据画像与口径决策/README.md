# F0: 项目域 P0 数据画像与口径决策

**优先级**: P0  
**状态**: BLOCKED

## 目标

在写 dbt 生产模型前，确认项目域源表是否已经按 `public.ods_ptr_mysql_*` 入湖，并把实摆组数、金额、快照粒度和停用项目过滤等口径交给业务方拍板。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 项目域源表入湖范围确认 | P0 | DONE | - |
| T02 | 项目实摆与摆位调整数据画像 | P0 | BLOCKED | T01；核心事实 ODS 已建空表，等待入数 |
| T03 | 5 个项目域口径决策访谈 | P0 | BLOCKED | T02 |
| T04 | adminweb 项目固定报表对账面锁定 | P0 | BLOCKED | T02 |

## 完成标准

- [x] `assets/project-source-catalog.md` 中每张表都有生产入湖状态。
- [ ] `p_project_green` 的 status/import_status/parent_id 分布完成画像。
- [ ] `assets/project-caliber-decisions.md` 五个决策均为 `RESOLVED`。
- [ ] 至少 2 个 adminweb 项目/统计类报表被选为 P2 对账基准。

## 当前阻塞

2026-05-29 本地 `biadmin.public` 已补齐 Sprint-25 所需 11 张 ODS 物理表，其中 `ods_ptr_mysql_p_project` / `ods_ptr_mysql_p_customer` 有数据，其他 9 张为新建空表。`p_project_green`、`p_position_adjustment*` 等事实表没有业务行前，T02 不能输出状态分布、金额口径或孤儿率结论，dbt 生产模型仍暂停。
