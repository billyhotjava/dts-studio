# 报花域 8 张 ads mart 设计清单

> sprint-22 F4 主交付物。每张 mart 从粒度、字段、物化策略、上游依赖、tags、回答的问句类型 6 个维度定义。

## 1. xycyl_ads_flowerbiz_lease_summary（租赁汇总）

| 项 | 内容 |
|---|---|
| 粒度 | 项目 × 客户 × 业务月份 |
| bizType 范围 | 1/2/3/4（换/加/减/调）|
| 物化策略 | `table` (每日 incremental by biz_month) |
| 关键字段 | 项目, 客户, 业务月份, 加摆金额, 撤摆金额, 净增金额, 报花单数 |
| 上游 | `xycyl_dws_flowerbiz_project_monthly` (biz_category='lease') |
| tags | `xycyl, xycyl-flowerbiz, lease, summary, monthly` |
| 主要问句 | 本月加摆撤摆净增减、项目租金变动、月度报花趋势 |

## 2. xycyl_ads_flowerbiz_lease_detail（租赁明细）

| 项 | 内容 |
|---|---|
| 粒度 | 单据 × 摆位 × 绿植 |
| bizType 范围 | 1/2/3/4 |
| 物化策略 | `view` |
| 关键字段 | flower_biz_id, biz_type_name, status_name, project, position, plant_name, plant_number, amount, apply_time, finish_time, applicant, curing_user, project_manager |
| 上游 | `xycyl_dwd_flowerbiz_item` JOIN `xycyl_dwd_flowerbiz_main` |
| tags | `xycyl, xycyl-flowerbiz, lease, detail` |
| 主要问句 | 万象城最近的报花单、养护人 X 经手的报花、近 N 天报花列表 |

## 3. xycyl_ads_flowerbiz_pending（待处理）

| 项 | 内容 |
|---|---|
| 粒度 | 单据 |
| bizType 范围 | 全部 |
| 物化策略 | `view` |
| 关键字段 | flower_biz_id, status_name, days_in_status, project, applicant, amount, apply_time |
| 上游 | `xycyl_dwd_flowerbiz_main` WHERE status NOT IN (5, -1) |
| tags | `xycyl, xycyl-flowerbiz, pending, realtime` |
| 主要问句 | 审核中超过 7 天、各状态报花单数、待结算清单 |

## 4. xycyl_ads_flowerbiz_sale_summary（销售汇总）

| 项 | 内容 |
|---|---|
| 粒度 | 项目 × 客户 × 业务月份 |
| bizType 范围 | 7/8（售/赠）|
| 物化策略 | `table` (每日 incremental) |
| 关键字段 | 项目, 客户, 业务月份, 销售金额, 赠送数量, 销售单数, 赠送单数 |
| 上游 | `xycyl_dws_flowerbiz_project_monthly` (biz_category IN ('sale','gift')) |
| tags | `xycyl, xycyl-flowerbiz, sale, summary, monthly` |
| 主要问句 | 本月销售金额、销售前 10 项目、赠送数量 |
| 注意 | **与 lease_summary 分离**！bizType=7/8 走 SaleAccount |

## 5. xycyl_ads_flowerbiz_baddebt_summary（坏账汇总）

| 项 | 内容 |
|---|---|
| 粒度 | 项目 × 客户 × 业务月份 |
| bizType 范围 | 6（坏账）|
| 物化策略 | `table` (每月 incremental) |
| 关键字段 | 项目, 客户, 业务月份, 坏账金额, 坏账类型 |
| 上游 | `xycyl_dws_flowerbiz_project_monthly` (biz_category='baddebt') |
| tags | `xycyl, xycyl-flowerbiz, baddebt, summary, monthly` |
| 主要问句 | 本月坏账客户、坏账 TOP、客户坏账历史 |
| 注意 | 短期可用；sprint-25 finance 域上线后客户欠款问句走 finance |

## 6. xycyl_ads_flowerbiz_change_log（变更日志）

| 项 | 内容 |
|---|---|
| 粒度 | 变更单 |
| bizType 范围 | 全部（来自 t_change_info）|
| 物化策略 | `view` |
| 关键字段 | flower_biz_id, change_type_name, before_amount, after_amount, before_lease_time, after_lease_time, change_time |
| 上游 | `xycyl_dwd_flowerbiz_change` |
| tags | `xycyl, xycyl-flowerbiz, change, audit` |
| 主要问句 | 起租期变更记录、金额变更、规格变更 |

## 7. xycyl_ads_flowerbiz_recovery_detail（回收明细）

| 项 | 内容 |
|---|---|
| 粒度 | 回收明细 |
| bizType 范围 | 3/4 触发的回收 |
| 物化策略 | `view` |
| 关键字段 | recovery_id, flower_biz_id, recovery_type_name, recovery_number, real_recovery_number, store_house, recovery_user, recovery_time |
| 上游 | `xycyl_dwd_flowerbiz_recovery` |
| tags | `xycyl, xycyl-flowerbiz, recovery, detail` |
| 主要问句 | 本月回收清单、报损了多少、回购量 |

## 8. xycyl_ads_flowerbiz_curing_workload（养护人工作量）

| 项 | 内容 |
|---|---|
| 粒度 | 养护人 × 业务月份 × biz_category |
| bizType 范围 | 全部 |
| 物化策略 | `table` (每月) |
| 关键字段 | curing_user, biz_month, biz_category, biz_count, total_amount |
| 上游 | `xycyl_dws_flowerbiz_curing_user_monthly` |
| tags | `xycyl, xycyl-flowerbiz, curing, workload, monthly` |
| 主要问句 | 养护人 X 本月经手多少报花、工作量排行 |

## 跨 mart 联合视图（可选）

如业务方需要"全量报花总览"，提供 view（不是 table）：

```sql
-- xycyl_ads_flowerbiz_overview（可选 view）
SELECT '租赁' AS biz_category, project, customer, biz_month, net_amount AS amount FROM xycyl_ads_flowerbiz_lease_summary
UNION ALL
SELECT '销售' AS biz_category, project, customer, biz_month, sale_amount AS amount FROM xycyl_ads_flowerbiz_sale_summary
UNION ALL
SELECT '坏账' AS biz_category, project, customer, biz_month, -baddebt_amount AS amount FROM xycyl_ads_flowerbiz_baddebt_summary
```

仅供"全口径"问句兜底，**不**作为日常 mart 推荐入口。
