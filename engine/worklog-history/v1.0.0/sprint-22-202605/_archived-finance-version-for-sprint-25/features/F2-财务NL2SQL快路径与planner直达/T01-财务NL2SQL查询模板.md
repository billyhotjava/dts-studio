# T01: 财务 NL2SQL 查询模板入库

**优先级**: P0
**状态**: READY
**依赖**: F1-T02

## 目标

把财务高频问句固化成 `nl2sql_query_template` 表中的 finance 模板，让 `TemplateMatcherService` 在路由前直接命中，不消耗 LLM token。

## 技术设计

### 1. 表结构参考

参考 `0010_nl2sql_query_template.xml`、`0011_mart_query_templates.xml`、`0015_procurement_query_templates.xml` 的列定义：

- `domain` —— `finance`
- `template_code` —— 全局唯一，`FIN-Q-*` 风格
- `intent_pattern` —— 关键词或正则匹配
- `sql_template` —— 参数化 SQL，支持 `{{param}}` 占位
- `parameter_schema_json` —— 参数定义
- `priority` —— 越大越优先，财务模板初始 `priority=80`
- `active` —— `true`

### 2. 模板清单（首批 12 条）

| template_code | intent_pattern | view | 参数 |
|---|---|---|---|
| `FIN-Q-MONTHLY-SETTLEMENT-BY-PROJECT` | `本月\|本期\|当月.*应收\|结算.*项目` | `authority.finance.settlement_summary` | `account_period?` |
| `FIN-Q-OUTSTANDING-BY-PROJECT` | `欠款\|未收\|待收.*项目\|按项目.*欠款` | `authority.finance.settlement_summary` | `account_period?` |
| `FIN-Q-CUSTOMER-AR-RANK-TOPN` | `客户欠款.*前\|欠款.*排行\|欠款.*排名` | `mart.finance.customer_ar_rank_daily` | `top_n?=10` |
| `FIN-Q-CUSTOMER-AR-DETAIL` | `客户.*欠款明细\|XX.*还欠多少` | `mart.finance.customer_ar_rank_daily` | `customer_name` |
| `FIN-Q-COLLECTION-PROGRESS-BY-PROJECT` | `回款进度\|XX.*回款` | `authority.finance.project_collection_progress` | `project_name?`, `account_period?` |
| `FIN-Q-PENDING-RECEIPTS-BY-AGING` | `账龄\|逾期.*待收\|账龄.*分桶` | `authority.finance.pending_receipts_detail` | `aging_bucket?` |
| `FIN-Q-PENDING-PAYMENT-OVERDUE` | `待付款.*超过\|审批.*超过.*天\|挂起.*久` | `authority.finance.pending_payment_approval` | `over_days?=30` |
| `FIN-Q-ADVANCE-OUTSTANDING` | `预支.*没核销\|未核销.*预支\|预支.*余额` | `authority.finance.advance_request_status` | `applicant_name?` |
| `FIN-Q-REIMBURSEMENT-BY-APPLICANT` | `XX.*报销\|报销.*单.*到\|报销.*状态` | `authority.finance.reimbursement_list` | `applicant_name` |
| `FIN-Q-INVOICE-AMOUNT-PERIOD` | `开票金额\|发票.*总.*金额\|Q[1-4].*开票` | `authority.finance.invoice_reconciliation` | `period_start`, `period_end` |
| `FIN-Q-RECEIVABLE-OVERVIEW-KPI` | `应收概览\|应收.*KPI\|总应收` | `authority.finance.receivable_overview` | `as_of_date?=CURDATE()` |
| `FIN-Q-SETTLEMENT-STATUS-COUNT` | `结算单.*多少\|按状态.*结算单\|结算.*分布` | `authority.finance.settlement_summary` | `account_period?` |

### 3. SQL 模板示例

#### `FIN-Q-CUSTOMER-AR-RANK-TOPN`

```sql
SELECT
  customer_name,
  ROUND(outstanding_amount, 2) AS 欠款金额,
  ROUND(aging_30d, 2) AS 30天内,
  ROUND(aging_60d, 2) AS 30到60天,
  ROUND(aging_90d, 2) AS 60到90天
FROM mart.finance.customer_ar_rank_daily
WHERE snapshot_date = (SELECT MAX(snapshot_date) FROM mart.finance.customer_ar_rank_daily)
ORDER BY outstanding_amount DESC
LIMIT {{top_n}}
```

参数 schema：
```json
{
  "params": [
    { "name": "top_n", "type": "integer", "label": "前 N 名", "default": 10, "min": 1, "max": 100 }
  ]
}
```

#### `FIN-Q-COLLECTION-PROGRESS-BY-PROJECT`

```sql
SELECT
  account_period AS 账期,
  ROUND(receivable_amount, 2) AS 应收,
  ROUND(received_amount, 2) AS 已收,
  ROUND(receivable_amount - received_amount, 2) AS 未收,
  ROUND(collection_rate * 100, 1) AS 回款率百分比
FROM authority.finance.project_collection_progress
WHERE 1=1
{{#project_name}}
  AND project_name LIKE CONCAT('%', '{{project_name}}', '%')
{{/project_name}}
{{#account_period}}
  AND account_period = '{{account_period}}'
{{/account_period}}
ORDER BY account_period DESC, 未收 DESC
```

#### `FIN-Q-PENDING-PAYMENT-OVERDUE`

```sql
SELECT
  payee_name,
  approval_node,
  submit_time,
  ROUND(payment_amount, 2) AS 待付金额,
  DATEDIFF(CURDATE(), submit_time) AS 已挂起天数
FROM authority.finance.pending_payment_approval
WHERE DATEDIFF(CURDATE(), submit_time) > {{over_days}}
ORDER BY submit_time ASC
```

### 4. 模板匹配优先级

模板间冲突解决：

- 关键词匹配数多者优先
- `priority` 字段大者优先（finance 域统一 80，与 procurement 持平）
- 当多个模板分数相同时，向 LLM 抛"二次澄清"问题（已有机制），不要静默选错

### 5. 参数解析

`TemplateMatcherService` 在命中模板后，仍需用 LLM 抽取参数（如 `customer_name`、`top_n`）。本 task 不重写抽取逻辑，只保证：

- 必填参数缺失时，模板返回"待澄清"，由会话补齐
- 数字范围（`top_n` ≤ 100）由模板 schema 校验，超限抛错
- 时间参数支持 `本月 / 上月 / 2025 年 3 月 / Q1` 等中文表述（复用现有时间解析器）

### 6. Liquibase 落地

```
dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_0NN__finance_query_templates.xml
```

按 `0015_procurement_query_templates.xml` 风格组织，每条模板一个 `<changeSet>`，含 `<sqlCheck>` 防重复。

## 影响范围

- `dts-copilot-ai/src/main/resources/config/liquibase/changelog/v1_0_0_0NN__finance_query_templates.xml` —— 新增
- `dts-copilot-ai/src/main/resources/config/liquibase/master.xml` —— include 新增
- `copilot_ai.nl2sql_query_template` —— INSERT 12 行
- 不动 `TemplateMatcherService.java`

## 验证

- [ ] Liquibase 在 dev 库 `mvn liquibase:update` 通过
- [ ] `SELECT template_code, domain FROM nl2sql_query_template WHERE domain='finance'` 返回 12 行
- [ ] 在 ChatPanel 输入"客户欠款前 10"，路由日志显示命中 `FIN-Q-CUSTOMER-AR-RANK-TOPN`，时延 < 1s
- [ ] 输入"万象城回款进度"，命中 `FIN-Q-COLLECTION-PROGRESS-BY-PROJECT`，参数 `project_name='万象城'` 自动抽取
- [ ] 输入"应收账款情况"，至少进入 `FIN-Q-RECEIVABLE-OVERVIEW-KPI` 候选

## 完成标准

- [ ] 12 条 finance 模板入库，`active=true`
- [ ] 8 条 sprint README 样本问句中 ≥ 6 条进入 TEMPLATE_FAST_PATH
- [ ] 没有模板冲突（同样问句不会同时命中两个模板）
