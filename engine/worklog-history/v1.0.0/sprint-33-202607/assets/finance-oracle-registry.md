# Sprint-33 财务 Oracle 注册表

**状态**: IN_PROGRESS
**来源**: adminapi/adminweb 只读盘点

本注册表定义 copilot 财务对账的真相来源。应用系统仍是 oracle，本 sprint 不改 adminapi 财务逻辑。

| 报表域 | Oracle 层级 | 结算链 | 权威端点 | 源表/账本 | 前端证据 |
|--------|-------------|--------|----------|-----------|----------|
| 月对账 | L2 应用报表端点 | rent-settlement | `GET /rs-flowers-base/operate/monthAccount/listMonthAccountingPage`; `POST /rs-flowers-base/operate/monthAccount/getMonthSettlementData` | `a_month_accounting`, `a_green_accounting` | `adminweb/src/api/flower/operate/monthAccount.js` |
| 售账 | L2 应用报表端点 | sale-gift-bad-debt | `GET /rs-flowers-base/operate/saleAccount/listSaleAccountPage` | `a_sale_account` | `adminweb/src/api/flower/operate/saleAccount.js` |
| 凭证 | L3 复式账本 | voucher-ledger | `GET /rs-flowers-base/finace/voucher/list`; `POST /rs-flowers-base/finace/voucher/listByCodes`; `GET /rs-flowers-base/finace/voucher/listVoucherItems` | `f_voucher`, `f_voucher_item`; `debit_amount` / `credit_amount` | `adminweb/src/api/flower/finance/voucher.js` |

## 口径边界

- 月对账（租摆链）用应用服务层输出作为 L2，对账不能退回到 `a_month_accounting` 裸 SUM。
- 售账（售/赠/坏链）和月对账不可直接混合 SUM；销售摊入租摆时必须按 Sprint-31 `CAL-SALE-IN-RENT` 规则处理双重计数。
- 凭证是 L3 会计级 oracle，后续 tie-out 以 `debit_amount = credit_amount` 为硬约束。

## 可重跑资产

- 结构化注册表：`dts-copilot-ai/src/main/resources/governance/finance-oracle-registry.v1.json`
- 单测：`mvn -q -pl dts-copilot-ai -Dtest=FinanceOracleRegistryTest test`
