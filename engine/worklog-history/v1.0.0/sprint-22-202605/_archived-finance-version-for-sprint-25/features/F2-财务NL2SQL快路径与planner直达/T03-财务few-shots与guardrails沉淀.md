# T03: 财务域 few-shots 与 guardrails 沉淀

**优先级**: P0
**状态**: READY
**依赖**: F1-T02, F2-T01, F2-T02

## 目标

把 finance.json 内置的 fewShots / guardrails 与已发现的失败案例打通，让 AGENT_WORKFLOW 路径下 LLM 得到的 prompt 含足够强的财务域上下文，写出口径正确的 SQL。

## 技术设计

### 1. Few-shot 来源与放置

- **finance.json 内置 fewShots**（F1-T02 已写）—— 简短示例，加载到主 prompt
- **prompts/finance-few-shots.txt**（新增）—— 长例子，仅在 AGENT_WORKFLOW 路径触发时注入

参考 `prompts/settlement-few-shots.txt` 的格式：

```
# Few-shot 1
USER: 2025 年 3 月哪些项目还没收款完？
THOUGHT: 用户关心项目级回款进度，按账期=2025-03 过滤，回款率<1.0 表示没收完。走 authority.finance.project_collection_progress。
SQL:
SELECT project_name, customer_name, ROUND(receivable_amount,2) AS 应收, ROUND(received_amount,2) AS 已收, ROUND(receivable_amount-received_amount,2) AS 未收金额, collection_rate
FROM authority.finance.project_collection_progress
WHERE account_period = '2025-03' AND collection_rate < 1.0
ORDER BY 未收金额 DESC

# Few-shot 2
USER: 客户 A 还欠多少？
THOUGHT: 客户欠款明细，从客户欠款排行视图过滤客户名，取最新快照。
SQL:
SELECT customer_name, ROUND(outstanding_amount,2) AS 欠款金额, aging_30d, aging_60d, aging_90d
FROM mart.finance.customer_ar_rank_daily
WHERE customer_name LIKE '%客户A%'
  AND snapshot_date = (SELECT MAX(snapshot_date) FROM mart.finance.customer_ar_rank_daily)

# Few-shot 3
... (共 8-10 条，覆盖 8 类问句)
```

### 2. Constraints 文件（参考 settlement-constraints.txt）

新增 `prompts/finance-constraints.txt`，用于 prompt 末尾的硬约束注入：

```
[FINANCE DOMAIN HARD CONSTRAINTS]

1. 金额聚合（SUM / AVG）必须 ROUND(..., 2)。
2. 账期 account_period 是 'YYYY-MM' 字符串；不要 DATE_FORMAT(create_time,'%Y-%m')。
3. 回款率 collection_rate 是小数（0.85），展示百分比要 ROUND(... * 100, 1)。
4. 客户欠款必须读 mart.finance.customer_ar_rank_daily 当日快照，不要现场聚合 settlement_summary。
5. 待付款审批数据来自 authority.finance.pending_payment_approval；不要从 act_* 流程表 JOIN。
6. 报销/预支/发票走对应 authority.finance.* 视图；不要跨域 JOIN f_* / a_* 物理表。
7. 项目作为主语 + 关心租金/摆位 → 用 v_monthly_settlement（project 域）；
   项目作为主语 + 关心应收/欠款/回款 → 用 authority.finance.*（finance 域）。
8. 客户名 / 项目名用 LIKE '%xxx%' 模糊匹配；账期、状态用精确等值。
9. 如果用户问"前 N"，N 默认 10，硬上限 100。
```

### 3. Guardrails 与失败回归集

新增 `assets/finance-guardrail-cases.md`，记录禁止重复出现的失败案例：

| Case | 错误问法 | LLM 容易写错的 SQL | 正确做法 |
|---|---|---|---|
| C1 | 客户欠款前 10 | `SUM(outstanding_amount) FROM authority.finance.settlement_summary GROUP BY customer_name` | 走 mart 视图 + snapshot_date 取最新 |
| C2 | 万象城 2025 回款进度 | 拼 `f_settlement` + `a_collection_record` JOIN | 走 `authority.finance.project_collection_progress` |
| C3 | 待付款审批 | 从 `act_ru_task` 反查 | 走 `authority.finance.pending_payment_approval` |
| C4 | 上月预支没核销 | 走 `t_advance_info.status=2`（数字硬编码） | 走 `authority.finance.advance_request_status` + `outstanding_advance > 0` |
| C5 | 张三报销到哪步 | 拼 `a_expense_account_info` + Flowable | 走 `authority.finance.reimbursement_list` + `status_name` |
| C6 | 项目应收 | 走 `v_project_overview` 找应收（无应收字段） | 走 `authority.finance.settlement_summary` |
| C7 | Q1 开票 | `t_invoice` 自拼 + 不过滤作废 | 走 `authority.finance.invoice_reconciliation` |
| C8 | 月度结算 | 走 `v_monthly_settlement` 但回款率字段缺失 | 涉及回款率改走 `authority.finance.project_collection_progress` |

每个 case 在文档中给出：

- 错误 SQL（已发生过的或预测会犯的）
- 错误根因（视图缺失、字段口径错、表链错）
- 正确 SQL
- 在 `assets/finance-authority-catalog.md` 中的对应章节链接

### 4. Prompt 注入位置

```
[SYSTEM PROMPT]
...
{semantic_pack_summary}        ← 来自 finance.json
{routing_decision_explain}     ← 来自 IntentRouterService
{finance_constraints}          ← 来自 prompts/finance-constraints.txt（仅 finance 域）
{finance_few_shots}            ← 来自 prompts/finance-few-shots.txt（仅 AGENT_WORKFLOW 路径）

[USER]
{user_question}
```

注入由 `Nl2SqlService.buildPrompt()` 完成，本 task **不**修改 Java 代码（finance.json 中的 fewShots 已经够覆盖 TEMPLATE_FAST_PATH 漏掉的场景），只在两个文本文件中沉淀长例。如果现有逻辑只能注入 JSON 内 fewShots，则把所有内容压进 `finance.json.fewShots`，不再用 .txt。

> 实施时按现有 prompt 加载机制选择路径，不增加新机制。

### 5. 与现网失败案例的对接

T03 实施期间，需要从 `ai_audit_log` 拉一份历史财务问句（最近 60 天）：

```sql
SELECT created_at, question, response_kind, generated_sql
FROM copilot_ai.ai_audit_log
WHERE created_at > NOW() - INTERVAL '60 days'
  AND (question LIKE '%欠款%' OR question LIKE '%应收%' OR question LIKE '%回款%' OR question LIKE '%报销%' OR question LIKE '%发票%' OR question LIKE '%预支%' OR question LIKE '%结算%')
ORDER BY created_at DESC;
```

把命中错链 / 走错域 / 给错口径的真实问句补进 `finance-guardrail-cases.md`，让 guardrails 反映真实历史失败而不是猜测。

## 影响范围

- `dts-copilot-ai/src/main/resources/prompts/finance-few-shots.txt` —— 新增（如果选 .txt 路径）
- `dts-copilot-ai/src/main/resources/prompts/finance-constraints.txt` —— 新增（如果选 .txt 路径）
- `worklog/v1.0.0/sprint-22-202605/assets/finance-guardrail-cases.md` —— 新增

## 验证

- [ ] 在 ChatPanel 输入 8 个 case 中的问句，生成的 SQL 不再命中错链
- [ ] AGENT_WORKFLOW 路径下，prompt 末尾注入了 finance-constraints
- [ ] `ai_audit_log` 中过去 60 天的失败问句已 100% 在 `finance-guardrail-cases.md` 中找到对应条目或被其覆盖

## 完成标准

- [ ] 8-10 条长 few-shots 沉淀
- [ ] 9 条硬 constraints 沉淀
- [ ] 至少 8 条 guardrail case 文档化（含错误 SQL 与正确 SQL 对照）
- [ ] AGENT_WORKFLOW 路径下财务问句失败率（生成 SQL 执行报错或口径错位）较 sprint-22 起点下降 ≥ 50%
