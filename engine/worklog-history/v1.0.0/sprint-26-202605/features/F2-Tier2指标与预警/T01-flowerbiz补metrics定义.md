# T01: flowerbiz.json 补 metrics 定义

**优先级**: P1
**状态**: DONE
**依赖**: F1/T01

## 目标

在 `metrics` 节集中定义报花域派生指标，统一口径，杜绝同一指标在不同问句下口径漂移。

## 技术设计

示例指标：

| name | object | expr | caliber |
|------|--------|------|---------|
| 项目租金净额 | 租赁经营汇总 | `SUM("加摆金额") - SUM("撤摆金额")` | 已结口径，仅 status=7 |
| 坏账率 | 项目 | `坏账金额 / NULLIF(应收金额,0)` | percent |
| 客户在租金额 | 客户 | 按租赁口径汇总 | 在摆 status 过滤 |

口径必须与 dbt 4 列金额标准（rent/cost/sale/extra_cost）一一对应，金额符号沿用 dim_biztype 注释的权威约定。

## 影响范围

- `flowerbiz.json` 的 `metrics` 节。

## 验证

- [x] 每个 metric 的 expr 字段在对应 mart 视图中存在。
- [x] 口径与 dbt 4 列金额标准一致（rent/cost/sale/extra_cost）；adminweb 对账在 T04 做数值证据。

## 完成标准

- [x] metrics 集中定义、口径可追溯到 dbt 注释。

## 证据

- `it/test_flowerbiz_metrics.sh`
- `it/evidence/20260530-local/flowerbiz-metrics.md`
