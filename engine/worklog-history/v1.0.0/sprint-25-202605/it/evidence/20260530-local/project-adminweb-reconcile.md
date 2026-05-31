# Sprint-25 F0/T04 Project adminweb Reconciliation

**Date**: 2026-05-30; live rerun 2026-05-31 00:27 CST  
**Scope**: adminweb ProjectSummary listPage vs Sprint-25 dbt `xycyl_ads_project_overview`.

## Source Surface

- adminweb API: `adminweb/src/api/flower/statistics/projectSummary.js`, endpoint `/rs-flowers-base/statistics/projectSummary/listPage`.
- adminweb page: `adminweb/src/views/flower/statistics/projectsummary/list-project-summary.vue`, columns `totalMonthRent`, `totalMonthCost`, `totalRealNumber`.
- adminapi controller: `adminapi/rs-modules/rs-flowers-base/src/main/java/com/rs/flowers/base/statistics/controller/ProjectSummaryController.java`.
- adminapi SQL: `adminapi/rs-modules/rs-flowers-base/src/main/resources/mapper/statistics/ProjectSummaryMapper.xml`.

## Reconciliation Contract

The dbt mart now carries explicit `*_adminweb_*` fields for the current operational adminweb ProjectSummary formulas:

- `rent_amount_adminweb_sum`: `SUM(total_number * rent)` where `status=1` and `import_status=2`.
- `cost_amount_adminweb_sum`: `SUM(total_number * cost)` where `status=1` and `import_status=2`.
- `real_good_number_adminweb_sum`: `SUM(good_number)` where `status=1`, `import_status=2`, and `parent_id=-1`.
- `green_number_adminweb_sum`, `flowerpot_number_adminweb_sum`, `flowerrack_number_adminweb_sum`: `SUM(total_number)` split by `good_type`.

The acceptance threshold is `0.5%` max relative difference per metric.

## Verification Commands

```bash
bash worklog/v1.0.0/sprint-25-202605/it/test_project_adminweb_reconcile.sh
RUN_LIVE=1 bash worklog/v1.0.0/sprint-25-202605/it/test_project_adminweb_reconcile.sh
```

## Live Result

`RUN_LIVE=1 bash worklog/v1.0.0/sprint-25-202605/it/test_project_adminweb_reconcile.sh` passed after rebuilding the Sprint-25 project dbt package against local DTS PostgreSQL.

```text
metric                          | adminweb_value | dbt_value  | diff_pct | status
--------------------------------+----------------+------------+----------+-------
cost_amount_adminweb_sum        | 4038819.03     | 4038819.03 | 0.0000   | PASS
flowerpot_number_adminweb_sum   | 49376.00       | 49376.00   | 0.0000   | PASS
flowerrack_number_adminweb_sum  | 3145.00        | 3145.00    | 0.0000   | PASS
green_number_adminweb_sum       | 148409.00      | 148409.00  | 0.0000   | PASS
project_count                   | 222.00         | 222.00     | 0.0000   | PASS
real_good_number_adminweb_sum   | 75464.00       | 75464.00   | 0.0000   | PASS
rent_amount_adminweb_sum        | 1067755.65     | 1067755.65 | 0.0000   | PASS
```

Summary: `7/7 PASS`, max relative difference `0.0000%`, threshold `0.5%`.

## Boundary

This locks the adminweb fixed report reconciliation surface. It does not replace business-owner signoff for the final project-domain P0 decisions in `assets/project-caliber-decisions.md`; it documents the current operational source of truth used for dbt/adminweb parity.
