# Sprint-23 PRS Agent BI Golden Questions

**范围**: PRS 租赁/报花域自然语言导报表。  
**默认时间窗**: `2025-05-01` 至当前日期。  
**验收目标**: 同一问句稳定路由到正确响应类型，并携带 `reportCode`、`dataSurface`、`qualityLevel`、展示建议和安全 SQL/大屏目标。

## 验收规则

- `FIXED_REPORT`: 必须返回已有大屏或固定报表目标，不能生成自由 SQL。
- `REPORT_DRAFT`: 必须只生成 `SELECT` SQL，默认查询 `public.xycyl_ads_flowerbiz_*`、`public.xycyl_dws_flowerbiz_*` 或允许的维表。
- `BUSINESS_DETAIL`: 必须走 L0 只读业务面或业务深链，不允许替代业务系统状态。
- `BUSINESS_INSIGHT`: 必须基于可追溯数据面给建议，并展示口径限制。
- `ACTION_PROPOSAL`: 只生成动作提案，不允许直接调用业务写接口。
- `MEDIUM` / `LOW` 数据质量的问题必须展示 `qualityNotes`。

## Golden Questions

| # | 问句 | 期望响应 | 期望数据面 | 期望目标/报表编码 | 展示建议 | 质量要求 | 资产化要求 |
|---|---|---|---|---|---|---|---|
| 1 | 打开 PRS 租赁经营总览大屏 | `FIXED_REPORT` | `L2_FIXED_REPORT` | `screen.prs-flowerbiz-overview-v1` / `PRS-FLOWERBIZ-OVERVIEW` | screen | `MEDIUM` | 可打开大屏 |
| 2 | 打开租赁报花执行看板 | `FIXED_REPORT` | `L2_FIXED_REPORT` | `screen.prs-flowerbiz-lease-execution-v1` / `PRS-FLOWERBIZ-LEASE-EXECUTION` | screen | `MEDIUM` | 可打开大屏 |
| 3 | 查看销售坏账与费用看板 | `FIXED_REPORT` | `L2_FIXED_REPORT` | `screen.prs-flowerbiz-finance-cost-v1` / `PRS-FLOWERBIZ-FINANCE-COST` | screen | `MEDIUM` | 可打开大屏 |
| 4 | 打开养护人工作量大屏 | `FIXED_REPORT` | `L2_FIXED_REPORT` | `screen.prs-flowerbiz-curing-workload-v1` / `PRS-FLOWERBIZ-CURING-WORKLOAD` | screen | `MEDIUM` | 可打开大屏 |
| 5 | 打开在途审批与操作监控 | `FIXED_REPORT` | `L2_FIXED_REPORT` | `screen.prs-flowerbiz-pending-approval-v1` / `PRS-FLOWERBIZ-PENDING-APPROVAL` | screen | `MEDIUM` | 可打开大屏 |
| 6 | 打开项目客户经营看板 | `FIXED_REPORT` | `L2_FIXED_REPORT` | `screen.prs-flowerbiz-project-customer-v1` / `PRS-FLOWERBIZ-PROJECT-CUSTOMER` | screen | `MEDIUM` | 可打开大屏 |
| 7 | 从 2025 年 5 月到现在，租赁收入按月趋势怎么样 | `REPORT_DRAFT` | `L1_DBT_MART` | `prs.flowerbiz.lease_execution_monthly` | line | `MEDIUM` + 时间窗提示 | 可保存草稿、建图、加入大屏 |
| 8 | 从 2025 年 5 月到现在，每个月加摆和撤摆金额分别是多少 | `REPORT_DRAFT` | `L1_DBT_MART` | `prs.flowerbiz.lease_execution_monthly` | line/bar | `MEDIUM` | 可保存草稿、建图、加入大屏 |
| 9 | 本月各项目报花金额排名 | `REPORT_DRAFT` | `L1_DBT_MART` | `public.xycyl_dws_flowerbiz_project_monthly` 或 `public.xycyl_ads_flowerbiz_lease_summary` | bar/table | `MEDIUM` | 可保存草稿、建图、加入大屏 |
| 10 | 哪些客户坏账金额最高，涉及哪些项目 | `REPORT_DRAFT` | `L1_DBT_MART` | `prs.flowerbiz.baddebt_rank` | bar/table | `MEDIUM` + 客户关联提示 | 可保存草稿、建图、加入大屏 |
| 11 | 本月待审批报花单按类型统计 | `REPORT_DRAFT` | `L1_DBT_MART` | `prs.flowerbiz.pending_approval` | bar | `MEDIUM` + 当前状态以业务系统为准 | 可保存草稿 |
| 12 | 审核中超过 7 天的报花单有哪些 | `REPORT_DRAFT` | `L1_DBT_MART` | `public.xycyl_ads_flowerbiz_pending` | table | `MEDIUM` | 可预览明细 |
| 13 | 各养护人本月处理工作量排行 | `REPORT_DRAFT` | `L1_DBT_MART` | `public.xycyl_ads_flowerbiz_curing_workload` | bar/table | `MEDIUM` | 可保存草稿、建图 |
| 14 | 本月回收去向按项目统计一下 | `REPORT_DRAFT` | `L1_DBT_MART` | `public.xycyl_ads_flowerbiz_recovery_detail` | stacked bar/table | `LOW` 或 `MEDIUM` + 覆盖度提示 | 可保存草稿，禁止自动动作 |
| 15 | 近三个月变更类型分布和影响金额 | `REPORT_DRAFT` | `L1_DBT_MART` | `public.xycyl_ads_flowerbiz_change_log` | bar/table | `MEDIUM` + 变更覆盖度提示 | 可保存草稿 |
| 16 | 本月额外费用按费用类型汇总 | `REPORT_DRAFT` | `L1_DBT_MART` | `public.xycyl_ads_flowerbiz_extra_cost_summary` | pie/bar | `MEDIUM` | 可保存草稿、建图 |
| 17 | 这个项目有哪些待确认账单 | `BUSINESS_DETAIL` | `L0_ADMINAPI_READONLY` | `prs.rental.pending_bill_detail` / `/rs-flowers-base/operate/monthAccount/listGreenAccountingPage` | table/deep link | `MEDIUM` | 只读明细，不写业务 |
| 18 | 这个客户最近有哪些待开票和待回款事项 | `BUSINESS_DETAIL` | `L0_ADMINAPI_READONLY` | `prs.rental.pending_bill_detail` | table/deep link | `MEDIUM` | 只读明细，不写业务 |
| 19 | 哪些客户最近回款异常，需要跟进 | `ACTION_PROPOSAL` 或 `BUSINESS_INSIGHT` | `ACTION_PROPOSAL` / `L0_ADMINAPI_READONLY` | `prs.rental.collection_followup_proposal` | table/advice | `MEDIUM` | 只生成建议或提案 |
| 20 | 帮我发起催收任务 | `ACTION_PROPOSAL` | `ACTION_PROPOSAL` | `prs.rental.collection_followup_proposal` | proposal | `MEDIUM` | 不得直接执行写动作 |
| 21 | 近 30 天采购金额和库存周转异常有哪些 | `BUSINESS_INSIGHT` | `L0_ADMINAPI_READONLY` | `prs.purchase.inventory_turnover` | table | `MEDIUM` | 只读建议 |
| 22 | 本周巡检和养护任务延期最多的人是谁 | `BUSINESS_INSIGHT` | `L0_ADMINAPI_READONLY` | `prs.task.supervise_workload` | table | `MEDIUM` | 只读建议 |
| 23 | 审批待办积压最多的流程类型是什么 | `BUSINESS_INSIGHT` | `L0_ADMINAPI_READONLY` | `prs.workflow.audit_backlog` | table | `HIGH`/`MEDIUM` | 只读建议 |
| 24 | 基于租赁执行趋势生成一张可加入大屏的折线图 | `REPORT_DRAFT` | `L1_DBT_MART` | `prs.flowerbiz.lease_execution_monthly` | line | `MEDIUM` + 时间窗提示 | 必须提供保存草稿和加入大屏入口 |

## 覆盖矩阵

| 类型 | 问句数 | 覆盖点 |
|---|---:|---|
| `FIXED_REPORT` | 6 | 12 张 PRS 大屏中的核心 6 张 L2 快路径 |
| `REPORT_DRAFT` | 11 | 租赁趋势、坏账、待审批、养护、回收、变更、费用 |
| `BUSINESS_DETAIL` | 2 | 租赁月账/待确认账单只读深链 |
| `BUSINESS_INSIGHT` | 3 | 采购库存、任务巡检、审批待办 |
| `ACTION_PROPOSAL` | 2 | 催收建议和动作草稿，不执行写动作 |

## 最低通过标准

- 至少 20 条问句有稳定响应。
- 至少 8 条 `REPORT_DRAFT` 可以保存为 `analysis_draft`。
- 至少 3 条 `REPORT_DRAFT` 可以创建图表并加入大屏。
- 至少 4 条问句命中已有 PRS screen。
- 所有动态 SQL 均为 `SELECT`，且不访问 `ods_ptr_mysql_*`，除非用户明确要求源表排查。
