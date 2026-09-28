# Sprint-33 财务权威基准注册表

**状态**: IN_PROGRESS
**来源**: adminapi/adminweb 只读盘点

本注册表定义 copilot 财务对账的真相来源。这里的“权威基准”是业务答案源/对账基准，不是 Oracle 数据库；应用数据库仍是 MySQL。本 sprint 不改 adminapi 财务逻辑，只把应用系统和应用 MySQL 原表作为只读对账基准。

| 报表域 | 权威层级 | 结算链 | 权威端点 | 源表/账本 | 前端证据 |
|--------|-------------|--------|----------|-----------|----------|
| 月对账 | L2 应用报表端点 | rent-settlement | `GET /rs-flowers-base/operate/monthAccount/listMonthAccountingPage`; `POST /rs-flowers-base/operate/monthAccount/getMonthSettlementData` | `a_month_accounting`, `a_green_accounting` | `adminweb/src/api/flower/operate/monthAccount.js` |
| 售账 | L2 应用报表端点 | sale-gift-bad-debt | `GET /rs-flowers-base/operate/saleAccount/listSaleAccountPage` | `a_sale_account` | `adminweb/src/api/flower/operate/saleAccount.js` |
| 凭证 | L3 复式账本 | voucher-ledger | `GET /rs-flowers-base/finace/voucher/list`; `POST /rs-flowers-base/finace/voucher/listByCodes`; `GET /rs-flowers-base/finace/voucher/listVoucherItems` | `f_voucher`, `f_voucher_item`; `debit_amount` / `credit_amount` | `adminweb/src/api/flower/finance/voucher.js` |

## 口径边界

- 月对账（租摆链）用应用服务层输出作为 L2，对账不能退回到 `a_month_accounting` 裸 SUM。
- 售账（售/赠/坏链）和月对账不可直接混合 SUM；销售摊入租摆时必须按 Sprint-31 `CAL-SALE-IN-RENT` 规则处理双重计数。
- 凭证是 L3 会计级权威基准，后续 tie-out 以 `debit_amount = credit_amount` 为硬约束。

## 可重跑资产

- 结构化注册表：`dts-copilot-ai/src/main/resources/governance/finance-authority-registry.v1.json`
- 单测：`mvn -q -pl dts-copilot-ai -Dtest=FinanceAuthorityRegistryTest test`
- 运行日志与下游 registry 失败信息使用 `authority binding` 文案；历史 `oracle` 命名仅作为兼容类名、兼容字段或历史 evidence 保留。
