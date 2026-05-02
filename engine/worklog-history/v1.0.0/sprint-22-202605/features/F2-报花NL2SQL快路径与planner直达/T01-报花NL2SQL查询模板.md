# T01: 报花 NL2SQL 查询模板入库

**优先级**: P0
**状态**: READY
**依赖**: F1-T02

## 目标

把报花高频问句固化成 `nl2sql_query_template` 表中的 flowerbiz 模板，让 `TemplateMatcherService` 在路由前直接命中，不消耗 LLM token。

## 模板清单（首批 12 条）

| template_code | intent_pattern | view | 关键参数 |
|---|---|---|---|
| `FB-Q-LEASE-NET-MONTHLY` | `本月\|本期.*加摆\|撤摆\|净增\|净减` | `xycyl_ads_flowerbiz_lease_summary` | `biz_month?` |
| `FB-Q-LEASE-BY-PROJECT` | `XX.*项目.*报花\|XX.*加摆\|XX.*撤摆` | `xycyl_ads_flowerbiz_lease_summary` | `project_name`, `biz_month?` |
| `FB-Q-LEASE-DETAIL-RECENT` | `最近.*报花\|最近.*加摆\|最近.*撤摆` | `xycyl_ads_flowerbiz_lease_detail` | `project_name?`, `top_n?=20` |
| `FB-Q-PENDING-OVERDUE` | `审核中超过\|备货中超过\|挂起.*天\|久` | `xycyl_ads_flowerbiz_pending` | `status?`, `over_days?=7` |
| `FB-Q-PENDING-COUNT-BY-STATUS` | `各状态.*报花\|按状态.*报花单数` | `xycyl_ads_flowerbiz_pending` | `biz_month?` |
| `FB-Q-SALE-MONTHLY-RANK` | `销售.*前\|售花.*排行\|本月销售` | `xycyl_ads_flowerbiz_sale_summary` | `top_n?=10`, `biz_month?` |
| `FB-Q-BADDEBT-BY-CUSTOMER` | `坏账.*客户\|哪些客户.*坏账` | `xycyl_ads_flowerbiz_baddebt_summary` | `biz_month?` |
| `FB-Q-CHANGE-LOG-RECENT` | `变更.*起租\|变更.*金额\|起租期改` | `xycyl_ads_flowerbiz_change_log` | `change_type?`, `top_n?=20` |
| `FB-Q-RECOVERY-BY-PROJECT` | `回收.*项目\|报损\|回购\|留用` | `xycyl_ads_flowerbiz_recovery_detail` | `project_name?`, `recovery_type?` |
| `FB-Q-CURING-WORKLOAD` | `养护人.*经手\|XX.*师傅.*报花\|工作量` | `xycyl_ads_flowerbiz_curing_workload` | `curing_user?`, `biz_month?` |
| `FB-Q-FIRST-LEASE-BY-CUSTOMER` | `新签客户.*首次\|首次.*报花\|新客户` | `xycyl_ads_flowerbiz_lease_summary` | `biz_month?` |
| `FB-Q-LEASE-CUMULATIVE` | `累计.*加摆\|总.*加摆\|从.*以来` | `xycyl_ads_flowerbiz_lease_summary` | `project_name?`, `since?` |

## 模板示例

### `FB-Q-LEASE-NET-MONTHLY`

```sql
SELECT 
  项目, 客户,
  ROUND(SUM(加摆金额),2) AS 加摆,
  ROUND(SUM(撤摆金额),2) AS 撤摆,
  ROUND(SUM(净增金额),2) AS 净增,
  SUM(报花单数) AS 单数
FROM xycyl_ads.xycyl_ads_flowerbiz_lease_summary
WHERE 业务月份 = COALESCE('{{biz_month}}', DATE_FORMAT(CURDATE(),'%Y-%m'))
GROUP BY 项目, 客户
ORDER BY 净增 DESC
```

### `FB-Q-PENDING-OVERDUE`

```sql
SELECT *
FROM xycyl_ads.xycyl_ads_flowerbiz_pending
WHERE 1=1
{{#status}} AND 状态 = '{{status}}' {{/status}}
{{#over_days}} AND 已停留天数 > {{over_days}} {{/over_days}}
ORDER BY 已停留天数 DESC
```

## Liquibase

`dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_0NN__flowerbiz_query_templates.xml`，按 `0015_procurement_query_templates.xml` 风格组织。

## 影响范围

- `dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_0NN__flowerbiz_query_templates.xml` —— 新增
- `copilot_ai.nl2sql_query_template` —— INSERT 12 行

## 验证

- [ ] Liquibase update 通过
- [ ] `SELECT template_code FROM nl2sql_query_template WHERE domain='flowerbiz'` 返回 12 行
- [ ] ChatPanel 输入"本月加摆撤摆"命中 `FB-Q-LEASE-NET-MONTHLY`，时延 < 1s

## 完成标准

- [ ] 12 条模板入库
- [ ] 8 条 sprint README 样本问句中 ≥ 6 条进入 fast path
