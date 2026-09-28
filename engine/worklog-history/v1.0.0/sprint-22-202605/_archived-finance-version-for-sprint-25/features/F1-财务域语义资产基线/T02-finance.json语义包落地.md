# T02: finance.json 语义包落地

**优先级**: P0
**状态**: READY
**依赖**: T01

## 目标

按 `procurement.json` 的格式标准，新增 `finance.json` 语义包，让 `SemanticPackService` 在加载 finance 域时返回完整的 objects / synonyms / fewShots / guardrails。

## 技术设计

### 1. 文件位置与加载机制

- 路径：`dts-copilot-ai/src/main/resources/semantic-packs/finance.json`
- 加载入口：`SemanticPackService` 启动时扫描 `semantic-packs/*.json`
- 不需要改 Java 代码，只需保证 JSON 结构与现有 schema 一致

### 2. JSON 结构骨架

```json
{
  "domain": "finance",
  "description": "财务结算主题域，覆盖月度结算、应收应付、回款进度、客户欠款排行、预支、报销、发票对账。authority 口径基于 sprint-19 ~ sprint-21 已落地的 authority/mart 视图。",
  "objects": [
    {
      "name": "月度结算",
      "view": "authority.finance.settlement_summary",
      "description": "项目 × 账期粒度的应收/已收/未收，是财务问句的主入口。金额已由 Java 结算服务计算完成。",
      "keyDimensions": ["project_name", "customer_name", "account_period", "settlement_status_name"],
      "keyMeasures": ["total_rent", "receivable_amount", "received_amount", "outstanding_amount"],
      "commonFilters": ["account_period", "project_name", "customer_name", "settlement_status_name"],
      "defaultTimeField": "account_period"
    },
    {
      "name": "应收概览",
      "view": "authority.finance.receivable_overview",
      "description": "全局应收账款 KPI 视图，含总应收、总欠款、按账龄分桶（30/60/90 天）。",
      "keyDimensions": ["as_of_date"],
      "keyMeasures": ["total_receivable", "total_outstanding", "over_30d", "over_60d", "over_90d"],
      "commonFilters": ["as_of_date"],
      "defaultTimeField": "as_of_date"
    },
    {
      "name": "待收款明细",
      "view": "authority.finance.pending_receipts_detail",
      "description": "尚未收款完成的结算单据明细，每行是一笔待收。",
      "keyDimensions": ["project_name", "customer_name", "due_date", "aging_bucket"],
      "keyMeasures": ["outstanding_amount"],
      "commonFilters": ["project_name", "customer_name", "due_date", "aging_bucket"],
      "defaultTimeField": "due_date"
    },
    {
      "name": "项目回款进度",
      "view": "authority.finance.project_collection_progress",
      "description": "项目 × 账期的回款率，回答 'XX 项目 2025 年回款多少' 类问题。",
      "keyDimensions": ["project_name", "customer_name", "account_period"],
      "keyMeasures": ["receivable_amount", "received_amount", "collection_rate"],
      "commonFilters": ["account_period", "project_name", "customer_name"],
      "defaultTimeField": "account_period"
    },
    {
      "name": "客户欠款排行",
      "view": "mart.finance.customer_ar_rank_daily",
      "description": "客户 × 日的欠款快照，含分账龄余额，回答 '客户欠款前 N' 类问题。",
      "keyDimensions": ["customer_name", "snapshot_date"],
      "keyMeasures": ["outstanding_amount", "aging_30d", "aging_60d", "aging_90d"],
      "commonFilters": ["snapshot_date", "customer_name"],
      "defaultTimeField": "snapshot_date"
    },
    {
      "name": "待付款审批",
      "view": "authority.finance.pending_payment_approval",
      "description": "正在 Flowable 审批中的付款单据。`approval_node` 为当前流转节点中文名。",
      "keyDimensions": ["payee_name", "approval_node", "submit_time"],
      "keyMeasures": ["payment_amount"],
      "commonFilters": ["payee_name", "approval_node", "submit_time"],
      "defaultTimeField": "submit_time"
    },
    {
      "name": "预支申请状态",
      "view": "authority.finance.advance_request_status",
      "description": "员工预支申请单据，含核销/未核销金额。",
      "keyDimensions": ["applicant_name", "status_name"],
      "keyMeasures": ["advance_amount", "offset_amount", "outstanding_advance"],
      "commonFilters": ["applicant_name", "status_name", "apply_time"],
      "defaultTimeField": "apply_time"
    },
    {
      "name": "报销列表",
      "view": "authority.finance.reimbursement_list",
      "description": "报销单据明细，按申请人 / 类别 / 状态分组分析。",
      "keyDimensions": ["applicant_name", "category_name", "status_name"],
      "keyMeasures": ["expense_amount"],
      "commonFilters": ["applicant_name", "category_name", "status_name", "submit_time"],
      "defaultTimeField": "submit_time"
    },
    {
      "name": "发票对账",
      "view": "authority.finance.invoice_reconciliation",
      "description": "客户发票与回款的对账明细。",
      "keyDimensions": ["customer_name", "invoice_no", "status_name"],
      "keyMeasures": ["invoice_amount", "reconciled_amount"],
      "commonFilters": ["customer_name", "status_name", "invoice_date"],
      "defaultTimeField": "invoice_date"
    }
  ],
  "synonyms": [
    { "term": "应收", "field": "receivable_amount", "views": ["authority.finance.settlement_summary", "authority.finance.receivable_overview", "authority.finance.project_collection_progress"] },
    { "term": "应收账款", "field": "receivable_amount", "views": ["authority.finance.settlement_summary", "authority.finance.receivable_overview"] },
    { "term": "待收", "field": "outstanding_amount", "views": ["authority.finance.settlement_summary", "authority.finance.pending_receipts_detail"] },
    { "term": "欠款", "field": "outstanding_amount", "views": ["authority.finance.settlement_summary", "mart.finance.customer_ar_rank_daily"] },
    { "term": "未收", "field": "outstanding_amount", "views": ["authority.finance.settlement_summary"] },
    { "term": "已收", "field": "received_amount", "views": ["authority.finance.settlement_summary", "authority.finance.project_collection_progress"] },
    { "term": "已回款", "field": "received_amount", "views": ["authority.finance.project_collection_progress"] },
    { "term": "实收", "field": "received_amount", "views": ["authority.finance.settlement_summary"] },
    { "term": "回款率", "field": "collection_rate", "views": ["authority.finance.project_collection_progress"] },
    { "term": "已付", "field": "payment_amount", "views": ["authority.finance.pending_payment_approval"] },
    { "term": "实付", "field": "payment_amount", "views": ["authority.finance.pending_payment_approval"] },
    { "term": "已支付", "field": "payment_amount", "views": ["authority.finance.pending_payment_approval"] },
    { "term": "账期", "field": "account_period", "views": ["authority.finance.settlement_summary", "authority.finance.project_collection_progress"] },
    { "term": "月份", "field": "account_period", "views": ["authority.finance.settlement_summary", "authority.finance.project_collection_progress"] },
    { "term": "期间", "field": "account_period", "views": ["authority.finance.settlement_summary"] },
    { "term": "账龄", "field": "aging_bucket", "views": ["authority.finance.pending_receipts_detail", "mart.finance.customer_ar_rank_daily"] },
    { "term": "审批节点", "field": "approval_node", "views": ["authority.finance.pending_payment_approval"] },
    { "term": "核销", "field": "offset_amount", "views": ["authority.finance.advance_request_status"] },
    { "term": "销账", "field": "offset_amount", "views": ["authority.finance.advance_request_status"] },
    { "term": "未核销", "field": "outstanding_advance", "views": ["authority.finance.advance_request_status"] },
    { "term": "报销金额", "field": "expense_amount", "views": ["authority.finance.reimbursement_list"] },
    { "term": "开票金额", "field": "invoice_amount", "views": ["authority.finance.invoice_reconciliation"] },
    { "term": "对账金额", "field": "reconciled_amount", "views": ["authority.finance.invoice_reconciliation"] }
  ],
  "fewShots": [
    {
      "question": "本月各项目应收多少？",
      "sql": "SELECT project_name, customer_name, ROUND(SUM(receivable_amount), 2) AS 应收金额 FROM authority.finance.settlement_summary WHERE account_period = DATE_FORMAT(CURDATE(), '%Y-%m') GROUP BY project_name, customer_name ORDER BY 应收金额 DESC"
    },
    {
      "question": "客户欠款前 10 是谁？",
      "sql": "SELECT customer_name, outstanding_amount AS 欠款金额, aging_30d, aging_60d, aging_90d FROM mart.finance.customer_ar_rank_daily WHERE snapshot_date = (SELECT MAX(snapshot_date) FROM mart.finance.customer_ar_rank_daily) ORDER BY outstanding_amount DESC LIMIT 10"
    },
    {
      "question": "2025 年 3 月哪些项目还没收款完？",
      "sql": "SELECT project_name, customer_name, receivable_amount, received_amount, ROUND(receivable_amount - received_amount, 2) AS 未收金额, collection_rate FROM authority.finance.project_collection_progress WHERE account_period = '2025-03' AND collection_rate < 1.0 ORDER BY 未收金额 DESC"
    },
    {
      "question": "万象城那个项目 2025 年回款进度",
      "sql": "SELECT account_period, ROUND(receivable_amount, 2) AS 应收, ROUND(received_amount, 2) AS 已收, ROUND(collection_rate * 100, 1) AS 回款率百分比 FROM authority.finance.project_collection_progress WHERE project_name LIKE '%万象城%' AND account_period LIKE '2025-%' ORDER BY account_period"
    },
    {
      "question": "待付款审批里有没有超过 30 天的？",
      "sql": "SELECT payee_name, approval_node, submit_time, ROUND(payment_amount, 2) AS 待付金额, DATEDIFF(CURDATE(), submit_time) AS 已挂起天数 FROM authority.finance.pending_payment_approval WHERE DATEDIFF(CURDATE(), submit_time) > 30 ORDER BY submit_time"
    },
    {
      "question": "上月预支单还有多少没核销？",
      "sql": "SELECT applicant_name, status_name, ROUND(SUM(outstanding_advance), 2) AS 未核销 FROM authority.finance.advance_request_status WHERE outstanding_advance > 0 AND apply_time >= DATE_SUB(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL 1 MONTH) AND apply_time < DATE_FORMAT(CURDATE(), '%Y-%m-01') GROUP BY applicant_name, status_name ORDER BY 未核销 DESC"
    },
    {
      "question": "2025 年 Q1 总开票金额",
      "sql": "SELECT ROUND(SUM(invoice_amount), 2) AS Q1开票总额 FROM authority.finance.invoice_reconciliation WHERE invoice_date >= '2025-01-01' AND invoice_date < '2025-04-01'"
    },
    {
      "question": "张三的报销单到哪一步了？",
      "sql": "SELECT applicant_name, category_name, status_name, ROUND(expense_amount, 2) AS 报销金额, submit_time FROM authority.finance.reimbursement_list WHERE applicant_name = '张三' ORDER BY submit_time DESC"
    }
  ],
  "guardrails": [
    "不要直接拼 f_settlement / f_settlement_item / a_month_accounting / a_collection_record 等底层物理表，统一走 authority.finance.* 视图。",
    "金额聚合（SUM / AVG）必须 ROUND(..., 2)，否则会出现 0.005 级精度漂移。",
    "账期口径用 account_period（YYYY-MM 字符串）而不是 DATE_FORMAT(create_time,'%Y-%m')，前者已在视图内做过软删过滤。",
    "回款率 collection_rate 已是小数（0.85），如果要展示百分比要乘 100；不要再除一次 receivable_amount。",
    "v_monthly_settlement 是 project 域协同视图，仅当问句以「项目」为主语且关心租金/摆位时使用；以「应收/欠款/待收/回款」为主语统一走 authority.finance.*。",
    "审批中状态只能从 authority.finance.pending_payment_approval / authority.finance.advance_request_status 这类 *_status 视图读，禁止从 act_* 流程引擎表 JOIN。",
    "客户欠款排行必须读 mart.finance.customer_ar_rank_daily 当日快照，不要现场聚合 settlement_summary —— mart 视图已经做了账龄分桶。"
  ]
}
```

### 3. 与 project-fulfillment.json 的隔离

- `v_monthly_settlement` **不**移入 finance.json，保留在 project-fulfillment.json
- `total_rent` 同义词在两个 pack 中都存在，但 finance.json 的 `total_rent` 仅指向 `authority.finance.settlement_summary`，避免冲突
- `SemanticPackService` 路由时按 domain 关键词命中（见 F2-T02）

### 4. 加载顺序与冲突处理

- 三个 pack 在 SemanticPackService 启动时同序加载
- 同义词冲突优先按 `domain` 与问句关键词匹配度决定，不在 pack 层处理
- guardrails 在 prompt 注入时按 domain 取对应 pack 的 guardrails，不混用

## 影响范围

- `dts-copilot-ai/src/main/resources/semantic-packs/finance.json` —— 新增（本 task 核心交付物）
- 不改 `SemanticPackService.java` —— 现有加载逻辑已经足够
- 不改其它两个 pack

## 验证

- [ ] `mvn test -pl dts-copilot-ai` 通过
- [ ] 启动 copilot-ai，调用 `GET /api/ai/copilot/semantic-packs/finance` 能返回完整 JSON（如果 endpoint 不存在则用日志验证 SemanticPackService 加载）
- [ ] JSON schema 校验：`objects[].view` 都能在 `analytics_table` 找到
- [ ] 不与 procurement.json / project-fulfillment.json 中的 `view` / `term` 字段产生模糊命中冲突

## 完成标准

- [ ] `finance.json` 落地，覆盖 9 个业务对象、20+ 同义词、8 条 fewShots、7 条 guardrails
- [ ] 启动加载无报错，`SemanticPackService` 日志显示 finance 域已注册
- [ ] sprint README 列出的 8 条样本问句都能在 fewShots 中找到对应或相近样例
