# T04: adminweb 项目固定报表对账面锁定

**优先级**: P0  
**状态**: DONE
**依赖**: T02

## 目标

选定项目域 dbt ADS 的对账来源，确保 `xycyl_ads_project_*` 不是脱离现有业务系统的孤立口径。

## 已锁定对账面

- adminweb API：`adminweb/src/api/flower/statistics/projectSummary.js`，endpoint `/rs-flowers-base/statistics/projectSummary/listPage`。
- adminweb 页面：`adminweb/src/views/flower/statistics/projectsummary/list-project-summary.vue`，列 `totalMonthRent`、`totalMonthCost`、`totalRealNumber`。
- adminapi Controller：`adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/statistics/controller/ProjectSummaryController.java`。
- adminapi SQL：`adminapi/rs-modules/rs-flowers-base/src/main/resources/mapper/statistics/ProjectSummaryMapper.xml`。

dbt 保留 raw 金额字段，同时新增 `*_adminweb_*` 字段承接当前运营报表口径，避免把 adminweb 现状误写成业务方最终口径。

## 对账指标

| 指标 | adminweb 公式 | ADS 字段 |
|---|---|---|
| 项目数 | `COUNT(DISTINCT p_project.id)` | `COUNT(*)` on `xycyl_ads_project_overview` |
| 月租金 | `SUM(total_number * rent)` where `status=1 and import_status=2` | `rent_amount_adminweb_sum` |
| 成本 | `SUM(total_number * cost)` where `status=1 and import_status=2` | `cost_amount_adminweb_sum` |
| 实摆总数量 | `SUM(good_number)` where `status=1 and import_status=2 and parent_id=-1` | `real_good_number_adminweb_sum` |
| 绿植/花盆/花架数量 | `SUM(total_number)` split by `good_type` | `green_number_adminweb_sum` / `flowerpot_number_adminweb_sum` / `flowerrack_number_adminweb_sum` |

## 验证

- [x] 固定报表 SQL 或 Mapper 方法定位到文件。
- [x] 记录输入条件和预期指标：项目数、实摆数量、租金、成本。
- [x] 明确误差门槛：关键金额误差 < 0.5%。
- [x] `RUN_LIVE=1 bash worklog/v1.0.0/sprint-25-202605/it/test_project_adminweb_reconcile.sh` 通过。

2026-05-31 live 结果：7 项指标 `7/7 PASS`，最大误差 `0.0000%`。证据见 `it/evidence/20260530-local/project-adminweb-reconcile.md`。

## 完成标准

- [x] F1/F3 有可复现对账基准。
