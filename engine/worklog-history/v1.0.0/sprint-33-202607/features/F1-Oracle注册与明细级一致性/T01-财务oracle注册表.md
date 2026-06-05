# T01: 财务 oracle 注册表（报表↔权威端点/账本）

**优先级**: P0
**状态**: DONE
**依赖**: 无

## 目标

为每张财务报表显式登记"真相来源"，避免对错基准对账（如对原始表而非应用系统输出）。oracle 分三层，按强度选最高可得者。

## 技术设计

注册表 `assets/finance-oracle-registry.md`，每条登记：
- **报表/问题域**：月对账（租摆链）、售账（售/赠/坏链）、开票、收款、凭证。
- **L2 报表端点**（应用系统实际计算面）：
  - 月对账 → `MonthAccountController.getMonthSettlementData` / `listMonthAccountingPage`
  - 售账 → `SaleAccountController.listSaleAccountPage`
  - 开票/收款 → 对应 finance 端点（盘点补全）
- **L3 账本**：`VoucherController./list //listByCodes`（`debit_amount`/`credit_amount` 复式）。
- **结算链归属**：租摆链(bizType 1/2/3/4)→a_month_accounting；售赠坏链(6/7/8)→a_sale_account（口径铁律#2，绝不混）。
- **每条标注**：以哪层为准、过滤参数（项目/月份/日期）、已知口径差异。

## 影响范围

- 只读盘点 + 文档；产出 `assets/finance-oracle-registry.md`
- 关联 adminapi finance/operate controller（盘点端点入参/出参）

## 实施记录

- 2026-06-05：盘点 adminapi/adminweb 月对账、售账、凭证入口，确认月对账和售账为 L2 应用报表端点，凭证为 L3 复式账本。
- 2026-06-05：新增结构化注册表 `dts-copilot-ai/src/main/resources/governance/finance-oracle-registry.v1.json`。
- 2026-06-05：新增 `FinanceOracleRegistry` 和 `FinanceOracleRegistryTest`，固定三张核心 oracle 的端点、结算链、源表、金额列和 adminweb 证据路径。

## 验证

- [x] 三张核心报表 + 凭证均有 L2/L3 绑定且入参口径清晰
- [ ] 注册的端点实际可调通并返回与 adminweb 一致的数

## 完成标准

- [x] oracle 注册表成文，作为 T02/F2/F4 对账的唯一基准来源
