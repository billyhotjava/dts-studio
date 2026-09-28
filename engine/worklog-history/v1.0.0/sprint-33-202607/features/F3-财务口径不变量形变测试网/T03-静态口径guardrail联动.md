# T03: 静态口径 guardrail 联动（生成 SQL 层拦截）

**优先级**: P0
**状态**: DONE
**依赖**: T01

## 目标

把 SQL 级不变量（无法纯靠结果断言的）落到生成 SQL 的静态拦截，使 copilot **生成阶段**就拦住已知错误模式——即便过滤条件从没见过，也不可能踩坑。

## 技术设计

- 在 copilot 生成 SQL 后、执行前做静态校验（扩展 sprint-30 F3 guardrail + Sprint-31 F2 生成的 guardrails）：
  - **#6 两链不混**：检测同一聚合/同一 SELECT 是否同时 SUM a_month_accounting 与 a_sale_account 列 → 拦。
  - **#8 双重计数**：检测 a_sale_account 与 a_month_accounting 合并时是否缺 source_type=8 去重 → 拦/告警。
  - **#7 坏账排除**：收入聚合是否未排除坏账 biz_type/链 → 拦/告警。
  - 三级金额列误用（结合 F1-T03）。
- 拦截即返回结构化错误 + 建议修正，并记入路由 telemetry（弱路径财务问题信号）。
- 与结果级回归（T02）互补：T03 防生成、T02 验结果。

## 当前落地（2026-06-05）

- `CaliberRuleRegistry` 已把 Sprint-31 的 `CAL-SETTLEMENT-CHAIN`、`CAL-SALE-IN-RENT` 等 SQL_STATIC 规则暴露为机器校验。
- `SqlSafetyChecker.validate(domain, sql)` 在基础 SQL 安全规则之后，对财务域/结算域追加口径规则校验，并返回 `SqlSafetyValidation.reasons()` 中的规则 ID 与原因。
- `Nl2SqlService.nl2sql(..., domain)` 在生成 SQL sanitize 后调用 domain-aware 校验，使财务混链 SQL 在返回前被拦截。
- `FinanceCaliberGuardrail` 在 `dts-copilot-analytics` 执行入口补齐同一套财务物理表口径 gate，覆盖 `prepare`、`executeRaw` / `executeWithCompliance` 的实际执行前检查，以及 auto-fix 后重试 SQL 的二次检查。
- 当前覆盖：链不混、`source_type=8` 双重计数风险、`a_sale_account` 收入聚合未排除 `t_flower_biz_info.biz_type=6` 坏账风险；生成层与 analytics 执行前 gate 均已接入。

## 影响范围

- `dts-copilot-ai` guardrail 校验逻辑（财务专项）+ `dts-copilot-analytics` 执行前 gate
- 关联 sprint-30 F3 / Sprint-31 F2 guardrails

## 验证

- [x] 构造混链 SQL → 被静态拦截（`SqlSafetyCheckerCaliberGuardrailTest`）
- [x] 构造 `source_type=8` 跨链汇总 SQL → 被静态拦截（`SqlSafetyCheckerCaliberGuardrailTest`）
- [x] 生成层 domain-aware NL2SQL 返回前拦截财务混链 SQL（`Nl2SqlServiceCaliberGuardrailTest`）
- [x] 构造含坏账收入误聚合 SQL → 被静态拦截（`CaliberRuleRegistryTest` / `SqlSafetyCheckerCaliberGuardrailTest`）
- [x] 正确财务 SQL 不被误拦（已覆盖 `biz_type <> 6` 与 `biz_type IN (7,8)` 正例）
- [x] analytics 执行前阻断坏账收入误聚合，且数据库执行未被调用（`QueryExecutionFacadeTest`）
- [x] analytics 既有测试不回归（`FinanceCaliberGuardrailTest` / `QueryExecutionFacadeTest`）

## 完成标准

- [x] SQL 级不变量在生成层与 analytics 执行前 gate 强制，反例必拦、正例必放
