# Sprint-33: 财务可证明正确性（对账保证）

**时间**: 2026-07
**前缀**: FA (Finance Assurance)
**状态**: IN_PROGRESS
**目标**: 让 copilot 的财务查询结果——**明细与应用系统一致、汇总可锚到复式凭证、自定义条件经口径不变量证明、并持续对账可审计**——使财务这一最敏感域的 NL2SQL 结果可被业务签字采信。

## 北极星目标（承接）

> 让 agent 在多业务场景下可靠地 NL2SQL 拿到口径正确的业务数据，且新场景接入可复制。

财务是口径最敏感、错一分都不行的域。本 sprint 把"口径只有一个真相"（Sprint-31）落到"**财务结果可证明正确**"，回答两个硬要求：
1. 应用系统里查的数，copilot 必须一致。
2. copilot 的汇总数据 / 各种自定义查询条件数据，如何**证明**正确。

## 背景

### 三个工程命题

| 要求 | 命题 | 证明手段 |
|------|------|----------|
| ① 一致性 | 明细级：同一业务单，copilot 行级 == 应用系统 | 对账 equality |
| ② 汇总正确 | copilot SUM == oracle SUM，且锚到复式凭证 | 双路计算 + 凭证 tie-out |
| ② 自定义条件正确 | 无穷过滤组合 | **口径不变量（任意条件成立）+ 差分抽样** |

②的"各种自定义条件"无法穷举证明，只能靠"对任意条件都成立的性质"（metamorphic invariants）来**证明**而非抽查——这是本 sprint 的灵魂。

### Oracle 三层（强度递增，必须对正确的基准对账）

```
L1 原始业务表镜像 (ods_ptr_mysql_a_month_accounting …)   只证 “mart=表聚合”，必要不充分
L2 应用系统报表端点输出 (MonthAccountController.getMonthSettlementData …)  证 “copilot=业务所见” ← 满足 req#1
L3 已过账凭证 (VoucherController, debit_amount=credit_amount 复式)  会计级真相，最强 oracle
```

### 现状缺口（诚实记录）

`worklog/v1.0.0/sprint-30-202606/it/sql/f4_finance_adminweb_reconciliation.sql` 对的是 **L1 原始表 vs ADS mart**——只证明"mart 没抄错表"。但应用系统的数是 Java 服务层算出来的（SpEL 开关、坏账排除、折扣、source_type=8 摊入），≠ 对表裸 SUM。要满足 req#1，对账基准须升到 **L2/L3**。本 sprint 即补这层。

## 设计依据

- **Oracle 端点（已核实存在）**：`MonthAccountController.listMonthAccountingPage` / `getMonthSettlementData`（月对账）、`SaleAccountController.listSaleAccountPage`（售/赠/坏链）、`VoucherController./list //export`（凭证）。
- **凭证复式结构（已核实）**：`debit_amount`/`credit_amount` + `debitSubjectId`/`creditSubjectId`，天然 借=贷。
- **口径铁律**：`reference_xycyl_caliber_traps`（两条结算链、三级金额、坏账、双重计数）→ Sprint-31 F1-T03 规则化产物，本 sprint 复用为不变量。
- 复用 Sprint-30 财务对账 SQL（升级基准）、Sprint-32 F1 路由 telemetry（弱路径财务问题入对账集）。

## Feature 列表

| ID | Feature | Task 数 | 优先级 | 状态 | 阶梯 | 解决 |
|----|---------|---------|--------|------|------|------|
| F1 | Oracle 注册与明细级一致性 | 3 | P0 | IN_PROGRESS | L0+L1 | req#1 |
| F2 | 复式凭证 tie-out 与汇总双路对账 | 3 | P0 | IN_PROGRESS | L2+L3 | req#2 汇总 |
| F3 | 财务口径不变量/形变测试网 | 3 | P0 | DONE | L3 | req#2 自定义条件 |
| F4 | 差分抽样与持续对账记分卡 | 3 | P1 | IN_PROGRESS | L4 | 持续可证明 |
| F5 | 可审计溯源与财务签字基线 | 2 | P1 | IN_PROGRESS | L4 | 业务可信 |

## 依赖顺序

```
Sprint-31(口径 SoT/不变量) ──> F1(明细对账)+F2(凭证 tie-out) ──> F3(不变量网) ──> F4(差分+记分卡) ──> F5(溯源+签字)
                                                              （F3 复用 Sprint-31 F4 回归网）
```

## 本 sprint 不做

- 不改 adminapi 财务业务逻辑（应用系统是 oracle，只读对账，不动它）。
- 不重写 Sprint-30 财务 mart（只把对账基准从 L1 升到 L2/L3）。
- 不一次性覆盖全部财务报表，先收**月对账（租摆链）+ 售账（售赠坏链）+ 凭证**三张核心表，跑通对账保证范式，其它报表复用。

## 完成标准

- [ ] 财务 oracle 注册表成文：每张核心报表↔权威端点/账本明确绑定
- [ ] 明细级对账 harness：真实业务单 copilot 结果与 L2 报表端点逐额（到分）相等，三级金额列各自对齐
- [ ] 汇总锚到复式凭证：核心收入/应收/回款聚合能 tie-out 到 `debit=credit` 凭证，差异为 0 或登记
- [x] 8 条财务口径不变量机器可检，对代表性过滤条件断言（接 Sprint-31 F4 回归网；live oracle/differential 归 F1/F4）
- [x] 两链不混 SUM / 坏账排除 / source_type=8 去重在生成 SQL 层与 analytics 执行前 gate 被静态 guardrail 拦截
- [ ] 差分抽样：代表性过滤网格 vs oracle 端点全绿；持续对账记分卡每日可跑、漂移告警
- [ ] 每个财务回答附可审计溯源（SQL+口径规则+lineage）；财务对一次基线签字
- [ ] `it/README.md` 真实可重跑证据（明细对账、凭证 tie-out、不变量回归、差分网格、记分卡）

## 相邻 sprint 关系

- 输入：Sprint-31 口径 SoT/不变量、Sprint-30 财务垂直切片与对账 SQL、Sprint-32 路由 telemetry。
- 输出：财务对账保证资产（oracle 注册 + 不变量网 + 记分卡）→ 复制到采购/库存等口径敏感域；财务签字基线 → 业务采信 copilot 的前提。
