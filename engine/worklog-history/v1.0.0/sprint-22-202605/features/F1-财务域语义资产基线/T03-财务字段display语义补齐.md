# T03: 财务字段 display / semantic 语义补齐（短期方案）

**优先级**: P0
**状态**: READY
**依赖**: T02

## 双轨说明

本 task 是**短期方案**：直接 UPDATE `analytics_table` / `analytics_field`，让财务字段在 sprint-22 上线时立即可用。

**长期由 F4-T04 替代**：F4 完成后，元数据从 OpenMetadata Catalog API 派生（dbt manifest → OpenMetadata → dts-copilot 启动时拉取）；本 task 写入的 UPDATE 数据成为 fallback 兜底。

> 不本 sprint 删除本 task 写入的元数据 —— 独立部署场景仍需要兜底。

## 目标

把 finance.json 的同义词和字段口径下沉到 `analytics_table` / `analytics_field` 元数据表，让 BI 层（copilot-analytics）的字段选择器、列头、自动汇总规则也能对齐财务语义。

## 技术设计

### 1. 元数据写入策略

参考 sprint-20 F6-T02 的做法（`analytics_table` 的 `display_name`/`business_description`，`analytics_field` 的 `display_label`/`synonyms`/`semantic_type`）。

新建 changelog：

```
dts-copilot-analytics/src/main/resources/config/liquibase/changelog/
  v1_0_0_0NN__finance_field_semantic.xml
```

包含两组 UPDATE：

#### a) `analytics_table` UPDATE

| target_object | display_name | business_description |
|---|---|---|
| `authority.finance.settlement_summary` | 月度结算汇总 | 项目 × 账期粒度的应收/已收/未收，财务问句主入口 |
| `authority.finance.receivable_overview` | 应收概览 | 全局应收账款 KPI，含账龄分桶 |
| `authority.finance.pending_receipts_detail` | 待收款明细 | 尚未收款完成的结算单据 |
| `authority.finance.project_collection_progress` | 项目回款进度 | 项目 × 账期的回款率 |
| `mart.finance.customer_ar_rank_daily` | 客户欠款排行 | 客户 × 日的欠款快照 |
| `authority.finance.pending_payment_approval` | 待付款审批 | Flowable 审批中的付款单 |
| `authority.finance.advance_request_status` | 预支申请状态 | 预支单据，含核销/未核销 |
| `authority.finance.reimbursement_status` | 报销状态 | 报销审批状态 |
| `authority.finance.reimbursement_list` | 报销列表 | 报销单据明细 |
| `authority.finance.invoice_reconciliation` | 发票对账 | 客户发票与回款对账 |

#### b) `analytics_field` UPDATE

按字段批量更新 display_label / synonyms / semantic_type（若 schema 字段名不同，按实际项目调整）：

| field_name | display_label | synonyms | semantic_type |
|---|---|---|---|
| `receivable_amount` | 应收金额 | `应收 / 应收账款` | `currency` |
| `received_amount` | 已收金额 | `已收 / 已回款 / 实收` | `currency` |
| `outstanding_amount` | 待收金额 / 欠款金额 | `待收 / 欠款 / 未收` | `currency` |
| `total_rent` | 月租金 | `租金 / 月租` | `currency` |
| `payment_amount` | 付款金额 | `已付 / 实付 / 已支付` | `currency` |
| `expense_amount` | 报销金额 | `费用 / 报销` | `currency` |
| `invoice_amount` | 开票金额 | `开票 / 发票金额` | `currency` |
| `reconciled_amount` | 对账金额 | `已对账` | `currency` |
| `advance_amount` | 预支金额 | `预支` | `currency` |
| `offset_amount` | 核销金额 | `核销 / 销账` | `currency` |
| `outstanding_advance` | 未核销预支 | `未核销 / 待核销` | `currency` |
| `account_period` | 账期 | `月份 / 期间 / 财务月` | `month_string` |
| `as_of_date` | 截止日 | `截止 / 截至` | `date` |
| `snapshot_date` | 快照日 | `快照` | `date` |
| `aging_bucket` | 账龄分桶 | `账龄` | `categorical` |
| `aging_30d` / `aging_60d` / `aging_90d` | 30/60/90 天账龄 | `30 天内 / 60 天内 / 90 天以上` | `currency` |
| `collection_rate` | 回款率 | `回款率 / 收款率` | `percent_decimal` |
| `approval_node` | 审批节点 | `审批 / 流转节点` | `categorical` |
| `customer_name` | 客户 | `客户名 / 甲方` | `entity_name` |
| `project_name` | 项目点 | `项目 / 项目名` | `entity_name` |
| `applicant_name` | 申请人 | `申请 / 报销人 / 预支人` | `person_name` |
| `payee_name` | 收款人 | `收款 / 受款方` | `person_name` |

### 2. semantic_type 与下游联动

下游消费 `semantic_type` 的位置：

- copilot-analytics 的字段聚合规则（`currency` 自动 SUM + ROUND，`percent_decimal` 不参与 SUM）
- copilot-webapp 字段选择器排序与图标
- NL2SQL planner（finance fields 优先选 currency 类型作度量）

如果项目当前 schema 还没有 `semantic_type` 列，本 task **不**新增列；将 semantic 信息写入 `synonyms` JSON 字段或 `extra` JSON 字段（按实际 schema）。**T01 完成时需校对实际表结构**。

### 3. 与 BizEnumDictionary 的边界

- `*_name` 字段（已在视图层翻译）：本 task 不动 BizEnumDictionary
- 仍是数字编码的字段（如果有）：交给 T04
- `aging_bucket` 已经是 `0-30 / 30-60 / 60-90 / 90+` 字符串，作为 categorical 处理，不入字典

## 影响范围

- `dts-copilot-analytics/src/main/resources/config/liquibase/changelog/v1_0_0_0NN__finance_field_semantic.xml` —— 新增
- `dts-copilot-analytics/src/main/resources/config/liquibase/master.xml` —— 新增 include 行
- `copilot_analytics.analytics_table` —— UPDATE 10 行
- `copilot_analytics.analytics_field` —— UPDATE 约 25 行

## 验证

- [ ] Liquibase 在本地 dev 库 `mvn liquibase:update` 通过
- [ ] `SELECT target_object, display_name FROM analytics_table WHERE target_object LIKE 'authority.finance.%' OR target_object LIKE 'mart.finance.%'` 返回 10 行，display_name 全部非空
- [ ] copilot-webapp 查询页字段选择器中，财务视图字段显示中文 label
- [ ] 反向 rollback 脚本可执行（changelog 内含 `<rollback>` 块）

## 完成标准

- [ ] 10 张财务视图的元数据完整
- [ ] 25+ 字段的 display_label / synonyms 与 finance.json 一致
- [ ] 与 procurement / project 域字段元数据无冲突
