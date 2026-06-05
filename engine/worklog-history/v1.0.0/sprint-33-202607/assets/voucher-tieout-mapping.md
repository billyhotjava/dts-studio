# 凭证 Ledger Tie-out Mapping

**版本**: 2026-07-sprint33-voucher-ledger-tieout-v1
**来源**: Sprint-33 F2-T01
**性质**: 只读对账锚点；不写凭证、不改 adminapi 财务逻辑

## Oracle 入口

| 层级 | 入口 | 用途 |
|------|------|------|
| L3 | `GET /rs-flowers-base/finace/voucher/list` | 凭证主表查询，含 `bizCode`、`bizType`、`accountPriod` |
| L3 | `POST /rs-flowers-base/finace/voucher/listByCodes` | 按凭证号批量回查主表 |
| L3 | `GET /rs-flowers-base/finace/voucher/listVoucherItems` | 凭证明细查询，含 `voucherCode`、`subjectId`、借贷金额 |
| L3 | `GET /rs-flowers-base/finace/voucher/getcountItems` | 凭证明细按科目汇总 |

## 表与字段

| 表 | 字段 | 业务含义 |
|----|------|----------|
| `f_voucher` | `code` | 凭证号，连接明细 `voucher_code` |
| `f_voucher` | `biz_code` | 业务单号，如结算单、报销单、付款记录 |
| `f_voucher` | `biz_type` | 凭证来源类型 |
| `f_voucher` | `account_priod` | 财务账期 |
| `f_voucher_item` | `voucher_code` | 明细所属凭证号 |
| `f_voucher_item` | `account_priod` | 明细账期 |
| `f_voucher_item` | `subject_id` | 会计科目 |
| `f_voucher_item` | `debit_amount` | 借方金额 |
| `f_voucher_item` | `credit_amount` | 贷方金额 |
| `f_voucher_item` | `status` | 明细状态；现有 SQLProvider 过滤 `status > 0` |

## Join 与 Tie-out Key

主表与明细表连接：

```sql
f_voucher.code = f_voucher_item.voucher_code
and f_voucher.account_priod = f_voucher_item.account_priod
```

对账 key：

```text
biz_code + account_priod + voucher_code + subject_id
```

聚合口径：

```sql
sum(debit_amount) as debit_amount,
sum(credit_amount) as credit_amount
```

凭证内自检：

```text
for each biz_code + account_priod + voucher_code:
  sum(debit_amount) == sum(credit_amount)
```

## 前端证据

- `adminweb/src/api/flower/finance/voucher.js`
- `adminweb/src/views/flower/finance/voucher/list-voucher.vue`
- `adminweb/src/views/flower/finance/voucher/detail-index.vue`
- `adminweb/src/views/flower/finance/voucher/summary.vue`

## 机器化资产

- `dts-copilot-ai/src/main/resources/governance/voucher-ledger-tieout-mapping.v1.json`
- `VoucherLedgerTieoutRegistryTest`
- `VoucherLedgerTieoutService`

## 边界

- F2-T01 只完成凭证账本映射与本地可重复复式平衡校验。
- F2-T03 继续完成收入、应收、回款到具体凭证科目的业务 tie-out。
- 若 live 环境接入，必须沿用该映射并保留 `debit=credit` 差异登记。
