# T02: 财务域 planner 直达规则

**优先级**: P0
**状态**: READY
**依赖**: F1-T02

## 目标

在 `IntentRouterService` 的 `Nl2SqlRoutingRule` 中显式声明 finance 域的关键词、视图集合和优先级，让"财务/应收/欠款/回款/付款/报销/发票/预支"类问句**不会被错路由**到 procurement / project。

## 技术设计

### 1. 现状

当前 `IntentRouterService` 用关键词词频做意图路由。已有规则示例（来自 `0009_nl2sql_routing.xml`）：

- `domain=settlement` 关键词包含"租金/应收/结算"，主视图 `v_monthly_settlement`
- `domain=procurement` 关键词包含"采购/供应商/采购金额"，主视图 `authority.procurement.*`
- `domain=project` 关键词包含"项目/客户/合同"，主视图 `v_project_overview`

问题：
- 旧规则中的 `settlement` 域指向 `v_monthly_settlement`（project 域协同视图），与本 sprint 新建的 `finance` 域冲突
- "欠款 / 待付 / 报销 / 发票 / 回款" 等关键词没有规则，词频法兜底容易漂

### 2. 路由规则设计

#### 重命名旧规则

把 `domain='settlement'` 重命名为 `domain='finance'`，但 `primary_view` 改为 `authority.finance.settlement_summary`，关键词扩展。

新建 changelog：

```
dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_0NN__finance_routing_rules.xml
```

#### 规则清单

| domain | keywords | primary_view | secondary_views | priority |
|---|---|---|---|---|
| `finance` | `应收, 应收账款, 待收, 欠款, 未收, 已收, 已回款, 实收, 已付, 实付, 已支付, 账期, 月份, 期间, 财务月, 账龄, 回款, 回款率, 报销, 报销金额, 发票, 开票, 对账, 预支, 核销, 销账, 待付款, 审批节点, 客户欠款, 月度结算, 项目结算` | `authority.finance.settlement_summary` | `authority.finance.receivable_overview, authority.finance.pending_receipts_detail, authority.finance.project_collection_progress, mart.finance.customer_ar_rank_daily, authority.finance.pending_payment_approval, authority.finance.advance_request_status, authority.finance.reimbursement_list, authority.finance.invoice_reconciliation` | 90 |

> 注：finance 域优先级 90，高于 procurement / project（80），避免"应收"被路由到 procurement。

#### 子路由（关键词二级匹配）

由 `Nl2SqlService` 在拿到 `domain=finance` 后，根据问句二级关键词选择主视图：

| 二级关键词 | 主视图 |
|---|---|
| `客户欠款排行 / 欠款.*前 N` | `mart.finance.customer_ar_rank_daily` |
| `待付款审批 / 审批.*超过.*天 / 挂起` | `authority.finance.pending_payment_approval` |
| `回款进度 / XX.*回款 / 回款率` | `authority.finance.project_collection_progress` |
| `预支 / 核销 / 销账` | `authority.finance.advance_request_status` |
| `报销 / XX.*报销.*单` | `authority.finance.reimbursement_list` |
| `发票 / 开票金额 / 对账` | `authority.finance.invoice_reconciliation` |
| `账龄 / 30 天 / 60 天 / 90 天` | `authority.finance.pending_receipts_detail` 或 `mart.finance.customer_ar_rank_daily`（按问句主体决定） |
| `本月.*应收 / 当月.*应收 / 月度.*结算` | `authority.finance.settlement_summary`（默认） |

二级匹配实现方式：

- 不需要新建表，复用 `Nl2SqlService.selectPrimaryView()` 方法（已有），在该方法中对 `finance` 域增加二级关键词分支
- 如果当前实现不支持二级关键词，**不本 sprint 改 Java**，留待 sprint-23 / sprint-24 统一收口；本 sprint 仅落 routing rule

### 3. 与 procurement / project 的冲突回归

可能冲突的关键词：

- `项目` —— project 域 vs finance 域（"项目应收" / "项目回款"）
  - 解决：finance 关键词带"应收/回款/收款/欠款"等"金额行为"动词时，finance 优先
- `金额` —— finance / procurement 都用
  - 解决：finance 关键词强调"应收/已收/待付"，procurement 强调"采购金额"，由二级关键词区分
- `客户` —— finance / project 都用
  - 解决：客户作为主语 + "欠款 / 应收" 走 finance；客户作为筛选条件 + "项目"走 project

回归测试见 F3-T01。

### 4. 旧规则迁移

`0009_nl2sql_routing.xml` 中如果存在 `domain='settlement'` 的旧记录：

- 用 `<update>` 改 `domain` 为 `finance`，更新 `primary_view`
- 不直接删除以保持回滚能力
- 在 changelog 中加 `<rollback>` 反向 SQL

如果旧规则用的是 `0009_nl2sql_routing.xml` 之外的来源（如代码硬编码），T02 完成时需在 README "现状核实" 节补充。

## 影响范围

- `dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_0NN__finance_routing_rules.xml` —— 新增
- `dts-copilot-ai/src/main/resources/config/liquibase/master.xml` —— include 新增
- `copilot_ai.nl2sql_routing_rule` —— UPDATE 旧 settlement 行 + INSERT finance 二级规则
- 不动 `IntentRouterService.java`（除非二级匹配需要小改）

## 验证

- [ ] Liquibase update 通过
- [ ] `SELECT domain, primary_view FROM nl2sql_routing_rule WHERE domain='finance'` 返回 1 条主规则
- [ ] 在 ChatPanel 输入"客户欠款排行前 10"，路由日志显示 `domain=finance`，`primary_view=mart.finance.customer_ar_rank_daily`
- [ ] 输入"项目应收"，路由到 finance 而非 project
- [ ] 输入"采购金额前 10"，仍路由到 procurement，不被 finance 抢
- [ ] 输入"项目租金"，路由到 project（v_monthly_settlement）—— 与 finance 协同边界保持

## 完成标准

- [ ] finance 域路由规则入库
- [ ] 关键问句不再错路由
- [ ] 与 procurement / project 域无冲突
- [ ] 旧 settlement 规则平滑迁移到 finance
