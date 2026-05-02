# T02: 报花域 planner 直达规则

**优先级**: P0
**状态**: READY
**依赖**: F1-T02

## 目标

在 `IntentRouterService` 的 `Nl2SqlRoutingRule` 中显式声明 flowerbiz 域的关键词、视图集合和优先级，让"加摆/撤摆/换花/调花/坏账/销售/赠花/起租/审核"等问句**不被错路由**到 procurement / project / finance。

## 路由规则

新建 `dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_0NN__flowerbiz_routing_rules.xml`：

| domain | keywords | primary_view | secondary_views | priority |
|---|---|---|---|---|
| `flowerbiz` | `报花, 报花单, 加摆, 加花, 撤摆, 撤花, 减花, 换花, 调花, 调拨, 售花, 销售, 赠花, 坏账, 配料, 加盆架, 减盆架, 起租, 减租, 净增, 净减, 审核中, 备货中, 核算中, 待结算, 已结束, 回收, 报损, 回购, 留用, 养护人, 项目经理, 业务经理` | `xycyl_ads_flowerbiz_lease_summary` | `xycyl_ads_flowerbiz_lease_detail, xycyl_ads_flowerbiz_pending, xycyl_ads_flowerbiz_sale_summary, xycyl_ads_flowerbiz_baddebt_summary, xycyl_ads_flowerbiz_change_log, xycyl_ads_flowerbiz_recovery_detail, xycyl_ads_flowerbiz_curing_workload` | 95 |

> 优先级 95 高于 procurement / project / finance（80），避免"项目报花"被路由到 project 域。

## 二级关键词路由

由 `Nl2SqlService` 在拿到 `domain=flowerbiz` 后选择主视图：

| 二级关键词 | 主视图 |
|---|---|
| `审核中超过 / 备货中超过 / 待结算超过 / 挂起` | `xycyl_ads_flowerbiz_pending` |
| `销售 / 售花 / 赠花` | `xycyl_ads_flowerbiz_sale_summary` |
| `坏账 / 死账` | `xycyl_ads_flowerbiz_baddebt_summary` |
| `回收 / 报损 / 回购 / 留用` | `xycyl_ads_flowerbiz_recovery_detail` |
| `养护人 / 师傅.*工作量 / 经手` | `xycyl_ads_flowerbiz_curing_workload` |
| `变更 / 起租期改 / 金额改` | `xycyl_ads_flowerbiz_change_log` |
| `最近.*报花 / XX.*报花单` | `xycyl_ads_flowerbiz_lease_detail` |
| `加摆 / 撤摆 / 净增 / 净减 / 月度` | `xycyl_ads_flowerbiz_lease_summary`（默认） |

## 与其他域的协调

| 关键词 | 冲突域 | 决策 |
|---|---|---|
| `项目` | flowerbiz / project | 项目作为筛选 + 报花动作 → flowerbiz；项目主体（合同/客户）→ project |
| `客户` | flowerbiz / project | 客户作为筛选 + 报花/销售/坏账 → flowerbiz；客户主体（联系方式/合同）→ project |
| `金额` | flowerbiz / procurement / finance(sprint-25) | 金额带"加摆/撤摆/销售/坏账" → flowerbiz；带"采购/进货" → procurement |
| `结算` | flowerbiz / finance(sprint-25) | 结算单 → finance（sprint-25 上线后）；待结算的报花 → flowerbiz |

## 影响范围

- `dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_0NN__flowerbiz_routing_rules.xml` —— 新增
- `copilot_ai.nl2sql_routing_rule` —— INSERT 1 主规则

## 验证

- [ ] Liquibase update 通过
- [ ] ChatPanel 输入"本月加摆撤摆" → 路由 `domain=flowerbiz`
- [ ] 输入"项目租金" → 仍走 project（v_monthly_settlement）
- [ ] 输入"采购金额前 10" → 仍走 procurement
- [ ] 输入"客户欠款"（sprint-25 后）→ 走 finance 不被 flowerbiz 抢

## 完成标准

- [ ] flowerbiz 域路由规则入库
- [ ] 跨域不冲突
