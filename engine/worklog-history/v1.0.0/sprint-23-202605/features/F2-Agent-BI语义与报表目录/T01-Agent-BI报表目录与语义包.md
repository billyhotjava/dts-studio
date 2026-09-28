# T01: Agent BI 报表目录与语义包

**优先级**: P0  
**状态**: DONE  
**依赖**: F1-T01

## 目标

建立 PRS Agent BI 报表目录，让自然语言问题优先匹配业务域和已有资产。

## 报表目录字段

| 字段 | 说明 |
|---|---|
| `reportCode` | 稳定编码，例如 `prs.flowerbiz.lease_execution_monthly` |
| `domain` | `flowerbiz` / `project` / `finance` / `purchase` / `inventory` / `task` |
| `intentExamples` | 高频自然语言问句 |
| `dataSurface` | `L2_FIXED_REPORT` / `L1_DBT_MART` / `L0_ADMINAPI_READONLY` |
| `sourceRefs` | screen id、dbt model、adminapi endpoint 或 query template |
| `qualityLevel` | `HIGH` / `MEDIUM` / `LOW` |
| `defaultDisplay` | table / line / bar / pie / number / screen |
| `guardrails` | 时间口径、金额口径、禁用字段、权限要求 |

## 首批目录

| reportCode | 业务域 | 说明 | 数据面 |
|---|---|---|---|
| `prs.flowerbiz.overview` | 租赁/报花 | 报花总览大屏 | L2 screen |
| `prs.flowerbiz.lease_execution` | 租赁/报花 | 租赁执行与加摆/撤摆趋势 | L1 dbt + L2 screen |
| `prs.flowerbiz.pending_approval` | 流程/报花 | 待审批与处理积压 | L1 dbt + L0 adminapi |
| `prs.flowerbiz.recovery` | 回收 | 回收去向与项目分布 | L1 dbt |
| `prs.flowerbiz.baddebt` | 财务风险 | 坏账项目/客户排名 | L1 dbt |
| `prs.rental.monthly_receivable` | 租赁应收 | 月应收、待开票、待回款 | L2 固定报表 + L0 |
| `prs.project.customer_value` | 项目/客户 | 项目/客户贡献排行 | L1/L0 |

## 验证

- [x] 每个目录项至少有 3 条自然语言样例。
- [x] 每个目录项明确数据源和质量等级。
- [x] 租赁/报花域目录能覆盖 12 张 screen JSON。

## 完成标准

- [x] Planner 和前端均可接收同一套报表目录元数据。
