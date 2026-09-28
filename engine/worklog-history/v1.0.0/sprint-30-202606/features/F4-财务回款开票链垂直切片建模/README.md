# F4: 财务回款/开票链垂直切片建模

**优先级**: P1
**状态**: DONE

## 目标

以"财务回款/开票链"为空白域的第一个完整垂直切片,按 dbt 5 层范式建模:月对账应收/折后实收/已回款进度 + 开票进度 + 收款明细,落地 ads mart + 语义包对象/fewShots/guardrails,并与 adminweb 内建报表对账。这是本 sprint 的核心交付,也是后续库存/督导/薪资域复用的范式样板。

## Task 列表

| ID | Task | 优先级 | 状态 | 依赖 |
|----|------|--------|------|------|
| T01 | 月对账应收/折后/回款 dbt 模型 | P1 | DONE | F2/F3 |
| T02 | 开票进度 ads | P1 | DONE | T01 |
| T03 | 收款明细 ads | P1 | DONE | T01 |
| T04 | 财务语义包对象+fewShots+guardrails | P1 | DONE | T01,T02,T03 |
| T05 | 与 adminweb 内建报表对账 | P1 | DONE | T01,T02,T03 |

## 完成标准

- [x] 月对账三级金额、回款率、开票率 ads 落地,口径与 §3 一致
- [x] 财务语义对象可被 NL2SQL 命中,fewShots/guardrails 区分两链与含税
- [x] 与 `rentalMonthlyReport` / `list-report` 口径对账 SQL 成文,可在目标库重跑

## 证据

- `it/test_f4_finance_vertical_slice.sh`
- `it/evidence/20260601-local/f4-finance-vertical-slice-summary.md`
- `it/evidence/20260601-local/f4-dbt-compile-selected.txt`
- `it/sql/f4_finance_adminweb_reconciliation.sql`
