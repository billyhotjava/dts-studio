# T02: 远程数据 IT 脚本

**优先级**: P0
**状态**: READY
**依赖**: T01

## 目标

参考 `sprint-20/it/test_procurement_query_regression.sh` 的形态，编写 `test_finance_query_regression.sh`，让财务域 12 条回归记录在远程库可一键执行验证。

## 技术设计

### 1. 脚本骨架（参照采购）

```bash
#!/usr/bin/env bash
set -euo pipefail

DB_HOST="${FLOWER_DB_HOST:-db.weitaor.com}"
DB_PORT="${FLOWER_DB_PORT:-3306}"
DB_USER="${FLOWER_DB_USER:-flowerai}"
DB_NAME="${FLOWER_DB_NAME:-rs_cloud_flower}"
DB_PASSWORD="${FLOWER_DB_PASSWORD:-}"

if [[ -z "${DB_PASSWORD}" ]]; then
  echo "FLOWER_DB_PASSWORD is required" >&2
  exit 1
fi

run_sql() {
  local sql="$1"
  MYSQL_PWD="${DB_PASSWORD}" mysql \
    -h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}" -D "${DB_NAME}" \
    -N -B \
    -e "${sql}"
}

# === R1: 客户欠款排行前 10（最新快照） ===
echo "== R1: customer ar rank top 10 =="
r1_top1="$(run_sql "SELECT customer_name, ROUND(outstanding_amount,2) FROM mart.finance.customer_ar_rank_daily WHERE snapshot_date = (SELECT MAX(snapshot_date) FROM mart.finance.customer_ar_rank_daily) ORDER BY outstanding_amount DESC LIMIT 1;")"
echo "${r1_top1}"
# 断言示例（按真实数据填）
# [[ "${r1_top1}" == $'XX客户\t999999.99' ]] || { echo "R1 baseline mismatch" >&2; exit 1; }

# === R2: 本月各项目应收 ===
echo "== R2: monthly settlement by project (current month) =="
r2_count="$(run_sql "SELECT COUNT(DISTINCT project_name) FROM authority.finance.settlement_summary WHERE account_period = DATE_FORMAT(CURDATE(),'%Y-%m');")"
echo "rows=${r2_count}"
[[ "${r2_count}" -gt 0 ]] || { echo "R2: no rows for current month" >&2; exit 1; }

# === R3: 2025-03 未收款项目 ===
echo "== R3: 2025-03 incomplete collection =="
r3_total="$(run_sql "SELECT COUNT(*), ROUND(SUM(receivable_amount - received_amount),2) FROM authority.finance.project_collection_progress WHERE account_period='2025-03' AND collection_rate < 1.0;")"
echo "${r3_total}"
# [[ "${r3_total}" == $'NN\tMMMMM.MM' ]] || { echo "R3 mismatch" >&2; exit 1; }

# === R4: 万象城回款进度（如生产无该项目，跳过；记录预期空集 OK） ===
echo "== R4: wanxiangcheng collection progress =="
r4="$(run_sql "SELECT COUNT(*) FROM authority.finance.project_collection_progress WHERE project_name LIKE '%万象城%';")"
echo "rows=${r4}"

# === R5: 待付款 > 30 天 ===
echo "== R5: pending payment > 30 days =="
r5="$(run_sql "SELECT COUNT(*) FROM authority.finance.pending_payment_approval WHERE DATEDIFF(CURDATE(), submit_time) > 30;")"
echo "overdue_count=${r5}"

# === R6: 上月预支未核销 ===
echo "== R6: last month advance outstanding =="
r6="$(run_sql "SELECT COUNT(*), ROUND(SUM(outstanding_advance),2) FROM authority.finance.advance_request_status WHERE outstanding_advance > 0 AND apply_time >= DATE_SUB(DATE_FORMAT(CURDATE(),'%Y-%m-01'), INTERVAL 1 MONTH) AND apply_time < DATE_FORMAT(CURDATE(),'%Y-%m-01');")"
echo "${r6}"

# === R7: 2025 Q1 开票总额 ===
echo "== R7: 2025 Q1 invoice total =="
r7="$(run_sql "SELECT ROUND(SUM(invoice_amount),2) FROM authority.finance.invoice_reconciliation WHERE invoice_date >= '2025-01-01' AND invoice_date < '2025-04-01';")"
echo "q1_total=${r7}"

# === R8: 报销列表（用真实存在的申请人） ===
# echo "== R8: reimbursement by applicant =="
# 需要先 SELECT DISTINCT applicant_name FROM authority.finance.reimbursement_list LIMIT 5; 选一个

# === R9: 客户欠款明细 ===
# 类似 R8，需要一个真实 customer_name

# === R10: 应收概览 KPI ===
echo "== R10: receivable overview =="
r10="$(run_sql "SELECT total_receivable, total_outstanding FROM authority.finance.receivable_overview WHERE as_of_date = (SELECT MAX(as_of_date) FROM authority.finance.receivable_overview);")"
echo "${r10}"

# === Guardrail: 错链反例（不应在 LLM 生成的 SQL 中出现） ===
# 这部分由 ChatPanel 真人联调验证，不在 IT 脚本内执行

echo "Finance regression baseline passed."
```

### 2. 断言策略

- **数值断言**（强）：客户欠款 Top1 / Q1 开票总额这类有稳定数据样本的，写硬断言
- **行数断言**（中）：本月应收行数 > 0、待付超 30 天行数 ≥ 0 这类
- **结构断言**（弱）：仅打印不断言，给人工巡检
- **跨域不影响**：脚本只查财务视图，不依赖 procurement / project 域数据

### 3. 数据样本采集

T02 实施时先在远程库跑：

```sql
-- 拉一份 baseline，记录到 finance-query-regression.md
SELECT customer_name, ROUND(outstanding_amount,2) FROM mart.finance.customer_ar_rank_daily WHERE snapshot_date = (SELECT MAX(snapshot_date) FROM mart.finance.customer_ar_rank_daily) ORDER BY outstanding_amount DESC LIMIT 5;

SELECT account_period, COUNT(*), ROUND(SUM(receivable_amount-received_amount),2) FROM authority.finance.project_collection_progress WHERE collection_rate < 1.0 GROUP BY account_period ORDER BY account_period DESC LIMIT 6;

SELECT applicant_name, COUNT(*), ROUND(SUM(expense_amount),2) FROM authority.finance.reimbursement_list GROUP BY applicant_name ORDER BY 3 DESC LIMIT 10;

SELECT customer_name, ROUND(SUM(invoice_amount),2) FROM authority.finance.invoice_reconciliation WHERE invoice_date >= '2025-01-01' AND invoice_date < '2025-04-01' GROUP BY customer_name ORDER BY 2 DESC LIMIT 10;
```

把结果作为硬断言基线写进 `finance-query-regression.md`。

### 4. 数据漂移处理

财务数据每天都在变（mart 视图每日刷新），硬断言不能写死具体金额。策略：

- Top1 客户欠款金额：**只断言"≥ Top2"**（排序正确性）
- 月份记录数：**只断言 > 0**（数据不为空）
- Q1 开票：**只断言"> 0 且与上次跑差 < 5%"**（用上次 baseline 文件比对）

避免每天都因为数据变化失败。

### 5. CI 集成

- 在 `dts-copilot` 的 CI 中加入 finance 脚本
- 设置 GitHub Actions / Jenkins 每天 9:00 跑一次（财务数据日刷新后）
- 失败时通知 #data-quality 频道
- 配置：`FLOWER_DB_PASSWORD` 走 secret

## 影响范围

- `worklog/v1.0.0/sprint-22-202605/it/test_finance_query_regression.sh` —— 新增（chmod +x）
- `worklog/v1.0.0/sprint-22-202605/it/finance-query-regression.md` —— T01 输出，本 task 补充数据样本
- CI 配置（如适用）

## 验证

- [ ] `bash worklog/v1.0.0/sprint-22-202605/it/test_finance_query_regression.sh` 在远程库通过
- [ ] 至少 5 条断言（R1/R3/R5/R7/R10）有具体数值或范围检查
- [ ] 执行时间 < 30 秒
- [ ] 失败时输出明确的 `Rxx baseline mismatch` 错误，便于排查

## 完成标准

- [ ] IT 脚本可执行
- [ ] 数据样本采集完成并写入文档
- [ ] 12 条回归条目至少 10 条在脚本中有覆盖（R8/R9 因依赖具体人名/客户名作为示例可以软覆盖）
- [ ] 配置 CI 定时执行（如本 sprint 不做，至少有 README 说明手动执行步骤）
